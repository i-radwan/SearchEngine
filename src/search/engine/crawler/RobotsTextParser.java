package search.engine.crawler;

import search.engine.utils.WebUtilities;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RobotsTextParser {

    //
    // Member variables
    //
    public RobotsTextManager mManager;


    /**
     * Constructor.
     *
     * @param manager Robots Text Manager object
     */
    RobotsTextParser(RobotsTextManager manager) {
        mManager = manager;
    }

    /**
     * Checks if the given URL is allowed by robots rules or not.
     *
     * @param url a web page URL object
     * @return {@code true} if the given url doesn't violate the robots rules, {@code false} otherwise
     */
    public boolean allowedURL(URL url) {
        getRobotTxt(url);
        return mManager.allowedURL(url);
    }

    /**
     * Generates the rules of the robots.txt File from the given URL if the URL rules was there then don't do anything
     * If the URL was already cached check if the thread responsible for getting the robots.txt has gotten it or not
     * If the thread has got it then use it else wait on the thread to notify you that he has got it
     *
     * @param url
     */
    private void getRobotTxt(URL url) {
        // Check if the URL is already cached in the data and if it wasn't cached cache it
        if (mManager.cacheURL(url)) {
            List<String> parsedRobotsTxt = parseRobotsText(WebUtilities.fetchRobotsText(url));
            Output.logURLRules(url.toString(), parsedRobotsTxt);
            mManager.updateRules(url, parsedRobotsTxt);
        } else {
            String baseURL = WebUtilities.getBaseURL(url);
            int id = mManager.getURLId(baseURL);
            RobotsRules sync = mManager.URLRules.get(id);

            synchronized (sync) {
                //The robots.txt is still not ready
                while (!sync.status) {
                    try {
                        Output.log(" waiting for url : " + baseURL);

                        sync.wait();

                        if (sync.status)
                            Output.log(" woke up and did find url : " + baseURL);
                        else
                            Output.log(" woke up and didn't find url : " + baseURL);

                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Parse the given robots text and returns only the
     * disallowed rules of the manager user agent.
     *
     * @param robotsTxt a list of strings of a robots text
     * @return the disallowed rules of the manager user agent
     */
    private List<String> parseRobotsText(List<String> robotsTxt) {
        List<String> parsedRobotTxt = new ArrayList<>();
        String curUserAgent = null;

        // Loop on each line of the file:
        for (String line : robotsTxt) {
            // If it starts with "user-agent" then it's a new user agent
            if (line.startsWith("user-agent")) {
                curUserAgent = line.split(":")[1].trim();
            }
            // If it starts with "allow"/"disallow" then it's a new rule for the current user agent
            else if (mManager.userAgent.equals(curUserAgent) && line.startsWith("disallow")) {
                String tmp = line.split(":", 2)[1].trim();

                tmp = tmp.replaceAll("\\*", ".*");
                tmp = tmp.replaceAll("\\?", "[?]");

                if (tmp.length() > 0) {
                    parsedRobotTxt.add(tmp);
                }
            }
        }

        return parsedRobotTxt;
    }

    /**
     * Checks whether the given url matches any of the the given rules.
     *
     * @param url   a web page URL string to check
     * @param rules list of robots rules to match against
     * @return {@code true} if there is a match between the given URL and rules, {@code flase} otherwise.
     */
    public static boolean matchRules(String url, List<String> rules) {
        // Loop through each rule and start matching it using the regex
        for (String rule : rules) {
            Pattern pat = Pattern.compile(rule);
            Matcher matcher = pat.matcher(url);

            if (matcher.find()) {
                return true;
            }
        }

        // Return false if no match was found
        return false;
    }
}
