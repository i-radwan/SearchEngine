package search.engine.crawler;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.*;


public class CrawlerThread extends java.lang.Thread {

    //
    // Static variables
    //
    public static int sWebPagesCnt = 0;
    public static BlockingQueue<String> sURLsQueue = new LinkedBlockingDeque<>();
    public static ConcurrentSkipListSet<String> sVisitedURLs = new ConcurrentSkipListSet<>();
    public static ConcurrentHashMap<String, Integer> sBaseURLVisitedCnt = new ConcurrentHashMap<>();

    //
    // Member variables
    //
    private RobotsTextParser mRobotTxtParser;


    /**
     * Constructs a new crawler thread.
     *
     * @param robotManager robot manger object to handle robots text parsing
     */
    CrawlerThread(RobotsTextManager robotManager) {
        mRobotTxtParser = new RobotsTextParser(robotManager);
    }

    /**
     * Gets the front URL of the queue and returns it.
     * Throws an exception if it couldn't poll any URLs in {@code MAX_POLL_WAIT_TIME_MS} millis.
     *
     * @return the front url string of the queue
     * @throws Exception
     */
    private String getNextUrl() throws Exception {
        String url = "";

        try {
            url = sURLsQueue.poll(Constants.MAX_POLL_WAIT_TIME_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Output.log(e.getMessage());
        }

        if (url == null) {
            System.out.println("Crawler " + this.getName() + " is exiting...");
            throw new Exception("Cannot poll anymore URLs.");
        }

        return url;
    }

    /**
     * Adds the given URL to the queue and marks it as visited.
     *
     * @param url a web page URL to be added
     */
    private void addUrl(String url) {
        sURLsQueue.add(url);
        sVisitedURLs.add(url);

        try {
            URL x = new URL(url);
            int cnt = sBaseURLVisitedCnt.getOrDefault(WebUtilities.getBaseURL(x), 0);
            sBaseURLVisitedCnt.put(WebUtilities.getBaseURL(x), cnt + 1);
            sWebPagesCnt++;
        } catch (MalformedURLException e) {
            //e.printStackTrace();
        }

        Output.logURL(url);
    }

    /**
     * Checks if the given URL is valid to be crawled.
     * <p>
     * Valid rules:
     * <ul>
     * <li>The maximum limit of crawling web pages is not reached.</li>
     * <li>The url is not visited before.</li>
     * </ul>
     * <p>
     * The function must be called with <b>exclusive</b> access to:
     * <ul>
     * <li>{@code sWebPagesCnt}</li>
     * <li>{@code sBaseURLVisitedCnt}</li>
     * <li>{@code sVisitedURLs}</li>
     * </ul>
     *
     * @param url a web page URL object
     * @return {@code true} if the given URL is valid to be crawled
     */
    private boolean crawlable(URL url) {
        return sWebPagesCnt < Constants.MAX_WEB_PAGES_COUNT
                && sBaseURLVisitedCnt.getOrDefault(WebUtilities.getBaseURL(url), 0) < Constants.MAX_BASE_URL_COUNT
                && !sVisitedURLs.contains(url.toString());
    }

    /**
     * Checks if the given URL does not violate robots text rules.
     *
     * @param url a web page URL string
     * @return {@code true} if the given URL is allowed by robots text
     */
    private boolean allowedByRobotsText(String url) {
        try {
            URL tmp = new URL(url);

            if (!mRobotTxtParser.allowedURL(tmp)) {
                Output.log("Couldn't crawl url : " + url + " because of robots.txt !!!!!!");
                return false;
            }

            return true;
        } catch (MalformedURLException e) {
            Output.log(e.getMessage());
        }
        return false;
    }

    /**
     * Takes the HTML Document and inserts the URLs in the document into the Queue
     *
     * @param doc
     */
    private void extractURLS(Document doc) {
        Elements links = WebPageManager.processWebPage(doc);
        URL nextUrl;
        for (Element link : links) {
            try {
                nextUrl = new URL(link.attr("abs:href"));
            } catch (MalformedURLException e) {
                Output.log(e.getMessage());
                continue;
            }

            //lock the arrays and insert in them
            synchronized (sVisitedURLs) {
                synchronized (sURLsQueue) {
                    synchronized (sBaseURLVisitedCnt) {
                        if (crawlable(nextUrl))
                            addUrl(nextUrl.toString());
                        else
                            Output.log("skipped: " + nextUrl);
                    }
                }
            }
        }
    }

    /**
     * When a URL is found to be a bad url which is when a URL is not allowed by robots.txt or it gives an error
     * during the connection remove it from the webpages limit
     *
     * @param url
     */
    private void removeURLFromCnt(String url) {
        synchronized (sBaseURLVisitedCnt) {
            try {
                URL x = new URL(url);
                Integer cnt = sBaseURLVisitedCnt.getOrDefault(WebUtilities.getBaseURL(x), 1);
                sWebPagesCnt--;
                sBaseURLVisitedCnt.put(WebUtilities.getBaseURL(x), cnt - 1);
            } catch (MalformedURLException e) {

            }
        }
    }

    /**
     * Crawler main function.
     * As long as there exists URLs to crawl the function would be running getting documents and new URLs.
     */
    @Override
    public void run() {
        System.out.println("- Crawler " + this.getName() + " started");

        while (true) {
            String curUrl;

            try {
                curUrl = getNextUrl();
            } catch (Exception e) {
                Output.log(e.getMessage());
                return;
            }

            if (sWebPagesCnt > Constants.MAX_WEB_PAGES_COUNT) {
                continue;
            }

            // If the following check placed earlier then the crawler would be more interested in
            // fetching robots.txt rather than fetching the web pages themselves.
            if (!allowedByRobotsText(curUrl)) {
                removeURLFromCnt(curUrl);
                continue;
            }

            Output.log("Crawling: " + curUrl);
            Document doc = WebUtilities.getWebPage(curUrl);

            if (doc == null) {
                removeURLFromCnt(curUrl);
                Output.logDisallowedURL(curUrl);
                continue;
            }

            //if (!doc.baseUri().equals(curUrl))
            //	Output.logVisitedURL(doc.baseUri());

            Output.logVisitedURL(curUrl);
            extractURLS(doc);
        }
    }
}
