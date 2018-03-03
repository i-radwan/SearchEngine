package search.engine.indexer;

import java.util.Arrays;
import java.util.List;

public class Constants {

    /**
     * Database constants
     */
    public static final String DATABASE_NAME = "spider_engine";
    public static final String DATABASE_HOST_ADDRESS = "localhost";
    public static final int DATABASE_PORT_NUMBER = 27017;

    /**
     * Collection constants
     */
    public static final String COLLECTION_WEB_PAGES = "web_pages";

    /**
     * Fields constants
     */
    public static final String FIELD_ID = "_id";
    public static final String FIELD_URL = "url";
    public static final String FIELD_RANK = "rank";
    public static final String FIELD_CONNECTED_TO = "connected_to";
    public static final String FIELD_PAGE_CONTENT = "content";
    public static final String FIELD_WORDS_COUNT = "words_count";
    public static final String FIELD_DICTIONARY = "dictionary";
    public static final String FIELD_WORD = "word";
    public static final String FIELD_POSITIONS = "positions";
    public static final String FIELD_SCORES = "scores";

    /**
     * Common fields lists
     */
    public static final List<String> FIELDS_FOR_RANKING = Arrays.asList(
            FIELD_ID,
            FIELD_URL,
            FIELD_RANK,
            FIELD_WORDS_COUNT,
            FIELD_DICTIONARY
    );
}
