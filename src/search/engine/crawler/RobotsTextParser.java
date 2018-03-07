package search.engine.crawler;


import java.util.ArrayList;

import java.net.URL;


public class RobotsTextParser {

    public static RobotsTextManager mManager;

    /**
     * Constructor that takes RobotTxtManager
     *
     * @param manager
     */
    RobotsTextParser(RobotsTextManager manager) {
        mManager = manager;
    }

    /**
     * Generates the rules of the robots.txt File from the given URL if the URL rules was there then don't do anything
     * If the URL was already cached check if the thread responsible for getting the robots.txt has gotten it or not
     * If the thread has got it then use it else wait on the thread to notify you that he has got it
     *
     * @param url
     */
    private void generateRobotTxt(URL url) {
        //check if the URL is already cached in the data and if it wasn't cached cache it
        if (mManager.cacheURL(url)) {
            //parse the file to a HashMap
            ArrayList<String> parsedRobotsTxt = parseRobotsText(URLUtilities.getRobotsText(url));

            Output.logURLRule(url.toString(), parsedRobotsTxt);
            mManager.updateRules(url, parsedRobotsTxt);
        } else {
            RobotsTextManager.RulesStatus sync = RobotsTextManager.mURLRules.get(mManager.getURLId(URLUtilities.getBaseURL(url)));
            synchronized (sync) {
                //The robots.txt is still not ready
                while (!sync.status) {
                    try {
                        Output.log(" waiting for url : " + URLUtilities.getBaseURL(url));
                        sync.wait();
                        if (sync.status)
                            Output.log(" woke up and did find url : " + URLUtilities.getBaseURL(url));
                        else
                            Output.log(" woke up and didn't find url : " + URLUtilities.getBaseURL(url));
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    /**
     * Returns true if the given url doesn't violate the robots.txt rules
     *
     * @param url
     * @return
     */
    public boolean isUrlAllowed(URL url) {
        generateRobotTxt(url);
        return mManager.isUrlAllowed(url);
    }

    /**
     * Takes lines of the robots.txt and parses it
     *
     * @param robotTxt
     * @return
     */
    private ArrayList<String> parseRobotsText(ArrayList<String> robotTxt) {
        ArrayList<String> parsedRobotTxt = new ArrayList<>();
        String curUserAgent = null;

        //loop on each line of the file if it starts with user-agent then it's a new user-agent
        //if it starts with allow/disallow then it's a new rule for the current user-agent
        for (String line : robotTxt) {
            if (line.startsWith("user-agent")) {
                curUserAgent = line.split(":")[1].trim();
            } else if (mManager.mUserAgent.equals(curUserAgent) && line.startsWith("disallow")) {
                String tmp[] = line.split(":", 2);

                String _line = tmp[1].trim();

                _line = _line.replaceAll("\\*", ".*");
                _line = _line.replaceAll("\\?", "[?]");

                if (_line.length() > 0)
                    parsedRobotTxt.add(_line);
            }
        }

        return parsedRobotTxt;
    }
}
