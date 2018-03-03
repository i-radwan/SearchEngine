package search.engine.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constants {

    public static final String[] STOP_WORDS = {
            "I", "a", "about", "an", "are", "as", "at", "be", "by", "com", "for",
            "from", "how", "in", "is", "it", "of", "on", "or", "that", "the",
            "this", "to", "was", "what", "when", "where", "who", "will",
            "with", "the", "www"
    };

    public static final Set<String> STOP_WORDS_SET = new HashSet<>(Arrays.asList(STOP_WORDS));
}
