package search.engine.indexer;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class WebPage {

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
     * PR(u) = Σ PR(v) / OutDegree(v)
     * <p>
     * where:
     * u: the current web page.
     * <p>
     * v: the web pages connected to u.
     */
    public double rank = 1.0;

    /**
     * List of urls mentioned in the current page.
     */
    public List<String> outUrls = null;

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
     * @param doc JSON-like document representing the web page.
     */
    public WebPage(Document doc) {
        id = doc.getObjectId(Constants.FIELD_ID);
        url = doc.getString(Constants.FIELD_URL);
        rank = doc.getDouble(Constants.FIELD_RANK);
        content = doc.getString(Constants.FIELD_PAGE_CONTENT);
        wordsCount = doc.getInteger(Constants.FIELD_WORDS_COUNT);

        // TODO: find a better way to cast
        outUrls = (List<String>) doc.get(Constants.FIELD_CONNECTED_TO);
        parseDictionary((List<Document>) doc.get(Constants.FIELD_DICTIONARY));
    }

    /**
     * Constructs a web page object from the raw HTML content of the page.
     *
     * @param url     Web page url.
     * @param content Web page raw content.
     */
    public WebPage(String url, String content) {
        // TODO: parse the given web page with @AbdoEid
    }

    /**
     * Returns a JSON-like document representing this web page object.
     *
     * @return Web page document.
     */
    public Document toDocument() {
        Document doc = new Document();

        if (id != null) {
            doc.append(Constants.FIELD_ID, id);
        }

        doc.append(Constants.FIELD_URL, url);
        doc.append(Constants.FIELD_RANK, rank);
        doc.append(Constants.FIELD_CONNECTED_TO, outUrls);
        doc.append(Constants.FIELD_PAGE_CONTENT, content);
        doc.append(Constants.FIELD_WORDS_COUNT, wordsCount);
        doc.append(Constants.FIELD_DICTIONARY, getDictionary());

        return doc;
    }

    /**
     * Returns the dictionary index of this web page.
     *
     * @return List of documents representing the dictionary this given web page.
     */
    private List<Document> getDictionary() {
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
     * Parse the given dictionary.
     *
     * @param dictionary List of documents representing the dictionary this given web page.
     */
    private void parseDictionary(List<Document> dictionary) {
        if (dictionary == null) {
            return;
        }

        wordPosMap = new TreeMap<>();
        wordScoreMap = new TreeMap<>();

        for (Document doc : dictionary) {
            String word = doc.getString(Constants.FIELD_WORD);
            wordPosMap.put(word, (List<Integer>) doc.get(Constants.FIELD_POSITIONS));
            wordScoreMap.put(word, (List<Integer>) doc.get(Constants.FIELD_SCORES));
        }
    }
}