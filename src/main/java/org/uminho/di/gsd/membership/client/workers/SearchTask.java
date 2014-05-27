/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.membership.client.workers;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.membership.client.MembershipRepositoryClient;
import org.ws4d.java.client.SearchParameter;

/**
 *
 * @author fjoc
 */
public class SearchTask extends MembershipWorkingTask {
    static Logger logger = Logger.getLogger(SearchTask.class);

    public SearchTask(MembershipRepositoryClient cli)
    {
        super(cli);
    }

    @Override
    public void run() {
        logger.info("Waking up...");
        SearchParameter sp = new SearchParameter();

        getClient().searchService(sp);
        logger.info("Going to sleep...");
    }

}
