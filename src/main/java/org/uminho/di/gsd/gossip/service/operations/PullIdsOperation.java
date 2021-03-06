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
package org.uminho.di.gsd.gossip.service.operations;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.CommunicationProtocol;
import org.uminho.di.gsd.common.Constants;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.URI;

/**
 *
 * @author fjoc
 */
public class PullIdsOperation extends PullOperation {

    static Logger logger = Logger.getLogger(PullIdsOperation.class);

    public PullIdsOperation() {
        super(Constants.PullIdsOperationName, Constants.GossipLazyPortQName);
    }

    @Override
    protected void initInput() {
        ComplexType pullIdsMessageType = new ComplexType(ComplexType.CONTAINER_SEQUENCE);
        pullIdsMessageType.addElement(getTimeIntervalElement());
        Element svcEprElement = getSvcEprElement();
        svcEprElement.setMinOccurs(0);
        svcEprElement.setMaxOccurs(1);
        pullIdsMessageType.addElement(svcEprElement);
        
        Element in = new Element(Constants.PullIdsOperationQName, pullIdsMessageType);
        this.setInput(in);
    }

    @Override
    public ParameterValue common_invoke(CommunicationProtocol proto, long now, ParameterValue pv, SOAPHeader header) {
        logger.info("PullIds Op invoked! pv=" + pv.toString());

        URI svcEPR = getSender(header, pv);
        if (svcEPR != null) {
            // get desired messages from processor
            List messages = getRecentMessages(now, pv);
            logger.debug("Processor returned " + messages.size() + " messages.");

            if (messages.size() > 0) {
                processor.respond_to_pull_ids(proto, header, messages, processor.getService().getSvcEPR(), svcEPR);
            } else {
                logger.info("No messages to return to PullIds!");
            }
        }

        // in-only message
        return null;
    }

}
