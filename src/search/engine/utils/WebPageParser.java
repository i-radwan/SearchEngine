package search.engine.utils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import search.engine.models.WebPage;

import java.net.URL;
import java.util.*;


public class WebPageParser {

    //
    // Static Member variables
    //
    private static int sCurIdx;
    private static StringBuilder sContent;
    private static Map<String, List<Integer>> sWordPosMap;
    private static Map<String, List<Integer>> sWordScoreMap;
    private static Set<String> sOutLinks;


    /**
     * Parses the given web page document and returns a {@code WebPage} object
     * with the parsed data.
     *
     * @param doc a web page document to parse
     * @return a web page object constructed from the given document
     */
    public static WebPage parse(Document doc) {
        // Initializing variables
        sCurIdx = 0;
        sContent = new StringBuilder();
        sWordPosMap = new HashMap<>();
        sWordScoreMap = new HashMap<>();
        sOutLinks = new HashSet<>();

        // Parsing
        extractOutLinks(doc);
        dfs(doc, doc.tagName());

        // Assigning variables
        WebPage ret = new WebPage();
        ret.url = doc.baseUri();
        ret.outLinks = new ArrayList<>();
        ret.outLinks.addAll(sOutLinks);
        ret.content = sContent.toString().trim();
        ret.wordsCount = sCurIdx;
        ret.wordPosMap = sWordPosMap;
        ret.wordScoreMap = sWordScoreMap;

        return ret;
    }

    /**
     * Extracts all out links from the given raw web page document
     * and adds them to {@code outLinks} list.
     *
     * @param doc web page raw content
     */
    private static void extractOutLinks(Document doc) {
        Elements links = doc.body().select("link[href], a[href]");

        for (Element element : links) {
            String link = element.attr("abs:href");
            URL url = WebUtilities.getURL(link);

            if (url != null && WebUtilities.validURL(link)) {
                sOutLinks.add(WebUtilities.normalizeURL(url));
            }
        }
    }

    /**
     * Perform a depth-first search on the web page tags in order
     * to extract and parse the textual content.
     *
     * @param cur         current node in the DFS
     * @param previousTag the parent node tag name
     */
    private static void dfs(Node cur, String previousTag) {
        // If its a text node then process its text
        if (cur instanceof TextNode) {
            TextNode node = (TextNode) cur;
            processText(node.text().trim(), previousTag);
            return;
        }

        // If its an element node then recursively call
        // the DFS with the children node if the current node is of allowed tag
        if (cur instanceof Element) {
            Element element = (Element) cur;
            String tag = element.tagName();

            if (!Constants.ALLOWED_TAGS_SET.contains(tag)) {
                //System.out.println(tag);
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
     * @param tag the string tag
     */
    private static void processText(String str, String tag) {
        if (str.isEmpty()) {
            return;
        }

        //
        // Append the exact text to the page sContent variable
        //
        sContent.append(str);
        sContent.append(" ");

        //
        // Process the string and construct words index map
        //
        str = Utilities.processString(str);
        List<String> words = Utilities.removeStopWords(str.split(" "));

        int score = Constants.TAG_TO_SCORE_MAP.getOrDefault(tag, 1);

        for (String word : words) {
            sWordPosMap.putIfAbsent(word, new ArrayList<>());
            sWordScoreMap.putIfAbsent(word, new ArrayList<>());
            sWordPosMap.get(word).add(sCurIdx++);
            sWordScoreMap.get(word).add(score);
        }
    }
}
