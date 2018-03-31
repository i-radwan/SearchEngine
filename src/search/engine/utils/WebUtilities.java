package search.engine.utils;

import org.jsoup.Jsoup;
import org.jsoup.UncheckedIOException;
import org.jsoup.nodes.Document;
import search.engine.crawler.Output;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public final class WebUtilities {

    /**
     * Returns a URL object from the given URL string.
     *
     * @param url a web page URL string
     * @return URL object representing the given string, or null if invalid URL was given
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
     * Returns the host name of the given web page URL string.
     *
     * @param urlStr a web page URL string
     * @return string representing the base address, or null if invalid URL was given
     */
    public static String getHostName(String urlStr) {
        String ret = null;
        try {
            URL url = new URL(urlStr);
            ret =  url.getHost();
        } catch (MalformedURLException e) {
            //e.printStackTrace();
        }
        return ret;
    }

    /**
     * Connects to the robots.txt URL of the given web page URL object
     * and returns its as an array of string lines.
     *
     * @param url a web page URL object
     * @return list of strings representing the robots text of the given web page
     */
    public static List<String> fetchRobotsText(URL url) {
        // List of lines to hold robots.txt
        List<String> ret = new ArrayList<>();

        try {
            // Get web page robots text url
            url = new URL(url.getProtocol() + "://" + url.getHost() + "/robots.txt");

            // Get connection and set read timeout to avoid hanging
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(60000);

            //opens the robots.txt file as a buffered stream and start reading line by line.
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            while ((line = input.readLine()) != null) {
                ret.add(line.toLowerCase());
            }
        } catch (SocketTimeoutException e) {
            System.err.println(e.getMessage());
            Output.log("Fetching " + url.toString() + " read timeout");
        } catch (IOException e) {
            //e.printStackTrace();
        } catch (Exception e) {
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
        } catch (SocketTimeoutException e) {
            //System.out.println("Timeout Error while fetching " + url);
        } catch (IOException e) {
            //System.out.println("IO Error while fetching " + url);
        } catch (UncheckedIOException e) {
            //System.out.println("Unchecked IO Error while fetching " + url);
        } catch (Exception e) {
            //e.printStackTrace();
        }

        return ret;
    }

    /**
     * Returns true if the given URL is of valid type to be crawled.
     *
     * @param url a web page URL string to check
     * @return {@code true} if the given url is valid to be fetched, {@code false} otherwise
     */
    public static boolean crawlable(String url) {
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
