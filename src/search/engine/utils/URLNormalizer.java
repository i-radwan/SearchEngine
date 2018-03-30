package search.engine.utils;

import spark.utils.StringUtils;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class URLNormalizer {

    /**
     * Returns a normalized URL string of the given web page URL object.
     * <p>
     * Applies the following rules:
     * <p>
     * [1]  Remove fragment parts.
     * [2]  Convert to lower case.
     * [3]  IP address to domain name.
     * [4]  Remove default port.
     * [5]  Remove "/index.html"
     * [6]  https -> http
     * [7]  Remove "www."
     * [8]  Sorting
     * [9]  Remove empty
     * [10] Capitalize percent-encoded octets (e.g. %XX)
     *
     * @param url a web page URL object
     * @return normalized URL string
     */
    public static String normalize(URL url) {
        return
                //obtainProtocol(url.getProtocol()) + "://" +
                url.getProtocol() + "://" +
                        obtainDomainName(url.getHost()) +
                        obtainPort(url.getPort()) +
                        obtainPath(url.getPath()) +
                        obtainQueryParameters(url.getQuery());
    }

    /**
     * Obtains fixed protocol from the given protocol
     * (i.e. convert https -> http).
     *
     * @param protocol protocol to be fixed
     * @return the fixed protocol
     */
    private static String obtainProtocol(String protocol) {
        return protocol.replace("https", "http").toLowerCase();
    }

    /**
     * Obtains domain name from host name.
     *
     * @param host host name to be converted
     * @return the domain name
     */
    private static String obtainDomainName(String host) {
        String domainName = host.toLowerCase();

        // Check if IP address -> Replace IP with domain name
        if (domainName.matches("[0-9.]*")) {
            domainName = IPAddressToHostName(domainName);
        }

        // Check for www, www2, www3, ...etc -> Then remove them
        if (domainName.length() > 3 && domainName.startsWith("www")) {
            domainName = domainName.substring(domainName.indexOf(".") + 1, domainName.length());
        }

        return domainName;
    }

    /**
     * Converts given IPAddress to domain name.
     *
     * @param IPAddress the address to be converted
     * @return the hostname, or the IP address if getting host name failed
     */
    private static String IPAddressToHostName(String IPAddress) {
        // Convert IP to host name
        try {
            return InetAddress.getByName(IPAddress).getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return IPAddress;
    }

    /**
     * Obtains fixed port from the given port
     * (i.e. removes default port).
     *
     * @param port port to be fixed
     * @return the fixed port
     */
    private static String obtainPort(int port) {
        return (port == 80 || port == -1) ? "" : (":" + String.valueOf(port));
    }

    /**
     * Obtains fixed path from given path
     * (i.e. removes directory index.html).
     *
     * @param path path to be fixed
     * @return the fixed path
     */
    private static String obtainPath(String path) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }

        return capitalizePercentEncodedOctets(
                path
                        .replace("index.html", "")
                        .replace("index.htm", "")
                        .replace("index.php", "")
                        .toLowerCase());
    }

    /**
     * Obtains query parameter by apply:
     * [1] Sorting
     * [2] Removing empty
     * [3] Capitalizing percent-encoded octets (e.g. %XX)
     *
     * @param queryParams the original URL query parameters
     * @return the fixed query parameters
     */
    private static String obtainQueryParameters(String queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return "";
        }

        // To lower
        queryParams = queryParams.toLowerCase();

        // Split queries
        ArrayList<String> queries = new ArrayList<>(Arrays.asList(queryParams.split("&")));

        // Remove empty queries
        queries.removeIf(q -> q.endsWith("=") && q.chars().filter(ch -> ch == '=').count() == 1);

        // Sort queries
        Collections.sort(queries);

        return (queries.isEmpty() ? "" : ("?" + capitalizePercentEncodedOctets(String.join("&", queries))));
    }

    /**
     * Capitalize percent encoded octets (e.g. %3A)
     *
     * @param s the input string
     * @return the capitalized string
     */
    private static String capitalizePercentEncodedOctets(String s) {
        // Pattern matching for %XX
        Pattern pattern = Pattern.compile("%[0-9a-z]{2}");
        Matcher matcher = pattern.matcher(s);

        // Check all occurrences
        while (matcher.find()) {
            String substring = s.substring(matcher.start(), matcher.end());
            s = s.replace(substring, substring.toUpperCase());
        }

        return s;
    }
}