package search.engine.ranker;

import org.omg.CORBA.INTERNAL;
import search.engine.indexer.WebPage;
import search.engine.utils.Constants;

import java.util.*;

public class Ranker {

    /**
     * @param webPages
     * @param queryFilterWords
     * @param page_number
     * @return
     */
    public List<WebPage> get_results(ArrayList<WebPage> webPages, List<String> queryFilterWords, int page_number) {
        webPages = rankPages(webPages, queryFilterWords);

        int startIndex = Constants.SINGLE_PAGE_RESULTS_COUNT * (page_number - 1);

        return webPages.subList(startIndex, startIndex + Constants.SINGLE_PAGE_RESULTS_COUNT);
    }

    /**
     * @param webPages
     * @param queryFilterWords
     * @return
     */
    private ArrayList<WebPage> rankPages(ArrayList<WebPage> webPages, List<String> queryFilterWords) {
        HashMap<String, Double> pagesScores = new HashMap<String, Double>();

        // For each page calculate its TF-IDF score
        for (int page = 0; page < webPages.size(); page++) {
            WebPage webPage = webPages.get(page);
            pagesScores.put(webPage.id.toString(), calculatePageScore(webPage, queryFilterWords));
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
    private Double calculatePageScore(WebPage webPage, List<String> queryFilerWords) {
        Double pageTFIDFScore = 0.0;
        Double pagePosScore = 0.0;

        // For each word in the query filter words
        for (int iWord = 0; iWord < queryFilerWords.size(); iWord++) {
            String word = queryFilerWords.get(iWord);

            int wordTF = webPage.wordPosMap.get(word).size();
            double wordIDF = 1.0; // TODO @Samir55 see this
            pageTFIDFScore += wordTF * wordIDF;

            for (int i = 0; i < webPage.wordScoreMap.get(word).size(); i++) {
                int score = webPage.wordScoreMap.get(word).get(i);

                pagePosScore += score;
            }
        }

        return (0.2 * pageTFIDFScore) * (0.55 * pagePosScore) * (0.25 * webPage.rank); // pagePopularity * pageRank(Relevance)
    }
}
