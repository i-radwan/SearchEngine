package search.engine.utils;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.*;


public class Utilities {

    private static SnowballStemmer stemmer = new englishStemmer();

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
     * Removes the stop words from the given list of words.
     *
     * @param words list of words
     * @return a new list of non-stopping words.
     */
    public static List<String> removeStopWords(List<String> words) {
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
     * Converts the given list of words into their stemmed version.
     *
     * @param words list of words
     * @return a new list of stemmed words.
     */
    public static List<String> stemWords(List<String> words) {
        List<String> ret = new ArrayList<>();

        for (String word : words) {
            ret.add(stemWord(word));
        }

        return ret;
    }

    /**
     * Converts the given word into its stemmed version.
     *
     * @param word string to be stemmed
     * @return a new string of stemmed word.
     */
    public static String stemWord(String word) {
        String lastWord = word;

        while (true) {
            stemmer.setCurrent(word);
            stemmer.stem();
            word = stemmer.getCurrent();

            if (word.equals(lastWord))
                break;

            lastWord = word;
        }

        return word;
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
            stemmer.setCurrent(word);
            stemmer.stem();
            String stem = stemmer.getCurrent();

            // Map[stem].insert(word)
            ret.putIfAbsent(stem, new ArrayList<>());
            ret.get(stem).add(word);
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
     * Returns the number of occurrences of the given character in the string.
     *
     * @param str the string to search in
     * @param c   the character to search for
     * @return the number of occurrences
     */
    public static int countCharOccurrence(String str, char c) {
        int res = 0;

        for (int i = 0; i < str.length(); ++i) {
            if (str.charAt(i) == c) {
                res++;
            }
        }

        return res;
    }

    /**
     * Returns the given word after removing surrounding special chars
     *
     * @param word the word to be cleaned
     * @return the clean word
     */
    public static String removeSpecialCharsAroundWord(String word) {
        int firstLetterIdx = 0, lastLetterIdx = word.length() - 1;

        // Prefix chars
        for (int i = 0; i < word.length(); ++i) {
            if (Character.isLetterOrDigit(word.charAt(i))) {
                firstLetterIdx = i;
                break;
            }
        }

        // Postfix chars
        for (int i = word.length() - 1; i >= 0; --i) {
            if (Character.isLetterOrDigit(word.charAt(i))) {
                lastLetterIdx = i;
                break;
            }
        }

        return word.substring(firstLetterIdx, lastLetterIdx + 1);
    }
}
