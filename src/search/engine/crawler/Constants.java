package search.engine.crawler;


public class Constants {

    //
    // File directories constants
    //
    public static String SEED_FILE_NAME = "data/crawler/seed.txt";
    public static String LOG_FILE_NAME = "data/crawler/log.txt";
    public static String URLS_FILE_NAME = "data/crawler/urls_to_crawl.txt";
    public static String VISITED_URLS_FILE_NAME = "data/crawler/visited_urls.txt";
    public static String ALLOWED_URLS_FILE_NAME = "data/crawler/allowed_urls.txt";
    public static String DISALLOWED_URLS_FILE_NAME = "data/crawler/disallowed_urls.txt";
    public static String URL_IDS_FILE_NAME = "data/crawler/url_ids.txt";
    public static String URL_RULES_FILE_NAME = "data/crawler/url_rules.txt";

    //
    // Robots text constants
    //
    public static String INIT_URL_RULE_FILE = "URL_RULE: ";
    public static String USER_AGENT = "*";

    //
    // Limits constants
    //
    public static int MAX_POLL_WAIT_TIME_MS = 10000;
    public static int MAX_BASE_URL_COUNT = 10;
    public static int MAX_WEB_PAGES_COUNT = 10;
}
