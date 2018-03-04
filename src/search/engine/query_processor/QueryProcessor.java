package search.engine.query_processor;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import search.engine.utils.Constants;

import java.util.ArrayList;

public class QueryProcessor {

    private boolean isPhraseSearch = false;

    public ArrayList<ArrayList<String>> process(String queryString) {
        StringBuilder queryStringBuilder = new StringBuilder(queryString);
        ArrayList<ArrayList<String>> ret = new ArrayList<>();

        this.isPhraseSearch = this.checkIfPhraseSearch(queryString);

        // Remove special chars
        queryStringBuilder = this.removeSpecialChars(queryStringBuilder);

        // Split query into words
        ArrayList<String> wordsArrayList = this.splitStringToWords(queryStringBuilder.toString());

        // Remove stop words
        wordsArrayList = this.removeStopWords(wordsArrayList);
        ret.add(wordsArrayList);

        // Stem words
        ArrayList<String> stemmedWordsArrayList = this.stemWords(wordsArrayList);

        // Spread and  get all possible words

        ret.add(stemmedWordsArrayList);
        return ret;
    }

    public boolean isPhraseSearch() {
        return this.isPhraseSearch;
    }

    private boolean checkIfPhraseSearch(String queryString) {
        return queryString.charAt(0) == queryString.charAt(queryString.length() - 1)
                && queryString.charAt(0) == '"';
    }

    private StringBuilder removeSpecialChars(StringBuilder sb) {
        StringBuilder ret = new StringBuilder("");

        for (int i = 0; i < sb.length(); i++) {
            if ((sb.charAt(i) >= 'a' && sb.charAt(i) <= 'z')
                    || (sb.charAt(i) >= 'A' && sb.charAt(i) <= 'Z')
                    || (sb.charAt(i) >= '1' && sb.charAt(i) <= '9'))
                ret.append(sb.charAt(i));
        }

        return ret;
    }

    private ArrayList<String> splitStringToWords(String s) {
        ArrayList<String> wordsArrayList = new ArrayList<>();
        for (String word : s.split(" ")) {
            wordsArrayList.add(word);
        }

        return wordsArrayList;
    }

    private ArrayList<String> removeStopWords(ArrayList<String> words) {
        ArrayList<String> ret = new ArrayList<>();

        for (String word : words) {
            if (word.isEmpty()) continue;
            if (this.isStopWord(word)) continue;
            ret.add(word);
        }

        return ret;
    }

    private ArrayList<String> stemWords(ArrayList<String> words) {
        ArrayList<String> ret = new ArrayList<>();
        SnowballStemmer stemmer = new englishStemmer();

        for (String word : words) {
            stemmer.setCurrent(word);
            stemmer.stem();

            ret.add(stemmer.getCurrent());
        }

        return ret;
    }

    private boolean isStopWord(String word) {
        if (word.length() < 2) return true;
        if (word.charAt(0) >= '0' && word.charAt(0) <= '9') return true;
        if (Constants.STOP_WORDS_SET.contains(word)) return true;
        else return false;
    }
}
