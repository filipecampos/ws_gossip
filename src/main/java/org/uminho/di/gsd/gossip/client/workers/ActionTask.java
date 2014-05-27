package org.uminho.di.gsd.gossip.client.workers;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.gossip.GossipVariants;
import org.uminho.di.gsd.gossip.client.GossipClient;

public class ActionTask extends GossipWorkingTask {

    static Logger logger = Logger.getLogger(ActionTask.class);

    private GossipVariants activeVariant;

    public ActionTask(GossipClient cli) {
        super(cli);
    }

    public GossipVariants getActiveVariant() {
        return activeVariant;
    }

    public void setActiveVariant(GossipVariants activeVariant) {
        this.activeVariant = activeVariant;
    }

    @Override
    public void run() {

        long sleepTime = 1000;
        if (period > 50) {
            sleepTime = period;
        }

        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        }

        while (!client.allMessages() && !terminate.get()) {
            logger.info("Waking up...");
            try {
                getClient().fireAction(activeVariant);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                terminate.set(true);
            }
            logger.info("Going to sleep...");
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ex) {
                logger.error(ex.getMessage(), ex);
                terminate.set(true);
            }
        }

        logger.info(client.getDevice().getIdStr() + " : " + activeVariant + " task terminating!!!!!!!");
    }
}
