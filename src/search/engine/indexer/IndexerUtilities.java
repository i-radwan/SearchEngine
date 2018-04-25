package search.engine.indexer;

import com.mongodb.client.MongoIterable;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class IndexerUtilities {

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
