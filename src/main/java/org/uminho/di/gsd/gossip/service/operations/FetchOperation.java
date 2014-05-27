/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.gossip.service.operations;

import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.CommunicationProtocol;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.common.MessageUtil;
import org.uminho.di.gsd.gossip.service.repo.Message;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

/**
 *
 * @author fjoc
 */
public class FetchOperation extends GossipOperation {

    static Logger logger = Logger.getLogger(FetchOperation.class);
    
    public FetchOperation()
    {
        this(Constants.FetchOperationName, Constants.GossipLazyPortQName);
    }

    public FetchOperation(String opName, QName opType)
    {
        super(opName, opType);

        initInput();
    }

    @Override
    protected void initInput() {
        ComplexType pullMessagesMessageType = new ComplexType(ComplexType.CONTAINER_SEQUENCE);
        pullMessagesMessageType.addElement(getIdentifiersListElement());
        Element svcEprElement = getSvcEprElement();
        svcEprElement.setMinOccurs(0);
        svcEprElement.setMaxOccurs(1);
        pullMessagesMessageType.addElement(svcEprElement);

        Element pullMessagesMessageElement = new Element(Constants.FetchOperationQName, pullMessagesMessageType);

        setInput(pullMessagesMessageElement);
    }

    @Override
    protected void initOutput() {
        
    }

    @Override
    public ParameterValue invoke(ParameterValue pv) throws InvocationException, TimeoutException {
        // get current timestamp
        long now = System.currentTimeMillis();

        return common_invoke(CommunicationProtocol.TCP, now, pv, null);
    }

    public ParameterValue common_invoke(CommunicationProtocol proto, long now, ParameterValue pv, SOAPHeader header) {
        String containerPrefix = Constants.IdentifiersListElementName + "/" + Constants.MessageIdentifierElementName;
        // retirar identifiersList de pv
        int num = pv.getChildrenCount(containerPrefix);
        logger.info("Fetch Op invoked! Children IDs:" + num + "; pv=" + pv.toString());

        if(logger.isDebugEnabled())
        {
            StringBuilder sb = new StringBuilder();
            String prefix = null;
            // iterar sobre a lista e processar cada id
            for(int i = 0; i < num; i++)
            {
                prefix = containerPrefix + "[" + i + "]";

                sb.append("Identifier[");
                sb.append(i);
                sb.append("]: ");
                sb.append(pv.getValue(prefix));
            }
            logger.debug(sb.toString());
        }
        
        URI svcEPR = getSender(header, pv);
        if (svcEPR != null) {
            List identifiers = MessageUtil.extractIdentifiersFromPV(pv);

            List messages = processor.getMessages(identifiers);
            logger.debug("Processor returned " + messages.size() + " messages.");

            if (messages.size() > 0) {
                Iterator iter = messages.iterator();
                List cloned_messages = new ArrayList();
                try {
                    while(iter.hasNext())
                    {
                        Message msg = (Message) iter.next();
                        Message cloned = (Message) msg.clone();
                        cloned.setRounds(0);
                        cloned_messages.add(cloned);
                    }
                } catch (CloneNotSupportedException ex) {
                    java.util.logging.Logger.getLogger(FetchOperation.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                processor.respondToAsyncPull(proto, cloned_messages, svcEPR.toString(), header);
            }
            else
                logger.info("No messages to return to PullMessages!");

        }

        // in-only message
        return null;
    }

}
