package search.engine.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public final class WebUtilities {

    /**
     * Returns a URL object from the given URL string.
     *
     * @param url a web page URL string
     * @return URL object representing the given string, or null not valid.
     */
    public static URL getURL(String url) {
        URL ret = null;

        try {
            ret = new URL(url);
        } catch (MalformedURLException e) {
            //e.printStackTrace();
        }

        return ret;
    }

    /**
     * Returns a normalized URL string of the given web page URL object.
     * (i.e. removes the fragment part from the URL).
     *
     * @param url a web page URL object
     * @return normalized URL string representing
     */
    public static String normalizeURL(URL url) {
        return url.getProtocol() + "://" + url.getHost() + url.getPath();
    }

    /**
     * Returns the base URL string of the given web page URL object.
     *
     * @param url a web page URL object
     * @return string representing the base address
     */
    public static String getBaseURL(URL url) {
        //return string containing the base url
        return url.getProtocol() + "://" + url.getHost();
    }

    /**
     * Returns the robots text URL of the given web page URL object.
     *
     * @param url a web page URL object
     * @return string representing the robots text URL.
     * <p>
     * (i.e base_address/robots.txt)
     */
    public static URL getRobotsTextURL(URL url) {
        String baseUrl = getBaseURL(url);
        return getURL(baseUrl + "/robots.txt");
    }

    /**
     * Connects to the robots.txt URL of the given web page URL object
     * and returns its as an array of string lines.
     *
     * @param url a web page URL object
     * @return list of strings representing the robots text of the given web page
     */
    public static List<String> fetchRobotsText(URL url) {
        // Get web page robots text url
        url = getRobotsTextURL(url);

        // List of lines to hold robots.txt
        List<String> ret = new ArrayList<>();

        try {
            //opens the robots.txt file as a buffered stream and start reading line by line.
            BufferedReader inp = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;

            while ((line = inp.readLine()) != null) {
                ret.add(line.toLowerCase());
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }

        return ret;
    }

    /**
     * Connects to the given web page and
     * returns document representing its HTML content.
     *
     * @param url a web page URL string
     * @return {@code jsoup.nodes.Document} representing the content of the given web page
     */
    public static Document fetchWebPage(String url) {
        Document ret = null;

        try {
            ret = Jsoup.connect(url).get();
        } catch (Exception e) {
            //e.printStackTrace();
        }

        return ret;
    }

    /**
     * Returns true if the given URL is of valid type to be fetched.
     *
     * @param url a web page URL string to check
     * @return {@code true} if the given url is valid to be fetched, {@code false} otherwise
     */
    public static boolean validURL(String url) {
        return !url.isEmpty()
                && !url.contains(".css")
                && !url.contains(".jpg")
                && !url.contains(".pdf")
                && !url.contains(".docs")
                && !url.contains(".js")
                && !url.contains(".png")
                && !url.contains(".ico");
    }
}
