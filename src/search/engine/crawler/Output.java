package search.engine.crawler;

import search.engine.utils.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


public class Output {

    //
    // Static variables
    //
    private static PrintWriter sLogFile;
    private static PrintWriter sURLFile;
    private static PrintWriter sVisitedURLsFile;


    /**
     * Outputs the given string to the log file.
     *
     * @param str the string to be logged
     */
    public static void log(String str) {
        synchronized (sLogFile) {
            sLogFile.println(Thread.currentThread().getName() + "\t\t=> " + str);
            sLogFile.flush();
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
     * Deletes all the output log files and re-opens them.
     */
    public static void clearFiles() {
        File file = new File(Constants.URLS_FILE_NAME);
        file.delete();
        file = new File(Constants.VISITED_URLS_FILE_NAME);
        file.delete();
    }

    /**
     * Opens all the output log files (called after deletion).
     */
    public static void openFiles() {
        try {
            sLogFile = new PrintWriter(new FileWriter(Constants.LOG_FILE_NAME));
            sURLFile = new PrintWriter(new FileWriter(Constants.URLS_FILE_NAME, true));
            sVisitedURLsFile = new PrintWriter(new FileWriter(Constants.VISITED_URLS_FILE_NAME, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the opened log files.
     * To be called after log being finished.
     */
    public static void closeFiles() {
        sLogFile.close();
        sURLFile.close();
        sVisitedURLsFile.close();
    }
}
