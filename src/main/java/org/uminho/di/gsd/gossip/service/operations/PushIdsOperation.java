/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.gossip.service.operations;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.CommunicationProtocol;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.common.MessageUtil;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.URI;

/**
 *
 * @author fjoc
 */
public class PushIdsOperation extends GossipOperation {

    static Logger logger = Logger.getLogger(PushIdsOperation.class);
//
    public PushIdsOperation()
    {
        super(Constants.PushIdsOperationName, Constants.GossipLazyPortQName);

        initInput();
    }

    @Override
    protected void initInput() {
        setInput(getPushIdsElement());
    }

    @Override
    protected void initOutput() {
        
    }

    @Override
    public ParameterValue invoke(ParameterValue pv) throws InvocationException, TimeoutException {
        long millisTime = System.currentTimeMillis();

        common_invoke(CommunicationProtocol.TCP, millisTime, pv, null);

        // in-only message
        return null;
    }

    public ParameterValue common_invoke(CommunicationProtocol proto, long millisTime, ParameterValue pv, SOAPHeader header)
    {
        String containerPrefix = Constants.MessagesInfoListElementName + "/" + Constants.MessageInfoElementName;

        int num = pv.getChildrenCount(containerPrefix);
        logger.info("PushIds Op invoked! Children:" + num + "; pv=" + pv.toString());

        if(logger.isDebugEnabled())
        {
            StringBuilder sb = new StringBuilder("Device ");
            sb.append(processor.getService().getSvcEPR());
            sb.append(" received:");
            String prefix = null;
            // iterar sobre a lista e processar cada id
            for(int i = 0; i < num; i++)
            {
                prefix = containerPrefix + "[" + i + "]/";
                sb.append("\nIdentifier[");
                sb.append(i);
                sb.append("]: Action=");
                sb.append(pv.getValue(prefix + Constants.ActionElementName));
                sb.append("; Id=");
                sb.append(pv.getValue(prefix + Constants.MessageIdentifierElementName));
            }

            logger.debug(sb.toString());
        }
        
        URI svcEPR = getSender(header, pv);
        if (svcEPR != null) {
            
            if(processor != null)
            {
                List msgsInfo = MessageUtil.extractMessagesInfoFromPV(pv);
                logger.debug("Extracted info on " + msgsInfo.size() + " messages.");

                if (msgsInfo.size() > 0) {
                    processor.respond_to_push_ids(proto, header, msgsInfo, processor.getService().getSvcEPR(), svcEPR);
                } else {
                    logger.info("No messages to invoke Fetch with!");
                }
            }
        }

        // in-only message
        return null;
    }

}
