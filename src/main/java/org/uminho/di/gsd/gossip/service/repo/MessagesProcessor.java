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
package org.uminho.di.gsd.gossip.service.repo;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.CommunicationProtocol;
import org.uminho.di.gsd.common.MessageUtil;
import org.uminho.di.gsd.gossip.GossipVariants;
import org.uminho.di.gsd.gossip.client.GossipClient;
import org.uminho.di.gsd.gossip.client.aggregation.AggGossipClient;
import org.uminho.di.gsd.gossip.service.GossipService;
import org.uminho.di.gsd.gossip.service.repo.aggregation.AggregationMessage;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.LinkedMap;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.URI;

public class MessagesProcessor {

	static Logger logger = Logger.getLogger(MessagesProcessor.class);
	private LinkedMap messages;
	protected GossipClient client;
	private GossipService service;
	String value;

	public MessagesProcessor(GossipService svc) {
		service = svc;
		messages = new LinkedMap();
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public GossipService getService() {
		return service;
	}

	public void setService(GossipService service) {
		this.service = service;
	}

	public void setClient(GossipClient cli) {
		client = cli;
	}

	public void process_push(CommunicationProtocol proto, long nanoTime, long millisTime, URI sender, ParameterValue pv) {
		// Extract messages from pv
		Iterator msgs = MessageUtil.extractMessagesFromPV(millisTime, pv);

		List to_send = null;
		if (service.getActiveVariant().equals(GossipVariants.Push)) {
			to_send = new ArrayList();
		}

		Message msg = null;

		int messagesCounter = 0;
		// For each message:
			while (msgs.hasNext()) {
				msg = (Message) msgs.next();
				messagesCounter++;

				//      Verify if message is already in repository, otherwise add it
				boolean present = addMessage(msg);


				String val = msg.getIdentifier().toString();
				int hops = (int) msg.decrementAndGetRounds();
				setReceivingTimeAndHops(val, nanoTime, hops);

				//      If not present, push it.
				if ((hops > 0) && (to_send != null) && !present) {
					logger.debug("Adding msg " + msg + " to sending list.");
					to_send.add(msg);
				}
			}

			if ((to_send != null) && (!to_send.isEmpty())) {
				// some messages were filtered
				logger.debug("Client going to send " + to_send + " messages.");
				client.invokePush(proto, null, sender, to_send, null);
				logger.debug("Client sending " + to_send + " messages.");
			}
	}

	public boolean addMessage(Message msg) {
		// If not present add it. No need to update the timestamp at the addition moment
		//      as it is collected at the object constructor.
		boolean present = messages.containsKey(msg.getIdentifier());
		if (!present) {
			logger.debug("Adding info on message " + msg.getIdentifier());
			messages.put(msg.getIdentifier(), msg);
		} else {
			logger.debug("Message " + msg.getIdentifier() + " is a duplicate!");
		}

		return present;
	}

	public synchronized List getRecentMessages(long timestamp) {
		List msgs = new ArrayList();

		Iterator allMsgs = messages.values().iterator();

		logger.debug("Messages Processor has " + messages.size() + " messages.TimeStamp: " + timestamp);
		Message msg = null;

		try {
			while (allMsgs.hasNext()) {
				msg = (Message) allMsgs.next();

				logger.debug("Message received_at=" + msg.getReceived_at());
				if (msg.getReceived_at() >= timestamp) {
					Message new_msg = (Message) msg.clone();
					new_msg.setRounds(0);
					msgs.add(new_msg);
				}
			}
		} catch (CloneNotSupportedException ex) {
			logger.error(ex.getMessage(), ex);
		}
		return msgs;
	}

	public void respondToAsyncPull(CommunicationProtocol proto, List messages, String svcEPR, SOAPHeader requestHeader) {
		List services = new ArrayList(1);
		services.add(svcEPR);

		client.invokePush(proto, services, null, messages, requestHeader);
	}

	public void respondToAsyncPushPull(CommunicationProtocol proto, List messages, String svcEPR, SOAPHeader requestHeader) {
		respondToAsyncPull(proto, messages, svcEPR, requestHeader);
	}

	public void respondToAsyncPullIds(CommunicationProtocol proto, List messages, String svcEPR, SOAPHeader requestHeader) {
		List services = new ArrayList(1);
		services.add(svcEPR);

//		client.invokePushIds(proto, services, null, messages);
	}

	public List getMessages(List identifiers) {
		List msgList = new ArrayList();

		if (identifiers.size() > 0) {
			Iterator ids = identifiers.iterator();

			while (ids.hasNext()) {
				URI id = (URI) ids.next();
				if (messages.containsKey(id)) {
					msgList.add(messages.get(id));
				}
			}
		}

		return msgList;
	}

	public Message getMessage(URI msgId) {
		return (Message) messages.get(msgId);
	}

	public void respond_to_pull_ids(CommunicationProtocol proto, SOAPHeader requestHeader, List messages, String svcEPR, URI destSvcEPR) {
		client.respondToPullIds(proto, requestHeader, messages, svcEPR, destSvcEPR);
	}

	public void respond_to_push_ids(CommunicationProtocol proto, SOAPHeader requestHeader, List msgsInfo, String svcEPR, URI destSvcEPR) {
		List ids = getWantedMessagesIds(msgsInfo);

		logger.debug("Got " + ids.size() + " wanted messages to fetch.");
		if (!ids.isEmpty()) {
			client.respondToPushIds(proto, requestHeader, ids, svcEPR, destSvcEPR);
		}
	}

	private List getWantedMessagesIds(List msgsInfo) {
		List ids = new ArrayList();

		int size = msgsInfo.size();
		Message msg = null;
		for (int i = 0; i < size; i++) {
			msg = (Message) msgsInfo.get(i);

			if (isWanted(msg)) {
				ids.add(msg.getIdentifier());
			}
		}

		return ids;
	}

	private boolean isWanted(Message msg) {
		boolean wanted = false;

		Message storedMsg = getMessage(msg.getIdentifier());
		wanted = storedMsg == null;
		// TODO: uncomment to use configured wanted action
		//        wanted = (storedMsg == null) && Configuration.isWantedAction(msg.getAction().toString());

		return wanted;
	}

	public String process_agg(ParameterValue newPV, long nanoTime, String msgId, AggregationMessage agg, String srcEpr) {
		String ret = null;

		int hops = (int) agg.decrementAndGetRounds();
		setReceivingTimeAndHops(msgId, nanoTime, hops);

		logger.debug("Client going to send " + newPV);

		ret = client.invokeTcpAgg(newPV, agg, srcEpr, -1);

		return ret;
	}

	public String process_tcp_agg_pull(ParameterValue pv, long nanoTime, String msgId, AggregationMessage agg, String srcEpr) {
		String ret = null;

		int hops = (int) agg.decrementAndGetRounds();
		setReceivingTimeAndHops(msgId, nanoTime, hops);

		logger.debug("Client going to send " + pv);

		// propagate pull request
		ret = client.invokeTcpAggPull(pv, agg, srcEpr, -1);

		// Respond to pull request

		return ret;
	}

	public void process_udp_agg_pull(ParameterValue pv, long nanoTime, String msgId, AggregationMessage agg, URI sender) {
		logger.debug(client.getDevice().getIdStr() + ": Client going to send " + pv);

		int hops = (int) agg.decrementAndGetRounds();
		setReceivingTimeAndHops(msgId, nanoTime, hops);

		// propagate pull request
		client.invokeUdpAggPull(pv, agg, sender, -1);
	}

	public synchronized void removeMessage(URI id) {
		if (messages.containsKey(id)) {
			messages.remove(id);
		}
	}

	private void setReceivingTimeAndHops(String msgId, long time, int hops) {
		logger.debug("MsgID: " + msgId);

		String[] parts = msgId.split("-");
		if (parts.length > 1) {
			int port = Integer.parseInt(parts[0]);
			int id = Integer.parseInt(parts[1]);

			client.setReceived(port, id, time);
			client.setReceivedHops(port, id, hops);
		}
	}

	// Should be invoked after processing an aggregation message
	public void setCurrentAggregateValue(String msgId, String value) {
		logger.debug(client.getDevice().getIdStr() + " - Setting Agg - MsgID: " + msgId + "; Value: " + value);

		String[] parts = msgId.split("-");
		if (parts.length > 1) {
			int port = Integer.parseInt(parts[0]);
			int id = Integer.parseInt(parts[1]);

			((AggGossipClient) client).setAggregateValue(port, id, value);
		}
	}

	public void process_agg_push(CommunicationProtocol comm, ParameterValue newPV, long nanoTime, String msgId, Message msg, String srcEpr) {
		int hops = (int) msg.decrementAndGetRounds();
		setReceivingTimeAndHops(msgId, nanoTime, hops);

		logger.debug("Client going to send " + newPV);
		if (comm.equals(CommunicationProtocol.TCP)) {
			client.invokeTcpAggPush(newPV, srcEpr);
		}
		else if (comm.equals(CommunicationProtocol.UDP)) {
			client.invokeUdpAggPush(newPV, srcEpr);
		}
	}
}
