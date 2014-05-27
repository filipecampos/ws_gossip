package org.uminho.di.gsd.gossip.client;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import net.sf.neem.impl.RandomSamples;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.uminho.di.gsd.common.CommunicationProtocol;
import org.uminho.di.gsd.common.Configuration;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.common.MessageUtil;
import org.uminho.di.gsd.common.OutSOAPoverUDPDatagramHandlerImpl;
import org.uminho.di.gsd.common.client.BasicClient;
import org.uminho.di.gsd.common.device.BasicDevice;
import org.uminho.di.gsd.common.device.TheDevice;
import org.uminho.di.gsd.gossip.GossipVariants;
import org.uminho.di.gsd.gossip.client.workers.ActionTask;
import org.uminho.di.gsd.gossip.client.workers.SendingTask;
import org.uminho.di.gsd.gossip.device.GossipDevice;
import org.uminho.di.gsd.gossip.service.operations.FetchOperation;
import org.uminho.di.gsd.gossip.service.operations.PullOperation;
import org.uminho.di.gsd.gossip.service.operations.PullIdsOperation;
import org.uminho.di.gsd.gossip.service.operations.PushIdsOperation;
import org.uminho.di.gsd.gossip.service.operations.PushOperation;
import org.uminho.di.gsd.gossip.service.operations.PushPullOperation;
import org.uminho.di.gsd.gossip.service.repo.Message;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.client.SearchParameter;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.communication.protocol.soap.SOAPoverUDPClient.SOAPoverUDPHandler;
import org.ws4d.java.communication.protocol.soap.generator.MessageReceiver;
import org.ws4d.java.communication.protocol.soap.server.SOAPoverUDPServer;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.structures.MessageIdBuffer;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.URI;

public class GossipClient extends BasicClient {

    protected ActionTask periodicTask;
    static Logger logger = Logger.getLogger(GossipClient.class);

    protected long targetSamplingCounter;
    // configuration parameters
    protected long maxRounds;
    protected long targetSamplingValue;
    protected String targetSamplingUnit;
    protected GossipVariants prefVariant;
    protected String monitoringService;
    protected int times = 0;

    protected PushOperation pushOp;
    protected PullOperation pullOp;
    protected PushPullOperation pushPullOp;
    protected PushIdsOperation pushIdsOp;
    protected PullIdsOperation pullIdsOp;
    protected FetchOperation fetchOp;

    public GossipClient(GossipDevice dvc) {
        device = dvc;
        targetSamplingCounter = 0;

        udpIp = ((BasicDevice) device).getIp();
        udpPort = Integer.parseInt(((BasicDevice) device).getPort());

        registerServiceListening();
    }

    /* getters and setters */
    public void setPeriodicTask(ActionTask periodicTask) {
        this.periodicTask = periodicTask;
    }

    public PushOperation getPushOp() {
        if (pushOp == null) {
            pushOp = device.getGossipService().getPushOperation();
        }

        return pushOp;
    }

    public PullOperation getPullOp() {
        if (pullOp == null) {
            pullOp = device.getGossipService().getPullOperation();
        }

        return pullOp;
    }

    public PushPullOperation getPushPullOp() {
        if (pushPullOp == null) {
            pushPullOp = device.getGossipService().getPushPullOperation();
        }

        return pushPullOp;
    }

    public PullIdsOperation getPullIdsOp() {
        if (pullIdsOp == null) {
            pullIdsOp = device.getGossipService().getPullIdsOperation();
        }

        return pullIdsOp;
    }

    public PushIdsOperation getPushIdsOp() {
        if (pushIdsOp == null) {
            pushIdsOp = device.getGossipService().getPushIdsOperation();
        }

        return pushIdsOp;
    }

    public FetchOperation getFetchOp() {
        if (fetchOp == null) {
            fetchOp = device.getGossipService().getFetchOperation();
        }

        return fetchOp;
    }

    public boolean setReceivedHops(int port, int msgId, int num) {
        boolean duplicate = false;

        // get array
        AtomicIntegerArray array = hops.get(port);

        // create it if doesn't exist
        if (array == null) {
            array = new AtomicIntegerArray(iters);
            hops.put(port, array);
        }

        // compute actual number of hops
        int rounds = ((int) maxRounds) - num;

        if (logger.isDebugEnabled()) {
            logger.debug("Array Hops[" + msgId + "]=" + array.get(msgId) + " Got " + rounds + " hops to set.");
        }

        duplicate = !array.compareAndSet(msgId, 0, rounds);

        if (sending.get(msgId) != 0) {
            duplicate = true;
        }

        logger.debug("me: " + device.getIdStr() + "; port: " + port + "; msgId: " + msgId + "; num: " + num + "; rounds: " + rounds + "; maxRounds: " + maxRounds);

        if (logger.isDebugEnabled()) {
            logger.debug("Array Hops[" + msgId + "]=" + array.get(msgId));
        }

        return duplicate;
    }

    public boolean setReceived(int port, int msgId, long time) {
        boolean duplicate = false;
        AtomicLongArray array = receiving.get(port);

        if (array == null) {
            array = new AtomicLongArray(iters);
            receiving.put(port, array);
        } else {
            if (array.get(msgId) != 0) {
                duplicate = true;
            } else {
                messageCounter++;
            }
        }

        array.compareAndSet(msgId, 0, time);

        if (array.get(msgId) > time) {
            array.set(msgId, time);
            logger.debug("Received message " + msgId + " at " + time);
        } else {
            logger.debug("Received duplicate message " + msgId + " at " + time);
        }

        if (sending.get(msgId) != 0) {
            duplicate = true;
        }

        return duplicate;
    }

    public void setSent(int msgId, long time) {
        sending.compareAndSet(msgId, 0, time);

        if (sending.get(msgId) < time) {
            sending.set(msgId, time);
        }

        counter++;
    }

    public void setUdpUnicastServer(SOAPoverUDPServer udpServer) {
        this.udpServer = udpServer;
    }

    public void setTargets(String[] eprs) {
        int size = eprs.length;
        logger.debug("Setting " + size + " targets...");
        if (size > 0) {
            targets = new ArrayList(size);
            udpTargets = new ArrayList(size);

            int i = 0;
            try {
                for (; i < size; i++) {
                    URI uri = new URI(eprs[i]);
                    udpTargets.add(uri);
                    logger.debug("Setting uri " + i + "=" + uri);
                    Service svc = getServiceReference(new EndpointReference(uri)).getService();
                    Thread.sleep(500);
                    targets.add(svc);
                }
                logger.info(size + " targets were set!");
            } catch (TimeoutException ex) {
                logger.error("Timeouted getting service " + i + "..." + ex.getMessage(), ex);
            } catch (InterruptedException ex) {
                logger.error("Interrupted while sleeping after getting service " + i + "..." + ex.getMessage(), ex);
            }

            logger.debug("Started shuffle array with " + size + " positions.");
            shuffle = RandomSamples.mkUniverse(size);
        }
    }

    public void readConfiguration() {
        if (device != null) {
            fanout = (Long) Configuration.getConfigParamValue(Configuration.fanout);
            maxRounds = (Long) Configuration.getConfigParamValue(Configuration.maxRounds);
            logger.debug("MaxRounds = " + maxRounds);
            targetSamplingValue = (Long) Configuration.getConfigParamValue(Configuration.targetSamplingValue);
            // TODO if ms set alarm to retrieve targets
            targetSamplingUnit = (String) Configuration.getConfigParamValue(Configuration.targetSamplingUnit);

            prefVariant = (GossipVariants) Configuration.getConfigParamValue(Configuration.prefVariant);
            monitoringService = (String) Configuration.getConfigParamValue(Configuration.monitoringService);
        } else {
            device = new TheDevice();
            monitoringService = "first";
        }
    }

    public void initializeMonitoringService() {
        logger.info("Invoking initializeMonitoringService..." + (++times));
        if ((monitoringService != null) && (!monitoringService.isEmpty())) {
            if (monitoringService.equalsIgnoreCase("first")) {
                // search membership service
                searchMembershipService();
            } else {
                // Membership URI provided
                try {
                    EndpointReference epr = new EndpointReference(new URI(monitoringService));
                    ServiceReference svcRef = getServiceReference(epr);
                    device.setMonitoringMembershipService(svcRef.getService());
                } catch (TimeoutException ex) {
                    logger.error(ex.getMessage(), ex);
                    monitoringService = "first";
                    initializeMonitoringService();
                }

                if (device.getMonitoringMembershipService() == null) {
                    monitoringService = "first";
                    initializeMonitoringService();
                } else {
                    if ((targets == null) || (targets.isEmpty())) {
                        retrieveTargets();
                    }
                }
            }
        }
    }

    public void initializeUdpClient() {
        MessageIdBuffer mIdBuf = new MessageIdBuffer();
        MessageReceiver mr = new OutSOAPoverUDPDatagramHandlerImpl(mIdBuf);

        handler = new SOAPoverUDPHandler(mr, mIdBuf);

        udpIp = ((BasicDevice) device).getIp();
    }

    protected void checkTargetSamplingCounter() {
        if (targetSamplingCounter == targetSamplingValue) {
            targetSamplingCounter = 0;
        }
    }

    public void retrieveTargets() {
        logger.debug(device.getIdStr() + " Client initializing targets list...");
        Service membershipService = device.getMonitoringMembershipService();
        if (membershipService == null) {
            logger.debug("retrieveTargets: MembershipService is null");
            initializeMonitoringService();
        } else {
            logger.debug("retrieveTargets: MembershipService ok");
            Operation getTargetsOp = membershipService.getAnyOperation(Constants.MembershipPortTypeQName, Constants.GetTargetsOperationName);

            ParameterValue input = getTargetsOp.createInputValue();

            String svcEPR = null;
            if (device.getGossipService() != null) {
                svcEPR = device.getGossipService().getSvcEPR();
            }

            if (svcEPR == null) {
                logger.error("No EPR for Gossip Service!");
            } else {
                input.setValue(Constants.ServiceTypeElementName, Constants.NameSpace + "/" + Constants.GossipPushPortName);
                input.setValue(Constants.SvcEprElementName, svcEPR);
                input.setValue(Constants.FanoutElementName, Long.toString(fanout));

                logger.debug("Invoking GetTargets:SvcType=" + Constants.NameSpace + "/" + Constants.GossipPushPortName
                        + ";SvcEPR=" + svcEPR
                        + ";Fanout=" + fanout);

                try {
                    ParameterValue output = getTargetsOp.invoke(input);

                    if (output != null) {
                        String basePrefix = Constants.TargetsListElementName + "/" + Constants.EndpointElementName;
                        int num = output.getChildrenCount(basePrefix);

                        if (num > 0) {
                            targets = new ArrayList(num);
                            udpTargets = new ArrayList(num);
                            for (int i = 0; i < num; i++) {
                                String prefix = basePrefix + "[" + i + "]";
                                String epr = output.getValue(prefix);
                                logger.debug("Adding new target" + prefix + " = " + epr);
                                URI uri = new URI(epr);
                                udpTargets.add(uri);
                                Service svc = getServiceReference(new EndpointReference(uri)).getService();
                                targets.add(svc);
                            }
                        } else {
                            logger.warn("Received empty response!");
                        }
                    }
                } catch (InvocationException ex) {
                    logger.error(device.getIdStr() + ex.getMessage(), ex);
                } catch (TimeoutException ex) {
                    logger.error(device.getIdStr() + ex.getMessage(), ex);
                }
                if ((targets != null) && (!targets.isEmpty())) {
                    FileWriter fw = ((BasicDevice) device).getFileWriter();
                    if (fw != null) {
                        try {
                            fw.write(device.getIdStr() + " has targets\n");
                            fw.flush();
                        } catch (IOException ex) {
                            logger.error(device.getIdStr() + ex.getMessage(), ex);
                        }
                    }
                }
            }
        }
    }

    protected void searchMembershipService() {
        logger.debug("Searching for MembershipService...");

        SearchParameter parameter = new SearchParameter();
        if (device == null) {
            parameter.setServiceTypes(new QNameSet(Constants.MembershipPortTypeQName));
            searchService(parameter);
        } else {
            searchDevice(parameter);
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void fireAction(GossipVariants variant) {
        CommunicationProtocol proto = CommunicationProtocol.TCP;

        if (udpServer != null) {
            proto = CommunicationProtocol.UDP;
        }

        switch (variant) {
            case Pull:
                invokePull(proto, null);
                break;
            case Push_Pull:
                invokePushPull(proto);
                break;
            case Lazy_Push:
                invokePushIds(proto);
                break;
            case Two_Phase_Pull:
                invokePullIds(proto);
                break;
        }
        logger.debug("GossipClient firing action " + variant);
    }

    public void fireInfoTempPush(double value) {
        String element = "<as:InfoTemp xmlns:as=\"http://gsd.di.uminho.pt/example/\"><as:TempValue>" + value + "</as:TempValue></as:InfoTemp>";
        Message msg = new Message(new URI(udpPort + "-" + Long.toString(counter)), new URI(ApplicationServiceConstants.infoTempOpName), maxRounds, element, System.currentTimeMillis());

        long currentTime = firePush(msg);

        sending.set(counter, currentTime);
        logger.debug("Sent notification " + counter + " at " + currentTime);

        counter++;
    }

    public long firePush(Message msg) {
        List msgs = new ArrayList(1);

        msgs.add(msg);

        long currentTime = -1;
        CommunicationProtocol proto = CommunicationProtocol.TCP;

        if (udpServer != null) {
            proto = CommunicationProtocol.UDP;
        }

        currentTime = invokePush(proto, null, null, msgs, null);

        return currentTime;
    }

    public long invokePush(CommunicationProtocol proto, List svcEPRs, URI sender, List to_send, SOAPHeader requestHeader) {
        long ret = -1;
        List tempTargets = null;

        if (svcEPRs == null) {
            // no targets set
            if (targets == null) {
                logger.debug(device.getIdStr() + " Client initializing targets list...");
                retrieveTargets();
            }

            tempTargets = getTargets();
        }

        if (proto.equals(CommunicationProtocol.TCP)) {
            // TCP
            if ((tempTargets == null) && (svcEPRs != null)) {
                tempTargets = getTargets(svcEPRs);
            }

            if (tempTargets != null) {
                ret = invokeTcpPushOperationOnTargets(to_send, tempTargets);
            } else {
                logger.error("No targets to send messages to!");
            }
        } else {
            // UDP
            if ((tempTargets == null) && (svcEPRs != null)) {
                tempTargets = getTargetsUris(svcEPRs);
                logger.debug(device.getIdStr() + " Client got UDP Targets URIs.");
            } else {
                logger.debug(device.getIdStr() + " Client selecting UDP targets.");
                tempTargets = getUdpTargets();
            }

            // remove peer from whom messages were received
            if (sender != null) {
                for (int i = 0; i < tempTargets.size(); i++) {
                    URI target = (URI) tempTargets.get(i);
                    if (sender.equalsRFC3986(target) || sender.equalsSTRCMP0(target)) {
                        tempTargets.remove(i);
                    }
                }
            } else {
                logger.warn("Received messages from no sender! Producer perhaps? " + device.getIdStr());
            }

            if (tempTargets != null) {
                ret = invokeUdpPushOperationOnTargets(to_send, tempTargets, requestHeader);
            } else {
                logger.error("No targets to send messages to!");
            }
        }

        if (logger.isDebugEnabled()) {
            if ((to_send != null) && (!to_send.isEmpty())) {
                Message msg = (Message) to_send.get(0);

                Iterator iter = tempTargets.iterator();

                while (iter.hasNext()) {
                    Object obj = iter.next();
                    logger.debug("Device " + getDevice().getIdStr() + " sent message " + msg.getIdentifier() + " to " + obj);
                }
            }
        }

        return ret;
    }

    public long invokeTcpPushOperationOnTargets(List messages, List targetServices) {
        long ret = -1;

        ParameterValue message = getPushOp().createInputValue();

        int size = targetServices.size();
        SendingTask[] tasks = new SendingTask[size];

        for (int i = 0; i < size; i++) {
            message = MessageUtil.convertMessagesToPV(messages, message);
            message.setValue(Constants.SvcEprElementName, getDevice().getGossipService().getSvcEPR());
            logger.debug("Built message for target " + i + ": " + message);
            Service svc = (Service) targetServices.get(i);
            Operation currPushOp = getOperationFromService(Constants.PushOperationName, Constants.GossipPushPortQName, svc);
            String str = device.getIdStr() + "Time:" + i + " Client invoked Push Op in " + svc + " for msg " + message;
            tasks[i] = new SendingTask(currPushOp, message, str);
        }

        ret = System.nanoTime();
        for (int i = 0; i < size; i++) {
            DPWSFramework.getThreadPool().execute(tasks[i]);
        }

        logger.debug(device.getIdStr() + " Sent message " + message + " at " + ret);

        return ret;
    }

    public void invokePull(CommunicationProtocol proto, List svcEPRs) {
        ParameterValue pv = null;

        // build pv
        pv = getPullOp().createInputValue();
        // time interval
        pv.setAttributeValue(Constants.MillisecondsAttributeName, Constants.TimeIntervalElementName, getPeriodStr());

        // rounds
//            pv.setValue(Constants.RoundsElementName, "" + maxRounds);


        if (proto.equals(CommunicationProtocol.TCP)) {
            // uri
            pv.setValue(Constants.SvcEprElementName, device.getGossipService().getSvcEPR());
            invokeTcpAction(Constants.PullOperationName, Constants.GossipPullPortQName, pv, svcEPRs);
        } else {
            String action = Constants.NameSpace + "/" + Constants.PullOperationName;
            invokeUdpAction(pv, null, svcEPRs, action);
        }
    }

    public void invokePushPull(CommunicationProtocol proto) {
        ParameterValue pv = null;

        // build pv
        pv = getPushPullOp().createInputValue();
        // time interval
        pv.setAttributeValue(Constants.MillisecondsAttributeName, Constants.TimeIntervalElementName, getPeriodStr());

        // get recent messages and insert them into pv
        long now = System.currentTimeMillis();
        List messages = getPushPullOp().getRecentMessages(now, pv);

        pv = MessageUtil.convertMessagesToPV(messages, pv);

        if (proto.equals(CommunicationProtocol.TCP)) {
            // uri
            pv.setValue(Constants.SvcEprElementName, device.getGossipService().getSvcEPR());
            invokeTcpAction(Constants.PushPullOperationName, Constants.GossipPullPortQName, pv, null);
        } else {
            String action = Constants.NameSpace + "/" + Constants.PushPullOperationName;
            invokeUdpAction(pv, null, null, action);
        }
    }

    public void invokePushIds(CommunicationProtocol proto) {
        ParameterValue pv = null;

        // build pv
        pv = getPushIdsOp().createInputValue();

        // get recent messages and insert them into pv
        long now = System.currentTimeMillis();
        List messages = getPushIdsOp().getRecentMessages(now, getPeriod());

        if((messages != null) && (!messages.isEmpty()))
        {
            if (proto.equals(CommunicationProtocol.TCP)) {
                pv = MessageUtil.convertMessagesInfoToPV(messages, pv, device.getGossipService().getSvcEPR());
                invokeTcpAction(Constants.PushIdsOperationName, Constants.GossipLazyPortQName, pv, null);
            } else {
                pv = MessageUtil.convertMessagesInfoToPV(messages, pv, null);
                String action = Constants.NameSpace + "/" + Constants.PushIdsOperationName;
                invokeUdpAction(pv, null, null, action);
            }
        }
    }

    public void invokePullIds(CommunicationProtocol proto) {
        ParameterValue pv = null;
        pv = getPullIdsOp().createInputValue();
        // time interval
        pv.setAttributeValue(Constants.MillisecondsAttributeName, Constants.TimeIntervalElementName, getPeriodStr());
        if (proto.equals(CommunicationProtocol.TCP)) {
            // uri
            pv.setValue(Constants.SvcEprElementName, device.getGossipService().getSvcEPR());
            invokeTcpAction(Constants.PullIdsOperationName, Constants.GossipLazyPortQName, pv, null);
        } else {
            String action = Constants.NameSpace + "/" + Constants.PullIdsOperationName;
            invokeUdpAction(pv, null, null, action);
        }
    }

    public long invokeRawPush(ParameterValue pv) {
        long ret = -1;

        List svcEPRs = getTargets();
        logger.debug(device.getIdStr() + "Client going to invoke Push Op in " + svcEPRs.size() + " services for msg " + pv);

        if ((svcEPRs != null) && (!svcEPRs.isEmpty())) {
            int size = svcEPRs.size();
            Operation currPushOp = null;
            String msg;
            SendingTask[] tasks = new SendingTask[size];

            for (int i = 0; i < size; i++) {
                currPushOp = getOperationFromService(Constants.PushOperationName, Constants.GossipPushPortQName, (Service) svcEPRs.get(i));
                msg = device.getIdStr() + "Time:" + i + " Client invoked Push Op in " + svcEPRs.get(i) + " for msg " + pv;
                tasks[i] = new SendingTask(currPushOp, pv, msg);
            }

            ret = System.nanoTime();
            for (int i = 0; i < size; i++) {
                DPWSFramework.getThreadPool().execute(tasks[i]);
            }

        }

        return ret;
    }

    private long invokeUdpPushOperationOnTargets(List messages, List tempTargets, SOAPHeader requestHeader) {
        long ret = -1;

        int size = messages.size();

        if (size > 4) {

            int i = 0;
            for (int j = 3; j < size; i += 4, j += 4) {
                List tempList = messages.subList(i, j);

                long temp = createAndInvokeUDPPush(tempList, requestHeader, tempTargets);

                if (ret == -1) {
                    ret = temp;
                }
            }

            if (i < size) {
                List tempList = messages.subList(i, size - 1);
                createAndInvokeUDPPush(tempList, requestHeader, tempTargets);
            }
        } else {
            ret = createAndInvokeUDPPush(messages, requestHeader, tempTargets);
        }

        return ret;
    }

    private long createAndInvokeUDPPush(List messages, SOAPHeader requestHeader, List targets) {
        // initialize message
        String action = Constants.NameSpace + "/" + Constants.PushOperationName;
        // creating SOAPHeader to set action and replyTo fields
        SOAPHeader soapHeader = SOAPHeader.createRequestHeader(action);
        URI svcURI = new URI(device.getGossipService().getSvcEPR());
        EndpointReference myEPR = new EndpointReference(svcURI);
        soapHeader.setReplyTo(myEPR);
        soapHeader.setEndpointReference(myEPR);
        InvokeMessage msg = new InvokeMessage(soapHeader);

        ParameterValue pv = getPushOp().createInputValue();
        pv = MessageUtil.convertMessagesToPV(messages, pv);

        msg.setContent(pv);
        msg.setInbound(false);

        msg = MessageUtil.setMessageResponseTo(msg, requestHeader);

        return sendUDPMessages(device.getIdStr(), udpServer, msg, targets);
    }

    public void respondToPullIds(CommunicationProtocol proto, SOAPHeader requestHeader, List messages, String svcEPR, URI destSvcEPR) {
        if(proto.equals(CommunicationProtocol.TCP))
        {
            try {
                Operation tempPushIdsOp = getOperationFromService(Constants.PushIdsOperationName, Constants.GossipLazyPortQName, destSvcEPR);
                if (tempPushIdsOp != null) {
                    ParameterValue response = tempPushIdsOp.createInputValue();
                    response = MessageUtil.convertMessagesInfoToPV(messages, response, svcEPR);
                    tempPushIdsOp.invoke(response);
                    logger.info("PushIds Op invoked in response to PullIds!");
                }
            } catch (TimeoutException ex) {
                logger.error(ex.getMessage(), ex);
            } catch (InvocationException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        else if(proto.equals(CommunicationProtocol.UDP))
        {
            Operation tempPushIdsOp = getPushIdsOp();
            if (tempPushIdsOp != null) {
                ParameterValue response = tempPushIdsOp.createInputValue();
                response = MessageUtil.convertMessagesInfoToPV(messages, response, null);
                List dests = new ArrayList();
                dests.add(destSvcEPR.toString());
                String action = Constants.NameSpace + "/" + Constants.PushIdsOperationName;
                invokeUdpAction(response, requestHeader, dests, action);
                logger.info("PushIds Op invoked in response to PullIds!");
            }
        }
    }

    public void respondToPushIds(CommunicationProtocol proto, SOAPHeader requestHeader, List ids, String svcEPR, URI destSvcEPR) {
        if(proto.equals(CommunicationProtocol.TCP))
        {
            try {
                Operation tempFetchOp = getOperationFromService(Constants.FetchOperationName, Constants.GossipLazyPortQName, destSvcEPR);
                if (tempFetchOp != null) {
                    ParameterValue response = tempFetchOp.createInputValue();
                    response = MessageUtil.convertIdentifiersToPV(ids, response, svcEPR);
                    tempFetchOp.invoke(response);
                    logger.info("Fetch Op invoked in response to PushIds!");
                }
            } catch (TimeoutException ex) {
                logger.error(ex.getMessage(), ex);
            } catch (InvocationException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        else if(proto.equals(CommunicationProtocol.UDP))
        {
            Operation tempFetchOp = getFetchOp();
            if (tempFetchOp != null) {
                ParameterValue response = tempFetchOp.createInputValue();
                response = MessageUtil.convertIdentifiersToPV(ids, response, null);
                List dests = new ArrayList();
                dests.add(destSvcEPR.toString());
                String action = Constants.NameSpace + "/" + Constants.FetchOperationName;
                invokeUdpAction(response, requestHeader, dests, action);
                logger.info("Fetch Op invoked in response to PushIds!");
            }
        }
    }

    public static void main(String[] args) {
        // always start the framework first
        DPWSFramework.start(args);

        GossipClient client = new GossipClient(null);
        client.readConfiguration();
        client.initializeMonitoringService();
    }

    public void startPushGossipDissemination() {

        double value = 543.12d;

        String idStr = device.getIdStr();

        logger.info(idStr + ":Gonna fire " + iters + " notifications with a period of "
                + timeInterval + " ms");


        try {
            for (int i = 0; i < iters; i++) {
                logger.debug(idStr + ":Firing notification number " + i + "...");
                fireInfoTempPush(value);
                logger.info(idStr + ":Fired notification number " + i + "!");
                Thread.sleep(timeInterval);
            }
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    
}
