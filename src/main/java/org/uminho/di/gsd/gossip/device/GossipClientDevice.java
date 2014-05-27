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

package org.uminho.di.gsd.gossip.device;

import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Configuration;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.gossip.GossipVariants;
import org.uminho.di.gsd.gossip.client.GossipClient;
import org.uminho.di.gsd.gossip.client.workers.ActionTask;
import org.uminho.di.gsd.gossip.client.workers.GossipWorkingTask;
import org.uminho.di.gsd.gossip.service.GossipService;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.communication.HTTPBinding;
import org.ws4d.java.service.Service;
import org.ws4d.java.types.URI;

public class GossipClientDevice extends GossipServiceDevice implements GossipDevice {

	static Logger logger = Logger.getLogger(GossipClientDevice.class);

	private GossipClient client;
	private Service monitoringMembershipService = null;

	private ScheduledThreadPoolExecutor scheduledPool;

	public GossipClient getClient() {
		return client;
	}

	public void setClient(GossipClient client) {
		this.client = client;
	}

	@Override
	public Service getMonitoringMembershipService() {
		return monitoringMembershipService;
	}

	@Override
	public void setMonitoringMembershipService(Service service) {
		monitoringMembershipService = service;
	}

	@Override
	public String getIdStr() {
		return PORT;
	}

	@Override
	public void initializeGossipService() {
		gossipService = new GossipService();
		String svcEPR = "http://" + IP + ":" + PORT + "/gossip/service";
		gossipService.addBinding(new HTTPBinding(new URI(svcEPR)));
		gossipService.setServiceEPR(svcEPR);

		this.addService(gossipService);
	}

	public void initializeClient() {
		client = new GossipClient(this);
		gossipService.setClient(client);
		client.initializeMonitoringService();
	}

	public void initializeWorkers() {
		// check config and initialize working thread if necessary
		GossipVariants prefVariant = GossipVariants.valueOf((String) Configuration.getConfigParamValue(Configuration.prefVariant));

		GossipWorkingTask workingTask = null;

		if(prefVariant.equals(GossipVariants.Pull))
		{
			// pull task
			workingTask = new ActionTask(client);
		}

		if(workingTask != null)
		{
			scheduledPool = new ScheduledThreadPoolExecutor(1);
			Random random = new Random();
			scheduledPool.scheduleWithFixedDelay(workingTask, 
					(Constants.numberOfDevices * 500)
					+ Constants.updateInitialDelay
					+ random.nextInt((int) Constants.updateInitialDelay),
					Constants.updatePeriod, TimeUnit.MILLISECONDS);
		}
	}

	public static void main(String[] args) throws Exception {
		// always start the framework first
		DPWSFramework.start(args);

		// create a simple device ...
		GossipClientDevice device = new GossipClientDevice();

		device.initializeConfiguration();

		device.initializeBinding();

		// ... and a service
		device.initializeGossipService();

		device.initializeClient();

		// add service to device in order to support automatic discovery ...
		device.startDevice();
	}
}
