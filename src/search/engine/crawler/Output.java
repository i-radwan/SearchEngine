package search.engine.crawler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;


public class Output {

    //
    // Static variables
    //
    private static PrintWriter sLogFile;
    private static PrintWriter sURLFile;
    private static PrintWriter sVisitedURLsFile;
    private static PrintWriter sAllowedURLsFile;
    private static PrintWriter sDisallowedURLsFile;
    private static PrintWriter sURLIdsFile;
    private static PrintWriter sURLRulesFile;


    /**
     * Initializes the output files.
     */
    public static void init() {
        openFiles();
    }

    /**
     * Outputs the given string to the log file.
     *
     * @param str the string to be logged
     */
    public static void log(String str) {
        synchronized (sLogFile) {
            sLogFile.println("Crawler " + Thread.currentThread().getName() + " => " + str);
        }
    }

    /**
     * Outputs the given URL to the URLs file.
     *
     * @param url the URL to be logged
     */
    public static void logURL(String url) {
        synchronized (sURLFile) {
            sURLFile.println(url);
            sURLFile.flush();
        }
    }

    /**
     * Outputs the given URL to the visited URLs file.
     *
     * @param url the URL to be logged
     */
    public static void logVisitedURL(String url) {
        synchronized (sVisitedURLsFile) {
            sVisitedURLsFile.println(url);
            sVisitedURLsFile.flush();
        }
    }

    /**
     * Outputs the given URL to the allowed URLs file.
     *
     * @param url the URL to be logged
     */
    public static void logAllowedURL(String url) {
        synchronized (sAllowedURLsFile) {
            sAllowedURLsFile.println(url);
            sAllowedURLsFile.flush();
        }
    }

    /**
     * Outputs the given URL to the disallowed URLs file.
     *
     * @param url the URL to be logged
     */
    public static void logDisallowedURL(String url) {
        synchronized (sDisallowedURLsFile) {
            sDisallowedURLsFile.println(url);
            sDisallowedURLsFile.flush();
        }
    }

    /**
     * Outputs the given URL ID pair to the URL IDs file.
     *
     * @param id  the id of the URL to be logged
     * @param url the URL to be logged
     */
    public static void logURLId(Integer id, String url) {
        synchronized (sURLIdsFile) {
            sURLIdsFile.println(id);
            sURLIdsFile.println(url);
            sURLIdsFile.flush();
        }
    }

    /**
     * Outputs the given robot rules to the URL rules file.
     *
     * @param url   the URL related to the given rules
     * @param rules list of rules to be logged
     */
    public static void logURLRules(String url, List<String> rules) {
        synchronized (sURLRulesFile) {
            sURLRulesFile.println(Constants.INIT_URL_RULE_FILE + url);
            for (String rule : rules) {
                sURLRulesFile.println(rule);
            }
            sURLRulesFile.flush();
        }
    }

    /**
     * Deletes all the output log files and re-opens them.
     * TODO: find a better way for clearing the files.
     */
    public static synchronized void clearFiles() {
        // Close the files first
        closeFiles();

        //
        // Delete output files
        //
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

        // Re-open the log files
        openFiles();
    }

    /**
     * Opens all the output log files (called after deletion).
     */
    private static synchronized void openFiles() {
        try {
            sLogFile = new PrintWriter(new FileWriter(Constants.LOG_FILE_NAME, true));
            sURLFile = new PrintWriter(new FileWriter(Constants.URLS_FILE_NAME, true));
            sVisitedURLsFile = new PrintWriter(new FileWriter(Constants.VISITED_URLS_FILE_NAME, true));
            sDisallowedURLsFile = new PrintWriter(new FileWriter(Constants.DISALLOWED_URLS_FILE_NAME, true));
            sAllowedURLsFile = new PrintWriter(new FileWriter(Constants.ALLOWED_URLS_FILE_NAME, true));
            sURLIdsFile = new PrintWriter(new FileWriter(Constants.URL_IDS_FILE_NAME, true));
            sURLRulesFile = new PrintWriter(new FileWriter(Constants.URL_RULES_FILE_NAME, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the opened log files.
     * To be called after log being finished.
     */
    public static synchronized void closeFiles() {
        sLogFile.close();
        sURLFile.close();
        sVisitedURLsFile.close();
        sDisallowedURLsFile.close();
        sAllowedURLsFile.close();
        sURLIdsFile.close();
        sURLRulesFile.close();
    }
}
