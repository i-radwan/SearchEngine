package search.engine.server;

import search.engine.utils.Constants;
import search.engine.utils.Utilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class SnippetExtractor {

    //
    // Member fields
    //
    private ArrayList<Snippet> nominatedSnippets;
    private List<Snippet> selectedSnippets;
    private List<String> originalQueryStems;
    private String[] pageContentArray;
    private String content;
    private String originalQuery;

    //
    // Member methods
    //

    /**
     * Extracts the important snippets from the given web page content.
     * <p>
     * The general steps to extract page snippet:
     * <ul>
     * <li>Iterate over the content and get all possible snippets (stems matching)</li>
     * <li>Sort these nominated snippets by their length descending
     * (s.t. the close keywords will always be with higher priority, this handles phrase search)</li>
     * <li>Select top Constants.MAX_SNIPPETS_COUNT snippets</li>
     * <li>Sort the selected snippets by their left index ascending (to display the overall
     * snippet with the same order as they appear in the document)</li>
     * <li>If no page snippet found (which mustn't occur I think), we select the first
     * Constants.MAX_SNIPPETS_CHARS_COUNT chars from the content</li>
     * <li>If the page snippet is shorter than Constants.MAX_SNIPPETS_CHARS_COUNT,
     * fill more chars after the end of the last selected snippet</li>
     * </ul>
     *
     * @param content       the web page content string
     * @param originalQuery the user's original raw search query
     * @return concatenated web page snippets
     */
    public String extractWebPageSnippet(String content, String originalQuery) {
        this.content = content;
        this.originalQuery = originalQuery;

        // Process the original query string, for snippet extraction purposes
        init();

        // Extract all possible snippets from the document
        getNominatedSnippets();

        // Select top snippets w.r.t. size
        getSelectedSnippets();

        // Concatenate small snippets
        StringBuilder pageSnippet = concatenateSnippets();

        // Fill the snippet in case it was so short
        return completeSnippetFilling(pageSnippet);
    }

    /**
     * Pre-processes the user's original raw search query
     * and constructs a stemmed version of it.
     */
    private void init() {
        originalQuery = Utilities.removeSpecialCharsAroundWord(originalQuery);

        originalQuery = originalQuery
                .substring(0, Math.min(originalQuery.length(), Constants.QUERY_MAX_LENGTH))
                .toLowerCase();

        originalQueryStems = new ArrayList<>();

        String queryWords[] = originalQuery.split(" ");

        for (String word : queryWords) {
            originalQueryStems.add(Utilities.stemWord(Utilities.removeSpecialCharsAroundWord(word)));
        }
    }

    /**
     * Returns list of nominated snippets, which contains matches between query words and web page content.
     *
     * @return list of nominated snippets
     */
    private ArrayList<Snippet> getNominatedSnippets() {
        nominatedSnippets = new ArrayList<>();

        this.pageContentArray = content.split(" ");

        int pageContentArrayLength = pageContentArray.length;

        int lastKeywordIdx = -3;

        for (int key = 0; key < pageContentArrayLength; ++key) {
            String word = prepareWordForSnippet(pageContentArray[key]);

            if (originalQueryStems.indexOf(word) == -1) continue;

            Snippet snippet = new Snippet();
            Snippet lastSnippet = null;
            Integer snippetStringStartIdx;

            if (!nominatedSnippets.isEmpty())
                lastSnippet = nominatedSnippets.get(nominatedSnippets.size() - 1);

            // New snippet
            if (key - lastKeywordIdx > 2) {
                snippet.L = Math.max(key - 2, (lastSnippet != null ? lastSnippet.R + 1 : 0));
                snippet.R = Math.min(pageContentArrayLength - 1, key + 2);

                snippetStringStartIdx = snippet.L;

                nominatedSnippets.add(snippet);
            }
            // Merge snippets
            else {
                int oldLastSnippetR = lastSnippet.R = Math.min(pageContentArrayLength - 1, key + 2);

                snippet = lastSnippet;

                // Separate newly added words from the previous snippet.str words
                snippet.str.append(" ");

                snippetStringStartIdx = oldLastSnippetR + 1;
            }

            // Add/Update snippet words
            fillSnippetStr(snippet, snippetStringStartIdx);

            lastKeywordIdx = key;
        }

        // Sort by snippet length desc. (this will be better, and manages phrases too)
        nominatedSnippets.sort(Comparator.comparingInt(s -> (s.L - s.R)));

        return nominatedSnippets;
    }

    /**
     * Fills small snippet string contents from/to the given limits,
     * if any word is keyword, we highlight it.
     *
     * @param snippet               the snippet to fill its str attribute
     * @param snippetStringStartIdx the starting index relative to the whole page content
     */
    private void fillSnippetStr(Snippet snippet, int snippetStringStartIdx) {
        for (int i = snippetStringStartIdx; i <= snippet.R; ++i) {
            String tmpWord = prepareWordForSnippet(pageContentArray[i]);

            boolean isKeyword = (originalQueryStems.indexOf(tmpWord) > -1);

            snippet.str.append((isKeyword) ? "<b>" : "");
            snippet.str.append(pageContentArray[i]);
            snippet.str.append((isKeyword) ? "</b>" : "");
            snippet.str.append((i < snippet.R) ? " " : "");
        }
    }

    /**
     * Selects top nominated snippets and sorts them according to their start index.
     *
     * @return list of sorted snippets ready to be concatenated
     */
    private List<Snippet> getSelectedSnippets() {
        selectedSnippets = nominatedSnippets.subList(0,
                Math.min(Constants.MAX_SNIPPETS_COUNT, nominatedSnippets.size())
        );

        // Sort again by L to print them in order
        selectedSnippets.sort(Comparator.comparingInt(s -> s.L));

        return selectedSnippets;
    }

    /**
     * Concatenates snippets.
     *
     * @return string that contains the semi-finished page snipped
     */
    private StringBuilder concatenateSnippets() {
        StringBuilder snippet = new StringBuilder("...");

        // Concatenate to get page snippet
        for (int i = 0; i < selectedSnippets.size(); ++i) {
            snippet.append(selectedSnippets.get(i).str);
            snippet.append((i < selectedSnippets.size() - 1) ? "..." : "");
        }

        return snippet;
    }


    /**
     * Fills the snippet to make all snippets with almost equal size, for better looking.
     *
     * @param snippet semi-finished page snippet
     * @return string contains the finished web page snippet
     */
    private String completeSnippetFilling(StringBuilder snippet) {
        // If no selected snippet
        if (selectedSnippets.size() == 0) {
            int len = Math.min(content.length(), Constants.MAX_SNIPPETS_CHARS_COUNT);
            return content.substring(0, len) + "...";
        }

        // Fill more to show full-like snippet
        int index = selectedSnippets.get(selectedSnippets.size() - 1).R + 1;

        while (snippet.length() < Constants.MAX_SNIPPETS_CHARS_COUNT && index < pageContentArray.length) {
            snippet.append(" ").append(pageContentArray[index++]);
        }

        return snippet.toString();
    }

    /**
     * Prepares a word for comparison to be selected as snippet word or not.
     * Here:
     * <ul>
     * <li>The surrounding special chars get removed.</li>
     * <li>The word gets converted to lower case.</li>
     * <li>The word gets stemmed</li>
     * </ul>
     *
     * @param word the word to be processed
     * @return string contains the processes word
     */
    private String prepareWordForSnippet(String word) {
        return Utilities.stemWord(Utilities.removeSpecialCharsAroundWord(word)).toLowerCase();
    }

    private class Snippet {
        StringBuilder str = new StringBuilder();
        int L, R;
    }
}
