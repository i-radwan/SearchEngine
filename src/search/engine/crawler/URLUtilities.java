package search.engine.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class URLUtilities {

    /**
     * Return the base url
     *
     * @param url
     * @return
     */
    public static String getBaseURL(URL url) {
        //return string containing the base url
        return url.getProtocol() + "://" + url.getHost();
    }

    /**
     * Returns the Base url + "/robots.txt"
     *
     * @param url
     * @return
     */
    public static URL getRobotsTextURL(URL url) {
        String baseUrl = getBaseURL(url);

        URL robotURL = null;
        try {
            robotURL = new URL(baseUrl + "/robots.txt");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return robotURL;
    }

    /**
     * Connects to the robots.txt URL and return the file
     *
     * @param url
     * @return
     */
    public static ArrayList<String> getRobotsText(URL url) {
        url = getRobotsTextURL(url);
        ArrayList<String> lines = new ArrayList<>();
        String s;

        //opens the robots.txt file as a buffered stream and start reading it just like a normal file
        try (BufferedReader inp = new BufferedReader(new InputStreamReader(url.openStream()))) {
            while ((s = inp.readLine()) != null)
                lines.add(s.toLowerCase());
        } catch (Exception e) {

        }

        return lines;
    }

    /**
     * Takes in the URL and the thread name (for logging purposes) and returns document containing the content of the page
     *
     * @param url
     * @return
     */
    public static Document getWebPage(String url) {
        Document ret;
        try {
            ret = Jsoup.connect(url).get();
            return ret;
        } catch (Exception e) {
            Output.log(e.getMessage());
            return null;
        }
    }

    /**
     * Takes a URL and a set of rules and return true if the url matches the disallowed rules
     * TODO: parse robots.txt after getting it.
     *
     * @param url
     * @param rules
     * @return
     */
    public static boolean isURLDisallowed(String url, ArrayList<String> rules) {
        //loop on each rule and start matching it using the library regex
        for (String rule : rules) {

            String tmp[] = rule.split(":");

            if (tmp.length != 2) {
                // Invalid rule
                continue;
            }

            String directive = tmp[0].trim();

            if (!directive.equals("disallow")) {
                continue;
            }

            String _rule = tmp[1].trim();

            Pattern pat = Pattern.compile(_rule);
            Matcher matcher = pat.matcher(url);

            if (matcher.find())
                return true;
        }

        return false;
    }
}
