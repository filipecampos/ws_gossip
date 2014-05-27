package org.uminho.di.gsd.gossip.client.aggregation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import org.uminho.di.gsd.gossip.client.*;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.uminho.di.gsd.common.Configuration;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.common.MessageUtil;
import org.uminho.di.gsd.common.RunConstants;
import org.uminho.di.gsd.common.device.BasicDevice;
import org.uminho.di.gsd.gossip.client.workers.AggRequestReplyTask;
import org.uminho.di.gsd.gossip.client.workers.SendingTask;
import org.uminho.di.gsd.gossip.device.GossipDevice;
import org.uminho.di.gsd.gossip.service.operations.aggregation.AggOperation;
import org.uminho.di.gsd.gossip.service.operations.aggregation.AggPullOperation;
import org.uminho.di.gsd.gossip.service.operations.aggregation.AggPushOperation;
import org.uminho.di.gsd.gossip.service.repo.aggregation.AggregationMessage;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.URI;

public class AggGossipClient extends GossipClient {

    static Logger logger = Logger.getLogger(AggGossipClient.class);
    protected AggPushOperation aggPushOp;
    protected AggOperation aggOp;
    protected AggPullOperation aggPullOp;
    protected long waitResponses;
    protected long waitTime;
    protected boolean producer;

    protected HashMap<Integer, String[]> aggregateValues;

    public AggGossipClient(GossipDevice dvc) {
        super(dvc);
        aggregateValues = new HashMap<Integer, String[]>();
    }

    public AggPushOperation getAggPushOp() {
        if (aggPushOp == null) {
            aggPushOp = device.getGossipService().getAggPushOperation();
        }

        return aggPushOp;
    }

    public Operation getAggOp() {
        if (aggOp == null) {
            aggOp = device.getGossipService().getAggOperation();
        }

        return aggOp;
    }

    public Operation getAggPullOp() {
        if (aggPullOp == null) {
            aggPullOp = device.getGossipService().getAggPullOperation();
        }

        return aggPullOp;
    }

    public boolean isProducer() {
        return producer;
    }

    public void setProducer(boolean producer) {
        this.producer = producer;
    }

    private void setSendingTime(int index, long time) {
        if (producer) {
            sending.set(index, time);
            logger.debug(device.getIdStr() + ": Sent aggregation message " + index + " at " + time);
//            counter++;
        }
    }

    @Override
    public void readConfiguration() {
        super.readConfiguration();
        if (device != null) {
            waitTime = (Long) Configuration.getConfigParamValue(Configuration.waitTime);
            waitResponses = (Long) Configuration.getConfigParamValue(Configuration.waitResponses);
        }
    }

    public static void main(String[] args) {
        // always start the framework first
        DPWSFramework.start(args);

        AggGossipClient client = new AggGossipClient(null);
        client.readConfiguration();
        client.initializeMonitoringService();
    }

    public void startTcpAggPushGossipDissemination() {

        producer = true;

        double value = 543.12d;
        ParameterValue pv = null;
        // initialize xsltReader
        String xslt = "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:gsd=\"http://gsd.di.uminho.pt/example/\">"
                + "<xsl:template match=\"/\">"
//                + "<xsl:value-of select=\"avg(//gsd:TempValue)\" />"
                + "<xsl:value-of select=\"max(//gsd:TempValue)\" />"
                + "</xsl:template>"
                + "</xsl:stylesheet>";
        String prefix = Constants.XsltMessageListElementName + "/" + Constants.XsltMessageElementName + "[0]/";
        String mcPrefix = prefix + Constants.MessageContainerElementName + "/";

        URI action = new URI(ApplicationServiceConstants.infoTempOpName);
        String srcEpr = device.getGossipService().getSvcEPR();

        logger.info(device.getIdStr() + ": Gonna fire " + iters + " push aggregations with a period of "
                + timeInterval + " ms");
        try {
            for (int i = 0; i < iters; i++) {
                // get current value
                value = device.getGossipService().getAppService().getValue();
                String ownValue = "" + value;

                String msgIdStr = udpPort + "-" + Long.toString(counter);
                URI msgId = new URI(msgIdStr);

                // build PV and set values
                pv = getAggPushOp().createInputValue();
                // set sender EPR
                pv.setValue(Constants.SvcEprElementName, srcEpr);
                // set XSLT
                pv.setValue(prefix + Constants.XsltElementName + "/" + Constants.ContentElementName, xslt);

                // set MsgId
                pv.setValue(mcPrefix + Constants.MessageIdentifierElementName, msgIdStr);

                // set rounds
                pv.setValue(mcPrefix + Constants.RoundsElementName, "" + maxRounds);

                // set action
                pv.setValue(mcPrefix + Constants.ActionElementName, ApplicationServiceConstants.infoTempOpName);

                //message
                pv.setValue(mcPrefix + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName, ownValue);


                AggregationMessage aggMsg = new AggregationMessage(msgId, action, maxRounds, ownValue, 0, ownValue, 0, 0, 0, xslt);

                device.getGossipService().getProcessor().addMessage(aggMsg);


                // build AggregationMessage
                logger.debug(device.getIdStr() + ": Firing push aggregation number " + i + "...");

                long currentTime = invokeTcpAggPush(pv, srcEpr);

                setSendingTime(i, currentTime);
                counter++;
                logger.debug(device.getIdStr() + ": Fired push aggregation number " + i + " at " + currentTime + "!");
                Thread.sleep(timeInterval);
            }
        } catch (InterruptedException ex) {
            logger.error(device.getIdStr() + ":" + ex.getMessage(), ex);
        }
    }

    public void startUdpAggPushGossipDissemination() {

        producer = true;

        double value = 543.12d;
        ParameterValue pv = null;
        // initialize xsltReader
        String xslt = "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:gsd=\"http://gsd.di.uminho.pt/example/\">"
                + "<xsl:template match=\"/\">"
//                + "<xsl:value-of select=\"avg(//gsd:TempValue)\" />"
                + "<xsl:value-of select=\"max(//gsd:TempValue)\" />"
                + "</xsl:template>"
                + "</xsl:stylesheet>";
        String prefix = Constants.XsltMessageListElementName + "/" + Constants.XsltMessageElementName + "[0]/";
        String mcPrefix = prefix + Constants.MessageContainerElementName + "/";

        URI action = new URI(ApplicationServiceConstants.infoTempOpName);
        String srcEpr = device.getGossipService().getSvcEPR();

        logger.info(device.getIdStr() + ": Gonna fire " + iters + " push aggregations with a period of "
                + timeInterval + " ms");
        try {
            for (int i = 0; i < iters; i++) {
                // get current value
                value = device.getGossipService().getAppService().getValue();
                String ownValue = "" + value;

                String msgIdStr = udpPort + "-" + Long.toString(counter);
                URI msgId = new URI(msgIdStr);

                // build PV and set values
                pv = getAggPushOp().createInputValue();
                // set sender EPR
                pv.setValue(Constants.SvcEprElementName, srcEpr);
                // set XSLT
                pv.setValue(prefix + Constants.XsltElementName + "/" + Constants.ContentElementName, xslt);

                // set MsgId
                pv.setValue(mcPrefix + Constants.MessageIdentifierElementName, msgIdStr);

                // set rounds
                pv.setValue(mcPrefix + Constants.RoundsElementName, "" + maxRounds);

                // set action
                pv.setValue(mcPrefix + Constants.ActionElementName, ApplicationServiceConstants.infoTempOpName);

                //message
                pv.setValue(mcPrefix + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName, ownValue);


                AggregationMessage aggMsg = new AggregationMessage(msgId, action, maxRounds, ownValue, 0, ownValue, 0, 0, 0, xslt);

                device.getGossipService().getProcessor().addMessage(aggMsg);


                // build AggregationMessage
                logger.debug(device.getIdStr() + ": Firing push aggregation number " + i + "...");

                long currentTime = invokeUdpAggPush(pv, srcEpr);

                setSendingTime(i, currentTime);
                counter++;
                logger.debug(device.getIdStr() + ": Fired push aggregation number " + i + " at " + currentTime + "!");
                Thread.sleep(timeInterval);
            }
        } catch (InterruptedException ex) {
            logger.error(device.getIdStr() + ":" + ex.getMessage(), ex);
        }
    }

    @Override
    public long invokeTcpAggPush(ParameterValue newPV, String srcEpr) {
        long time = -1;
        List tempTargets = null;

        // no targets set
        if (targets == null) {
            retrieveTargets();
        }

        tempTargets = getTargets();

        int localFanout = 0;

        if (tempTargets != null) {
            if (tempTargets.size() > 0) {
                if (srcEpr != null) {
                    tempTargets = MessageUtil.filterSender(tempTargets, srcEpr);
                }
            }

            localFanout = tempTargets.size();
        }

        if (localFanout > 0) {
            logger.debug(device.getIdStr() + ": Invoking AggPush on " + localFanout + " peers...");

            time = invokeTcpAggPushOperationOnTargets(newPV, tempTargets);
        } else {
            logger.warn(device.getIdStr() + ": No targets to invoke AggPush operation!");
        }

        return time;
    }

    @Override
    public long invokeUdpAggPush(ParameterValue newPV, String srcEpr) {
        long time = -1;
        List tempTargets = null;

        // no targets set
        if (udpTargets == null) {
            retrieveTargets();
        }

        tempTargets = getUdpTargets();

        int localFanout = 0;

        if (tempTargets != null) {
            // remove peer from whom messages were received
            if (srcEpr != null) {
                URI srcUri = new URI(srcEpr);
                for (int i = 0; i < tempTargets.size(); i++) {
                    URI target = (URI) tempTargets.get(i);
                    if (srcUri.equalsRFC3986(target) || srcUri.equalsSTRCMP0(target)) {
                        tempTargets.remove(i);
                    }
                }
            } else {
                logger.warn(device.getIdStr() + ": Received messages from no sender! Producer perhaps? " + device.getIdStr());
            }

            localFanout = tempTargets.size();
        }

        if (localFanout > 0) {
            logger.debug(device.getIdStr() + ": Invoking AggPush on " + localFanout + " peers...");

            time = invokeUdpAggPushOperationOnTargets(newPV, tempTargets);
        } else {
            logger.warn(device.getIdStr() + ": No targets to invoke AggPush operation!");
        }

        return time;
    }

    @Override
    public String invokeTcpAgg(ParameterValue pv, AggregationMessage agg, String srcEpr, int index) {
        String ret = null;

        List tempTargets = null;

        // no targets set
        if (targets == null) {
            retrieveTargets();
        }

        tempTargets = getTargets();

        int localFanout = 0;

        if (tempTargets != null) {
            if (tempTargets.size() > 0) {
                if (srcEpr != null) {
                    tempTargets = MessageUtil.filterSender(tempTargets, srcEpr);
                }
            }

            localFanout = tempTargets.size();
        }

        if (localFanout > 0) {
            agg.setMax(localFanout);
            agg.checkReceivedResponses();
            long time = invokeTcpAggOperationOnTargets(pv, tempTargets, agg);
            if (index > -1) {
                setSendingTime(index, time);
                counter++;
            }
        } else {
            logger.warn(device.getIdStr() + ": No targets to invoke Agg operation!");
            agg.timeout();
        }
        ret = agg.getResponseValue();

        return ret;
    }

    @Override
    public String invokeUdpAgg(ParameterValue pv, AggregationMessage agg, String srcEpr, int index) {
        String ret = null;

        List tempTargets = null;

        // no targets set
        if (udpTargets == null) {
            retrieveTargets();
        }

        tempTargets = getUdpTargets();

        int localFanout = 0;

        if (tempTargets != null) {
            if (tempTargets.size() > 0) {
                if (srcEpr != null) {
                    tempTargets = MessageUtil.filterSender(tempTargets, srcEpr);
                }
            }

            localFanout = tempTargets.size();
        }

        if (localFanout > 0) {
            agg.setMax(localFanout);
            agg.checkReceivedResponses();
            long time = invokeTcpAggOperationOnTargets(pv, tempTargets, agg);
            if (index > -1) {
                setSendingTime(index, time);
                counter++;
            }
        } else {
            logger.warn(device.getIdStr() + ": No targets to invoke Agg operation!");
            agg.timeout();
        }
        ret = agg.getResponseValue();

        return ret;
    }

    @Override
    public String invokeTcpAggPull(ParameterValue pv, AggregationMessage agg, String srcEpr, int index) {
        String ret = null;

        List tempTargets = null;

        // no targets set
        if (targets == null) {
            retrieveTargets();
        }

        tempTargets = getTargets();

        int localFanout = 0;

        if (tempTargets != null) {
            if (tempTargets.size() > 0) {
                if (srcEpr != null) {
                    tempTargets = MessageUtil.filterSender(tempTargets, srcEpr);
                }
            }

            localFanout = tempTargets.size();
        }

        if (localFanout > 0) {
            agg.setMax(localFanout);
            agg.checkReceivedResponses();
            logger.debug(device.getIdStr() + ": Invoking AggPull on " + localFanout + " peers...");
            long time = invokeTcpAggPullOperationOnTargets(pv, tempTargets, agg);
            if (index > -1) {
                setSendingTime(index, time);
                counter++;
            }
        } else {
            logger.warn(device.getIdStr() + ": No targets to invoke AggPull operation!");
            agg.timeout();
        }
        ret = agg.getResponseValue();

        return ret;
    }

    @Override
    public String invokeUdpAggPull(ParameterValue pv, AggregationMessage agg, URI sender, int index) {
        String ret = null;

        List tempTargets = null;

        // no targets set
        if (udpTargets == null) {
            retrieveTargets();
        }

        tempTargets = getUdpTargets();

        int localFanout = 0;

        if ((sender != null) && (tempTargets != null)) {
            // remove peer from whom messages were received
            for (int i = 0; i < tempTargets.size(); i++) {
                URI target = (URI) tempTargets.get(i);
                if (sender.equalsRFC3986(target) || sender.equalsSTRCMP0(target)) {
                    tempTargets.remove(i);
                }
            }
        } else {
            logger.warn(device.getIdStr() + ": Received messages from no sender! Producer perhaps? " + device.getIdStr());
        }
        localFanout = tempTargets.size();
        int localCounter = 0;

        if (localFanout > 0) {
            agg.setMax(localFanout);
            agg.checkReceivedResponses();
            logger.debug(device.getIdStr() + ": Invoking AggPull on " + localFanout + " peers: " + tempTargets);
            long time = invokeUdpAggPullOperationOnTargets(pv, tempTargets);
            if (index > -1) {
                setSendingTime(index, time);
                counter++;
            }
        } else {
            logger.warn(device.getIdStr() + ": No targets to invoke AggPull operation!");
            agg.timeout();
        }
        logger.debug("Getting aggregate value...");
        long start = System.currentTimeMillis();
        // Should block here waiting for aggregate value
        ret = agg.getResponseValue();
        long end = System.currentTimeMillis();
        logger.debug("Got aggregate value in " + (end - start) + " ms! Responses: " + agg.getCurrentResponses());
        
        String[] parts = agg.getIdentifier().toString().split("-");
        if (parts.length > 1) {
            int port = Integer.parseInt(parts[0]);
            int id = Integer.parseInt(parts[1]);
            setAggregateValue(port, id, ret);
            logger.debug("Set aggregate value " + ret + " for " + port + "-" + id);
        }

        if((ret != null) && !ret.isEmpty())
        {
            logger.debug("Got result " + ret + " and gonna reply!");
            

            // Create AggPullResponse and send it to all peers that sent AggPullRequest
            ParameterValue response = getAggPullOp().createOutputValue();
            String responsePrefix = Constants.MessagesListElementName + "/"
                                + Constants.MessageContainerElementName + "[" + localCounter + "]/";
            response.setValue(responsePrefix + Constants.RoundsElementName, Integer.toString(0));
            // set action
            String action = agg.getAction().toString();
            response.setValue(responsePrefix + Constants.ActionElementName, action);

            // set msgid
            String msgId = agg.getIdentifier().toString();
            response.setValue(responsePrefix + Constants.MessageIdentifierElementName, msgId);

            // set resulting message
            response.setValue(responsePrefix + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName, ret);

            //send response message to waiting peers
            invokeUdpAggPullOperationOnTargets(response, agg.getInvokers());
        }

        return ret;
    }

    private long invokeTcpAggPushOperationOnTargets(ParameterValue newPV, List targetServices) {
        long time = -1;

        int size = targetServices.size();
        SendingTask[] tasks = new SendingTask[size];

        logger.debug(device.getIdStr() + ": Invoking " + newPV + " on " + size + " peers...");

        for (int i = 0; i < size; i++) {
            ParameterValue message = getAggPushOp().createInputValue();
//            message.setValue(Constants.SvcEprElementName, srcEpr);
            message = MessageUtil.duplicateSvcEprPV(newPV, message);
            message = MessageUtil.duplicateXSLTMessageListPV(newPV, message);
            Service svc = (Service) targetServices.get(i);
            Operation op = getOperationFromService(Constants.AggPushOperationName, Constants.AggregationPortQName, svc);
            String str = device.getIdStr() + "Time:" + i + " Client invoked AggPush Op in " + svc + " for msg " + message;
            tasks[i] = new SendingTask(op, message, str);
        }

        logger.debug(device.getIdStr() + ": Created " + size + " SendingTasks!");

        time = System.nanoTime();
        for (int i = 0; i < size; i++) {
            DPWSFramework.getThreadPool().execute(tasks[i]);
        }

        logger.debug(device.getIdStr() + " Sent message " + newPV + " at " + time);

        return time;
    }

    private long invokeUdpAggPushOperationOnTargets(ParameterValue newPV, List targetServices) {
        long time = -1;

        int size = targetServices.size();

        logger.debug(device.getIdStr() + ": Invoking " + newPV + " on " + size + " peers...");

        InvokeMessage msg = new InvokeMessage(Constants.NameSpace + "/" + Constants.AggPushOperationName);

        msg.setContent(newPV);
        msg.setInbound(false);

        time = sendUDPMessages(device.getIdStr(), udpServer, msg, targetServices);
        logger.debug(device.getIdStr() + " Sent message " + newPV + " at " + time);

        return time;
    }

    private long invokeTcpAggOperationOnTargets(ParameterValue pv, List targetServices, AggregationMessage agg) {
        long ret = -1;

        int size = targetServices.size();
        SendingTask[] tasks = new SendingTask[size];

        for (int i = 0; i < size; i++) {
            ParameterValue message = getAggOp().createInputValue();

            message.setValue(Constants.SvcEprElementName, device.getGossipService().getSvcEPR());
            message = MessageUtil.duplicateXSLTMessageListPV(pv, message);
            Service svc = (Service) targetServices.get(i);
            Operation op = getOperationFromService(Constants.AggOperationName, Constants.AggregationPortQName, svc);
            String str = device.getIdStr() + "Time:" + i + " Client invoked AggOp in " + svc + " for msg " + message;
            tasks[i] = new AggRequestReplyTask(op, message, str, agg);
        }

        ret = System.nanoTime();
        for (int i = 0; i < size; i++) {
            DPWSFramework.getThreadPool().execute(tasks[i]);
        }

        logger.debug(device.getIdStr() + " Sent message " + pv + " at " + ret);

        return ret;
    }

    private ParameterValue duplicateAggPullRequestMessage(ParameterValue pv)
    {
        ParameterValue message = getAggPullOp().createInputValue();

            // svcEpr should come correctly set from invoker
            message = MessageUtil.duplicateSvcEprPV(pv, message);
            message = MessageUtil.duplicateRoundsPV(pv, message);
//            message.setValue(Constants.SvcEprElementName, device.getGossipService().getSvcEPR());
            message = MessageUtil.duplicateXSLTActionListPV(pv, message);

        return message;
    }

    private long invokeTcpAggPullOperationOnTargets(ParameterValue pv, List targetServices, AggregationMessage agg) {
        long ret = -1;

        int size = targetServices.size();
        SendingTask[] tasks = new SendingTask[size];

        for (int i = 0; i < size; i++) {
            ParameterValue message = duplicateAggPullRequestMessage(pv);
            Service svc = (Service) targetServices.get(i);
            Operation op = getOperationFromService(Constants.AggPullOperationName, Constants.AggregationPortQName, svc);
            String str = device.getIdStr() + "Time:" + i + " Client invoked AggPullOp in " + svc + " for msg " + message;
            tasks[i] = new AggRequestReplyTask(op, message, str, agg);
        }

        ret = System.nanoTime();
        for (int i = 0; i < size; i++) {
            DPWSFramework.getThreadPool().execute(tasks[i]);
        }

        logger.debug(device.getIdStr() + " Sent message " + pv + " at " + ret);

        return ret;
    }

    private long invokeUdpAggPullOperationOnTargets(ParameterValue pv, List targetServices) {
        long ret = -1;

//        int size = targetServices.size();

        // initialize message
        InvokeMessage msg = new InvokeMessage(Constants.NameSpace + "/" + Constants.AggPullOperationName);
        msg.setContent(pv);
        msg.setInbound(false);

        ret = sendUDPMessages(device.getIdStr(), udpServer, msg, targetServices);

        logger.debug(device.getIdStr() + " Sent message " + pv + " at " + ret);

        return ret;
    }

    public void startTcpAggGossipDissemination() {
        double value = 543.12d;
        ParameterValue pv = null;
        // initialize xsltReader
        String xslt = "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:gsd=\"http://gsd.di.uminho.pt/example/\">"
                + "<xsl:template match=\"/\">"
                + "<xsl:value-of select=\"avg(//gsd:TempValue)\" />"
                + "</xsl:template>"
                + "</xsl:stylesheet>";
        String prefix = Constants.XsltMessageListElementName + "/" + Constants.XsltMessageElementName + "[0]/";
        String mcPrefix = prefix + Constants.MessageContainerElementName + "/";

        URI action = new URI(ApplicationServiceConstants.infoTempOpName);
        String srcEpr = device.getGossipService().getSvcEPR();

        logger.info(device.getIdStr() + ": Gonna fire " + iters + " aggregations with a period of "
                + timeInterval + " ms");
        try {
            for (int i = 0; i < iters; i++) {
                // get current value
                value = device.getGossipService().getAppService().getValue();
                String ownValue = "" + value;

                String msgIdStr = udpPort + "-" + Long.toString(counter);
                URI msgId = new URI(msgIdStr);

                // build PV and set values
                pv = getAggOp().createInputValue();
                // set sender EPR
                pv.setValue(Constants.SvcEprElementName, srcEpr);
                // set XSLT
                pv.setValue(prefix + Constants.XsltElementName + "/" + Constants.ContentElementName, xslt);

                // set MsgId
                pv.setValue(mcPrefix + Constants.MessageIdentifierElementName, msgIdStr);

                // set rounds
                pv.setValue(mcPrefix + Constants.RoundsElementName, "" + maxRounds);

                // set action
                pv.setValue(mcPrefix + Constants.ActionElementName, ApplicationServiceConstants.infoTempOpName);

                //message
                pv.setValue(mcPrefix + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName, ownValue);


                long millis = System.currentTimeMillis();
                AggregationMessage aggMsg = new AggregationMessage(msgId, action, maxRounds, ownValue, millis, ownValue, (int) fanout, (int) waitResponses, waitTime, xslt);

                device.getGossipService().getProcessor().addMessage(aggMsg);


                // build AggregationMessage
                logger.debug(device.getIdStr() + ": Firing push aggregation number " + i + "...");

                String ret = invokeTcpAgg(pv, aggMsg, srcEpr, i);

                logger.debug(device.getIdStr() + ": Fired aggregation number " + i + "! Result: " + ret);
                Thread.sleep(timeInterval);
            }
        } catch (InterruptedException ex) {
            logger.error(device.getIdStr() + ":" + ex.getMessage(), ex);
        }
    }

    public void startUdpAggGossipDissemination() {
        double value = 543.12d;
        ParameterValue pv = null;
        // initialize xsltReader
        String xslt = "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:gsd=\"http://gsd.di.uminho.pt/example/\">"
                + "<xsl:template match=\"/\">"
                + "<xsl:value-of select=\"avg(//gsd:TempValue)\" />"
                + "</xsl:template>"
                + "</xsl:stylesheet>";
        String prefix = Constants.XsltMessageListElementName + "/" + Constants.XsltMessageElementName + "[0]/";
        String mcPrefix = prefix + Constants.MessageContainerElementName + "/";

        URI action = new URI(ApplicationServiceConstants.infoTempOpName);
        String srcEpr = device.getGossipService().getSvcEPR();

        logger.info(device.getIdStr() + ": Gonna fire " + iters + " aggregations with a period of "
                + timeInterval + " ms");
        try {
            for (int i = 0; i < iters; i++) {
                // get current value
                value = device.getGossipService().getAppService().getValue();
                String ownValue = "" + value;

                String msgIdStr = udpPort + "-" + Long.toString(counter);
                URI msgId = new URI(msgIdStr);

                // build PV and set values
                pv = getAggOp().createInputValue();
                // set sender EPR
                pv.setValue(Constants.SvcEprElementName, srcEpr);
                // set XSLT
                pv.setValue(prefix + Constants.XsltElementName + "/" + Constants.ContentElementName, xslt);

                // set MsgId
                pv.setValue(mcPrefix + Constants.MessageIdentifierElementName, msgIdStr);

                // set rounds
                pv.setValue(mcPrefix + Constants.RoundsElementName, "" + maxRounds);

                // set action
                pv.setValue(mcPrefix + Constants.ActionElementName, ApplicationServiceConstants.infoTempOpName);

                //message
                pv.setValue(mcPrefix + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName, ownValue);


                long millis = System.currentTimeMillis();
                AggregationMessage aggMsg = new AggregationMessage(msgId, action, maxRounds, ownValue, millis, ownValue, (int) fanout, (int) waitResponses, waitTime, xslt);

                device.getGossipService().getProcessor().addMessage(aggMsg);


                // build AggregationMessage
                logger.debug(device.getIdStr() + ": Firing aggregation number " + i + "...");

                String ret = invokeUdpAgg(pv, aggMsg, srcEpr, i);

                logger.debug(device.getIdStr() + ": Fired aggregation number " + i + "! Result: " + ret);
                Thread.sleep(timeInterval);
            }
        } catch (InterruptedException ex) {
            logger.error(device.getIdStr() + ":" + ex.getMessage(), ex);
        }
    }

    public void startTcpAggPullGossipDissemination() {
        ParameterValue pv = null;
        // initialize xsltReader
        String xslt = "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:gsd=\"http://gsd.di.uminho.pt/example/\">"
                + "<xsl:template match=\"/\">"
                + "<xsl:value-of select=\"avg(//gsd:TempValue)\" />"
//                + "<xsl:value-of select=\"min(//gsd:TempValue)\" />"
                + "</xsl:template>"
                + "</xsl:stylesheet>";
        String prefix = Constants.XsltActionListElementName + "/" + Constants.XsltActionElementName + "[0]/";
//        String mcPrefix = prefix + Constants.MessageContainerElementName + "/";

        URI action = new URI(ApplicationServiceConstants.infoTempOpName);
        String srcEpr = device.getGossipService().getSvcEPR();

        logger.info(device.getIdStr() + ": Gonna fire " + iters + " pull aggregations with a period of "
                + timeInterval + " ms");
        try {
            for (int i = 0; i < iters; i++) {
                String msgIdStr = udpPort + "-" + Long.toString(counter);
                URI msgId = new URI(msgIdStr);

                // build PV and set values
                pv = getAggPullOp().createInputValue();
                // set sender EPR
                pv.setValue(Constants.SvcEprElementName, srcEpr);

                // set rounds
                pv.setValue(Constants.RoundsElementName, ""  + maxRounds);

                // set Action
                pv.setValue(prefix + Constants.MessageInfoElementName + "/" + Constants.ActionElementName, ApplicationServiceConstants.infoTempOpName);

                // set msgId
                pv.setValue(prefix + Constants.MessageInfoElementName + "/" + Constants.MessageIdentifierElementName, msgIdStr);

                // set XSLT
                pv.setValue(prefix + Constants.XsltElementName + "/" + Constants.ContentElementName, xslt);


                String ownValue = "" + device.getGossipService().getAppService().getValue();

                long millis = System.currentTimeMillis();
                AggregationMessage aggMsg = new AggregationMessage(msgId, action, maxRounds, ownValue, millis, ownValue, (int) fanout, (int) waitResponses, waitTime, xslt);

                device.getGossipService().getProcessor().addMessage(aggMsg);


                // build AggregationMessage
                logger.debug(device.getIdStr() + ": Firing pull aggregation number " + i + "...");
                String ret = invokeTcpAggPull(pv, aggMsg, srcEpr, i);

                logger.debug(device.getIdStr() + ": Fired pull aggregation number " + i + "! Result: " + ret);
                Thread.sleep(timeInterval);
            }
        } catch (InterruptedException ex) {
            logger.error(device.getIdStr() + ":" + ex.getMessage(), ex);
        }
    }

    public void startUdpAggPullGossipDissemination() {
        ParameterValue pv = null;
        // initialize xsltReader
        String xslt = "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:gsd=\"http://gsd.di.uminho.pt/example/\">"
                + "<xsl:template match=\"/\">"
                + "<xsl:value-of select=\"avg(//gsd:TempValue)\" />"
//                + "<xsl:value-of select=\"max(//gsd:TempValue)\" />"
                + "</xsl:template>"
                + "</xsl:stylesheet>";
        String prefix = Constants.XsltActionListElementName + "/" + Constants.XsltActionElementName + "[0]/";
//        String mcPrefix = prefix + Constants.MessageContainerElementName + "/";

        URI action = new URI(ApplicationServiceConstants.infoTempOpName);
        String srcEpr = device.getGossipService().getSvcEPR();
        URI sender = new URI(srcEpr);

        logger.info(device.getIdStr() + ": Gonna fire " + iters + " pull aggregations with a period of "
                + timeInterval + " ms");
        try {
            for (int i = 0; i < iters; i++) {
                String msgIdStr = udpPort + "-" + Long.toString(counter);
                URI msgId = new URI(msgIdStr);

                // build PV and set values
                pv = getAggPullOp().createInputValue();
                // set sender EPR
                pv.setValue(Constants.SvcEprElementName, srcEpr);

                // set Action
                pv.setValue(prefix + Constants.MessageInfoElementName + "/" + Constants.ActionElementName, ApplicationServiceConstants.infoTempOpName);

                // set msgId
                pv.setValue(prefix + Constants.MessageInfoElementName + "/" + Constants.MessageIdentifierElementName, msgIdStr);

                // set XSLT
                pv.setValue(prefix + Constants.XsltElementName + "/" + Constants.ContentElementName, xslt);


                String ownValue = "" + device.getGossipService().getAppService().getValue();

                long millis = System.currentTimeMillis();
                AggregationMessage aggMsg = new AggregationMessage(msgId, action, maxRounds, ownValue, millis, ownValue, (int) fanout, (int) waitResponses, waitTime, xslt);

                device.getGossipService().getProcessor().addMessage(aggMsg);


                // build AggregationMessage
                logger.debug(device.getIdStr() + ": Firing pull aggregation number " + i + "...");
                String ret = invokeUdpAggPull(pv, aggMsg, sender, i);

                logger.debug(device.getIdStr() + ": Fired pull aggregation number " + i + "! Result: " + ret);
                Thread.sleep(timeInterval);
            }
        } catch (InterruptedException ex) {
            logger.error(device.getIdStr() + ":" + ex.getMessage(), ex);
        }
    }

    @Override
    protected void writeStatsToFile(FileWriter file, FileWriter hopsFile) {
        super.writeStatsToFile(file, hopsFile);

        FileWriter aggFileWriter = null;
        RunConstants runConstants = ((BasicDevice) device).getConstants();
        String filename = "values_" + runConstants.getDisseminationType() + runConstants.getFileName() + ".csv";

        try {
            File aggFile = new File(filename);
            aggFileWriter = new FileWriter(aggFile, true);
            
            if(aggFileWriter != null)
            {
                logger.debug(device.getIdStr() + ": Writing Agg Values to file " + filename);
                StringBuilder sb = new StringBuilder();
                // write sending times
                sb.append("Agg;");
                sb.append(device.getGossipService().getSvcEPR());
                sb.append(';');
                
                
                for(Integer key : aggregateValues.keySet())
                {
                    String[] values = aggregateValues.get(key);
                    if(values != null)
                    {
                        for (int i = 0; i < iters; i++) {
                            sb.append(values[i]);
                            sb.append(';');
                        }

//                        sb.append("\n");
                    }
                }
                sb.append("\n");

                aggFileWriter.append(sb.toString());
                aggFileWriter.flush();
            }

        } catch (IOException ex) {
            logger.error(device.getIdStr() + ":" + ex.getMessage(), ex);
        } finally {
            try {
                aggFileWriter.close();
            } catch (IOException ex) {
                logger.error(device.getIdStr() + ":" + ex.getMessage(), ex);
            }
        }
    }

    public void setAggregateValue(int port, int msgId, String value) {
        logger.debug(device.getIdStr() + " setting aggregate value " + value + " for port " + port + " and msgId " + msgId);

        String[] values = aggregateValues.get(port);

        if(values == null)
        {
            values = new String[iters];
            aggregateValues.put(port, values);
        }
        
        values[msgId] = value;

        // setting aggregate value on service
        if((value != null) && (!value.isEmpty()))
        {
            device.getGossipService().getAppService().setLastValue(Double.parseDouble(value));
        }
    }


}
