package search.engine.indexer;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import search.engine.utils.Constants;
import search.engine.utils.URLNormalizer;
import search.engine.utils.Utilities;
import search.engine.utils.WebUtilities;

import java.net.URL;
import java.util.*;


public class WebPageParser {

    //
    // Member variables
    //
    private StringBuilder sContent;
    private WebPage mPage;

    private int mParsedContentLen = 0;

    //
    // Member methods
    //

    /**
     * Parses the given web page raw document and prepares a {@code WebPage} object
     * with the parsed data.
     *
     * @param url the web page URL object
     * @param doc the web page document to parse
     */
    public WebPageParser(URL url, Document doc) {
        // Initializing variables
        sContent = new StringBuilder();
        mPage = new WebPage();
        mPage.wordPosMap = new HashMap<>();
        mPage.stemMap = new HashMap<>();

        // Assign page URL & title
        mPage.url = URLNormalizer.normalize(url);
        mPage.title = extractPageTitle(doc, url.getHost());

        // Parse to fill web page content and index
        dfs(doc.body(), "");

        // Assign words index variable
        mPage.content = sContent.toString().trim();
    }

    /**
     * Returns the parsed web page model representing the given raw web page.
     *
     * @return the parsed web page object
     */
    public WebPage getParsedWebPage() {
        return mPage;
    }

    /**
     * Returns the length of the web page content after being processed and parsed.
     *
     * @return the length of the parsed web page content
     */
    public int getParsedContentLength() {
        return mParsedContentLen;
    }

    /**
     * Extracts the web page title from the head tag.
     *
     * @param doc          the web page raw content
     * @param defaultTitle the web page default title if no title found in the web page document
     * @return the web page title
     */
    private String extractPageTitle(Document doc, String defaultTitle) {
        String ret = defaultTitle;
        String title = "";
        Elements titles = doc.head().select("title");

        if (titles.size() > 0) {
            title = titles.first().ownText().trim();
        }

        if (title.length() > 0) {
            ret = title;
            addToWordIndex(ret, "title");
        }

        return ret;
    }

    /**
     * Perform a depth-first search on the web page tags in order
     * to extract and parse the textual content.
     *
     * @param cur         current node in the DFS
     * @param previousTag the parent node tag name
     */
    private void dfs(Node cur, String previousTag) {
        // If its a text node then process its text
        if (cur instanceof TextNode) {
            TextNode node = (TextNode) cur;
            processText(node.text().trim(), previousTag);
            return;
        }

        // If it is an element node then recursively call the DFS function
        // with the children nodes of allowed tag
        if (cur instanceof Element) {
            Element element = (Element) cur;
            String tag = element.tagName();

            if (!Constants.ALLOWED_TAGS_SET.contains(tag)) {
                return;
            }

            for (Node child : cur.childNodes()) {
                dfs(child, tag);
            }
        }
    }

    /**
     * Processes the given string and constructs the words index.
     *
     * @param str the string to process
     * @param tag tag of the string
     */
    private void processText(String str, String tag) {
        if (str.isEmpty()) {
            return;
        }

        // Append the exact text to the page sContent variable
        sContent.append(str);
        sContent.append(" ");

        // Process the string and add it to the words index map
        addToWordIndex(str, tag);
    }

    /**
     * Process the string and add it to the words index map.
     *
     * @param str the string to process and add to index
     * @param tag the tag of the string
     */
    private void addToWordIndex(String str, String tag) {
        str = Utilities.processString(str);
        String words[] = str.split(" ");

        mParsedContentLen += str.length();

        int tagScore = Constants.TAG_TO_SCORE_MAP.getOrDefault(tag, 1);

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            //
            // Add new word position
            //
            int pos = mPage.wordsCount++;
            List<Integer> positions = new ArrayList<>();
            positions.add(pos);
            positions = mPage.wordPosMap.putIfAbsent(word, positions);
            if (positions != null) {
                positions.add(pos);
            }

            //
            // Count stem and sum score
            //
            if (Utilities.stopWord(word)) {
                continue;
            }

            // Get word's stem
            String stem = Utilities.stemWord(word);

            // Update web page stem map
            StemInfo info = mPage.stemMap.putIfAbsent(stem, new StemInfo(1, tagScore));
            if (info != null) {
                info.count++;
                info.score += tagScore;
            }
        }
    }

    /**
     * Extracts all out links from the given raw web page document
     * and adds them to {@code outLinks} list.
     *
     * @param doc the web page raw content
     */
    public static List<String> extractOutLinks(Document doc) {
        Set<String> outLinks = new HashSet<>();

        Elements links = doc.body().select("a[href]");

        for (Element element : links) {
            String link = element.attr("abs:href");

            if (WebUtilities.crawlable(link)) {
                try {
                    outLinks.add(URLNormalizer.normalize(new URL(link)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return new ArrayList<>(outLinks);
    }
}
