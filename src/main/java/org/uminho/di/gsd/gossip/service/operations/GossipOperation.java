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
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.gossip.service.repo.MessagesProcessor;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.schema.Attribute;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

/**
 *
 * @author fjoc
 */
public abstract class GossipOperation extends Operation {

    static Logger logger = Logger.getLogger(GossipOperation.class);

    protected MessagesProcessor processor;

    public GossipOperation(String operationName, QName operationType) {
        super(operationName, operationType);
    }

    public void setProcessor(MessagesProcessor proc) {
        this.processor = proc;
    }

    protected abstract void initInput();

    protected abstract void initOutput();

    protected Element getSvcEprElement() {
        Element svcEprElement = new Element(Constants.SvcEprElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_ANYURI));

        return svcEprElement;
    }

    protected Element getTimeIntervalElement() {
        Element timeIntervalElement = new Element(Constants.TimeIntervalElementQName);

        ComplexType timeIntervalType = new ComplexType(ComplexType.CONTAINER_SEQUENCE);
        Attribute msAttribute = new Attribute(Constants.MillisecondsAttributeQName);
        msAttribute.setType(SchemaUtil.getSchemaType(SchemaUtil.TYPE_UNSIGNED_LONG));
        msAttribute.setUse(Attribute.USE_REQUIRED);
        timeIntervalType.addAttribute(msAttribute);

        timeIntervalElement.setType(timeIntervalType);

        return timeIntervalElement;
    }

    protected Element getIdentifiersListElement() {
        Element identifiersList = new Element(Constants.IdentifiersListElementQName);

        ComplexType messagesListType = new ComplexType(ComplexType.CONTAINER_SEQUENCE);
        Element identifier = getMessageIdentifierElement();
        identifier.setMaxOccurs(-1);
        messagesListType.addElement(identifier);

        identifiersList.setType(messagesListType);

        return identifiersList;
    }

    protected Element getMessagesInfoListElement() {
        Element messagesInfoList = new Element(Constants.MessagesInfoListElementQName);

        ComplexType messagesInfoListType = new ComplexType(ComplexType.CONTAINER_SEQUENCE);
        Element messageInfo = getMessageInfoElement();
        messageInfo.setMaxOccurs(-1);
        messagesInfoListType.addElement(messageInfo);

        messagesInfoList.setType(messagesInfoListType);

        return messagesInfoList;
    }

    protected Element getMessageInfoElement() {
        Element messageInfo = new Element(Constants.MessageInfoElementQName);

        ComplexType messageInfoType = new ComplexType(Constants.MessageInfoTypeQName, ComplexType.CONTAINER_SEQUENCE);
        messageInfoType.addElement(getMessageIdentifierElement());
        messageInfoType.addElement(getMessageActionElement());
        
        messageInfo.setType(messageInfoType);

        return messageInfo;
    }

    protected Element getMessageIdentifierElement() {
        Element identifier = new Element(Constants.MessageIdentifierElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_ANYURI));

        return identifier;
    }

    protected Element getMessagesListElement() {
        Element messagesList = new Element(Constants.MessagesListElementQName);

        ComplexType messagesListType = new ComplexType(Constants.MessagesListTypeQName, ComplexType.CONTAINER_SEQUENCE);
        Element messageContainer = getMessageContainerElement();
        messageContainer.setMaxOccurs(-1);
        messagesListType.addElement(messageContainer);

        messagesList.setType(messagesListType);

        return messagesList;
    }

    protected Element getMessageContainerElement() {
        Element messageContainer = new Element(Constants.MessageContainerElementQName);

        ComplexType messageContainerType = new ComplexType(Constants.MessageContainerTypeQName, ComplexType.CONTAINER_SEQUENCE);

        messageContainerType.addElement(getMessageIdentifierElement());
        messageContainerType.addElement(getMessageActionElement());
        messageContainerType.addElement(getRoundsElement());
        messageContainerType.addElement(getMessageElement());

        messageContainer.setType(messageContainerType);

        return messageContainer;
    }

    protected Element getRoundsElement()
    {
        Element roundsElement = new Element(Constants.RoundsElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_UNSIGNED_LONG));

        return roundsElement;
    }

//    protected Element getMessageActionContainerElement() {
//        Element messageActionContainer = new Element(Constants.MessageActionContainerElementQName);
//
//        ComplexType messageActionContainerType = new ComplexType(Constants.MessageActionContainerTypeQName, ComplexType.CONTAINER_SEQUENCE);
//
//        messageActionContainerType.addElement(getMessageIdentifierElement());
//        messageActionContainerType.addElement(getMessageActionElement());
//
//        Element rounds = getRoundsElement();
//        rounds.setMinOccurs(0);
//        messageActionContainerType.addElement(rounds);
//
//        messageActionContainer.setType(messageActionContainerType);
//
//        return messageActionContainer;
//    }

    protected Element getMessageActionElement() {
        Element action = new Element(Constants.ActionElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_ANYURI));

        return action;
    }

    protected Element getMessageElement() {
        Element message = new Element(Constants.MessageElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_STRING));

//        ComplexType cType = new ComplexType(ComplexType.CONTAINER_SEQUENCE);
//        Element infoTemp = new Element(ApplicationServiceConstants.infoTempValueElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_DOUBLE));
//        cType.addElement(infoTemp);
//        message.setType(cType);

        return message;
    }

    protected Element getPullMessagesSyncElement()
    {
        Element pullMessages = new Element(Constants.FetchSyncOperationQName);

        ComplexType pullMessagesType = new ComplexType(ComplexType.CONTAINER_SEQUENCE);
        pullMessagesType.addElement(getIdentifiersListElement());

        pullMessages.setType(pullMessagesType);

        return pullMessages;
    }

    protected ComplexType getPushMessageType()
    {
        ComplexType pushMessageType = new ComplexType(Constants.PushMessageTypeQName, ComplexType.CONTAINER_SEQUENCE);
        pushMessageType.addElement(getMessagesListElement());
        
        return pushMessageType;
    }

    protected Element getPushElement()
    {
        ComplexType pushMessageType = new ComplexType(Constants.PushMessageTypeQName, ComplexType.CONTAINER_SEQUENCE);
        pushMessageType.addElement(getMessagesListElement());

        Element push = new Element(Constants.PushOperationQName, pushMessageType);

        return push;
    }

    protected Element getPushIdsElement()
    {
        Element pushIds = new Element(Constants.PushIdsOperationQName);

        ComplexType pushIdsType = new ComplexType(ComplexType.CONTAINER_SEQUENCE);
        pushIdsType.addElement(getMessagesInfoListElement());
        Element svcEPR = getSvcEprElement();
        svcEPR.setMinOccurs(0);
        svcEPR.setMaxOccurs(1);
        pushIdsType.addElement(svcEPR);

        pushIds.setType(pushIdsType);

        return pushIds;
    }

    protected URI getSender(SOAPHeader header, ParameterValue pv)
    {
        String svcEprStr = null;
        if(header != null)
        {
            // UDP
            EndpointReference replyTo = header.getReplyTo();
            if(replyTo != null)
                svcEprStr = header.getReplyTo().toString();
        }
        else
        {
            // TCP
            svcEprStr = pv.getValue(Constants.SvcEprElementName);
        }

        return new URI(svcEprStr);
    }

    public List getRecentMessages(long now, long interval)
    {
        return processor.getRecentMessages(now - interval);
    }

    public List getRecentMessages(long now, ParameterValue pv)
    {
        long timeInterval = 0;
        // retirar TimeInterval de pv
        String timeIntervalStr = pv.getAttributeValue(Constants.MillisecondsAttributeName, Constants.TimeIntervalElementName);
        logger.info("getRecentMessages invoked! TimeInterval:" + timeIntervalStr + "; pv=" + pv.toString());

        if (timeIntervalStr != null) {
            try {
                timeInterval = new Long(timeIntervalStr);
                logger.debug("now=" + now + "; timeInterval=" + timeInterval + "; now-timeInterval=" + (now - timeInterval));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            logger.error("timeIntervalStr has become null somehow!");
        }

        return processor.getRecentMessages(now - timeInterval);
    }
}
