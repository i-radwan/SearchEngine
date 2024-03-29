package search.engine.utils;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.*;

import static java.lang.Math.min;


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
     * We may need more than one iteration to get to the base stem.
     * (i.e. computerized -> computer -> comput)
     *
     * @param word string to be stemmed
     * @return a new string of stemmed word.
     */
    public static String stemWord(String word) {
        SnowballStemmer stemmer = new englishStemmer();

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
            String stem = stemWord(word);

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
     * Returns the given word after removing surrounding special chars.
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

    /**
     * Calculate how far the two strings match.
     * Find minimum number of edits (operations) required to convert ‘str1’ into ‘str2’
     * @param str1 String first string
     * @param str2 String
     * @return
     */
    public static int editDist(String str1, String str2) {
        // Create a table to store results of sub problems
        int m = str1.length();
        int n = str2.length();
        int dp[][] = new int[m + 1][n + 1];

        // Fill d[][] in bottom up manner
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                // If first string is empty, only option is to
                // insert all characters of second string
                if (i == 0)
                    dp[i][j] = j;  // Min. operations = j

                    // If second string is empty, only option is to
                    // remove all characters of second string
                else if (j == 0)
                    dp[i][j] = i; // Min. operations = i

                    // If last characters are same, ignore last char
                    // and recur for remaining string
                else if (str1.charAt(i - 1) == str2.charAt(j - 1))
                    dp[i][j] = dp[i - 1][j - 1];

                    // If last character are different, consider all
                    // possibilities and find minimum
                else {
                    dp[i][j] = 1 + min(dp[i][j - 1],  // Insert
                            dp[i - 1][j],  // Remove
                            dp[i - 1][j - 1]); // Replace
                }
            }
        }

        return dp[m][n];
    }

    static int min(int x, int y, int z) {
        if (x <= y && x <= z) return x;
        if (y <= x && y <= z) return y;
        else return z;
    }
}
