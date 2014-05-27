/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uminho.di.gsd.gossip.service.operations.aggregation;

import java.io.StringReader;
import java.io.StringWriter;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.uminho.di.gsd.common.CommunicationProtocol;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.gossip.service.repo.Message;
import org.uminho.di.gsd.gossip.service.repo.aggregation.AggregationMessage;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.io.xml.XmlSerializer;
import org.ws4d.java.io.xml.XmlSerializerImplementation;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.types.URI;

/**
 *
 * @author fjoc
 */
public class AggPushOperation extends AggregationOperation {

    static Logger logger = Logger.getLogger(AggPushOperation.class);

    public AggPushOperation() {
        super(Constants.AggPushOperationName, Constants.AggregationPortQName);

        initInput();
        // no output - in-only
    }

    @Override
    public ParameterValue invoke(ParameterValue parameterValue) throws InvocationException, TimeoutException {
        long nanoTime = System.nanoTime();
        long millisTime = System.currentTimeMillis();

        common_invoke(CommunicationProtocol.TCP, nanoTime, millisTime, null, parameterValue);
        return null;
    }

    @Override
    protected void initInput() {
        ComplexType req = new ComplexType(Constants.AggPushRequestTypeQName, ComplexType.CONTAINER_SEQUENCE);

        req.addElement(getSvcEprElement());
        req.addElement(getXsltMessageListElement());

        Element in = new Element(Constants.AggPushRequestElementQName, req);

        setInput(in);
    }

    @Override
    protected void initOutput() {
        // no response
    }

    public void common_invoke(CommunicationProtocol communicationProtocol, long nanoTime, long millisTime, URI sender, ParameterValue parameterValue) {
        
            // pick xslts and messages

            // check number of XsltMessage elements
            String prefix = Constants.XsltMessageListElementName + "/" + Constants.XsltMessageElementName;
            int num = parameterValue.getChildrenCount(prefix);

            logger.debug("Got " + num + " XsltMessages!");

            prefix += "[";

            if (num > 0) {
                // if there are messages


                // initialize serializer
                XmlSerializer serializer = new XmlSerializerImplementation();
                StringWriter sw = new StringWriter();
                serializer.setOutput(sw);

                String xml = null;
                String res = null;

                String msgId = null;

                Message msg = null;

                ParameterValue newPV = createInputValue();
                int counter = 0;

                String senderEpr = parameterValue.getValue(Constants.SvcEprElementName);
                newPV.setValue(Constants.SvcEprElementName, processor.getService().getSvcEPR());

                // cycle messages and extract each xslt
                for (int i = 0; i < num; i++) {
                    String tempPrefix = prefix + i + "]/";

                    msgId = parameterValue.getValue(tempPrefix + Constants.MessageContainerElementName + "/" + Constants.MessageIdentifierElementName);

                    msg = processor.getMessage(new URI(msgId));

                    if (msg == null) {
                        // new message

                        // extract xslt location
                        String xsltLocation = parameterValue.getValue(tempPrefix + Constants.XsltElementName + "/" + Constants.LocationElementName);
                        String xsltContent = null;
                        logger.debug("XSLT[" + i + "]: Location-" + xsltLocation);

                        if ((xsltLocation == null) || xsltLocation.isEmpty()) {
                            // extract xslt content if location is empty or not present
                            xsltContent = parameterValue.getValue(tempPrefix + Constants.XsltElementName + "/" + Constants.ContentElementName);
                            logger.debug("XSLT[" + i + "]: Content-" + xsltContent);

//                            if ((xsltContent != null) && !xsltContent.isEmpty()) {
//                                reader = new StringReader(xsltContent);
//                            } else {
//                                logger.warn("XSLT number " + i + " no location or content present!");
//                            }
                        } else {
                            // retrieve xslt
                            xsltContent = retrieveXslt(xsltLocation);
                        }


                        if ((xsltContent != null) && !xsltContent.isEmpty()) {
                            logger.debug("Going to extract value from message...");
                            String value = parameterValue.getValue(tempPrefix + Constants.MessageContainerElementName + "/" + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName);

                            logger.debug("Extracted value from message: " + value);

                            if ((value != null) && (!value.isEmpty())) {
                                StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>");
                                sb.append("<n1:NewTemp xmlns:n1=\"http://gsd.di.uminho.pt/example/\"><n1:TempValue>");
                                sb.append(value);
                                sb.append("</n1:TempValue></n1:NewTemp>");

                                // xslt processing
                                // merge received messages with own data
                                double mine = processor.getService().getAppService().getLastValue();
                                sb.append("<n1:NewTemp xmlns:n1=\"http://gsd.di.uminho.pt/example/\"><n1:TempValue>");
                                sb.append(mine);
                                sb.append("</n1:TempValue></n1:NewTemp>");
                                logger.debug("Appending mine (" + mine + ") to serialized xml...");
                                sb.append("</root>");
                                xml = sb.toString();
                                res = AggregationOperation.processMessage(xsltContent, xml);

                                if ((res != null) && (!res.isEmpty())) {
                                    // pass resulting info to service
                                    logger.debug("Got xslt result: " + res);
                                    processor.getService().getAppService().setLastValue(Double.parseDouble(res));

                                    // store aggregate value to array
                                    processor.setCurrentAggregateValue(msgId, res);

                                    // get rounds
                                    Integer rounds = Integer.parseInt(parameterValue.getValue(tempPrefix + Constants.MessageContainerElementName + "/" + Constants.RoundsElementName));
                                    if (rounds > 0) {
                                        // check rounds
                                        // if rounds > 0 still can send message
                                        String newPrefix = prefix + counter + "]/";
                                        boolean goodToGo = false;


                                        if (xsltContent != null) {
                                            // set xslt content
                                            newPV.setValue(newPrefix + Constants.XsltElementName + "/" + Constants.ContentElementName, xsltContent);
                                            goodToGo = true;
                                        } else if (xsltLocation != null) {

                                            newPV.setValue(newPrefix + Constants.XsltElementName + "/" + Constants.LocationElementName, xsltLocation);
                                            goodToGo = true;
                                        }

                                        if (goodToGo) {
                                            // set rounds decremented
                                            newPV.setValue(newPrefix + Constants.MessageContainerElementName + "/" + Constants.RoundsElementName, Integer.toString(rounds - 1));

                                            // set action
                                            String action = parameterValue.getValue(tempPrefix + Constants.MessageContainerElementName + "/" + Constants.ActionElementName);
                                            newPV.setValue(newPrefix + Constants.MessageContainerElementName + "/" + Constants.ActionElementName, action);

                                            // set msgid
                                            newPV.setValue(newPrefix + Constants.MessageContainerElementName + "/" + Constants.MessageIdentifierElementName, msgId);

                                            // set resulting message
                                            newPV.setValue(newPrefix + Constants.MessageContainerElementName + "/" + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName, res);
                                            logger.debug("Set " + newPrefix + Constants.MessageContainerElementName + "/" + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName + " value to " + res);

                                            counter++;

                                            msg = new AggregationMessage(new URI(msgId), new URI(action), Long.parseLong("" + rounds), res, 0, value, 0, 0, 0, xsltContent);
                                            processor.addMessage(msg);
                                        }
                                    }
                                }
                                
                            }
                        }
                    } else {
                        //duplicate message
                        logger.warn("Message with Id: " + msgId + " has already been received.");
                    }
                }

                if (counter > 0) {
                    processor.process_agg_push(communicationProtocol, newPV, nanoTime, msgId, msg, senderEpr);
//                    if (communicationProtocol.equals(CommunicationProtocol.TCP)) {
//                        // push resulting messages to other peers
//                        processor.process_tcp_agg_push(newPV, nanoTime, msgId, msg, senderEpr);
//                    }
//                    else
//                    {
//                        // UDP
//                        processor.process_udp_agg_push(newPV, nanoTime, msgId, msg, senderEpr);
//                    }
                }
                
            }
        
        
        return;
    }
}
