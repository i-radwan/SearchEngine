package search.engine.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constants {
    public final static String[] stopWords = {"I", "a", "about", "an", "are", "as", "at", "be", "by", "com", "for", "from", "how", "in", "is", "it", "of", "on", "or", "that", "the", "this", "to", "was", "what", "when", "where", "who", "will", "with", "the", "www"};
    public static Set<String> stopWordSet = new HashSet<>(Arrays.asList(stopWords));

}
