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
package org.uminho.di.gsd.gossip.service;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.application.service.ApplicationService;
import org.uminho.di.gsd.application.service.operations.InfoTempOperation;
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.uminho.di.gsd.common.CommunicationProtocol;
import org.uminho.di.gsd.common.device.BasicDevice;
import org.uminho.di.gsd.gossip.GossipVariants;
import org.uminho.di.gsd.gossip.client.GossipClient;
import org.uminho.di.gsd.gossip.service.operations.FetchOperation;
import org.uminho.di.gsd.gossip.service.operations.PullIdsOperation;
import org.uminho.di.gsd.gossip.service.operations.PullOperation;
import org.uminho.di.gsd.gossip.service.operations.PushIdsOperation;
import org.uminho.di.gsd.gossip.service.operations.PushOperation;
import org.uminho.di.gsd.gossip.service.operations.PushPullOperation;
import org.uminho.di.gsd.gossip.service.operations.aggregation.AggOperation;
import org.uminho.di.gsd.gossip.service.operations.aggregation.AggPullOperation;
import org.uminho.di.gsd.gossip.service.operations.aggregation.AggPushOperation;
import org.uminho.di.gsd.gossip.service.repo.Message;
import org.uminho.di.gsd.gossip.service.repo.MessagesProcessor;
import org.uminho.di.gsd.shadow.service.ShadowService;
import org.uminho.di.gsd.shadow.service.ShadowServiceFactory;
import org.ws4d.java.io.xml.XmlSerializer;
import org.ws4d.java.io.xml.XmlSerializerImplementation;
import org.ws4d.java.service.DefaultService;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.IDGenerator;

public class GossipService extends DefaultService {

	static Logger logger = Logger.getLogger(GossipService.class);

	private GossipVariants activeVariant;
	private CommunicationProtocol protocol;

	private BasicDevice device;

	protected ApplicationService appService;

	// my binding uri
	protected String svcEPR;

	// operations
	protected PushOperation pushOp;

	protected PullOperation pullOp;

	protected PushPullOperation pushPullOp;

	protected PullIdsOperation pullIdsOp;

	protected FetchOperation fetchOp;

	protected PushIdsOperation pushIdsOp;

	// Aggregation
	protected AggPushOperation aggPushOp;
	protected AggPullOperation aggPullOp;
	protected AggOperation aggOp;

	// message processor
	protected MessagesProcessor processor;

	// gossip client
	protected GossipClient client;

	int port;

	private Boolean activateShadowService = false;
	private List shadowServices;
	private HashMap indexedOps;

	public void setIndexedOps(HashMap ops)
	{
		indexedOps = ops;
	}

	public Boolean isActivateShadowService() {
		return activateShadowService;
	}

	public void setActivateShadowService(Boolean activateShadowService) {
		this.activateShadowService = activateShadowService;
	}

	public GossipService() {
		super();

		activeVariant = GossipVariants.Push;

		this.setServiceId(new URI("GossipService"));

		initializeProcessor();

		initializeOperations();
	}

	public void setDevice(BasicDevice device)
	{
		this.device = device;
		logger.debug("GossipService: Device is set!");

		if(logger.isDebugEnabled())
			device.inspectServicesAndOperations();
	}

	public void startShadowServices() throws IOException
	{
		if(device != null)
		{
			logger.debug("Replicating Device's Normal Services...");
			ShadowServiceFactory.replicateNormalServices(device, this);
			logger.debug("Replicated Device's Normal Services.");

			if(logger.isDebugEnabled())
				device.inspectServicesAndOperations();
		}
		else
		{
			logger.error("Device is null!");
			return;
		}
	}

	public void stopShadowServices() throws IOException
	{
		if((shadowServices != null) && (!shadowServices.isEmpty()))
		{
			Iterator iter = shadowServices.listIterator();
			while(iter.hasNext())
			{
				ShadowService svc = (ShadowService) iter.next();
				svc.stop();
			}
		}
	}

	public GossipVariants getActiveVariant() {
		return activeVariant;
	}

	public void setActiveVariant(GossipVariants activeVariant) {
		this.activeVariant = activeVariant;
	}

	public CommunicationProtocol getProtocol() {
		return protocol;
	}

	public void setProtocol(CommunicationProtocol protocol) {
		this.protocol = protocol;
	}

	public void setServiceEPR(String epr)
	{
		svcEPR = epr;
	}

	public String getSvcEPR() {
		return svcEPR;
	}

	public ApplicationService getAppService() {
		return appService;
	}

	public void setAppService(ApplicationService appService) {
		this.appService = appService;
	}

	protected void initializeProcessor()
	{
		processor = new MessagesProcessor(this);
	}

	public void setClient(GossipClient cli)
	{
		client = cli;
		processor.setClient(client);
		port = ((BasicDevice) client.getDevice()).getConstants().getPort();
	}

	protected void initializeOperations() {
		pushOp = new PushOperation();
		pushOp.setProcessor(processor);
		this.addOperation(pushOp);

		pullOp = new PullOperation();
		pullOp.setProcessor(processor);
		this.addOperation(pullOp);

		pushPullOp = new PushPullOperation();
		pushPullOp.setProcessor(processor);
		this.addOperation(pushPullOp);

		pullIdsOp = new PullIdsOperation();
		pullIdsOp.setProcessor(processor);
		this.addOperation(pullIdsOp);

		fetchOp = new FetchOperation();
		fetchOp.setProcessor(processor);
		this.addOperation(fetchOp);

		pushIdsOp = new PushIdsOperation();
		pushIdsOp.setProcessor(processor);
		this.addOperation(pushIdsOp);

		aggPushOp = new AggPushOperation();
		aggPushOp.setProcessor(processor);
		this.addOperation(aggPushOp);

		aggPullOp = new AggPullOperation();
		aggPullOp.setProcessor(processor);
		this.addOperation(aggPullOp);

		aggOp = new AggOperation();
		aggOp.setProcessor(processor);
		this.addOperation(aggOp);
	}

	public PushOperation getPushOperation() {
		return pushOp;
	}

	public PullOperation getPullOperation() {
		return pullOp;
	}

	public PushPullOperation getPushPullOperation() {
		return pushPullOp;
	}

	public PushIdsOperation getPushIdsOperation() {
		return pushIdsOp;
	}

	public PullIdsOperation getPullIdsOperation() {
		return pullIdsOp;
	}

	public FetchOperation getFetchOperation() {
		return fetchOp;
	}

	public void createMessage(int i) {
		String element = "<InfoTemp>" + i + "</InfoTemp>";

		long creationMillis = System.currentTimeMillis();
		long creationNanos = System.nanoTime();
		logger.debug("Creating message " + i + " at " + creationMillis);
		Message msg = new Message(new URI(port + "-" + Integer.toString(i)), new URI(ApplicationServiceConstants.infoTempOpName), 3, element, creationMillis);

		processor.addMessage(msg);

		// store creation time instant
		client.setSent(i, creationNanos);
	}

	public AggPushOperation getAggPushOperation() {
		return aggPushOp;
	}

	public AggOperation getAggOperation() {
		return aggOp;
	}

	public AggPullOperation getAggPullOperation() {
		return aggPullOp;
	}

	public MessagesProcessor getProcessor() {
		return processor;
	}

	public void disseminateShadowInvocation(String actionName, ParameterValue parameterValue, long nanoTime, long millisTime) {
		// always consider message as a new one
		// create messageId
		URI messageId = IDGenerator.getUUIDasURI();
		logger.debug("Creating message " + messageId + " at " + millisTime);
		XmlSerializer serializer = new XmlSerializerImplementation();
		StringWriter writer = new StringWriter();
		serializer.setOutput(writer);
		try {
			parameterValue.serialize(serializer);
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		}
		String text = writer.toString();
		logger.debug("Serialized pv: " + text);


		InfoTempOperation infoTempOp = new InfoTempOperation(appService);
		ParameterValue newPV = infoTempOp.createInputValue();

		newPV.setValue(text);
		logger.debug("Original pv: " + parameterValue);
		logger.debug("Parsed serialized pv: " + newPV);

		Message msg = new Message(messageId, new URI(actionName), 3, text, millisTime);

		// all variants: create and store message.
		processor.addMessage(msg);

		if(activeVariant == GossipVariants.Push)
		{
			// push: send message
			client.firePush(msg);
			logger.debug("Fired push for message " + msg);
		}

		// other variants: action will be called periodically and message will eventually be disseminated
	}


}
