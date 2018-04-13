package search.engine.ranker;

import search.engine.indexer.WebPage;

import java.util.Comparator;
import java.util.HashMap;


class SortPagesByRank implements Comparator<WebPage> {

    private HashMap<String, Double> pagesScores;

    public SortPagesByRank(HashMap<String, Double> scores) {
        pagesScores = scores;
    }

    @Override
    public int compare(WebPage page1, WebPage page2) {
        return Double.compare(pagesScores.get(page1.id.toString()), pagesScores.get(page2.id.toString()));
    }
}
