/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uminho.di.gsd.gossip.client;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.gossip.service.operations.PushOperation;
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
import org.ws4d.java.structures.HashSet;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.structures.Set;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;

/**
 *
 * @author fjoc
 */
public class PushClient extends DefaultClient {

    static Logger logger = Logger.getLogger(PushClient.class);
    private Service membershipService;

    public PushClient() {
        membershipService = null;

        registerHelloListening();

        registerServiceListening();
    }

    public void probe() {
        // we define the service to search.
        SearchParameter params = new SearchParameter();
//        params.setServiceTypes(new QNameSet(Constants.MembershipPortTypeQName));

        if (membershipService == null) {
            searchService(params);
            logger.info("Searching for a Device hosting a Membership Service...");

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
    
    @Override
    public void helloReceived(HelloData hd) {
        logger.info("Received Hello " + hd.toString());

        Set types = new HashSet();

        Iterator iter = hd.getDevicePortTypes();

        while(iter.hasNext())
            types.add(iter.next());

        QNameSet dvcTypes = QNameSet.newInstance(types);
        processDevice(getDeviceReference(hd), dvcTypes);
    }

    private void processDevice(DeviceReference devRef, QNameSet dvcTypes)
    {
        if ((dvcTypes != null) && (!dvcTypes.isEmpty())) {
            StringBuilder sb = new StringBuilder("Search Parameter was...\nDeviceTypes:");
            Iterator iterator = dvcTypes.iterator();

            while (iterator.hasNext()) {
                sb.append(iterator.next());
            }

            logger.debug(sb.toString());
        }

        Device device = null;
        ServiceReference svcRef = null;

        try {
            device = devRef.getDevice();
            Iterator services = device.getServiceReferences();
            while (services.hasNext()) {
                svcRef = (ServiceReference) services.next();
                logger.info("ServiceId " + svcRef.getServiceId());

                if (membershipService == null) {
                    Iterator portTypes = svcRef.getPortTypes();

                    while ((membershipService == null) && (portTypes.hasNext())) {
                        QName portType = (QName) portTypes.next();
                        logger.info("PortType: " + portType + " equals " + Constants.MembershipPortTypeQName + "? " + portType.equals(Constants.MembershipPortTypeQName));
                        if (portType.equals(Constants.MembershipPortTypeQName)) {
                            membershipService = svcRef.getService();
                            logger.info("MembershipService has been set.");
                        }
                    }
                }
            }

            if (membershipService != null) {
                logger.info("Invoking GetTargets on the MembershipService...");
                invokeGetTargetsOperation();
            }
        } catch (TimeoutException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void deviceFound(DeviceReference devRef, SearchParameter searchParams) {
        logger.info("Found Device with Reference " + devRef.getEndpointReference());

        processDevice(devRef, searchParams.getDeviceTypes());
    }

    @Override
    public void serviceFound(ServiceReference serviceRef, SearchParameter sp) {
        try {
            logger.info("Found Service with Id " + serviceRef.getServiceId());
            // services
            QNameSet svcTypes = sp.getServiceTypes();

            if ((svcTypes.contains(Constants.MembershipPortTypeQName)) && (membershipService == null)) {
                membershipService = serviceRef.getService();

                if (membershipService != null) {
                    logger.info("Invoking GetTargets on the MembershipService...");
                    // get targets and put them in gossip services list
                    try {
                        invokeGetTargetsOperation();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        } catch (TimeoutException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private ParameterValue buildPushInvocation() {
//        Operation pushOp = gossipService.getAnyOperation(Constants.GossipPushPortQName, "Push");
        Operation pushOp = new PushOperation();
        ParameterValue input = pushOp.createInputValue();
        String containerPrefix = Constants.MessagesListElementName + "/" + Constants.MessageContainerElementName + "[0]/";
        input.setValue(containerPrefix + Constants.ActionElementName, "MyAction");
        input.setValue(containerPrefix + Constants.MessageIdentifierElementName, "Msg0");
        input.setValue(containerPrefix + Constants.RoundsElementName, "5");
        String value = "<n2:InfoTemp xmlns:n2=\"http://gsd.di.uminho.pt/example/\"><n2:TempValue>45.8</n2:TempValue></n2:InfoTemp>";
        input.setValue(containerPrefix + Constants.MessageElementName, value);
//        input.setValue(containerPrefix + Constants.MessageElementName, "<Message>MyMessageWillBeHere</Message>");

        return input;
    }

    private void invokePushOperation(Service gossipService, ParameterValue input) {
        try {
            Operation pushOp = gossipService.getAnyOperation(Constants.GossipPushPortQName, "Push");

            pushOp.invoke(input);
            logger.info("Push Op invoked!");
        } catch (InvocationException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (TimeoutException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    protected void invokeGetTargetsOperation() {

        Operation getTargetsOp = membershipService.getAnyOperation(Constants.MembershipPortTypeQName, "GetTargets");

        if (getTargetsOp != null) {
            ParameterValue input = getTargetsOp.createInputValue();

            input.setValue(Constants.ServiceTypeElementName, Constants.GossipPushPortQName.getNamespace() + "/" + Constants.GossipPushPortQName.getLocalPart());
            input.setValue(Constants.FanoutElementName, String.valueOf(3));
            input.setValue(Constants.SvcEprElementName, "http://fakeGossipService");

            logger.debug("GetTargets invocation parameters set!");

            ParameterValue returnMessagePV = null;
            try {
                returnMessagePV = getTargetsOp.invoke(input);
                logger.info("GetTargets Op invoked!");

            } catch (InvocationException e) {
                logger.error(e.getMessage(), e);
            } catch (TimeoutException e) {
                logger.error(e.getMessage(), e);
            }

            if (returnMessagePV != null) {
                Iterator targets = returnMessagePV.getChildren("TargetsList");

                if (targets.hasNext()) {
                    ParameterValue targetsList = (ParameterValue) targets.next();

                    int num = targetsList.getChildrenCount("Endpoint");
                    logger.debug("TargetsList size " + num);
                    if (num > 0) {
                        try {
                            String firstTarget = returnMessagePV.getValue("TargetsList/Endpoint[0]");
                            logger.debug("GetTargets returned " + firstTarget);
                            EndpointReference targetEPR = new EndpointReference(new URI(firstTarget));
                            ServiceReference svcRef = getServiceReference(targetEPR);
                            invokePushOperation(svcRef.getService(), buildPushInvocation());
                        } catch (TimeoutException ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    }
                }
                else
                {
                    logger.warn("Received empty response!");
                }
            }
        } else {
            logger.debug("Operation is still null!");
        }
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();

        DPWSFramework.start(args);
        Log.setLogLevel(Log.DEBUG_LEVEL_INFO);
        PushClient client = null;

        try {
            client = new PushClient();

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
