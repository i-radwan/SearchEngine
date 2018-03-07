package search.engine.crawler;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class WebPageManager {

    /**
     * Takes in a document and returns Elements containing links to other webpages
     *
     * @param doc
     * @return
     */
    public static Elements extractURLS(Document doc) {
        return doc.select("link[href], a[href]");
    }

    /**
     * Returns true if the given URL is of the type to be crawlable
     *
     * @param url
     * @return
     */
    private static boolean isValidUrl(String url) {
        if (url.length() == 0 || url.contains(".css") || url.contains(".jpg") || url.contains(".pdf") || url.contains(".docs") || url.contains(".js") || url.contains(".png") || url.contains(".ico"))
            return false;

        return true;
    }

    /**
     * Takes in a document and returns Elements containing the headers in this webpage
     *
     * @param doc
     * @return
     */
    public static Elements extractHeaders(Document doc) {
        return doc.select("h1, h2, h3, h4");
    }

    /**
     * Takes in a webpage and process it and gives the data to the indexer to be indexed and returns the outlinks from
     * this webpage to the crawler
     *
     * @param doc
     * @return
     */
    public static Elements processWebPage(Document doc) {
        Elements links = extractURLS(doc);
        Elements outLinks = new Elements();
        for (Element link : links) {
            if (isValidUrl(link.attr("abs:href")))
                outLinks.add(link);
        }
        //Elements Headers = extractHeaders(doc);
        //String x = doc.text();
        //Output.log(x, "");
        //String y = Headers.text();
        //Elements m = doc.getAllElements();


        return outLinks;
    }
}
