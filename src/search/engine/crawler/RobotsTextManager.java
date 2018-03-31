package search.engine.crawler;

import search.engine.utils.Constants;
import search.engine.utils.WebUtilities;

import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class RobotsTextManager {

    //
    // Member variables
    //
    private ConcurrentHashMap<String, RobotsRules> mWebsiteRules = new ConcurrentHashMap<>();
    private String mUserAgent = Constants.DEFAULT_USER_AGENT;


    /**
     * Checks whether the given URL is allowed to be crawled.
     *
     * @param url a web page URL object
     * @return {@code true} if the given URL is allowed to be crawled, {@code false} otherwise
     */
    public boolean allowedURL(URL url) {
        String baseURL = url.getHost();

        // Prepare robots text file of the given website
        prepareRobotsText(url, baseURL);

        // Match the given URL with the disallowed rules
        boolean disallowed = RobotsTextParser.matchRules(
                url.toString(),
                mWebsiteRules.get(baseURL).rules
        );

        return !disallowed;
    }

    /**
     * Prepares the robots text file of the given web page URL.
     * If the robots text was already fetched then return.
     * Else, fetch the robots text and store it for further usage.
     * <p>
     * If the function was called from another thread while the robots text
     * was being fetched then the thread will wait until the robots text get fetched.
     *
     * @param url     the web page URL object to prepare its robots text
     * @param baseURL the web page base URL string
     */
    private void prepareRobotsText(URL url, String baseURL) {
        // Atomic insert & get from the map
        RobotsRules rules = mWebsiteRules.putIfAbsent(baseURL, new RobotsRules(false));

        //
        // If the robots text was not fetched before then go fetch and parse it,
        // and mark that the robots text is being fetched so that no other threads do the same job
        //
        if (rules == null) {
            Output.log("Fetching robots.txt of " + url.toString());

            updateRules(
                    baseURL,
                    // Parse robots text and extract only the disallowed rules of the current user agent
                    RobotsTextParser.parse(WebUtilities.fetchRobotsText(url), mUserAgent)
            );
            return;
        }

        //
        // The robots text is already fetched,
        // or it is being fetched by another thread at the mean time
        //
        synchronized (rules) {
            while (!rules.status) {
                // The robots.txt is still not ready
                try {
                    Output.log("Waiting for robots.txt : " + baseURL);
                    rules.wait();
                    Output.log("Woke up and " + (rules.status ? "did" : "didn't") + " find robots.txt : " + baseURL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Updates the robots rules of the given URL
     * and set the rules status to true to indicate that the rules was inserted
     * then notify the waiting threads.
     *
     * @param baseURL the web page base URL string to be updated
     * @param rules   list of new robots rules
     */
    private void updateRules(String baseURL, List<String> rules) {
        RobotsRules robotsRules = mWebsiteRules.get(baseURL);

        synchronized (robotsRules) {
            robotsRules.rules = rules;
            robotsRules.status = true;
            robotsRules.notifyAll();
            Output.log("Notifying for robots.txt : " + baseURL);
        }
    }
}
