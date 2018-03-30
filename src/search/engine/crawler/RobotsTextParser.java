package search.engine.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RobotsTextParser {

    /**
     * Parse the given robots text and returns only the
     * disallowed rules of the given user agent.
     *
     * @param robotsTxt a list of strings of a robots text
     * @return the disallowed rules of the given user agent
     */
    public static List<String> parse(List<String> robotsTxt, String userAgent) {
        List<String> parsedRobotTxt = new ArrayList<>();
        String curUserAgent = null;

        // Loop on each line of the file
        for (String line : robotsTxt) {
            // Change the robots text line to lower case
            line = line.toLowerCase();

            // If it starts with "user-agent" then it's a new user agent
            if (line.startsWith("user-agent")) {
                curUserAgent = line.split(":")[1].trim();
            }
            // If it starts with "allow"/"disallow" then it's a new rule for the current user agent
            else if (userAgent.equals(curUserAgent) && line.startsWith("disallow")) {
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
