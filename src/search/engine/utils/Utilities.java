package search.engine.utils;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.ArrayList;
import java.util.List;


public class Utilities {

    /**
     * Processes the given string as follows:
     * <ul>
     * <li>Converts the string into lowercase.</li>
     * <li>Replaces special chars with spaces.</li>
     * <li>Removes completely-numeric words.</li>
     * </ul>
     *
     * @param str the input string to be processed
     * @return the processed string.
     */
    public static String processString(String str) {
        // Convert to lower case
        str = str.toLowerCase();

        // Replace special chars with space
        str = str.replaceAll("[^\\dA-Za-z ]", " ");

        // Remove all completely-numeric words
        str = str.replaceAll("\\b(\\d+)\\b", " ");

        // Replace multiple consecutive spaces by only one white space
        str = str.replaceAll("\\s+", " ");

        return str.trim();
    }

    /**
     * Processes user search query.
     *
     * @param queryString the input search query string
     * @return
     */
    public static List<List<String>> processQuery(String queryString) {
        // Trim the query string
        queryString = queryString.trim();

        // Check if phrase search before processing the string (before removing special chars)
        boolean isPhraseSeach = isPhraseSearch(queryString);

        List<List<String>> ret = new ArrayList<>();

        // Split the processed query into words
        String processedQuery = processString(queryString);
        String[] words = processedQuery.split(" ");

        // Remove stop words
        List<String> wordsArrayList = removeStopWords(words);

        // Add original query words without stemming
        ret.add(wordsArrayList);

        // Stem words
        List<String> stemmedWordsArrayList = stemWords(wordsArrayList);
        ret.add(stemmedWordsArrayList);

        // ToDo: Spread and  get all possible words

        return ret;
    }

    /**
     * Removes the stop words from the given list of words.
     *
     * @param words list of words
     * @return a new list of non-stopping words.
     */
    public static List<String> removeStopWords(String[] words) {
        List<String> ret = new ArrayList<>();

        for (String word : words) {
            if (stopWord(word)) {
                continue;
            }

            ret.add(word);
        }

        return ret;
    }

    /**
     * Converts the given list of words into their stem version.
     *
     * @param words list of words
     * @return a new list of stemmed words.
     */
    public static List<String> stemWords(List<String> words) {
        List<String> ret = new ArrayList<>();
        SnowballStemmer stemmer = new englishStemmer();

        for (String word : words) {
            stemmer.setCurrent(word);
            stemmer.stem();
            ret.add(stemmer.getCurrent());
        }

        return ret;
    }

    /**
     * Checks whether the given word is stop word or not.
     *
     * @param word a string
     * @return {@code true} if the given word is a stop word, {@code false} otherwise
     */
    public static boolean stopWord(String word) {
        return word.length() <= 2 || Constants.STOP_WORDS_SET.contains(word);
    }

    /**
     * Checks whether the given search query is a phrase search or not.
     *
     * @param queryString the input search query string
     * @return {@code true} if the given string is surrounded by double quotes, {@code false} otherwise
     */
    public static boolean isPhraseSearch(String queryString) {

        return queryString.charAt(0) == '"'
                && queryString.charAt(queryString.length() - 1) == '"';
    }
}
