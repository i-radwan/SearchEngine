package search.engine.utils;

import java.util.*;


public class Constants {

    //
    // Global constants
    //

    public static final String DATA_PATH = "data";

    // ================================================================================================
    //
    // Crawler
    //

    /**
     * Crawler directories constants
     */
    public static final String CRAWLER_DATA_PATH = DATA_PATH + "/crawler";
    public static final String SEED_FILE_NAME = CRAWLER_DATA_PATH + "/seed.txt";
    public static final String LOG_FILE_NAME = CRAWLER_DATA_PATH + "/log.txt";
    public static final String URLS_FILE_NAME = CRAWLER_DATA_PATH + "/urls_to_crawl.txt";
    public static final String VISITED_URLS_FILE_NAME = CRAWLER_DATA_PATH + "/visited_urls.txt";

    /**
     * Robots text constants
     */
    public static final String DEFAULT_USER_AGENT = "*";

    /**
     * Limits constants
     */
    public static final int MAX_POLL_WAIT_TIME_MS = 10000;
    public static final int MAX_BASE_URL_COUNT = 10;
    public static final int MAX_WEB_PAGES_COUNT = 100;
    public static final int MAX_FETCH_SKIP_LIMIT = 8;

    // ================================================================================================
    //
    // Indexer
    //

    /**
     * Database constants
     */
    public static final String DATABASE_NAME = "search_engine";
    public static final String DATABASE_HOST_ADDRESS = "localhost";
    public static final int DATABASE_PORT_NUMBER = 27017;

    /**
     * Collection constants
     */
    public static final String COLLECTION_WEB_PAGES = "web_pages";
    public static final String COLLECTION_DICTIONARY = "dictionary";
    public static final String COLLECTION_SUGGESTIONS = "suggestions";

    /**
     * Fields constants
     */
    public static final String FIELD_ID = "_id";
    public static final String FIELD_URL = "url";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_PAGE_CONTENT = "content";
    public static final String FIELD_RANK = "rank";
    public static final String FIELD_CONNECTED_TO = "connected_to";
    public static final String FIELD_WORDS_COUNT = "words_count";
    public static final String FIELD_WORDS_INDEX = "words_index";
    public static final String FIELD_WORD = "word";
    public static final String FIELD_POSITIONS = "positions";
    public static final String FIELD_SCORES = "scores";
    public static final String FIELD_STEMS_INDEX = "stems_index";
    public static final String FIELD_STEM_WORD = "stem";
    public static final String FIELD_STEM_COUNT = "count";
    public static final String FILED_SYNONYMS = "synonyms";
    public static final String FILED_FETCH_SKIP_LIMIT = "fetch_skip_limit";
    public static final String FILED_FETCH_SKIP_COUNT = "fetch_skip_count";
    public static final String FIELD_SUGGESTION = "suggestion";

    /**
     * Common fields lists
     */
    public static final List<String> FIELDS_FOR_RANKING = Arrays.asList(
            FIELD_ID,
            FIELD_URL,
            FIELD_RANK,
            FIELD_WORDS_COUNT,
            FIELD_WORDS_INDEX,
            FIELD_STEMS_INDEX
    );

    public static final List<String> FIELDS_FOR_SEARCH_RESULTS = Arrays.asList(
            FIELD_ID,
            FIELD_URL,
            FIELD_TITLE,
            FIELD_PAGE_CONTENT
    );

    // ================================================================================================
    //
    // Web Page Parse
    //

    /**
     * Allowed tags to be traversed during web page content extraction.
     */
    public static final String[] ALLOWED_TAGS = {
            "body", "div", "p", "main", "article", "pre",
            "h1", "h2", "h3", "h4", "h5", "h6",
            "b", "i", "em", "blockquote", "strong",
            "a", "span", "ol", "ul", "li"
    };
    public static final Set<String> ALLOWED_TAGS_SET = new HashSet<>(Arrays.asList(ALLOWED_TAGS));

    /**
     * Map from an HTML tag to an integer score representing the importance of this tag.
     */
    public static final Map<String, Integer> TAG_TO_SCORE_MAP = new HashMap<String, Integer>() {
        {
            put("title", 30);
            put("h1", 15);
            put("h2", 10);
            put("i", 10);

            put("h3", 8);
            put("em", 5);
            put("b", 5);
            put("strong", 5);

            put("h4", 4);
            put("h5", 3);
            put("h6", 3);
        }
    };

    // ================================================================================================
    //
    // Ranker
    //

    /**
     * Page Ranker directories constants
     */
    public static final String RANKER_DATA_PATH = DATA_PATH + "/ranker";
    public static final String GRAPH_FILE_NAME = RANKER_DATA_PATH + "/graph.txt";
    public static final String PAGE_RANKS_FILE_NAME = RANKER_DATA_PATH + "/ranks.txt";

    public static final String CUDA_SRC_DIRECTORY = "Modules/CUDAPageRank/";
    public static final String CUDA_SCRIPT_PATH = CUDA_SRC_DIRECTORY + "run_cuda.sh";
    public static final String CUDA_PAGE_RANKS_FILE_NAME = CUDA_SRC_DIRECTORY + "build/cuda_page_ranks.txt";

    /**
     * Pagination constants
     */
    public static final int SINGLE_PAGE_RESULTS_COUNT = 12;

    // ================================================================================================
    //
    // Server
    //

    public static int SEARCH_ENGINE_PORT_NUMBER = 8080;

    // ================================================================================================
    //
    // Query processing
    //

    public static final String[] STOP_WORDS = {
            "i", "a", "about", "an", "are", "as", "at", "be", "by", "com", "for",
            "from", "how", "in", "is", "it", "of", "on", "or", "that", "the",
            "this", "to", "was", "what", "when", "where", "who", "will",
            "with", "the", "www", "can", "and"
    };

    public static final Set<String> STOP_WORDS_SET = new HashSet<>(Arrays.asList(STOP_WORDS));
}
