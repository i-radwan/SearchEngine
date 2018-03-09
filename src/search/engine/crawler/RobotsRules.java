package search.engine.crawler;

import java.util.ArrayList;
import java.util.List;


public class RobotsRules {

    /**
     * List of rules used for matching.
     */
    public List<String> rules;

    /**
     * Status flag to indicate whether the rules are fetched or not.
     */
    public boolean status;

    /**
     * Constructor.
     *
     * @param initStatus initial value for the status flag
     */
    RobotsRules(boolean initStatus) {
        rules = new ArrayList<>();
        status = initStatus;
    }
}
