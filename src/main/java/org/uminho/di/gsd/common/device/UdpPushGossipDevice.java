package org.uminho.di.gsd.common.device;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.uminho.di.gsd.common.RunConstants;
import org.uminho.di.gsd.membership.info.MembershipRepository;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.util.Log;

public class UdpPushGossipDevice extends PushGossipDevice {

    final static Logger logger = Logger.getLogger(UdpPushGossipDevice.class);

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
            Log.setLogLevel(Log.DEBUG_LEVEL_ERROR);

            // create the device ...
            UdpPushGossipDevice device = new UdpPushGossipDevice();
            device.setConstants(constants);

            try {
                device.initializeConfiguration();

                device.initializeBinding();

                // ... and the services
                device.initializeMembershipService();
                device.initializeGossipService();

                // initialize repository
                MembershipRepository repository = new MembershipRepository();
                repository.initializeWithDevice(device);

                device.getMembershipService().setRepository(repository);

                // start services and device
                device.initializeClient(repository);
                device.initializeGossipClient();

                device.initializeUDP();
//
//                device.initializeManagementService();

                device.getGossipClient().setFanout(constants.getFanout());
                device.getGossipClient().setIters(constants.getMessages());
                device.getGossipClient().setTimeInterval(constants.getTimeInterval());

                device.startDevice();                
            }
            catch(Exception e)
            {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
