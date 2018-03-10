package search.engine.utils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WebPageParser {

    //
    // Member variables
    //
    private int mCurIdx;
    private StringBuilder mContent;
    private Map<String, List<Integer>> mWordPosMap;
    private Map<String, List<Integer>> mWordScoreMap;


    /**
     * Parses the given web page document and extracts its textual content,
     * and construct word-position, and word-score map needed by the indexer.
     *
     * @param doc a web page document to parse
     */
    public void parse(Document doc) {
        mCurIdx = 0;
        mContent = new StringBuilder();
        mWordPosMap = new HashMap<>();
        mWordScoreMap = new HashMap<>();

        // TODO: parse <head> and get web page title
        dfs(doc, doc.tagName());
    }

    /**
     * Returns the words count after parsing the web page content
     * and after removing the numeric and special characters.
     *
     * @return the number of words after parsing
     */
    public int getWordsCount() {
        return mCurIdx;
    }

    /**
     * Returns the extracted web page content.
     *
     * @return the number of words after parsing
     */
    public String getPageContent() {
        // TODO: Remove strange characters
        return mContent.toString();
    }

    /**
     * Returns a map from a given word to a list of occurrence positions.
     *
     * @return the word-position map
     */
    public Map<String, List<Integer>> getWordsPositions() {
        return mWordPosMap;
    }

    /**
     * Returns a map from a given word to a list of score
     * representing the tag of their occurrence positions.
     *
     * @return the word-score map
     */
    public Map<String, List<Integer>> getWordsScore() {
        return mWordScoreMap;
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
        // Append the exact text to the page mContent variable
        //
        mContent.append(str);
        mContent.append(" ");

        //
        // Process the string and construct words index map
        //
        str = Utilities.processString(str);
        List<String> words = Utilities.removeStopWords(str.split(" "));

        int score = Constants.TAG_TO_SCORE_MAP.getOrDefault(tag, 1);

        for (String word : words) {
            mWordPosMap.putIfAbsent(word, new ArrayList<>());
            mWordScoreMap.putIfAbsent(word, new ArrayList<>());
            mWordPosMap.get(word).add(mCurIdx++);
            mWordScoreMap.get(word).add(score);
        }
    }
}
