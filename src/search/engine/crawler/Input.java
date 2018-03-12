package search.engine.crawler;

import search.engine.utils.Constants;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;


public class Input {
    // TODO: change Scanner to BufferedReader as it is much more faster

    /**
     * Reads the URL seeds and fills the URLs queue and the visited URLs set.
     */
    public static void readSeed() {
        try {
            Scanner file = new Scanner(new FileReader(Constants.SEED_FILE_NAME));

            while (file.hasNextLine()) {
                String url = file.nextLine();

                if (!CrawlerThread.sVisitedURLs.contains(url)) {
                    CrawlerThread.sURLsQueue.add(url);
                    CrawlerThread.sVisitedURLs.add(url);
                }
            }

            file.close();
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Reads the data of the previous runs in case of interruption.
     * Used to continue from the same state as before.
     */
    public static void readPreviousRunData() {
        try {
            CrawlerThread.sVisitedURLs.addAll(readVisitedURLs());
            CrawlerThread.sURLsQueue.addAll(readURLs());
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Reads the URLs that has been visited in the previous run.
     *
     * @return set of visited URLs.
     */
    private static Set<String> readVisitedURLs() throws FileNotFoundException {
        Set<String> ret = new HashSet<>();

        Scanner file = new Scanner(new FileReader(Constants.VISITED_URLS_FILE_NAME));

        while (file.hasNextLine()) {
            ret.add(file.nextLine());
        }

        file.close();

        return ret;
    }

    /**
     * Reads the URLs to be crawled.
     *
     * @return a list of URLs to be crawled
     */
    private static List<String> readURLs() throws FileNotFoundException {
        List<String> ret = new ArrayList<>();

        Scanner file = new Scanner(new FileReader(Constants.URLS_FILE_NAME));

        while (file.hasNextLine()) {
            String url = file.nextLine();

            if (!CrawlerThread.sVisitedURLs.contains(url)) {
                ret.add(url);
                CrawlerThread.sVisitedURLs.add(url);
            }
        }

        file.close();

        return ret;
    }
}
