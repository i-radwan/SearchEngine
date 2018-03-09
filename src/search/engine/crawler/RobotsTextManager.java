package search.engine.crawler;

import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;


public class RobotsTextManager {

    public ConcurrentSkipListSet<Integer> allowedURLs = new ConcurrentSkipListSet<>();
    public ConcurrentSkipListSet<Integer> disallowedURLs = new ConcurrentSkipListSet<>();
    public ConcurrentHashMap<Integer, RobotsRules> URLRules = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Integer> URLIds = new ConcurrentHashMap<>();
    public String userAgent = Constants.USER_AGENT;

    /**
     * Returns the URL ID if exists,
     * otherwise it creates a new ID and give it to the URL.
     *
     * @param url a web page url string
     * @return the ID of the given url
     */
    public int getURLId(String url) {
        int id;

        synchronized (URLIds) {
            if (URLIds.containsKey(url)) {
                return URLIds.get(url);
            }

            id = URLIds.size();
            URLIds.put(url, id);
        }

        Output.logURLId(id, url);
        return id;
    }

    /**
     * Checks whether the given robots text
     * of the given URL is cached before.
     *
     * @param url a web page URL object to check
     * @return {@code true} if the URL's robots.txt cached, @{code false} otherwise
     */
    public boolean cached(URL url) {
        int id = getURLId(WebUtilities.getBaseURL(url));

        synchronized (URLRules) {
            return (URLRules.containsKey(id));
        }
    }

    /**
     * Caches the given URL in the memory.
     * Returns false if the URL already cached, else it caches the URL by inserting a new {@code RobotsRules}
     * object and setting its status to false to indicate that the rules are being fetched.
     *
     * @param url a web page URL object
     * @return returns {@code true} if the URL was cached, {@code false} otherwise
     */
    public boolean cacheURL(URL url) {
        int id = getURLId(WebUtilities.getBaseURL(url));

        synchronized (URLRules) {
            if (!URLRules.containsKey(id)) {
                URLRules.put(id, new RobotsRules(false));
                return true;
            }
        }

        return false;
    }

    /**
     * Updates the robots rules of the given URL
     * and set the rules status to true to indicate that the rules was inserted
     * then notify the waiting threads.
     *
     * @param url   the web page URL to be updated
     * @param rules list of new robots rules
     */
    public void updateRules(URL url, List<String> rules) {
        String baseURL = WebUtilities.getBaseURL(url);
        int id = getURLId(baseURL);
        RobotsRules robotsRules;

        synchronized (URLRules) {
            robotsRules = URLRules.get(id);
        }

        synchronized (robotsRules) {
            robotsRules.rules = rules;
            robotsRules.status = true;
            robotsRules.notifyAll();
            Output.log("Notifying for url : " + baseURL);
        }
    }

    /**
     * Checks whether the given URL is allowed to be crawled.
     *
     * @param url a web page URL object
     * @return {@code true} if the given URL is allowed to be crawled, {@code false} otherwise
     */
    public boolean allowedURL(URL url) {
        int id = getURLId(url.toString());

        if (allowedURLs.contains(id))
            return true;
        if (disallowedURLs.contains(id))
            return false;

        // Match the given URL with the disallowed rules
        boolean disallowed = RobotsTextParser.matchRules(
                url.toString(),
                URLRules.get(getURLId(WebUtilities.getBaseURL(url))).rules
        );

        if (disallowed) {
            disallowedURLs.add(id);
            Output.logDisallowedURL(String.valueOf(id));
        } else {
            allowedURLs.add(id);
            Output.logAllowedURL(String.valueOf(id));
        }

        return !disallowed;
    }
}
