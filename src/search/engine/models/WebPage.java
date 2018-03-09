package search.engine.models;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import search.engine.utils.Constants;
import search.engine.utils.WebUtilities;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WebPage implements Comparable {

    //
    // Member variables
    //

    /**
     * Web page id in the database.
     */
    public ObjectId id = null;

    /**
     * Web page url.
     */
    public String url = null;

    /**
     * Web page rank.
     * <p>
     * To be calculated as follows:
     * <p>
     * <i>PR(u) = Σ PR(v) / OutDegree(v)</i>
     * <p>
     * where:
     * <ul>
     * <li><i>u</i>: the current web page.</li>
     * <li><i>v</i>: the web pages connected to u.</li>
     * </ul>
     */
    public double rank = 1.0;

    /**
     * List of urls mentioned in the current page.
     */
    public List<String> outLinks = null;

    /**
     * Web page document content.
     * Used when displaying the results to the users.
     */
    public String content = null;

    /**
     * Web page words count after removing stop words.
     * Used in normalizing terms frequencies.
     */
    public int wordsCount = 0;

    /**
     * Web page dictionary index holding all the occurrence positions
     * for every distinct word in the web page.
     */
    public Map<String, List<Integer>> wordPosMap = null;
    public Map<String, List<Integer>> wordScoreMap = null;


    //
    // Member methods
    //

    /**
     * Default constructor.
     */
    public WebPage() {

    }

    /**
     * Constructs a web page object from JSON-like document.
     *
     * @param doc JSON-like document representing the web page
     */
    public WebPage(Document doc) {
        id = (ObjectId) doc.getOrDefault(Constants.FIELD_ID, null);
        url = (String) doc.getOrDefault(Constants.FIELD_URL, null);
        rank = (double) doc.getOrDefault(Constants.FIELD_RANK, 1.0);
        content = (String) doc.getOrDefault(Constants.FIELD_PAGE_CONTENT, null);
        wordsCount = (int) doc.getOrDefault(Constants.FIELD_WORDS_COUNT, 0);

        // TODO: find a better way to cast
        outLinks = (List<String>) doc.getOrDefault(Constants.FIELD_CONNECTED_TO, null);
        parseWordsIndex((List<Document>) doc.getOrDefault(Constants.FIELD_WORDS_INDEX, null));
    }

    /**
     * Constructs a web page object from the raw HTML content of the page.
     *
     * @param doc web page raw content
     */
    public WebPage(org.jsoup.nodes.Document doc) {
        // Get web page url
        this.url = doc.baseUri();

        // Extract the list of out links.
        extractOutLinks(doc);

        // Extract page content
        //extractPageContent(doc);
    }

    /**
     * Constructs a web page object from the raw HTML content of the page.
     * TODO: parse the given web page with @AbdoEid
     *
     * @param url     Web page url.
     * @param content Web page raw content.
     */
    public WebPage(String url, String content) {
        this.url = url;

        // TODO: extract the list of out links and parse them.
        // TODO: parse urls to be all lower case and in the remove the prefix "http:/www." and similar.
        outLinks = new ArrayList<>();
        outLinks.add("codeforces.com");
        outLinks.add("csacademy.com");
        outLinks.add("hackerrank.com");

        // TODO: extract the web page body and remove HTML tags
        this.content = content;


        wordPosMap = new HashMap<>();
        wordScoreMap = new HashMap<>();

        // TODO: loop through each tag to create words index and give a score for each occurrence
        // TODO: use the parser of @IAR
        String[] words = content.split(" ");

        for (int i = 0; i < words.length; ++i) {
            String word = words[i];

            wordPosMap.putIfAbsent(word, new ArrayList<>());
            wordPosMap.get(word).add(i);

            wordScoreMap.putIfAbsent(word, new ArrayList<>());
            wordScoreMap.get(word).add(1);
        }

        wordsCount += words.length;
    }

    /**
     * Extracts all out links from the given raw web page document
     * and adds them to {@code outLinks} list.
     *
     * @param doc web page raw content
     */
    private void extractOutLinks(org.jsoup.nodes.Document doc) {
        outLinks = new ArrayList<>();

        Elements links = doc.select("link[href], a[href]");

        for (Element element : links) {
            String link = element.attr("abs:href");

            if (WebUtilities.validURL(link)) {
                outLinks.add(link);
            }
        }

        //System.out.println("#out_links: " + outLinks.size());
        //for (String link : outLinks) {
        //    System.out.println(link);
        //}
    }

    /**
     * Extracts the web page content from the given raw web page document.
     * TODO: extract visual text and add score
     *
     * @param doc web page raw content
     */
    private void extractPageContent(org.jsoup.nodes.Document doc) {
        Element body = doc.body();

        PrintWriter file;

        try {
            content = body.getElementsByTag("p").text();

            file = new PrintWriter(new FileWriter("tmp.txt"));
            file.println(content);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a JSON-like document representing this web page object.
     *
     * @return web page document
     */
    public Document toDocument() {
        Document doc = new Document();

        if (id != null) {
            doc.append(Constants.FIELD_ID, id);
        }

        doc.append(Constants.FIELD_URL, url);
        doc.append(Constants.FIELD_RANK, rank);
        doc.append(Constants.FIELD_CONNECTED_TO, outLinks);
        doc.append(Constants.FIELD_PAGE_CONTENT, content);
        doc.append(Constants.FIELD_WORDS_COUNT, wordsCount);
        doc.append(Constants.FIELD_WORDS_INDEX, getWordsIndex());

        return doc;
    }

    /**
     * Returns the words index of this web page.
     *
     * @return list of documents representing the words index this given web page
     */
    private List<Document> getWordsIndex() {
        if (wordPosMap == null) {
            return null;
        }

        // List of word documents
        ArrayList<Document> dictionary = new ArrayList<>();

        for (String word : wordPosMap.keySet()) {
            Document doc = new Document()
                    .append(Constants.FIELD_WORD, word)
                    .append(Constants.FIELD_POSITIONS, wordPosMap.get(word))
                    .append(Constants.FIELD_SCORES, wordScoreMap.get(word));

            dictionary.add(doc);
        }

        return dictionary;
    }

    /**
     * Parse the given wordsIndex.
     *
     * @param wordsIndex list of documents representing the dictionary this given web page
     */
    private void parseWordsIndex(List<Document> wordsIndex) {
        if (wordsIndex == null) {
            return;
        }

        wordPosMap = new HashMap<>();
        wordScoreMap = new HashMap<>();

        for (Document doc : wordsIndex) {
            String word = doc.getString(Constants.FIELD_WORD);
            wordPosMap.put(word, (List<Integer>) doc.get(Constants.FIELD_POSITIONS));
            wordScoreMap.put(word, (List<Integer>) doc.get(Constants.FIELD_SCORES));
        }
    }

    /**
     * Returns a hash code representing the web page.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return url.hashCode();
    }

    /**
     * Compares this web page to the specified object.
     * The result is {@code true} if and only if the argument is not {@code null} and
     * has the same url.
     *
     * @param obj The object to compare this web page against
     * @return {@code true} if the given object has the same url, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WebPage) {
            obj = ((WebPage) obj).url;
        }

        return url.equals(obj);
    }

    /**
     * Compares two web pages lexicographically according to their urls.
     *
     * @param obj The object to compare this web page against
     * @return the value {@code 0} if both urls are equal;
     * a value less than {@code 0} if this web page url
     * is lexicographically less than the argument; and a
     * value greater than {@code 0} if this web page url is
     * lexicographically greater than the string argument
     */
    @Override
    public int compareTo(Object obj) {
        if (obj instanceof WebPage) {
            obj = ((WebPage) obj).url;
        }

        return url.compareTo((String) obj);
    }
}
