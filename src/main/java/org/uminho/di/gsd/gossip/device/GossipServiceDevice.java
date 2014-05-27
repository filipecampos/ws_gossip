/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.gossip.device;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.device.BasicDevice;
import org.uminho.di.gsd.gossip.service.GossipService;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.communication.HTTPBinding;
import org.ws4d.java.service.Service;
import org.ws4d.java.types.URI;

/**
 *
 * @author fjoc
 */
public class GossipServiceDevice extends BasicDevice implements GossipDevice {
    static Logger logger = Logger.getLogger(GossipServiceDevice.class);

    protected GossipService gossipService;
    
    public void initializeGossipService() {
        gossipService = new GossipService();
        gossipService.addBinding(new HTTPBinding(new URI("http://" + IP + ":" + PORT + "/gossip/service")));

        this.addService(gossipService);
    }

    @Override
    public void startDevice() {
        this.startGossipService();

        super.startDevice();
    }

    private void startGossipService() {
        try {
            gossipService.start();
        } catch (IOException ex) {
            logger.error(idStr + ex.getMessage(), ex);
        }
    }

    @Override
    public GossipService getGossipService() {
        return gossipService;
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // always start the framework first
        DPWSFramework.start(args);

        // create a simple device ...
        GossipServiceDevice device = new GossipServiceDevice();
        device.initializeBinding();

        // ... and a service
        device.initializeGossipService();

        // add service to device in order to support automatic discovery ...
        device.startDevice();

//        DPWSFramework.stop();
    }

    @Override
    public void setMonitoringMembershipService(Service svc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Service getMonitoringMembershipService() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void initializeWorkers()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
