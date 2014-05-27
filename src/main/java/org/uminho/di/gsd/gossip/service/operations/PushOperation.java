/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.gossip.service.operations;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.CommunicationProtocol;
import org.uminho.di.gsd.common.Constants;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.types.URI;

/**
 *
 * @author fjoc
 */
public class PushOperation extends GossipOperation {

    static Logger logger = Logger.getLogger(PushOperation.class);

    public static AtomicLong counter;

    public PushOperation()
    {
        super(Constants.PushOperationName, Constants.GossipPushPortQName);

        counter = new AtomicLong();
        
        initInput();
    }

    @Override
    protected void initInput() {
        
        ComplexType pushMessageType = new ComplexType(Constants.PushMessageTypeQName, ComplexType.CONTAINER_SEQUENCE);
        pushMessageType.addElement(getMessagesListElement());
        Element svcEprElement = getSvcEprElement();
        svcEprElement.setMinOccurs(0);
        svcEprElement.setMaxOccurs(1);
        pushMessageType.addElement(svcEprElement);

        Element in = new Element(Constants.PushOperationQName, pushMessageType);
        
        this.setInput(in);
    }

    @Override
    protected void initOutput() {
        
    }

    @Override
    public ParameterValue invoke(ParameterValue pv) throws InvocationException, TimeoutException {
        long nanoTime = System.nanoTime();
        long millisTime = System.currentTimeMillis();

        return common_invoke(CommunicationProtocol.TCP, nanoTime, millisTime, pv, null);
    }

    public ParameterValue common_invoke(CommunicationProtocol proto, long nanoTime, long millisTime, ParameterValue pv, SOAPHeader header)
    {
        String containerPrefix = Constants.MessagesListElementName + "/" + Constants.MessageContainerElementName + "[0]/";
        logger.debug("Device " + processor.getService().getSvcEPR() + " Received message: " + pv.getValue(containerPrefix + Constants.MessageIdentifierElementName));
        if(processor != null)
        {
            processor.process_push(proto, nanoTime, millisTime, getSender(header, pv), pv);
        }

        long current_counter = counter.incrementAndGet();
        logger.debug("Push Op invoked " + current_counter + " times!PV=" + pv.toString());

        // in-only message
        return null;
    }

}
