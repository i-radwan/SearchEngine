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
     *
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
    public Map<String, List<Integer>> wordPosMap = new HashMap<>();
    public Map<String, StemInfo> stemMap = new HashMap<>();

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
        wordsCount = (int) doc.getOrDefault(Constants.FIELD_TOTAL_WORDS_COUNT, 0);

        rank = (double) doc.getOrDefault(Constants.FIELD_RANK, 1.0);
        outLinks = (List<String>) doc.getOrDefault(Constants.FIELD_CONNECTED_TO, null);

        // parseWordsIndex((List<Document>) doc.getOrDefault(Constants.FIELD_WORDS_INDEX, null));
        // parseStemsIndex((List<Document>) doc.getOrDefault(Constants.FIELD_STEMS_INDEX, null));

        fetchSkipLimit = (int) doc.getOrDefault(Constants.FILED_FETCH_SKIP_LIMIT, 1);
        fetchSkipCount = (int) doc.getOrDefault(Constants.FILED_FETCH_SKIP_COUNT, 0);
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
        doc.append(Constants.FIELD_TOTAL_WORDS_COUNT, wordsCount);

        doc.append(Constants.FIELD_RANK, rank);
        doc.append(Constants.FIELD_CONNECTED_TO, outLinks);

        // doc.append(Constants.FIELD_WORDS_INDEX, getWordsIndex());
        // doc.append(Constants.FIELD_STEMS_INDEX, getStemsIndex());

        doc.append(Constants.FILED_FETCH_SKIP_LIMIT, fetchSkipLimit);
        doc.append(Constants.FILED_FETCH_SKIP_COUNT, fetchSkipCount);

        return doc;
    }

    /**
     * Returns the words index of this web page.
     *
     * @return list of documents representing the words index of this web page
     */
    public List<Document> getWordsIndex() {
        if (wordPosMap == null) {
            return null;
        }

        // List of word documents
        List<Document> dictionary = new ArrayList<>();

        for (Map.Entry<String, List<Integer>> entry : wordPosMap.entrySet()) {
            Document doc = new Document()
                    .append(Constants.FIELD_TERM_DOC_ID, this.id)
                    .append(Constants.FIELD_TERM, entry.getKey())
                    .append(Constants.FIELD_TERM_POSITIONS, entry.getValue());

            dictionary.add(doc);
        }

        return dictionary;
    }

    /**
     * Parse the given words index document and fill {@code wordPosMap}.
     *
     * @param wordsIndex list of documents representing the dictionary of this web page
     */
    public void parseWordsIndex(List<Document> wordsIndex) {
        if (wordsIndex == null) {
            return;
        }

        for (Document doc : wordsIndex) {
            String word = doc.getString(Constants.FIELD_TERM);
            wordPosMap.put(word, (List<Integer>) doc.get(Constants.FIELD_TERM_POSITIONS));
        }
    }

    /**
     * Returns the stem words index of this web page.
     * (i.e. just a map from the stem word to its occurrence count in the web page along with a score
     * related to its occurrence)
     *
     * @return list of documents representing the stem index of this web page
     */
    public List<Document> getStemsIndex() {
        List<Document> ret = new ArrayList<>();

        for (Map.Entry<String, StemInfo> entry : stemMap.entrySet()) {
            Document doc = new Document()
                    .append(Constants.FIELD_TERM_DOC_ID, this.id)
                    .append(Constants.FIELD_TERM, entry.getKey())
                    .append(Constants.FIELD_TERM_COUNT, entry.getValue().count)
                    .append(Constants.FIELD_TERM_SCORE, entry.getValue().score);

            ret.add(doc);
        }

        return ret;
    }

    /**
     * Parse the given stem words count document
     * and fill {@code stemWordsCount} and {@code stemScoreMap}.
     *
     * @param stemsIndex list of documents representing the stems index of this web page
     */
    public void parseStemsIndex(List<Document> stemsIndex) {
        if (stemsIndex == null) {
            return;
        }

        for (Document doc : stemsIndex) {
            stemMap.put(
                    doc.getString(Constants.FIELD_TERM),
                    new StemInfo(
                            doc.getInteger(Constants.FIELD_TERM_COUNT),
                            doc.getInteger(Constants.FIELD_TERM_SCORE)
                    )
            );
        }
    }
}
