package org.uminho.di.gsd.gossip.device;

import org.uminho.di.gsd.gossip.service.GossipService;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.Service;

public interface GossipDevice extends Device {

    public String getIdStr();

    public void setMonitoringMembershipService(Service svc);
    public Service getMonitoringMembershipService();

    public void initializeGossipService();
    public GossipService getGossipService();

    public void initializeWorkers();
}