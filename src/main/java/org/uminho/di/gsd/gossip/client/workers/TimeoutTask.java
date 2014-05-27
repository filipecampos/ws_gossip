/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.gossip.client.workers;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.gossip.service.repo.aggregation.AggregationMessage;

/**
 *
 * @author fjoc
 */
public class TimeoutTask extends Thread {

    static final Logger logger = Logger.getLogger(TimeoutTask.class);

    AggregationMessage msg;
    long time;

    boolean cancelled;

    public TimeoutTask(AggregationMessage agg, long period)
    {
        msg = agg;
        time = period;
        cancelled = false;
    }

    @Override
    public void run() {
        logger.debug("Going to sleep for " + time + " ms...");
        try {
            Thread.sleep(time);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        }
        logger.debug("Wokeup from " + time + " ms sleep.");

        if(!cancelled)
            msg.timeout();
    }

    public void cancel()
    {
        cancelled = true;
        interrupt();
        logger.debug("Was cancelled!");
    }
}
