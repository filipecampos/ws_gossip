package org.uminho.di.gsd.membership.client;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.membership.info.DeviceInfo;
import org.uminho.di.gsd.membership.info.ServiceInfo;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.client.DefaultClient;
import org.ws4d.java.client.SearchParameter;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.dispatch.HelloData;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;

public class MembershipServiceClient extends DefaultClient {

    static Logger logger = Logger.getLogger(MembershipServiceClient.class);

    Service membershipService = null;

    public MembershipServiceClient() {
        this.init();

    }

    private void init() {
        this.initDiscoveryListening();
    }

    private void initDiscoveryListening() {
        // makes helloReceived catch Hello messages
        this.registerHelloListening();

        // Register client for service reference changes
        this.registerServiceListening();
    }

    /**
     * Callback method, if device hello was received
     * @param hd
     */
    @Override
    public void helloReceived(HelloData hd) {
        // TODO: get query value
        boolean query = true;

        EndpointReference endpointRef = hd.getEndpointReference();

        logger.debug("Received Hello from " + endpointRef);


        ServiceReference svcRef;
        Device device;
        EndpointReference epr;

        // if device announced Gossip or MembershipType or if, according to configuration, it is supposed to be queried
        if (query) {
            try {
                DeviceReference deviceRef = getDeviceReference(endpointRef);
                device = deviceRef.getDevice();
                DeviceInfo devInfo = new DeviceInfo(device);
                logger.debug("Created devInfo with ref : " + devInfo.getEndpointReference());
                Iterator services = null;

                services = device.getServiceReferences();
                while (services.hasNext()) {
                    svcRef = (ServiceReference) services.next();
                    logger.debug("helloReceived:ServiceId " + svcRef.getServiceId());
                    
                    if(logger.isDebugEnabled())
                    {
                        ServiceInfo si = new ServiceInfo(svcRef, devInfo.getEndpointReference());
                        logger.debug("Created ServiceInfo with preferredXAddress= " + si.getPreferredXAddress() + "; endpointAddresses: ");
                    
                        Iterator iterator = si.getEndpointAddresses().iterator();
                        while(iterator.hasNext())
                        {
                            epr = (EndpointReference) iterator.next();
                            logger.debug("EPR: " + epr);
                        }
                    }

                    if (membershipService == null) {
                        catchService(svcRef, Constants.MembershipServiceId);
                    }
                    if (membershipService != null) {
                        useGetTargetsOperation();
                    }
                }
            } catch (TimeoutException ex) {
                logger.error(ex);
            }
        }
    }

    /**
     * This method is called each time a service matching the initial search
     * criteria (as contained within argument search) has been found
     * @param sr
     * @param sp
     */
    @Override
    public void serviceFound(ServiceReference serviceRef, SearchParameter sp) {
        if(logger.isDebugEnabled())
        {
            logger.debug("Found Service with Id " + serviceRef.getServiceId());

            logger.debug("Search Parameter was...");
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
                logger.error(e);
            }

            logger.debug("Found " + svcId);
        } else {
            logger.warn(svcRef.getServiceId() + " not equal to " + svcId);
        }

    }

    public void probe() {
        // we define the service to search.
        SearchParameter params = new SearchParameter();
        params.setServiceTypes(new QNameSet(Constants.MembershipServiceQName));

        if (membershipService == null) {
            searchService(params);

            try {
                Thread.sleep(3000);
                logger.debug("Searching for MembershipService...");

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
            getTargetsOperation = membershipService.getAnyOperation(Constants.MembershipServiceQName, "GetTargets");
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        if (getTargetsOperation != null) {
            ParameterValue pValue = getTargetsOperation.createInputValue();

            pValue.setValue("ServiceType", String.valueOf("Gossip"));
            pValue.setValue("Fanout", String.valueOf(3));

            logger.debug("GetTargets invocation parameters set!");

            ParameterValue returnMessagePV = null;
            //now lets invoke our first operation
            try {
                returnMessagePV = getTargetsOperation.invoke(pValue);
                logger.debug("GetTargets invoked!");

            } catch (InvocationException e) {
                logger.error(e.getMessage(), e);
            } catch (TimeoutException e) {
                logger.error(e.getMessage(), e);
            }

            if (returnMessagePV != null) {
                Iterator targets = returnMessagePV.getChildren("TargetsList");

                if (targets.hasNext()) {
                    ParameterValue targetsList = (ParameterValue) targets.next();

                    logger.debug("TargetsList size " + targetsList.getChildrenCount("Endpoint"));
                }
                String firstTarget = returnMessagePV.getValue("TargetsList/Endpoint[0]");

                logger.debug("GetTargets returned " + firstTarget);
            }
        } else {
            logger.debug("Operation is still null!");
        }

    }

    public static void main(String[] args) {
        DPWSFramework.start(args);
        Log.setLogLevel(Log.DEBUG_LEVEL_INFO);
        MembershipServiceClient client = null;

        try {
            client = new MembershipServiceClient();
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
