package search.engine.utils;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

public class URLNormalizer {

    /**
     * Returns a normalized URL string of the given web page URL object.
     * <p>
     * Apply the following rules:
     * [1]  Remove fragment parts.
     * [2]  Convert to lower case.
     * [3]  IP address to domain name.
     * [4]  Remove default port.
     * [5]  Remove "/index.html"
     * [6] https -> http
     * [7] Remove "www."
     *
     * @param url a web page URL object
     * @return normalized URL string representing
     */
    public static String normalizeURL(URL url) {
        return
                obtainProtocol(url.getProtocol()) + "://" +
                        obtainDomainName(url.getHost()) +
                        obtainPort(url.getPort()) +
                        obtainPath(url.getPath());
    }

    /**
     * Convert given IPAddress to domain name
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
     * Obtain fixed port from the given port
     * (i.e. remove default port)
     *
     * @param URLPort port to be fixed
     * @return the fixed port
     */
    private static String obtainPort(int URLPort) {
        return (URLPort == 80 || URLPort == -1) ? "" : (":" + String.valueOf(URLPort));
    }

    /**
     * Obtain fixed protocol from the given protocol
     * (i.e. convert https -> http)
     *
     * @param URLProtocol protocol to be fixed
     * @return the fixed protocol
     */
    private static String obtainProtocol(String URLProtocol) {
        return URLProtocol.replace("https", "http").toLowerCase();
    }

    /**
     * Obtain domain name from host name
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
     * Obtain fixed path from given path
     * (i.e. remove directory index.html)
     *
     * @param URLPath path to be fixed
     * @return the fixed path
     */
    private static String obtainPath(String URLPath) {
        return URLPath
                .replace("index.html", "")
                .replace("index.htm", "")
                .replace("index.php", "")
                .toLowerCase();
    }
}