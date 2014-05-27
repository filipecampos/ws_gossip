package org.uminho.di.gsd.common.device;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.uminho.di.gsd.application.service.ApplicationService;
import org.uminho.di.gsd.common.InSOAPoverUDPDatagramHandlerImpl;
import org.uminho.di.gsd.common.RunConstants;
import org.uminho.di.gsd.gossip.device.GossipDevice;
import org.uminho.di.gsd.gossip.service.GossipService;
import org.uminho.di.gsd.gossip.client.GossipClient;
import org.uminho.di.gsd.membership.device.MembershipClientDevice;
import org.uminho.di.gsd.membership.info.MembershipRepository;
import org.uminho.di.gsd.membership.service.MembershipService;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.communication.HTTPBinding;
import org.ws4d.java.communication.protocol.soap.server.SOAPoverUDPServer;
import org.ws4d.java.communication.protocol.soap.server.SOAPoverUDPServer.SOAPoverUDPDatagramHandler;
import org.ws4d.java.service.Service;
import org.ws4d.java.structures.MessageIdBuffer;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;

public class PushGossipDevice extends MembershipClientDevice implements GossipDevice {

    final static Logger logger = Logger.getLogger(PushGossipDevice.class);

    static AtomicInteger udpTimes = new AtomicInteger();

    protected GossipService gossipService;
    protected GossipClient gossipClient;

    protected ApplicationService appService;

    SOAPoverUDPServer udpServer;
    SOAPoverUDPDatagramHandler handler;
    MessageIdBuffer msgIdBuffer;

    /* Getters and setters */
    public GossipClient getGossipClient() {
        return gossipClient;
    }

    public void setGossipClient(GossipClient client) {
        this.gossipClient = client;
    }

    @Override
    public GossipService getGossipService() {
        return gossipService;
    }

    public ApplicationService getApplicationService() {
        return appService;
    }

    /* Initializers */
    public void initializeApplicationService() {
        appService = new ApplicationService();
        String svcEPR = "http://" + IP + ":" + PORT + "/device/application/service";
        appService.addBinding(new HTTPBinding(new URI(svcEPR)));

        this.addService(appService);
    }

    @Override
    public void initializeGossipService() {
        gossipService = new GossipService();
        String svcEPR = "http://" + IP + ":" + PORT + "/device/gossip/service";
        gossipService.addBinding(new HTTPBinding(new URI(svcEPR)));
        gossipService.setServiceEPR(svcEPR);
        gossipService.setDevice(this);

        this.addService(gossipService);
    }

    protected void initializeUdpUnicastServer(String address, int port) {
        msgIdBuffer = new MessageIdBuffer();
        handler = new InSOAPoverUDPDatagramHandlerImpl(msgIdBuffer, gossipService);

        logger.debug("Invoked " + udpTimes.incrementAndGet() + " times in this JVM!");

        int tries = 3;
        while((udpServer == null) && (tries > 0))
        {
            tries--;
            try {
                udpServer = SOAPoverUDPServer.get(address, port, handler);
                if (udpServer != null) {
                    logger.debug(idStr + " UDP Unicast Server created!");
                } else {
                    logger.error(idStr + " UDP Unicast Server not created! Tries: " + tries);
                }
            } catch (IOException e) {
                logger.error(idStr + e.getMessage(), e);
                logger.error(idStr + " UDP Unicast Server not created!");
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                    logger.error(idStr + ex.getMessage(), ex);
                }
            }
        }

        if(udpServer == null)
        {
            logger.fatal(idStr + " UDP Unicast Server not created! Shutting down...");
            shutdown();
        }
    }

    protected void initializeUDP() {
        initializeUdpUnicastServer(getConstants().getIp(), getConstants().getPort());

        getGossipClient().setUdpUnicastServer(udpServer);
    }

    public void initializeGossipClient() {
        gossipClient = new GossipClient(this);
        gossipService.setClient(gossipClient);
        gossipClient.readConfiguration();
    }

    @Override
    public void initializeWorkers() {

        // Membership
        // service search
//        initializeSearchTask();

        // membership update
        initializeUpdateTask();

        // No Gossip Task as pure push is used
    }

    @Override
    public void startServices() {
        gossipService.setDevice(this);

        this.startGossipService();

        super.startServices();
    }

    protected void startGossipService() {
        try {
            gossipService.start();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void stopServices() {
        
        try {
            gossipService.stop();
            gossipService.stopShadowServices();
            gossipClient = null;

            appService.stop();
        } catch (IOException ex) {
            logger.error(idStr + ex.getMessage(), ex);
        }

        super.stopServices();
    }

    @Override
    public void writeStats() {
        if((fileWriter != null) && (hopsFileWriter != null))
        {
            getGossipClient().setFileWriters(fileWriter, hopsFileWriter);

            getGossipClient().writeStats();
        }
    }


    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length >= 2) {
            RunConstants constants = new RunConstants(args);

            // configure loggers
            PropertyConfigurator.configure("log4j.properties");

            // always start the framework first
            DPWSFramework.start(args);

//            Log.setLogLevel(Log.DEBUG_LEVEL_NO_LOGGING);
//            Log.setLogLevel(Log.DEBUG_LEVEL_ERROR);
            Log.setLogLevel(Log.DEBUG_LEVEL_DEBUG);

            // create the device ...
            PushGossipDevice device = new PushGossipDevice();
            device.setConstants(constants);

            try {
                device.initializeConfiguration();

                device.initializeBinding();

                // ... and the services
                device.initializeApplicationService();
                device.initializeMembershipService();
                device.initializeGossipService();

                // initialize repository
                MembershipRepository repository = new MembershipRepository();
                repository.initializeWithDevice(device);

                device.getMembershipService().setRepository(repository);

                device.initializeClient(repository);
                device.initializeGossipClient();

                device.getGossipClient().setFanout(constants.getFanout());

                device.initializeWorkers();
                // start services and device
                device.startDevice();
            }
            catch(Exception e)
            {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void setMonitoringMembershipService(Service svc) {
        membershipService = (MembershipService) svc;
    }

    @Override
    public Service getMonitoringMembershipService() {
        return membershipService;
    }

    public String getStats() {
        return getGossipClient().getStats();
    }

    public void initializeShadowServices() {

        logger.debug("Initializing Shadow Services...");
        gossipService.setActivateShadowService(true);
        try {
            gossipService.startShadowServices();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
