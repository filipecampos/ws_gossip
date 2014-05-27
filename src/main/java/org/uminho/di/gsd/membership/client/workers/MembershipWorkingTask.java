/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.membership.client.workers;

import org.uminho.di.gsd.membership.client.MembershipRepositoryClient;

/**
 *
 * @author fjoc
 */
public abstract class MembershipWorkingTask implements Runnable {

    protected MembershipRepositoryClient client;

    public MembershipWorkingTask(MembershipRepositoryClient cli)
    {
        this.client = cli;
    }

    public MembershipRepositoryClient getClient() {
        return client;
    }

    public void setClient(MembershipRepositoryClient client) {
        this.client = client;
    }

}
