package search.engine.ranker;

import search.engine.indexer.WebPage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Ranker {

    /**
     *
     * @param webPages
     * @param queryFilterWords
     * @return
     */
    public ArrayList<WebPage> rankPages(ArrayList<WebPage> webPages, List<String> queryFilterWords){

        HashMap<String, Double> pagesScores = new HashMap<String, Double>();

        // For each page calculate its TF-IDF score
        for (int page = 0;  page < webPages.size(); page++) {
            WebPage webPage = webPages.get(page);
            pagesScores.put(webPage.id.toString(), calculatePageScore(webPage, queryFilterWords));
        }

        // Sort webPages


        return webPages;
    }

    /**
     *
     * @param webPage
     * @param queryFilerWords
     * @return
     */
    public Double calculatePageScore (WebPage webPage, List<String> queryFilerWords) {
        Double pageScore = 0.0;

        // For each word in the query filter words
        for (int iWord = 0; iWord < queryFilerWords.size(); iWord++) {
            String word = queryFilerWords.get(iWord);

            int wordTF = webPage.wordPosMap.get(word).size();
            double wordIDF = 1.0; // TODO @Samir55 see this

            pageScore +=  wordTF * wordIDF;
        }

        return pageScore;
    }


}