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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uminho.di.gsd.gossip.service.operations.aggregation;

import java.io.StringReader;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.uminho.di.gsd.common.CommunicationProtocol;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.common.MessageUtil;
import org.uminho.di.gsd.gossip.service.repo.aggregation.AggregationMessage;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.types.URI;

/**
 *
 * @author fjoc
 */
public class AggPullOperation extends AggregationOperation {

    static Logger logger = Logger.getLogger(AggPullOperation.class);

    public AggPullOperation() {
        super(Constants.AggPullOperationName, Constants.AggregationPortQName);

        initInput();
        initOutput();
    }

    @Override
    protected void initInput() {

        ComplexType req = new ComplexType(Constants.AggPullRequestTypeQName, ComplexType.CONTAINER_SEQUENCE);

        req.addElement(getSvcEprElement());
        req.addElement(getRoundsElement());
        req.addElement(getXsltActionListElement());

        Element in = new Element(Constants.AggPullRequestElementQName, req);
        setInput(in);
    }

    @Override
    protected void initOutput() {
        ComplexType resp = new ComplexType(Constants.AggPullResponseTypeQName, ComplexType.CONTAINER_SEQUENCE);

        resp.addElement(getMessagesListElement());

        Element out = new Element(Constants.AggPullResponseElementQName, resp);
        setOutput(out);
    }

    @Override
    public ParameterValue invoke(ParameterValue parameterValue) throws InvocationException, TimeoutException {

        long nanoTime = System.nanoTime();
        long millisTime = System.currentTimeMillis();

        return common_invoke(CommunicationProtocol.TCP, nanoTime, millisTime, null, parameterValue);
    }

    public ParameterValue common_invoke(CommunicationProtocol communicationProtocol, long nanoTime, long millisTime, URI sender, ParameterValue pv) {
        ParameterValue ret = null;

        // pick xslts and messages

        // check number of XsltMessage elements
        String prefix = Constants.XsltActionListElementName + "/" + Constants.XsltActionElementName;
        int num = pv.getChildrenCount(prefix);

        logger.debug("Got " + num + " XsltMessages!");

        prefix += "[";

        if (num > 0) {
            // if there are actions

            // initialize serializer

//            StringReader reader = null;
            String xslt = null;

            ret = createOutputValue();
            int counter = 0;

            String src = pv.getValue(Constants.SvcEprElementName);

            String roundsStr = pv.getValue(Constants.RoundsElementName);
            logger.debug("Got message with " + roundsStr + " rounds!");
            int rounds = Integer.parseInt(roundsStr);

            // cycle messages and extract each xslt
            // WARNING: Assuming a single message
            for (int i = 0; i < num; i++) {
                String tempPrefix = prefix + i + "]/";
                // extract xslt
                xslt = extractXSLT(tempPrefix, pv);

                if ((xslt != null) && (!xslt.isEmpty())) {
                    // If there is an xslt

                    String responsePrefix = Constants.MessagesListElementName + "/"
                            + Constants.MessageContainerElementName + "[" + counter + "]/";

                    ret.setValue(responsePrefix + Constants.RoundsElementName, Integer.toString(0));

                    // set action
                    String action = pv.getValue(tempPrefix + Constants.MessageInfoElementName + "/" + Constants.ActionElementName);
                    ret.setValue(responsePrefix + Constants.ActionElementName, action);

                    // set msgid
                    String msgId = pv.getValue(tempPrefix + Constants.MessageInfoElementName + "/" + Constants.MessageIdentifierElementName);
                    ret.setValue(responsePrefix + Constants.MessageIdentifierElementName, msgId);


                    // invoke agg on peers assuming that only one XSLTMessage is being sent
                    AggregationMessage agg = (AggregationMessage) processor.getMessage(new URI(msgId));
                    String val = null;
                    String current = Double.toString(processor.getService().getAppService().getLastValue());
                    if (agg == null) {
                        logger.debug("New AggPull message with id: " + msgId);
                        
                        // new message, so invoke on peers
                        agg = new AggregationMessage(new URI(msgId), new URI(action), 0, current, 0, current, maxFanout, waitFanout, timeout, xslt);
                        processor.addMessage(agg);
                    }
                    else {
                        logger.debug("Duplicate AggPull message with id: " + msgId);
                        if (rounds == 0)
                        {
                            logger.debug("Got 0 rounds with known message " + msgId + "! Timeouting aggregated message...");
                            agg.timeout();
                        }
                    }

                    
                        if (rounds == 0)
                        {
                            logger.debug("Got 0 rounds with a new message " + msgId + "! Setting aggregated value as " + current);
                            val = current;
                        }
                        else
                        {
                            // set my own epr
                            pv.setValue(Constants.SvcEprElementName, processor.getService().getSvcEPR());

                            // set rounds decremented
                            pv.setValue(Constants.RoundsElementName, "" + (--rounds));

                            if (communicationProtocol.equals(CommunicationProtocol.TCP)) {
                                val = processor.process_tcp_agg_pull(pv, nanoTime, msgId, agg, src);
                            } else {
                                // UDP
//                                processor.process_udp_agg_pull(pv, nanoTime, msgId, agg, sender);
                            }
                        }


                    
                        // duplicate message, so just wait for response to return reply
                        val = agg.getResponseValue();
//                    }

                    if ((val != null) && (!val.isEmpty())) {
                        processor.getService().getAppService().setLastValue(Double.parseDouble(val));
                        // store aggregate value to array
                        processor.setCurrentAggregateValue(msgId, val);
                    } else {
                        logger.warn("Some error occurred processing the XSLT. Using last value...");
                        val = Double.toString(processor.getService().getAppService().getLastValue());
                    }

                    // set resulting message
                    ret.setValue(responsePrefix + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName, val);

                    counter++;

                    processor.removeMessage(new URI(msgId));
                }
                else
                {
                    logger.debug("Error with message! Didn't get XSLT!");
                }
            }
        }

        return ret;
    }

    public void invoke_request(long nanoTime, long millisTime, URI sender, ParameterValue pv) {
        // UDP request

        // check number of XsltMessage elements
        String prefix = Constants.XsltActionListElementName + "/" + Constants.XsltActionElementName;
        int num = pv.getChildrenCount(prefix);

        logger.debug("Got " + num + " XsltMessages!");

        prefix += "[";

        if (num > 0) {
            // if there are actions

            // initialize serializer
            String xslt = null;

            String src = pv.getValue(Constants.SvcEprElementName);


            // cycle messages and extract each xslt
            // WARNING: Assuming a single message
            for (int i = 0; i < num; i++) {
                String tempPrefix = prefix + i + "]/";
                // extract xslt
                xslt = extractXSLT(tempPrefix, pv);

                if ((xslt != null) && (!xslt.isEmpty())) {
                    // If there is an xslt

                    // get msgid
                    String msgId = pv.getValue(tempPrefix + Constants.MessageInfoElementName + "/" + Constants.MessageIdentifierElementName);

                    // invoke agg on peers assuming that only one XSLTMessage is being sent
                    AggregationMessage agg = (AggregationMessage) processor.getMessage(new URI(msgId));
                    if (agg == null) {
                        // new aggregation message
                        logger.debug("New AggPull message with id: " + msgId);

                        // set action
                        String action = pv.getValue(tempPrefix + Constants.MessageInfoElementName + "/" + Constants.ActionElementName);
                        String current = Double.toString(processor.getService().getAppService().getLastValue());
                        // new message, so invoke on peers
                        agg = new AggregationMessage(new URI(msgId), new URI(action), 0, current, 0, current, maxFanout, waitFanout, timeout, xslt);
                        agg.addInvoker(sender);
                        processor.addMessage(agg);

                        ParameterValue message = createInputValue();

                        // set my own epr
                        message.setValue(Constants.SvcEprElementName, processor.getService().getSvcEPR());
                        message = MessageUtil.duplicateRoundsPV(pv, message);
                        message = MessageUtil.duplicateXSLTActionListPV(pv, message);

                        logger.debug("Received message from " + src + " Changing SvcEpr to " + processor.getService().getSvcEPR() + ". Got " + message.getValue(Constants.SvcEprElementName));

                        processor.process_udp_agg_pull(message, nanoTime, msgId, agg, sender);
                    }
                    else
                    {
                        // existing aggregation message
                        logger.debug("AggregationMessage object already exists with id " + agg.getIdentifier() + "! Adding sender " + sender + "...");
                        if(logger.isDebugEnabled())
                        {
                            logger.debug("Current responses for " + agg.getIdentifier() + " are: " + agg.getCurrentResponses());
                        }
                        agg.addInvoker(sender);
                    }
                }
            }
        }
    }

    public void invoke_response(long nanoTime, long millisTime, URI sender, ParameterValue pv) {
        // UDP response

        // pick agg message id and insert response in agg object
        logger.debug("Received reply: " + pv + " from " + sender);

        // get msgid
        String msgId = pv.getValue(Constants.MessagesListElementName + "/"
                                    + Constants.MessageContainerElementName + "[" + 0 + "]/"
                                    + Constants.MessageIdentifierElementName);

        String value = pv.getValue(Constants.MessagesListElementName + "/"
                                    + Constants.MessageContainerElementName + "[" + 0 + "]/"
                                    + Constants.MessageElementName + "/"
                                    + ApplicationServiceConstants.infoTempValueElementName);

        // get agg object
        AggregationMessage agg = (AggregationMessage) processor.getMessage(new URI(msgId));
        logger.debug("Received response for AggregationMessage with id " + msgId + " with value: " + value);

        // insert response
        agg.addResponse(value);

//        processor.setCurrentAggregateValue(msgId, value);
    }
}
