package search.engine.utils;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.ArrayList;
import java.util.Arrays;

public class Utilities {
    /**
     * Process string as follows:
     * Convert to lower
     * Replace special chars with spaces
     * Remove completely-numeric words
     *
     * @param s input string
     * @return the processed string
     */
    public static String processString(String s) {
        // Convert to lower case
        s = s.toLowerCase();

        // Replace special chars with space
        s = s.replaceAll("[^\\dA-Za-z ]", " ");

        // Remove all completely-numeric words
        s = s.replaceAll("\\b(\\d+)\\b", " ");

        return s;
    }

    /**
     * Process user query
     * @param queryString
     * @return
     */
    public static ArrayList<ArrayList<String>> processQuery(String queryString) {
        // Check if phrase search before processing the string (removing special chars)
        boolean isPhraseSeach = checkIfPhraseSearch(queryString);

        ArrayList<ArrayList<String>> ret = new ArrayList<>();

        // Split the processed query into words
        ArrayList<String> wordsArrayList = splitStringToWords(processString(queryString));

        // Remove stop words
        wordsArrayList = removeStopWords(wordsArrayList);

        // Add original query words without stemming
        ret.add(wordsArrayList);

        // Stem words
        ArrayList<String> stemmedWordsArrayList = stemWords(wordsArrayList);
        ret.add(stemmedWordsArrayList);

        // ToDo: Spread and  get all possible words

        return ret;
    }

    /**
     * Split string into words
     *
     * @param s
     * @return
     */
    public static ArrayList<String> splitStringToWords(String s) {
        return new ArrayList<>(Arrays.asList(s.split(" ")));
    }

    /**
     * Remove stop words
     *
     * @param words
     * @return
     */
    public static ArrayList<String> removeStopWords(ArrayList<String> words) {
        ArrayList<String> ret = new ArrayList<>();

        for (String word : words) {
            if (word.isEmpty()) continue;
            if (isStopWord(word)) continue;
            ret.add(word);
        }

        return ret;
    }

    /**
     * Stem the given words
     *
     * @param words
     * @return
     */
    public static ArrayList<String> stemWords(ArrayList<String> words) {
        ArrayList<String> ret = new ArrayList<>();
        SnowballStemmer stemmer = new englishStemmer();

        for (String word : words) {
            stemmer.setCurrent(word);
            stemmer.stem();

            ret.add(stemmer.getCurrent());
        }

        return ret;
    }

    /**
     * Check if a given word is stop word
     *
     * @param word
     * @return
     */
    public static boolean isStopWord(String word) {
        return word.length() < 2 || Constants.stopWordSet.contains(word);
    }

    /**
     * Check if phrase search
     *
     * @param queryString
     * @return
     */
    public static boolean checkIfPhraseSearch(String queryString) {
        return queryString.charAt(0) == queryString.charAt(queryString.length() - 1)
                && queryString.charAt(0) == '"';
    }
}
