package org.uminho.di.gsd.membership.client;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Constants;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.client.DefaultClient;
import org.ws4d.java.client.SearchParameter;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;

public class ServiceSearchingClient extends DefaultClient {

    static Logger logger = Logger.getLogger(ServiceSearchingClient.class);

    Service membershipService = null;

    public ServiceSearchingClient() {
        this.init();
    }

    private void init() {
        this.initDiscoveryListening();
    }

    private void initDiscoveryListening() {
        // Register client for service reference changes
        this.registerServiceListening();
    }

    @Override
    public void serviceFound(ServiceReference serviceRef, SearchParameter sp) {
        logger.info("serviceFound:Found Service with Id " + serviceRef.getServiceId());

        logger.debug("serviceFound:Search Parameter was...");
        // devices
        QNameSet dvcTypes = sp.getDeviceTypes();
        if ((dvcTypes != null) && (!dvcTypes.isEmpty())) {
            logger.debug("DeviceTypes:");
            Iterator iterator = dvcTypes.iterator();

            while (iterator.hasNext()) {
                logger.debug(iterator.next());
            }
        }

        // services
        QNameSet svcTypes = sp.getServiceTypes();
        if ((svcTypes != null) && (!svcTypes.isEmpty())) {
            logger.debug("ServiceTypes:");
            Iterator iterator = svcTypes.iterator();

            while (iterator.hasNext()) {
                logger.debug(iterator.next());
            }
        }

        URI membershipSvcId = new URI("MembershipService");

        if (serviceRef.getServiceId().equals(membershipSvcId)) {
            catchService(serviceRef, membershipSvcId);

            // try our operations
            useGetTargetsOperation();
        }
    }

    private void catchService(ServiceReference svcRef, URI svcId) {
        if (svcRef.getServiceId().equals(svcId)) {
            try {
                membershipService = (Service) svcRef.getService();
            } catch (TimeoutException e) {
                logger.error(e.getMessage(), e);
            }

            logger.info("catchService:Found " + svcId);
        } else {
            logger.debug("catchService:" + svcRef.getServiceId() + " not equal to " + svcId);
        }

    }

    public void probe() {
        // we define the service to search.
        SearchParameter params = new SearchParameter();
//        params.setServiceTypes(new QNameSet(Constants.MembershipPortTypeQName));
        params.setServiceTypes(
                new QNameSet(
                new QName("http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01", "DiscoveryProxy")));
        

//        while(membershipService == null)
        if (membershipService == null) {
            searchService(params);

            try {
                Thread.sleep(3000);
                logger.info("probe:Searching for MembershipService...");

            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }

        }
    }

    public void useGetTargetsOperation() {
        Operation getTargetsOperation = null;

        try {
            //We need to get the operation from the service.
            //getAnyOperation returns the first Operation that fits the specification in the parameters.
            getTargetsOperation = membershipService.getAnyOperation(Constants.MembershipPortTypeQName, "GetTargets");

            // waiting period to allow communication to be performed
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        if (getTargetsOperation != null) {
            ParameterValue pValue = getTargetsOperation.createInputValue();

            pValue.setValue("ServiceType", String.valueOf("Gossip"));
            pValue.setValue("Fanout", String.valueOf(3));

            logger.info("GetTargets invocation parameters set!");

            ParameterValue returnMessagePV = null;
            //now lets invoke our first operation
            try {
                returnMessagePV = getTargetsOperation.invoke(pValue);
                logger.info("GetTargets invoked!");

            } catch (InvocationException e) {
                logger.error(e.getMessage(), e);
            } catch (TimeoutException e) {
                logger.error(e.getMessage(), e);
            }

            if (returnMessagePV != null) {
                Iterator targets = returnMessagePV.getChildren("TargetsList");

                if (targets.hasNext()) {
                    ParameterValue targetsList = (ParameterValue) targets.next();

                    logger.info("TargetsList size " + targetsList.getChildrenCount("Endpoint"));
                }
                String firstTarget = returnMessagePV.getValue("TargetsList/Endpoint[0]");

                logger.info("GetTargets returned " + firstTarget);
            }
        } else {
            logger.warn("Operation is still null!");
        }

    }

    public static void main(String[] args) {
        DPWSFramework.start(args);
        Log.setLogLevel(Log.DEBUG_LEVEL_INFO);
        ServiceSearchingClient client = null;

        try {
            client = new ServiceSearchingClient();
            client.probe();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if (client != null) {
                logger.warn("Shutting down...");
            }

            DPWSFramework.stop();
            System.exit(0);
        }
    }
}
