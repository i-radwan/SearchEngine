package search.engine.crawler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Output {
	private static PrintWriter mLogFile, mURLFile, mVisitedURLsFile, mDisallowedURLsFile, mAllowedURLsFile, mURLIdsFile, mURLRulesFile;

	public static void init() {
		openFiles();
	}

	/**
	 * output to the Log Files
	 * @param s
	 */
	public static void log(String s, String name) {
		synchronized (mLogFile) {
			mLogFile.println("CrawlerThread " + name + " => " + s);
		}
	}

	/**
	 * output to the crawled_urls file
	 * @param url
	 */
	public static void logURL(String url) {
		synchronized (mURLFile) {
			mURLFile.println(url);
			mURLFile.flush();
		}
	}

	/**
	 * output to the visited urls file
	 * @param url
	 */
	public static synchronized void logVisitedURL(String url) {
		mVisitedURLsFile.println(url);
		mVisitedURLsFile.flush();
	}

	/**
	 * output to the allowed urls file
	 * @param url
	 */
	public static synchronized void logAllowedURL(String url) {
		mAllowedURLsFile.println(url);
		mAllowedURLsFile.flush();
	}

	/**
	 * output to the disallowed urls file
	 * @param url
	 */
	public static synchronized void logDisallowedURL(String url) {
		mDisallowedURLsFile.println(url);
		mDisallowedURLsFile.flush();
	}

	/**
	 * output to the url ids file
	 * @param id
	 * @param url
	 */
	public static synchronized void logURLId(Integer id, String url) {
		mURLIdsFile.println(id);
		mURLIdsFile.println(url);
		mURLIdsFile.flush();
	}

	/**
	 * output to the url rules file
	 * @param url
	 * @param rules
	 */
	public static synchronized void logURLRule(String url, ArrayList<String> rules) {
		mURLRulesFile.println(Constants.INIT_URL_RULE_FILE + url);
		for (String rule : rules) {
			mURLRulesFile.println(rule);
		}
		mURLRulesFile.flush();
	}

	/**
	 * deletes all of the contents of the files
	 */
	public static synchronized void clearFiles() {
		closeFiles();
		File file = new File(Constants.LOG_FILE_NAME);
		file.delete();
		file = new File(Constants.URLS_FILE_NAME);
		file.delete();
		file = new File(Constants.VISITED_URLS_FILE_NAME);
		file.delete();
		file = new File(Constants.DISALLOWED_URLS_FILE_NAME);
		file.delete();
		file = new File(Constants.ALLOWED_URLS_FILE_NAME);
		file.delete();
		file = new File(Constants.URL_IDS_FILE_NAME);
		file.delete();
		file = new File(Constants.URL_RULES_FILE_NAME);
		file.delete();
		openFiles();
	}

	/**
	 * re-creates the files from the beginning (called after deletion)
	 */
	private static void openFiles() {
		try {
			mLogFile = new PrintWriter(new FileWriter(Constants.LOG_FILE_NAME, true));
			mURLFile = new PrintWriter(new FileWriter(Constants.URLS_FILE_NAME, true));
			mVisitedURLsFile = new PrintWriter(new FileWriter(Constants.VISITED_URLS_FILE_NAME, true));
			mDisallowedURLsFile = new PrintWriter(new FileWriter(Constants.DISALLOWED_URLS_FILE_NAME, true));
			mAllowedURLsFile = new PrintWriter(new FileWriter(Constants.ALLOWED_URLS_FILE_NAME, true));
			mURLIdsFile = new PrintWriter(new FileWriter(Constants.URL_IDS_FILE_NAME, true));
			mURLRulesFile = new PrintWriter(new FileWriter(Constants.URL_RULES_FILE_NAME, true));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Closes the opened files after finish
	 */
	public static void closeFiles() {
		mLogFile.close();
		mURLFile.close();
		mVisitedURLsFile.close();
		mDisallowedURLsFile.close();
		mAllowedURLsFile.close();
		mURLIdsFile.close();
		mURLRulesFile.close();
	}
}
