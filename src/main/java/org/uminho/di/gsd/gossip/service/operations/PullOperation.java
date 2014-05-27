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
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

/**
 *
 * @author fjoc
 */
public class PullOperation extends GossipOperation {

    static Logger logger = Logger.getLogger(PullOperation.class);

    public PullOperation() {
        this(Constants.PullOperationName, Constants.GossipPullPortQName);
    }

    public PullOperation(String operationName, QName operationType) {
        super(operationName, operationType);

        initInput();
    }

    @Override
    protected void initInput() {
        ComplexType pullMessageType = new ComplexType(ComplexType.CONTAINER_SEQUENCE);
        pullMessageType.addElement(getTimeIntervalElement());
        Element svcEprElement = getSvcEprElement();
        svcEprElement.setMinOccurs(0);
        svcEprElement.setMaxOccurs(1);
        pullMessageType.addElement(svcEprElement);
//        pullMessageType.addElement(getRoundsElement());

        Element in = new Element(Constants.PullOperationQName, pullMessageType);

        this.setInput(in);
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
        logger.debug("Pull Op invoked! pv=" + pv.toString());

//        String roundsStr = pv.getValue(Constants.RoundsElementName); // What are rounds for?
        URI svcEPR = getSender(header, pv);
        if (svcEPR != null) {
            // get desired messages from processor
            List messages = getRecentMessages(now, pv);
            logger.debug("Processor returned " + messages.size() + " messages.");

            if (messages.size() > 0) {
                processor.respondToAsyncPull(proto, messages, svcEPR.toString(), header);
            } else {
                logger.info("No messages to return to pull!");
            }
        }

        // in-only message
        return null;
    }

}
