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
     * Web page title.
     */
    public String title = null;

    /**
     * Web page document content.
     * Used when displaying the results to the users.
     */
    public String content = null;

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
     * Web page words count after removing stop words.
     * Used in normalizing terms frequencies.
     */
    public int wordsCount = 0;

    /**
     * Web page dictionary index holding all the occurrence positions
     * for every distinct word in the web page.
     */
    public Map<String, List<Integer>> wordPosMap = null;
    public Map<String, Integer> wordScoreMap = null;
    public Map<String, Integer> stemWordsCount = null;

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
        title = (String) doc.getOrDefault(Constants.FIELD_TITLE, null);
        content = (String) doc.getOrDefault(Constants.FIELD_PAGE_CONTENT, null);

        rank = (double) doc.getOrDefault(Constants.FIELD_RANK, 1.0);
        wordsCount = (int) doc.getOrDefault(Constants.FIELD_WORDS_COUNT, 0);
        fetchSkipLimit = (int) doc.getOrDefault(Constants.FILED_FETCH_SKIP_LIMIT, 1);
        fetchSkipCount = (int) doc.getOrDefault(Constants.FILED_FETCH_SKIP_COUNT, 0);

        outLinks = (List<String>) doc.getOrDefault(Constants.FIELD_CONNECTED_TO, null);
        parseWordsIndex((List<Document>) doc.getOrDefault(Constants.FIELD_WORDS_INDEX, null));
        parseStemsIndex((List<Document>) doc.getOrDefault(Constants.FIELD_STEMS_INDEX, null));
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
        doc.append(Constants.FIELD_TITLE, title);
        doc.append(Constants.FIELD_PAGE_CONTENT, content);

        doc.append(Constants.FIELD_RANK, rank);
        doc.append(Constants.FIELD_CONNECTED_TO, outLinks);
        doc.append(Constants.FIELD_WORDS_COUNT, wordsCount);
        doc.append(Constants.FIELD_WORDS_INDEX, getWordsIndex());
        doc.append(Constants.FIELD_STEMS_INDEX, getStemsIndex());

        doc.append(Constants.FILED_FETCH_SKIP_LIMIT, fetchSkipLimit);
        doc.append(Constants.FILED_FETCH_SKIP_COUNT, fetchSkipCount);

        return doc;
    }

    /**
     * Returns the words index of this web page.
     *
     * @return list of documents representing the words index of this web page
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
                    .append(Constants.FIELD_POSITIONS, wordPosMap.get(word));

            dictionary.add(doc);
        }

        return dictionary;
    }

    /**
     * Parse the given words index document
     * and fill {@code wordPosMap} and {@code wordScoreMap}.
     *
     * @param wordsIndex list of documents representing the dictionary of this web page
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
        }
    }

    /**
     * Returns the stem words index of this web page.
     * (i.e. just a map from the stem word to its occurrence count in the web page)
     *
     * @return list of documents representing the stem index of this web page
     */
    private List<Document> getStemsIndex() {
        List<Document> ret = new ArrayList<>();

        // TODO: convert the two maps into one map of pair of integers
        for (Map.Entry<String, Integer> entry : stemWordsCount.entrySet()) {
            Document doc = new Document()
                    .append(Constants.FIELD_STEM_WORD, entry.getKey())
                    .append(Constants.FIELD_STEM_COUNT, entry.getValue())
                    .append(Constants.FIELD_STEM_SCORE, wordScoreMap.get(entry.getKey()));

            ret.add(doc);
        }

        return ret;
    }

    /**
     * Parse the given stem words count document and fill {@code stemWordsCount}.
     *
     * @param stemsIndex list of documents representing the stems index of this web page
     */
    private void parseStemsIndex(List<Document> stemsIndex) {
        if (stemsIndex == null) {
            return;
        }

        stemWordsCount = new HashMap<>();

        for (Document doc : stemsIndex) {
            String stem = doc.getString(Constants.FIELD_STEM_WORD);

            stemWordsCount.put(
                    stem,
                    doc.getInteger(Constants.FIELD_STEM_COUNT)
            );

            wordScoreMap.put(
                    stem,
                    doc.getInteger(Constants.FIELD_STEM_SCORE)
            );
        }
    }
}
