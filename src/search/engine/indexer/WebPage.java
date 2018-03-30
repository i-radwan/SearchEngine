package search.engine.indexer;

import org.bson.Document;
import org.bson.types.ObjectId;
import search.engine.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

    /**
     * Variables used to adjust the frequency of fetching the web page content.
     */
    public int fetchSkipLimit = 1;
    public int fetchSkipCount = 0;


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
        if (doc == null) {
            return;
        }

        id = (ObjectId) doc.getOrDefault(Constants.FIELD_ID, null);
        url = (String) doc.getOrDefault(Constants.FIELD_URL, null);
        rank = (double) doc.getOrDefault(Constants.FIELD_RANK, 1.0);
        content = (String) doc.getOrDefault(Constants.FIELD_PAGE_CONTENT, null);
        wordsCount = (int) doc.getOrDefault(Constants.FIELD_WORDS_COUNT, 0);
        fetchSkipLimit = (int) doc.getOrDefault(Constants.FILED_FETCH_SKIP_LIMIT, 1);
        fetchSkipCount = (int) doc.getOrDefault(Constants.FILED_FETCH_SKIP_COUNT, 0);
        outLinks = (List<String>) doc.getOrDefault(Constants.FIELD_CONNECTED_TO, null);
        parseWordsIndex((List<Document>) doc.getOrDefault(Constants.FIELD_WORDS_INDEX, null));
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
        doc.append(Constants.FILED_FETCH_SKIP_LIMIT, fetchSkipLimit);
        doc.append(Constants.FILED_FETCH_SKIP_COUNT, fetchSkipCount);

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
        List<Document> dictionary = new ArrayList<>();

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
}
