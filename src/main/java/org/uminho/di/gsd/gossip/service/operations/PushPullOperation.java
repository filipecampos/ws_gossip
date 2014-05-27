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

/**
 *
 * @author fjoc
 */
public class PushPullOperation extends PullOperation {

    static Logger logger = Logger.getLogger(PushPullOperation.class);

    public PushPullOperation() {
        super(Constants.PushPullOperationName, Constants.GossipPullPortQName);
    }

    @Override
    protected void initInput() {
        ComplexType pushPullMessageType = new ComplexType(ComplexType.CONTAINER_SEQUENCE);
        pushPullMessageType.addElement(getTimeIntervalElement());
        pushPullMessageType.addElement(getMessagesListElement());
        Element svcEprElement = getSvcEprElement();
        svcEprElement.setMinOccurs(0);
        svcEprElement.setMaxOccurs(1);
        pushPullMessageType.addElement(svcEprElement);

        Element in = new Element(Constants.PushPullOperationQName, pushPullMessageType);

        this.setInput(in);
    }

    @Override
    public ParameterValue invoke(ParameterValue pv) throws InvocationException, TimeoutException {
        long nanoTime = System.nanoTime();
        long millisTime = System.currentTimeMillis();

        return common_invoke(CommunicationProtocol.TCP, nanoTime, millisTime, pv, null);
    }

    public ParameterValue common_invoke(CommunicationProtocol proto, long nanoTime, long millisTime, ParameterValue pv, SOAPHeader header) {
        logger.debug("PushPull Op invoked! pv=" + pv.toString());

        // Must process received messages like in PushOp
        if(processor != null)
        {
            processor.process_push(proto, nanoTime, millisTime, null, pv);
        }

        logger.debug("PushPull Op processed received messages! Now processing pull request...");

        // Processing the pull part of the message
        super.common_invoke(proto, millisTime, pv, header);

        return null;
    }

}
