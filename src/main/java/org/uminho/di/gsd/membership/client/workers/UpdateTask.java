/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.membership.client.workers;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.membership.client.MembershipRepositoryClient;

/**
 *
 * @author fjoc
 */
public class UpdateTask extends MembershipWorkingTask {
    static Logger logger = Logger.getLogger(UpdateTask.class);
    long initialWaitingPeriod;
    long period;

    public UpdateTask(MembershipRepositoryClient cli)
    {
        super(cli);
    }

    public UpdateTask(MembershipRepositoryClient client, Long initialWaitingPeriod, Long period) {
        super(client);

        this.initialWaitingPeriod = initialWaitingPeriod;
        this.period = period;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(initialWaitingPeriod);

            while(true)
            {
                logger.debug("Waking up...");
                try
                {
                    if(getClient() == null)
                        logger.error("Client is NULL! Couldn't initialize update...");
                    else
                        getClient().initUpdate();
                }
                catch(Exception e)
                {
                    logger.error(e.getMessage(), e);
                }
                logger.debug("Going to sleep...");
                Thread.sleep(period);
            }
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
