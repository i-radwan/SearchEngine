package search.engine.crawler;


public class Constants {
	
	public static String LOG_FILE_NAME = "crawler_data/log.txt";
	public static String SEED_FILE_NAME = "crawler_data/seed.txt";
	public static String URLS_FILE_NAME = "crawler_data/to_crawl_urls.txt";
	public static String VISITED_URLS_FILE_NAME = "crawler_data/visited_urls_file.txt";
	public static String DISALLOWED_URLS_FILE_NAME = "crawler_data/disallowed_urls_file.txt";
	public static String ALLOWED_URLS_FILE_NAME = "crawler_data/allowed_urls_file.txt";
	public static String URL_IDS_FILE_NAME = "crawler_data/url_ids_file.txt";
	public static String URL_RULES_FILE_NAME = "crawler_data/url_rules_file.txt";
	public static String INIT_URL_RULE_FILE = "URL_RULE: ";
	
	public static int MAX_POLL_WAIT_TIME = 10000;
	public static int MAX_BASE_URL_CNT = 10;
	public static int MAX_WEBPAGES_CNT = 5000;
}
