/*******************************************************************************
 * Copyright (c) 2014 Filipe Campos.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.uminho.di.gsd.common.client;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import net.sf.neem.impl.RandomSamples;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.common.RunConstants;
import org.uminho.di.gsd.common.device.BasicDevice;
import org.uminho.di.gsd.gossip.device.GossipDevice;
import org.uminho.di.gsd.gossip.service.repo.aggregation.AggregationMessage;
import org.uminho.di.gsd.gossip.client.workers.UdpServerSendingTask;
import org.ws4d.java.client.DefaultClient;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.client.SearchParameter;
import org.ws4d.java.communication.HTTPBinding;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.eventing.ClientSubscription;
import org.ws4d.java.eventing.EventSource;
import org.ws4d.java.eventing.EventingException;
import org.ws4d.java.service.LocalService;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;
import org.ws4d.java.communication.protocol.soap.generator.Message2SOAPGenerator;
import org.ws4d.java.types.ByteArrayBuffer;
import org.ws4d.java.communication.protocol.soap.SOAPoverUDPClient.SOAPoverUDPHandler;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.communication.protocol.soap.server.SOAPoverUDPServer;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.structures.Iterator;

public class BasicClient extends DefaultClient {

    static Logger logger = Logger.getLogger(BasicClient.class);

    protected FileWriter fw;
    protected FileWriter hopsFw;

    protected long fanout;
    protected List targets = null;
    protected List udpTargets = null;
    protected int numberOfTargets;
    protected String udpIp;
    protected int udpPort;
    protected SOAPoverUDPHandler handler;
    protected SOAPoverUDPServer udpServer;

    protected GossipDevice device;
    protected int iters = 0;
    protected AtomicLongArray sending;
    protected HashMap<Integer, AtomicLongArray> receiving;
    protected HashMap<Integer, AtomicIntegerArray> hops;

    protected int counter = 0;
    protected int messageCounter = 0;
    protected String period;
    protected long timeInterval;
    

    protected java.util.Random random;
    protected int[] shuffle;

    public BasicClient()
    {
        random = new Random();

        registerServiceListening();
    }
    
    public void setFanout(int f) {
        fanout = f;
    }

    public GossipDevice getDevice() {
        return device;
    }

    public boolean allMessages() {
        return messageCounter >= iters;
    }

    public void setIters(int iters) {
        this.iters = iters;

        sending = new AtomicLongArray(iters);
        receiving = new HashMap<Integer, AtomicLongArray>();
        hops = new HashMap<Integer, AtomicIntegerArray>();
    }

    public void setTimeInterval(long timeInterval) {
        this.timeInterval = timeInterval;
    }

    public Long getPeriod()
    {
        return ((BasicDevice) device).getConstants().getTimeInterval();
    }

    public String getPeriodStr() {
        if (period == null) {
            period = Long.toString(((BasicDevice) device).getConstants().getTimeInterval());
        }

        return period;
    }

    public void setFileWriters(FileWriter fw, FileWriter hFw) {
        this.fw = fw;
        this.hopsFw = hFw;
    }

    @Override
    public void deviceChanged(DeviceReference deviceRef) {
        
    }

    @Override
    public void serviceChanged(ServiceReference serviceRef) {
        super.serviceChanged(serviceRef);
    }

    @Override
    public void serviceCreated(ServiceReference serviceRef) {
        super.serviceCreated(serviceRef);
    }

    @Override
    public void serviceDisposed(ServiceReference serviceRef) {
        super.serviceDisposed(serviceRef);
    }

    @Override
    public void serviceFound(ServiceReference servRef, SearchParameter search) {
        super.serviceFound(servRef, search);
    }

    public Operation getOperationFromService(String opName, QName port, URI svcEPR) {
        Operation operation = null;

        ServiceReference svcRef = getServiceReference(new EndpointReference(svcEPR));
        try {
            if (svcRef.getLocation() == ServiceReference.LOCATION_LOCAL) {
                LocalService service = (LocalService) svcRef.getService();

                operation = getOperationFromService(opName, port, service);
            } else {
                Service service = svcRef.getService();

                operation = getOperationFromService(opName, port, service);
            }


        } catch (TimeoutException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return operation;
    }

    public Operation getOperationFromService(String operationName, QName portQName, Service svc) {
        return svc.getAnyOperation(portQName, operationName);
    }

    public List getTargetsUris(List eprs) {
        List ret = new ArrayList();

        int size = eprs.size();

        for (int i = 0; i < size; i++) {
            URI uri = new URI((String) eprs.get(i));
            ret.add(uri);
        }

        return ret;
    }

    public List getTargets(List eprs) {
        List ret = new ArrayList();

        int size = eprs.size();

        String epr = "none";
        try {
            for (int i = 0; i < size; i++) {
                epr = (String) eprs.get(i);
                logger.debug("Trying to get " + epr + " service object!");
                URI uri = new URI(epr);
                Service svc = getServiceReference(new EndpointReference(uri)).getService();
                ret.add(svc);
                logger.debug("Got " + epr + " service object!");
                logger.info(size + " targets were set!");
            }
        } catch (TimeoutException ex) {
            logger.error(this.device.getIdStr() + " timeouted! Failed to get " + epr);
        }


        return ret;
    }

    public String getStats() {
        String ret = "";

        StringBuilder sb = new StringBuilder();
        StringBuilder hopsSB = new StringBuilder();

        if (counter > 0) {
            // write sending times
            sb.append("Sending;");
            sb.append(device.getGossipService().getSvcEPR());
            sb.append(';');
            for (int i = 0; i < iters; i++) {
                sb.append(sending.get(i));
                sb.append(';');
            }

            sb.append("\n");
        } else {
            // write receiving times
            sb.append("Receiving;");
            sb.append(device.getGossipService().getSvcEPR());
            sb.append(';');

            hopsSB.append("Receiving;");
            hopsSB.append(device.getGossipService().getSvcEPR());
            hopsSB.append(';');

            TreeSet<Integer> keys = new TreeSet<Integer>(receiving.keySet());

            if (keys.isEmpty()) {
                logger.error("Receiving is empty for " + device.getGossipService().getSvcEPR());
            } else {
                for (Integer p : keys) {
                    AtomicLongArray array = receiving.get(p);
                    AtomicIntegerArray hopsArray = hops.get(p);
                    logger.debug("Rec[" + p + "] has " + array.length() + " elements.");
                    for (int i = 0; i < iters; i++) {
                        logger.debug("Rec[" + p + "][" + i + "]=" + array.get(i));
                        sb.append(array.get(i));
                        sb.append(';');
                        hopsSB.append(hopsArray.get(i));
                        hopsSB.append(';');
                    }
                }
            }
            sb.append("\n");
            hopsSB.append("\n");
        }

        ret = sb.toString();
        return ret;
    }

    protected void writeStatsToFile(FileWriter file, FileWriter hopsFile) {
        StringBuilder sb = new StringBuilder();
        StringBuilder hopsSB = new StringBuilder();

        if (counter > 0) {
            // write sending times
            sb.append("Sending;");
            sb.append(device.getGossipService().getSvcEPR());
            sb.append(';');
            for (int i = 0; i < iters; i++) {
                sb.append(sending.get(i));
                sb.append(';');
            }

            sb.append("\n");
        } else {
            // write receiving times
            sb.append("Receiving;");
            sb.append(device.getGossipService().getSvcEPR());
            sb.append(';');

            hopsSB.append("Receiving;");
            hopsSB.append(device.getGossipService().getSvcEPR());
            hopsSB.append(';');

            TreeSet<Integer> keys = new TreeSet<Integer>(receiving.keySet());

            if (keys.isEmpty()) {
                logger.error("Receiving is empty for " + device.getGossipService().getSvcEPR());
            } else {
                for (Integer p : keys) {
                    AtomicLongArray array = receiving.get(p);
                    AtomicIntegerArray hopsArray = hops.get(p);
                    logger.debug("Rec[" + p + "] has " + array.length() + " elements.");
                    for (int i = 0; i < iters; i++) {
                        logger.debug("Rec[" + p + "][" + i + "]=" + array.get(i));
                        sb.append(array.get(i));
                        sb.append(';');
                        hopsSB.append(hopsArray.get(i));
                        hopsSB.append(';');
                    }
                }
            }
            sb.append("\n");
            hopsSB.append("\n");
        }

        try {
            file.append(sb.toString());
            file.flush();

            hopsFile.append(hopsSB.toString());
            hopsFile.flush();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public void writeStats() {
        writeStatsToFile(fw, hopsFw);
    }

    public void subscribeToStartWorkers() {
        //
        RunConstants constants = ((BasicDevice) device).getConstants();

        // running on the same machine
        String producerIp = constants.getIp();
        if (constants.isSimulated()) {
            producerIp = "10.0.0.2";
        }
        String uri = "http://" + producerIp + ":" + constants.getBasePort() + "/management";
        URI firstProducerUri = new URI(uri);
        ServiceReference svcRef = getServiceReference(new EndpointReference(firstProducerUri));

        try {
            logger.debug(device.getIdStr() + ": Getting service " + uri + " ...");
            Service firstProducer = svcRef.getService();
            logger.debug(device.getIdStr() + ": Got service " + uri + ".");
            EventSource startWorkersNot = firstProducer.getAnyEventSource(Constants.ManagementPortQName, Constants.StartWorkersNotificationName);

            DataStructure bindings = new ArrayList(1);
            bindings.add(new HTTPBinding(constants.getIp(), constants.getPort(), "/StartWorkersEventSink"));

            ClientSubscription clientSubs = startWorkersNot.subscribe(this, 0, bindings);

            logger.debug(device.getIdStr() + ": Subscribed StartWorkersNotification from " + uri + " and status is " + clientSubs.getStatus());
        } catch (EventingException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (TimeoutException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public ParameterValue eventReceived(ClientSubscription subscription, URI actionURI, ParameterValue parameterValue) {
        long now = System.nanoTime();

        logger.info("Received event " + parameterValue);

        // start workers
        device.initializeWorkers();

        long time = System.nanoTime() - now;
        logger.info("Returning took " + time + "ns... Started workers!");

        return null;
    }

    public void invokeTcpAction(String operationName, QName portQName, ParameterValue pv, List targets) {

        if ((targets == null) || (targets.isEmpty())) {
            targets = getTargets();
        }

        if ((targets != null) && (!targets.isEmpty())) {
            // initialize message
            Iterator svcs = targets.iterator();

            // get operation and invoke for each service
            while (svcs.hasNext()) {
                Service svc = (Service) svcs.next();
                try {
                    Operation remoteOp = getOperationFromService(operationName, portQName, svc);
                    remoteOp.invoke(pv);
                    logger.info(portQName + ":" + operationName + " Op invoked!");
                } catch (InvocationException ex) {
                    logger.error(ex.getMessage(), ex);
                } catch (TimeoutException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }
    }

    public void invokeUdpAction(ParameterValue pv, SOAPHeader requestHeader, List targets, String action) {
        logger.info("UDP invoking " + action);
        List currentTargets = null;

        if ((targets != null) && (!targets.isEmpty())) {
            currentTargets = getTargetsUris(targets);
        } else {
            currentTargets = getUdpTargets();
        }


        if ((currentTargets != null) && (!currentTargets.isEmpty())) {
            // initialize message
            // creating SOAPHeader to set action and replyTo fields
            SOAPHeader soapHeader = SOAPHeader.createRequestHeader(action);
            URI svcURI = new URI(device.getGossipService().getSvcEPR());
            EndpointReference myEPR = new EndpointReference(svcURI);
            soapHeader.setReplyTo(myEPR);
            InvokeMessage msg = new InvokeMessage(soapHeader);
            msg.setContent(pv);
            msg.setInbound(false);
            msg.setTargetAddress(svcURI);

            if(requestHeader != null)
            {
                msg.setResponseTo(requestHeader);
            }

            sendUDPMessages(device.getIdStr(), udpServer, msg, currentTargets);
        }
    }

    private List selectTargets(List sourceTargets) {
        List svcEPRs = null;

        if (sourceTargets != null) {
            int size = sourceTargets.size();
            int f = (int) fanout;
            logger.debug("Has " + size + " possible targets. Selecting " + f + "...");


            if (size <= f) {
                svcEPRs = new ArrayList(sourceTargets);
            } else {
                /// random selection
                RandomSamples.uniformSample(f, shuffle, random);
                svcEPRs = new ArrayList(f);
                for (int i = 0; i < f; i++) {
                    svcEPRs.add(sourceTargets.get(shuffle[i]));
                }
            }
        }

        return svcEPRs;
    }

    protected List getTargets() {
        return selectTargets(targets);
    }

    protected List getUdpTargets() {
        return selectTargets(udpTargets);
    }

    protected long sendUDPMessages(String devId, SOAPoverUDPServer sender, InvokeMessage msg, List tempTargets) {
        int size = tempTargets.size();

        long ret = -1;

        ByteArrayBuffer b = null;
        try {
            b = Message2SOAPGenerator.generateSOAPMessage(msg);
        } catch (IOException ex) {
            logger.error(devId + ex.getMessage(), ex);
        }

        if (b != null) {
            byte[] data = b.getBuffer();
            int len = b.getContentLength();
            logger.debug(devId + " Gonna unicast " + msg.getContent());

            UdpServerSendingTask[] tasks = new UdpServerSendingTask[size];

            for (int i = 0; i < size; i++) {
                URI uri = (URI) tempTargets.get(i);
                tasks[i] = new UdpServerSendingTask(sender, uri.getHost(), uri.getPort(), data, len);
            }

            ret = System.nanoTime();
            for (int i = 0; i < size; i++) {
                DPWSFramework.getThreadPool().execute(tasks[i]);
            }
        } else {
            logger.error("Could not send messages! No targets!");
        }

        return ret;
    }

    // NOOPs
    public long invokeTcpAggPush(ParameterValue newPV, String srcEpr) {
        logger.error("Shouldn't invoke invokeAggPush on GossipClient!");

        return -1;
    }

    public long invokeUdpAggPush(ParameterValue newPV, String srcEpr) {
        logger.error("Shouldn't invoke invokeAggPush on GossipClient!");

        return -1;
    }

    public String invokeTcpAgg(ParameterValue pv, AggregationMessage agg, String srcEpr, int index) {
        logger.error("Shouldn't invoke invokeAgg on GossipClient!");
        return null;
    }

    public String invokeUdpAgg(ParameterValue pv, AggregationMessage agg, String srcEpr, int index) {
        logger.error("Shouldn't invoke invokeAgg on GossipClient!");
        return null;
    }

    public String invokeTcpAggPull(ParameterValue pv, AggregationMessage agg, String srcEpr, int index) {
        logger.error("Shouldn't invoke invokeAggPull on GossipClient!");
        return null;
    }

    public String invokeUdpAggPull(ParameterValue pv, AggregationMessage agg, URI sender, int index) {
        logger.error("Shouldn't invoke invokeAggPull on GossipClient!");
        return null;
    }
}
