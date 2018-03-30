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
    // Static Member variables
    //
    private int sCurIdx;
    private StringBuilder sContent;
    private Map<String, List<Integer>> sWordPosMap;
    private Map<String, List<Integer>> sWordScoreMap;


    /**
     * Parses the given web page document and returns a {@code WebPage} object
     * with the parsed data.
     *
     * @param url the web page URL object
     * @param doc the web page document to parse
     * @return a web page object constructed from the given document
     */
    public WebPage parse(URL url, Document doc) {
        // Initializing variables
        sCurIdx = 0;
        sContent = new StringBuilder();
        sWordPosMap = new HashMap<>();
        sWordScoreMap = new HashMap<>();

        // Parsing
        extractOutLinks(doc);
        dfs(doc, doc.tagName());

        // Assigning variables
        WebPage ret = new WebPage();
        ret.url = URLNormalizer.normalize(url);
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
    private void processText(String str, String tag) {
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
