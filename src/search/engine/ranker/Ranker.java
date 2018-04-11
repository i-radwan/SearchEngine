package search.engine.ranker;

import org.omg.CORBA.INTERNAL;
import search.engine.indexer.Indexer;
import search.engine.indexer.WebPage;
import search.engine.utils.Constants;

import java.util.*;

public class Ranker {

    Indexer indexer;
    int totalDocsCount;
    /**
     * @param webPages
     * @param queryFilterWords
     * @param page_number
     * @return
     */
    public List<WebPage> get_results(List<WebPage> webPages, List<String> queryFilterWords, int page_number) {
        indexer = new Indexer();
        totalDocsCount = indexer.getDocumentsCount();

        webPages = rankPages(webPages, queryFilterWords);

        int startIndex = Constants.SINGLE_PAGE_RESULTS_COUNT * (page_number - 1);

        return webPages.subList(startIndex, startIndex + Constants.SINGLE_PAGE_RESULTS_COUNT);
    }

    /**
     * @param webPages
     * @param queryFilterWords
     * @return
     */
    private List<WebPage> rankPages(List<WebPage> webPages, List<String> queryFilterWords) {
        HashMap<String, Double> pagesScores = new HashMap<String, Double>();

        // For each page calculate its TF-IDF score
        for (WebPage webPage : webPages) {
            pagesScores.put(webPage.id.toString(), calculatePageScore(webPage, queryFilterWords, totalDocsCount));
        }

        // Sort webPages
        Collections.sort(webPages, new SortPagesByRank(pagesScores));

        return webPages;
    }

    /**
     * @param webPage
     * @param queryFilerWords
     * @return
     */
    private Double calculatePageScore(WebPage webPage, List<String> queryFilerWords, int totalDocsCount) {
        Double pageTFIDFScore = 0.0;
        Double pagePosScore = 0.0;

        // For each word in the query filter words
        for (String word : queryFilerWords) {

            double wordTF = webPage.wordPosMap.get(word).size() / (double) webPage.wordsCount;
            double wordIDF =  Math.log(totalDocsCount / (double) indexer.getWordDocumentsCount(word)); // TODO @Samir55 see this

            pageTFIDFScore += wordTF * wordIDF;

//            for (int score : webPage.wordScoreMap.get(word)) {
//                pagePosScore += score;
//            }
        }

        return (0.2 * pageTFIDFScore) * (0.25 * webPage.rank); // pagePopularity * pageRank(Relevance)
    }
}
