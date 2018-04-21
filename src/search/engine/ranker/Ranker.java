package search.engine.ranker;

import org.bson.types.ObjectId;
import search.engine.indexer.Indexer;
import search.engine.indexer.WebPage;
import search.engine.utils.Constants;
import search.engine.utils.Utilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


public class Ranker {

    //
    // Member variables
    //
    Indexer mIndexer;

    List<WebPage> mWebPages;
    List<String> mQueryWords;
    List<String> mQueryStems;

    long mTotalDocsCount;

    /**
     * Constructs a ranker object for the given web pages and search query.
     *
     * @param indexer    a database indexer object to get web page statistics
     * @param webPages   the web pages to rank and sort
     * @param queryWords the user's search query after polishing
     * @param queryStems the user's search query after stemming the words
     */
    public Ranker(Indexer indexer, List<WebPage> webPages, List<String> queryWords, List<String> queryStems) {
        mIndexer = indexer;
        mTotalDocsCount = mIndexer.getDocumentsCount();

        mWebPages = webPages;
        mQueryWords = queryWords;
        mQueryStems = queryStems;
    }

    /**
     * Ranks the given web pages based on the given search query words
     * and returns a paginated results.
     *
     * @param pageNumber the pagination page number
     * @return list of sorted paginated web pages
     */
    private List<ObjectId> rank(int pageNumber) {
        // For each page calculate its TF-IDF score
        for (WebPage webPage : mWebPages) {
            webPage.rank = calculatePageScore(webPage);
        }

        // Sort webPages
        mWebPages.sort(new Comparator<WebPage>() {
            @Override
            public int compare(WebPage p1, WebPage p2) {
                return Double.compare(p1.rank, p2.rank);
            }
        });

        List<ObjectId> ret = new ArrayList<>();

        int idx = Constants.SINGLE_PAGE_RESULTS_COUNT * (pageNumber - 1);
        int cnt = Math.min(mWebPages.size(), Constants.SINGLE_PAGE_RESULTS_COUNT);

        while (cnt-- > 0) {
            ret.add(mWebPages.get(idx++).id);
        }

        return ret;
    }

    /**
     * Calculates the given web page score rank based on the users's search query
     * and the content of the page.
     * The score is calculated as the sum of product of the web page TF and IDF
     * for each search query word.
     *
     * @param webPage the web page to calculate its score
     * @return the calculated web page score
     */
    private double calculatePageScore(WebPage webPage) {
        double pageTFIDFScore = 0.0;

        // For each word in the query filter words
        for (int i = 0; i < mQueryWords.size(); ++i) {
            String word = mQueryWords.get(i);
            String stem = mQueryStems.get(i);

            // Exact word
            int wordCnt = webPage.wordPosMap.get(word).size();
            long wordDocCnt = mIndexer.getWordDocumentsCount(word);
            double wordTF = wordCnt / (double) webPage.wordsCount;
            double wordIDF = Math.log(mTotalDocsCount / (double) wordDocCnt);

            // Synonymous words
            int stemCnt = webPage.stemWordsCount.get(stem) - wordCnt;
            long stemDocCnt = mIndexer.getStemDocumentsCount(stem) - wordDocCnt;
            double stemTF = stemCnt / (double) webPage.wordsCount;
            double stemIDF = Math.log(mTotalDocsCount / (double) stemDocCnt);

            pageTFIDFScore += (wordTF * wordIDF) + (stemTF * stemIDF) * (0.5);
        }

        return (0.75 * pageTFIDFScore) * (0.25 * webPage.rank);
    }
}
