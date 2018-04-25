package search.engine.server;

import search.engine.utils.Constants;
import search.engine.utils.Utilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SnippetExtractor {

    /**
     * Extracts the important snippets from the given webpage content
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
     * @param content             the webpage content string
     * @param mOriginalQueryStems the user's query words stemmed version
     * @return concatenated webpage snippets
     */
    public static String extractWebpageSnippet(String content, List<String> mOriginalQueryStems) {
        // Extract all possible snippets from the document
        ArrayList<Snippet> nominatedSnippets = getNominatedSnippets(content, mOriginalQueryStems);

        // Select top snippets w.r.t. size
        List<Snippet> selectedSnippets = getSelectedSnippets(nominatedSnippets);

        // Concatenate small snippets
        String pageSnippet = concatenateSnippets(selectedSnippets);

        // Fill the snippet in case it was so short
        return completeSnippetFilling(content, pageSnippet, selectedSnippets);
    }

    /**
     * Returns list of nominated snippets, which contains matches between query words and webpage words
     *
     * @param content             The webpage content
     * @param mOriginalQueryStems The user's query stemmed version
     * @return list of nominated snippets
     */
    private static ArrayList<Snippet> getNominatedSnippets(String content, List<String> mOriginalQueryStems) {
        ArrayList<Snippet> nominatedSnippets = new ArrayList<>();

        String[] pageContentArray = content.split(" ");
        int pageContentArrayLength = pageContentArray.length;

        int lastKeywordIdx = -3;

        for (int key = 0; key < pageContentArrayLength; ++key) {
            String word = prepareWordForSnippet(pageContentArray[key]);

            if (mOriginalQueryStems.indexOf(word) == -1) continue;

            Snippet snippet = new Snippet();
            Snippet lastSnippet = null;
            int snippetStringStartIdx;

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
                int oldLastSnippetR = lastSnippet.R;

                lastSnippet.R = Math.min(pageContentArrayLength - 1, key + 2);

                snippet = lastSnippet;

                // Separate newly added words from the previous snippet.str words
                snippet.str += " ";

                snippetStringStartIdx = oldLastSnippetR + 1;
            }

            // Add/Update snippet words
            fillSnippetStr(snippet, snippetStringStartIdx, mOriginalQueryStems, pageContentArray);

            lastKeywordIdx = key;
        }

        // Sort by snippet length desc. (this will be better, and manages phrases too)
        nominatedSnippets.sort(Comparator.comparingInt(s -> (s.L - s.R)));

        return nominatedSnippets;
    }

    /**
     * Fills small snippet string contents from/to the given limits, if any word is keyword, we highlight it
     *
     * @param snippet               The snippet to fill its str attribute
     * @param snippetStringStartIdx The starting index relative to the whole page content
     * @param mOriginalQueryStems   The query stemmed version
     * @param pageContentArray      The page content array
     */
    private static void fillSnippetStr(Snippet snippet, int snippetStringStartIdx, List<String> mOriginalQueryStems, String[] pageContentArray) {
        for (int i = snippetStringStartIdx; i <= snippet.R; ++i) {
            String tmpWord = prepareWordForSnippet(pageContentArray[i]);

            boolean isKeyword = (mOriginalQueryStems.indexOf(tmpWord) > -1);

            snippet.str += (isKeyword) ? "<b>" : "";
            snippet.str += pageContentArray[i];
            snippet.str += (isKeyword) ? "</b>" : "";
            snippet.str += (i < snippet.R) ? " " : "";
        }
    }

    /**
     * Selects top nominated snippets and sorts them according to their start index
     *
     * @param nominatedSnippets The content nominated snippets
     * @return List of sorted snippets ready to be concatenated
     */
    private static List<Snippet> getSelectedSnippets(ArrayList<Snippet> nominatedSnippets) {
        List<Snippet> selectedSnippets = nominatedSnippets.subList(0,
                Math.min(Constants.MAX_SNIPPETS_COUNT, nominatedSnippets.size())
        );

        // Sort again by L to print them in order
        selectedSnippets.sort(Comparator.comparingInt(s -> s.L));

        return selectedSnippets;
    }

    /**
     * Concatenates snippets
     *
     * @param selectedSnippets list of snippets
     * @return string that contains the semi-finished page snipped
     */
    private static String concatenateSnippets(List<Snippet> selectedSnippets) {
        String snippet = "...";

        // Concatenate to get page snippet
        for (int i = 0; i < selectedSnippets.size(); ++i) {
            snippet += selectedSnippets.get(i).str;
            snippet += (i < selectedSnippets.size() - 1) ? "..." : "";
        }

        return snippet;
    }


    /**
     * Fills the snippet to make all snippets with almost equal size, for better looking
     *
     * @param content          page original content
     * @param snippet          semi-finished page snippet
     * @param selectedSnippets List of selected snippets
     * @return string contains the finished webpage snippet
     */
    private static String completeSnippetFilling(String content, String snippet, List<Snippet> selectedSnippets) {
        // If no selected snippet
        if (selectedSnippets.size() == 0) {
            snippet = content.substring(0, Constants.MAX_SNIPPETS_CHARS_COUNT) + "...";
        }
        // Fill more to show full-like snippet
        else if (snippet.length() < Constants.MAX_SNIPPETS_CHARS_COUNT) {
            int beginIndex = selectedSnippets.get(selectedSnippets.size() - 1).R + 1;
            int endIndex = beginIndex + Constants.MAX_SNIPPETS_CHARS_COUNT - snippet.length() + 1;

            snippet += "..." +
                    content.substring(beginIndex, Math.min(content.length(), endIndex));
        }

        return snippet;
    }

    /**
     * Prepares a word for comparison to be selected as snippet word or not
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
    private static String prepareWordForSnippet(String word) {
        return Utilities.stemWord(Utilities.removeSpecialCharsAroundWord(word)).toLowerCase();
    }

    private static class Snippet {
        String str = "";
        int L, R;
    }
}
