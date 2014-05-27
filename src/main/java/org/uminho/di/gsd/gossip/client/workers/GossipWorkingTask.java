package org.uminho.di.gsd.gossip.client.workers;

import java.util.concurrent.atomic.AtomicBoolean;

import org.uminho.di.gsd.gossip.client.GossipClient;

public abstract class GossipWorkingTask implements Runnable {

    protected GossipClient client;
    protected AtomicBoolean terminate;
    protected long period;

    public GossipWorkingTask()
    {
        terminate = new AtomicBoolean(false);
    }

    public GossipWorkingTask(GossipClient cli)
    {
        this();
        this.client = cli;
    }

    public GossipClient getClient() {
        return client;
    }

    public void setClient(GossipClient client) {
        this.client = client;
    }

    public boolean isTerminate() {
        return terminate.get();
    }

    public void setTerminate(boolean terminate) {
        this.terminate.set(terminate);
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }
    
    @Override
    public abstract void run();
}
