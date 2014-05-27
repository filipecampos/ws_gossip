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

package org.uminho.di.gsd.common;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.gossip.device.GossipDevice;
import org.uminho.di.gsd.gossip.service.repo.Message;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.schema.Element;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.URI;

public class MessageUtil {

    static Logger logger = Logger.getLogger(MessageUtil.class);

    public static ParameterValue buildMessagePV(int number, int round, String msg, GossipDevice device) {
        String actionName = Constants.NameSpace + "/" + Constants.PushOperationName;

        String basicPrefix = Constants.MessagesListElementName + "/" + Constants.MessageContainerElementName + "[0]/";

        // build element
        Element infoTemp = device.getGossipService().getPushOperation().getInput();

        // set element in parameter value
        ParameterValue pv = ParameterValue.createElementValue(infoTemp);
        pv.setValue(basicPrefix + Constants.MessageIdentifierElementName, Integer.toString(number));
        pv.setValue(basicPrefix + Constants.RoundsElementName, Integer.toString(round));
        pv.setValue(basicPrefix + Constants.ActionElementName, actionName);
        pv.setValue(basicPrefix + Constants.MessageElementName, msg);

        return pv;
    }

    public static ParameterValue buildMessagePVWithMultipleElements(int rounds, int start, int finish, String msg, GossipDevice device) {
        String actionName = Constants.NameSpace + "/" + Constants.PushOperationName;

        String basicPrefix = Constants.MessagesListElementName + "/" + Constants.MessageContainerElementName + "[";

        // build element
        Element infoTemp = device.getGossipService().getPushOperation().getInput();

        // set element in parameter value
        ParameterValue pv = ParameterValue.createElementValue(infoTemp);

        for (int i = start; i < finish; i++) {
            String prefix = basicPrefix + i + "]/";
            pv = setValues(pv, prefix, Integer.toString(i), rounds, actionName, msg);
        }

        return pv;
    }

    public static ParameterValue setValues(ParameterValue pv, String prefix, String msgId, int rounds, String action, String msg) {
        pv.setValue(prefix + Constants.MessageIdentifierElementName, msgId);
        pv.setValue(prefix + Constants.RoundsElementName, Integer.toString(rounds));
        pv.setValue(prefix + Constants.ActionElementName, action);
        pv.setValue(prefix + Constants.MessageElementName, msg);

        return pv;
    }

    public static String getIdAndRounds(ParameterValue pv) {
        String containerPrefix = Constants.MessagesListElementName + "/" + Constants.MessageContainerElementName + "[0]/";

        String ret = "MsgId:" + pv.getValue(containerPrefix + Constants.MessageIdentifierElementName)
                + ";Rounds:" + pv.getValue(containerPrefix + Constants.RoundsElementName);

        return ret;
    }

    public static Iterator extractMessagesFromPV(long time, ParameterValue pv) {
        List list = new ArrayList();

        String basicPrefix = Constants.MessagesListElementName + "/" + Constants.MessageContainerElementName;
        int children = pv.getChildrenCount(basicPrefix);

        StringBuilder msgsSB = null;
        if (logger.isDebugEnabled()) {
            msgsSB = new StringBuilder();
        }

        String containerPrefix = null;
        Message msg = null;
        for (int i = 0; i < children; i++) {
            containerPrefix = basicPrefix + "[" + i + "]/";

            String identifierStr = pv.getValue(containerPrefix + Constants.MessageIdentifierElementName);
            String actionStr = pv.getValue(containerPrefix + Constants.ActionElementName);
            String roundsStr = pv.getValue(containerPrefix + Constants.RoundsElementName);
            String msgStr = pv.getValue(containerPrefix + Constants.MessageElementName);

            msg = new Message(new URI(identifierStr), new URI(actionStr), Long.parseLong(roundsStr), msgStr, time);
            list.add(msg);

            if (logger.isDebugEnabled()) {
                msgsSB.append("\n\nContainer Number ").append(i);
                msgsSB.append("\nIdentifier: ").append(identifierStr);
                msgsSB.append("\nAction: ").append(actionStr);
                msgsSB.append("\nRounds: ").append(roundsStr);
                msgsSB.append("\nMessage: ").append(msgStr);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(msgsSB.toString());
        }

        return list.iterator();
    }

    public static ParameterValue convertMessagesToPV(List msgs, ParameterValue pv) {
        int size = msgs.size();
        String containerPrefix = null;
        Message msg = null;
        String basicPrefix = Constants.MessagesListElementName + "/" + Constants.MessageContainerElementName;

        for (int i = 0; i < size; i++) {
            containerPrefix = basicPrefix + "[" + i + "]/";

            msg = (Message) msgs.get(i);
            pv.setValue(containerPrefix + Constants.MessageElementName, msg.getMessage());
            pv.setValue(containerPrefix + Constants.ActionElementName, msg.getAction().toString());
            pv.setValue(containerPrefix + Constants.MessageIdentifierElementName, msg.getIdentifier().toString());
            pv.setValue(containerPrefix + Constants.RoundsElementName, Long.toString(msg.getRounds()));
        }

        return pv;
    }

    public static List extractIdentifiersFromPV(ParameterValue pv) {
        List ids = new ArrayList();

        String basicPrefix = Constants.IdentifiersListElementName + "/" + Constants.MessageIdentifierElementName;
        int children = pv.getChildrenCount(basicPrefix);

        String containerPrefix = null;
        URI id = null;
        for (int i = 0; i < children; i++) {
            containerPrefix = basicPrefix + "[" + i + "]";

            String idStr = pv.getValue(containerPrefix);

            logger.debug("Got id[" + i + "]: " + idStr);

            id = new URI(idStr);
            if (id != null) {
                ids.add(id);
            }
        }

        return ids;
    }

    public static ParameterValue convertIdentifiersToPV(List ids, ParameterValue pv, String svcEPR) {
        String basicPrefix = Constants.IdentifiersListElementName + "/" + Constants.MessageIdentifierElementName + "[";

        int size = ids.size();
        String containerPrefix = null;
        URI id = null;

        for (int i = 0; i < size; i++) {
            containerPrefix = basicPrefix + i + "]";
            id = (URI) ids.get(i);
            pv.setValue(containerPrefix, id.toString());
        }

        if ((svcEPR != null) && (!svcEPR.isEmpty())) {
            pv.setValue(Constants.SvcEprElementName, svcEPR);
        }

        return pv;
    }

    public static List extractMessagesInfoFromPV(ParameterValue pv) {
        List msgsInfo = new ArrayList();

        String basicPrefix = Constants.MessagesInfoListElementName + "/" + Constants.MessageInfoElementName;
        int children = pv.getChildrenCount(basicPrefix);

        String containerPrefix = null;
        Message msg = null;
        for (int i = 0; i < children; i++) {
            containerPrefix = basicPrefix + "[" + i + "]/";
            String actionStr = pv.getValue(containerPrefix + Constants.ActionElementName);

            String idStr = pv.getValue(containerPrefix + Constants.MessageIdentifierElementName);

            logger.debug("[" + i + "] - Id:" + idStr + "; Action: " + actionStr);

            if ((idStr != null) && (!idStr.isEmpty()) && (actionStr != null) && (!actionStr.isEmpty())) {
                msg = new Message(new URI(idStr), new URI(actionStr), 0, null, -1);

                msgsInfo.add(msg);
            }
        }

        return msgsInfo;
    }

    public static ParameterValue convertMessagesInfoToPV(List msgs, ParameterValue pv, String svcEPR) {
        Iterator msgsIter = msgs.iterator();

        String basicPrefix = Constants.MessagesInfoListElementName + "/" + Constants.MessageInfoElementName + "[";

        int i = 0;
        while (msgsIter.hasNext()) {
            Message msg = (Message) msgsIter.next();
            String prefix = basicPrefix + i + "]/";

            pv.setValue(prefix + Constants.ActionElementName, msg.getAction().toString());
            pv.setValue(prefix + Constants.MessageIdentifierElementName, msg.getIdentifier().toString());

            i++;
        }

        if ((svcEPR != null) && (!svcEPR.isEmpty())) {
            pv.setValue(Constants.SvcEprElementName, svcEPR);
        }

        return pv;
    }

    public static ParameterValue duplicateXSLTMessageListPV(ParameterValue newPV, ParameterValue message) {

        String prefix = Constants.XsltMessageListElementName + "/" + Constants.XsltMessageElementName;

        // see number of XSLT Message elements in pvs
        int count = newPV.getChildrenCount(prefix);

        for (int i = 0; i < count; i++) {
            String tempPrefix = prefix + "[" + i + "]/";

            // xslt
            String location = newPV.getValue(tempPrefix + Constants.XsltElementName + "/" + Constants.LocationElementName);

            if ((location != null) && (!location.isEmpty())) {
                message.setValue(tempPrefix + Constants.XsltElementName + "/" + Constants.LocationElementName, location);
            } else {
                message.setValue(tempPrefix + Constants.XsltElementName + "/" + Constants.ContentElementName, newPV.getValue(tempPrefix + Constants.XsltElementName + "/" + Constants.ContentElementName));
            }

            // message container
            String newPrefix = tempPrefix + Constants.MessageContainerElementName + "/";

            // rounds
            message.setValue(newPrefix + Constants.RoundsElementName, newPV.getValue(newPrefix + Constants.RoundsElementName));

            // action
            message.setValue(newPrefix + Constants.ActionElementName, newPV.getValue(newPrefix + Constants.ActionElementName));

            //msgId
            message.setValue(newPrefix + Constants.MessageIdentifierElementName, newPV.getValue(newPrefix + Constants.MessageIdentifierElementName));

            //message
            message.setValue(newPrefix + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName, newPV.getValue(newPrefix + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName));
        }

        return message;
    }

    public static ParameterValue duplicateSvcEprPV(ParameterValue pv, ParameterValue message) {
        message.setValue(Constants.SvcEprElementName, pv.getValue(Constants.SvcEprElementName));

        return message;
    }

    public static ParameterValue duplicateRoundsPV(ParameterValue pv, ParameterValue message) {
        message.setValue(Constants.RoundsElementName, pv.getValue(Constants.RoundsElementName));

        return message;
    }

    public static ParameterValue duplicateXSLTActionListPV(ParameterValue newPV, ParameterValue message) {
        String prefix = Constants.XsltActionListElementName + "/" + Constants.XsltActionElementName;

        // see number of XSLT Message elements in pvs
        int count = newPV.getChildrenCount(prefix);

        for (int i = 0; i < count; i++) {
            String tempPrefix = prefix + "[" + i + "]/";

            // xslt
            String location = newPV.getValue(tempPrefix + Constants.XsltElementName + "/" + Constants.LocationElementName);

            if ((location != null) && (!location.isEmpty())) {
                message.setValue(tempPrefix + Constants.XsltElementName + "/" + Constants.LocationElementName, location);
            } else {
                message.setValue(tempPrefix + Constants.XsltElementName + "/" + Constants.ContentElementName, newPV.getValue(tempPrefix + Constants.XsltElementName + "/" + Constants.ContentElementName));
            }

            // message container
            String newPrefix = tempPrefix + Constants.MessageInfoElementName + "/";

            //msgId
            message.setValue(newPrefix + Constants.MessageIdentifierElementName, newPV.getValue(newPrefix + Constants.MessageIdentifierElementName));

            // action
            message.setValue(newPrefix + Constants.ActionElementName, newPV.getValue(newPrefix + Constants.ActionElementName));
        }

        return message;
    }


    public static List filterSender(List svcs, String sender) {
        URI senderURI = new URI(sender);

        boolean remove = false;

        Service svc = null;

        for (int i = svcs.size() - 1; !remove && i >= 0; i--) {
            svc = (Service) svcs.get(i);
            Iterator iter = svc.getEndpointReferences();

            while (!remove && iter.hasNext()) {
                EndpointReference current = (EndpointReference) iter.next();
                URI curr = current.getAddress();

                remove = curr.equalsRFC3986(senderURI) || curr.equalsSTRCMP0(senderURI);
            }
        }

        if (remove) {
            svcs.remove(svc);
        }

        return svcs;
    }

    public static InvokeMessage setMessageResponseTo(InvokeMessage msg, SOAPHeader header) {
        if (header != null) {
            msg.setResponseTo(header);
            logger.debug("Message: relatesTo=" + msg.getRelatesTo() + "; to=" + msg.getTo());
        }

        return msg;
    }
}
