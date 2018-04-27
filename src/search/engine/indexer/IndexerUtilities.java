package search.engine.indexer;

import com.mongodb.client.MongoIterable;
import org.bson.Document;
import org.bson.types.ObjectId;
import search.engine.utils.Constants;
import search.engine.utils.Utilities;

import java.util.*;


public class IndexerUtilities {

    /**
     * Constructs an array filter document to be used in MongoDB aggregation pipeline during
     * projection stage.
     * The constructed document will filter the given array to pass only the array elements
     * having {@code arrayItem} equals any of the given filter words.
     *
     * @param arrayName   the name of the array to filter during projection
     * @param arrayItem   the name of the array item to compare with the given filter
     * @param filterWords the filter words
     * @return the constructed array filter document
     */
    public static Document AggregationFilter(String arrayName, String arrayItem, List<String> filterWords) {
        // Filter condition
        Document filterCond = new Document()
                .append("$in", Arrays.asList("$$this." + arrayItem, filterWords));

        // Project aggregation needed filter fields
        Document filterFields = new Document()
                .append("input", "$" + arrayName)
                .append("cond", filterCond);

        return new Document(arrayName, new Document("$filter", filterFields));
    }

    /**
     * Converts the results of find query into a list of web pages.
     *
     * @param documents list of documents to be converted
     * @return list of web pages
     */
    public static List<WebPage> toWebPages(MongoIterable<Document> documents) {
        List<WebPage> ret = new ArrayList<>();

        for (Document doc : documents) {
            ret.add(new WebPage(doc));
        }

        return ret;
    }

    /**
     * Parses the given MongoDB documents and fills the stems index of
     * every existing web page document.
     *
     * @param documents the raw MongoDB documents
     * @param map       a map of web pages to fill and/or update
     */
    public static void fillStemsIndex(MongoIterable<Document> documents, Map<ObjectId, WebPage> map) {
        for (Document doc : documents) {
            ObjectId id = doc.getObjectId(Constants.FIELD_TERM_DOC_ID);
            map.putIfAbsent(id, new WebPage());
            WebPage page = map.get(id);

            String stem = doc.getString(Constants.FIELD_TERM);
            int count = doc.getInteger(Constants.FIELD_TERM_COUNT);
            int score = doc.getInteger(Constants.FIELD_TERM_SCORE);

            page.stemMap.put(stem, new StemInfo(count, score));
        }
    }

    /**
     * Parses the given MongoDB documents and fills the words index of
     * every existing web page document.
     *
     * @param documents the raw MongoDB documents
     * @param map       a map of web pages to fill and/or update
     */
    public static void fillWordsIndex(MongoIterable<Document> documents, Map<ObjectId, WebPage> map) {
        for (Document doc : documents) {
            ObjectId id = doc.getObjectId(Constants.FIELD_TERM_DOC_ID);
            map.putIfAbsent(id, new WebPage());
            WebPage page = map.get(id);

            String word = doc.getString(Constants.FIELD_TERM);
            List<Integer> positions = (List<Integer>) doc.get(Constants.FIELD_TERM_POSITIONS);

            page.wordPosMap.put(word, positions);
        }
    }

    /**
     * Parses the given MongoDB documents and fills extra web page information of
     * every existing web page.
     *
     * @param documents the raw MongoDB documents
     * @param map       a map of web pages to fill and/or update
     * @return a list of filled web pages
     */
    public static List<WebPage> fillPageInfo(MongoIterable<Document> documents, Map<ObjectId, WebPage> map) {
        List<WebPage> ret = new ArrayList<>();

        for (Document doc : documents) {
            ObjectId id = doc.getObjectId(Constants.FIELD_ID);
            WebPage page = map.get(id);

            page.id = id;
            page.wordsCount = doc.getInteger(Constants.FIELD_TOTAL_WORDS_COUNT);
            page.rank = doc.getDouble(Constants.FIELD_RANK);

            ret.add(page);
        }

        return ret;
    }

    /**
     * Constructs and returns the words dictionary of the given words set.
     * <p>
     * Words dictionary is a map from a stemmed word to
     * all the words present in the given set having the same stem.
     *
     * @param words the words set to get their dictionary
     * @return the words dictionary
     */
    public static Map<String, List<String>> getWordsDictionary(Set<String> words) {
        Map<String, List<String>> ret = new TreeMap<>();

        for (String word : words) {
            // Stem word
            String stem = Utilities.stemWord(word);

            // Map[stem].insert(word)
            ret.putIfAbsent(stem, new ArrayList<>());
            ret.get(stem).add(word);
        }

        return ret;
    }

    /**
     * Checks if all of the filter words occurred in order
     * in the given web page words position map.
     *
     * @param wordPosMap  the words position map of the web page to search ing
     * @param filterWords list of words to find their occurrence in order
     * @return {@code true} if the filter words are occurred in order, {@code false} otherwise
     */
    public static boolean checkPhraseOccurred(Map<String, List<Integer>> wordPosMap, List<String> filterWords) {
        if (filterWords.size() < 2) {
            return true;
        }

        int minFilterWordIdx = 0;
        List<Integer> minFilterWordPosList = wordPosMap.get(filterWords.get(0));

        // Choose the filter word that occurred the least amount of times in the web page
        for (int i = 0; i < filterWords.size(); ++i) {
            List<Integer> wordPositions = wordPosMap.get(filterWords.get(i));

            if (minFilterWordPosList.size() > wordPositions.size()) {
                minFilterWordPosList = wordPositions;
                minFilterWordIdx = i;
            }
        }

        // Check that the filter words occur in the correct position
        for (Integer pos : minFilterWordPosList) {
            if (check(wordPosMap, filterWords, pos - minFilterWordIdx)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if all of the filter words occurred in order
     * in the given web page words position map starting from the given position.
     *
     * @param wordPosMap  the words position map of the web page to search ing
     * @param filterWords list of words to find their occurrence in order
     * @param position    the starting position to search from
     * @return {@code true} if the filter words are occurred in order, {@code false} otherwise
     */
    private static boolean check(Map<String, List<Integer>> wordPosMap, List<String> filterWords, int position) {
        for (int i = 0; i < filterWords.size(); ++i) {
            if (Collections.binarySearch(wordPosMap.get(filterWords.get(i)), position + i) < 0) {
                // Did not find the word in the required position
                return false;
            }
        }

        return true;
    }
}
