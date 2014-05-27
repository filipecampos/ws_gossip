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
package org.uminho.di.gsd.common.device;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.uminho.di.gsd.common.Configuration;
import org.uminho.di.gsd.common.RunConstants;
import org.uminho.di.gsd.gossip.GossipVariants;
import org.uminho.di.gsd.gossip.client.GossipClient;
import org.uminho.di.gsd.gossip.client.workers.ActionTask;
import org.uminho.di.gsd.gossip.client.workers.GossipWorkingTask;
import org.uminho.di.gsd.gossip.device.GossipDevice;
import org.uminho.di.gsd.gossip.service.GossipService;
import org.uminho.di.gsd.membership.device.MembershipClientDevice;
import org.uminho.di.gsd.membership.info.MembershipRepository;
import org.uminho.di.gsd.membership.service.MembershipService;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.communication.HTTPBinding;
import org.ws4d.java.service.Service;
import org.ws4d.java.types.URI;

public class GossipDeviceImpl extends MembershipClientDevice implements GossipDevice {

	final static Logger logger = Logger.getLogger(GossipDeviceImpl.class);

	private GossipService gossipService;
	private GossipClient gossipClient;

	public GossipClient getGossipClient() {
		return gossipClient;
	}

	public void setGossipClient(GossipClient client) {
		this.gossipClient = client;
	}

	@Override
	public Service getMonitoringMembershipService() {
		return getMembershipService();
	}

	@Override
	public void setMonitoringMembershipService(Service service) {
		setMembershipService((MembershipService) service);
	}

	@Override
	public GossipService getGossipService() {
		return gossipService;
	}

	@Override
	public void initializeGossipService() {
		gossipService = new GossipService();
		String svcEPR = "http://" + IP + ":" + PORT + "/device/gossip/service";
		gossipService.addBinding(new HTTPBinding(new URI(svcEPR)));
		gossipService.setServiceEPR(svcEPR);

		this.addService(gossipService);
	}

	public void initializeGossipClient() {
		gossipClient = new GossipClient(this);
		gossipService.setClient(gossipClient);
	}

	@Override
	public void initializeWorkers() {
		//        scheduledPool = new ScheduledThreadPoolExecutor(2);

		// Membership
		// service search
		//        initializeSearchTask();

		// membership update
		Long initialWaitingPeriod = initializeUpdateTask();


		// Gossip
		// check config and initialize working thread if necessary
		GossipVariants prefVariant = (GossipVariants) Configuration.getConfigParamValue(Configuration.prefVariant);

		GossipWorkingTask workingTask = null;

		if (prefVariant.equals(GossipVariants.Pull)) {
			// pull task
			workingTask = new ActionTask(gossipClient);
		}

		if (workingTask != null) {
			Long pullPeriod = (Long) Configuration.getConfigParamValue(Configuration.pullPeriod);
			//            scheduledPool.scheduleWithFixedDelay(workingTask, initialWaitingPeriod, pullPeriod, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public void startServices() {
		this.startGossipService();

		super.startServices();
	}

	private void startGossipService() {
		try {
			gossipService.start();
		} catch (IOException ex) {
			logger.error(ex);
		}
	}

	@Override
	public void stopServices() {
		try {
			gossipService.stop();
		} catch (IOException ex) {
			logger.error(ex);
		}
		super.stopServices();
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length >= 2) {
			RunConstants constants = new RunConstants(args);

			// configure loggers
			PropertyConfigurator.configure("log4j.properties");

			// always start the framework first
			DPWSFramework.start(args);

			// create the device ...
			GossipDeviceImpl device = new GossipDeviceImpl();
			device.setConstants(constants);

			try
			{
				device.initializeConfiguration();

				device.initializeBinding();

				// ... and the services
				device.initializeMembershipService();
				device.initializeGossipService();


				// start services and device
				device.startDevice();

				// initialize repository
				MembershipRepository repository = new MembershipRepository();
				repository.initializeWithDevice(device);

				device.getMembershipService().setRepository(repository);

				device.initializeClient(repository);
				device.initializeGossipClient();

				device.initializeWorkers();
			}
			catch (Exception e)
			{
				logger.error(e.getMessage(), e);
			}
		}
	}
}
