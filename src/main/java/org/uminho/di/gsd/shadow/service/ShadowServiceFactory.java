package org.uminho.di.gsd.shadow.service;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.common.device.BasicDevice;
import org.uminho.di.gsd.gossip.service.GossipService;
import org.ws4d.java.service.LocalService;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.types.QName;

public class ShadowServiceFactory {

    static Logger logger = Logger.getLogger(ShadowServiceFactory.class);

    final static java.util.ArrayList<QName> reservedPortTypes;

    static {
        reservedPortTypes = new java.util.ArrayList<QName>();
        reservedPortTypes.add(Constants.GossipPushPortQName);
        reservedPortTypes.add(Constants.GossipPullPortQName);
        reservedPortTypes.add(Constants.GossipLazyPortQName);
        reservedPortTypes.add(Constants.AggregationPortQName);
        reservedPortTypes.add(Constants.MembershipPortTypeQName);
        reservedPortTypes.add(Constants.ManagementPortQName);
    }

    public static HashMap indexAndReplicateNormalServicesOperations(BasicDevice device, GossipService gossipService)
    {
        HashMap indexedOps = new HashMap();
        List services = new ArrayList();
        Iterator servicesIter = device.getServices();

        boolean createShadow = gossipService.isActivateShadowService();

        while(servicesIter.hasNext())
        {
            LocalService tempService = (LocalService) servicesIter.next();

            if(isNormalService(tempService))
            {
                Iterator operationsIter = tempService.getOperations();

                while(operationsIter.hasNext())
                {
                    Operation op = (Operation) operationsIter.next();
                    String opInputAction = op.getInputAction();

                    logger.debug("Indexing operation with key " + opInputAction);

                    indexedOps.put(opInputAction, op);
                }


                if(createShadow)
                {
                    // creating shadow service
                    ShadowService shadowService = new ShadowService(device, gossipService, tempService);
                    if(shadowService != null)
                    {
                        shadowService.initialize();
                        services.add(shadowService);
                    }
                }
            }
        }

        gossipService.setIndexedOps(indexedOps);

        logger.debug("Finished indexing normal services' operations!");

        if(createShadow)
        {
            logger.debug("Finished replicating normal services!");

            servicesIter = services.iterator();
            while(servicesIter.hasNext())
            {
                ShadowService tempService = (ShadowService) servicesIter.next();
                try {
                    device.addService(tempService, true);
                } catch(IOException ex)
                {
                    logger.error(ex.getMessage(), ex);
                }
            }

            logger.debug("Finished adding replicated services!");
        }

        return indexedOps;
    }

    public static List replicateNormalServices(BasicDevice device, GossipService gossipService)
    {
        List services = new ArrayList();
        Iterator servicesIter = device.getServices();

        while(servicesIter.hasNext())
        {
            LocalService tempService = (LocalService) servicesIter.next();

            if(isNormalService(tempService))
            {
                ShadowService shadowService = new ShadowService(device, gossipService, tempService);
                if(shadowService != null)
                {
                    shadowService.initialize();
                    services.add(shadowService);
                }
            }
        }

        logger.debug("Finished replicating normal services!");

        servicesIter = services.iterator();
        while(servicesIter.hasNext())
        {
            ShadowService tempService = (ShadowService) servicesIter.next();
            try {
                device.addService(tempService, true);
            } catch(IOException ex)
            {
                logger.error(ex.getMessage(), ex);
            }
        }

        logger.debug("Finished adding replicated services!");

        return services;
    }

    private static boolean isNormalService(Service svc)
    {
        boolean normal = true;
        Iterator portTypes = svc.getPortTypes();

        while(normal && portTypes.hasNext())
        {
            QName portType = (QName) portTypes.next();

            normal = !reservedPortTypes.contains(portType);
            logger.debug("PortType:" + portType + " normal? " + normal);
        }

        return normal;
    }
}
