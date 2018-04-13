package search.engine.ranker;

import search.engine.indexer.Indexer;
import search.engine.indexer.WebPage;
import search.engine.utils.Constants;

import java.util.HashMap;
import java.util.List;


public class Ranker {

    Indexer mIndexer;
    long mTotalDocsCount;

    /**
     * @param webPages
     * @param queryFilterWords
     * @param pageNumber
     * @return
     */
    public List<WebPage> getResults(List<WebPage> webPages, List<String> queryFilterWords, int pageNumber) {
        mIndexer = new Indexer();
        mTotalDocsCount = mIndexer.getDocumentsCount();

        webPages = rankPages(webPages, queryFilterWords);

        int startIndex = Constants.SINGLE_PAGE_RESULTS_COUNT * (pageNumber - 1);

        return webPages.subList(startIndex, startIndex + Constants.SINGLE_PAGE_RESULTS_COUNT);
    }

    /**
     * @param webPages
     * @param queryFilterWords
     * @return
     */
    private List<WebPage> rankPages(List<WebPage> webPages, List<String> queryFilterWords) {
        HashMap<String, Double> pagesScores = new HashMap<>();

        // For each page calculate its TF-IDF score
        for (WebPage webPage : webPages) {
            pagesScores.put(webPage.id.toString(), calculatePageScore(webPage, queryFilterWords));
        }

        // Sort webPages
        webPages.sort(new SortPagesByRank(pagesScores));

        return webPages;
    }

    /**
     * @param webPage
     * @param queryFilerWords
     * @return
     */
    private Double calculatePageScore(WebPage webPage, List<String> queryFilerWords) {
        Double pageTFIDFScore = 0.0;
        Double pagePosScore = 0.0;

        // For each word in the query filter words
        for (String word : queryFilerWords) {

            double wordTF = webPage.wordPosMap.get(word).size() / (double) webPage.wordsCount;
            double wordIDF = Math.log(mTotalDocsCount / (double) mIndexer.getDocumentsCount(word)); // TODO @Samir55 see this

            pageTFIDFScore += wordTF * wordIDF;

            //for (int score : webPage.wordScoreMap.get(word)) {
            //    pagePosScore += score;
            //}
        }

        return (0.2 * pageTFIDFScore) * (0.25 * webPage.rank); // pagePopularity * pageRank(Relevance)
    }
}
