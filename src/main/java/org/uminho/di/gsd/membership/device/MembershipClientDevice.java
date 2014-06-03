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

package org.uminho.di.gsd.membership.device;

import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.uminho.di.gsd.common.Configuration;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.common.RunConstants;
import org.uminho.di.gsd.membership.client.MembershipRepositoryClient;
import org.uminho.di.gsd.membership.client.workers.SearchTask;
import org.uminho.di.gsd.membership.client.workers.UpdateTask;
import org.uminho.di.gsd.membership.info.MembershipRepository;
import org.ws4d.java.DPWSFramework;

public class MembershipClientDevice extends MembershipDevice {
	static Logger logger = Logger.getLogger(MembershipClientDevice.class);

	protected MembershipRepositoryClient client;

	public MembershipClientDevice() {
		super();
	}

	public MembershipRepositoryClient getClient() {
		return client;
	}

	public void initializeClient(MembershipRepository repository) {
		this.client = new MembershipRepositoryClient(repository);
		repository.setClient(client);
		this.client.setDevice(this);
	}

	public void initializeWorkers() {
		//        scheduledPool = new ScheduledThreadPoolExecutor(1);

		// service search
		//        initializeSearchTask();

		// membership update
		initializeUpdateTask();
	}

	protected Long initializeUpdateTask() {
		logger.debug(idStr + " Initializing Update task...");

		Random random = new Random();
		Long initialWaitingPeriod = (Constants.numberOfDevices * 500) + Constants.updateInitialDelay + random.nextInt((int) Constants.updateInitialDelay);
		Long period = (Long) Configuration.getConfigParamValue(Configuration.updatePeriod);
		UpdateTask updateTask = new UpdateTask(client, initialWaitingPeriod, period);
		DPWSFramework.getThreadPool().execute(updateTask);

		return initialWaitingPeriod;
	}

	protected void initializeSearchTask() {
		SearchTask searchTask = new SearchTask(client);
		// execute search once
		DPWSFramework.getThreadPool().execute(searchTask);
	}

	@Override
	public void stopServices()
	{
		DPWSFramework.getThreadPool().shutdown();

		client = null;

		super.stopServices();
	}

	public static void main(String[] args) throws Exception {
		if(args.length >= 2)
		{
			RunConstants constants = new RunConstants(args);
			// configure loggers
			PropertyConfigurator.configure("log4j.properties");

			MembershipClientDevice device = null;

			try {
				// always start the framework first
				DPWSFramework.start(args);

				// create a simple device ...
				device = new MembershipClientDevice();
				device.setConstants(constants);

				device.initializeConfiguration();

				device.initializeBinding();
				// ... and a service
				device.initializeMembershipService();

				device.startDevice();

				// initialize repository
				MembershipRepository repository = new MembershipRepository();
				repository.initializeWithDevice(device);

				device.getMembershipService().setRepository(repository);

				device.initializeClient(repository);

				device.initializeWorkers();

				repository = device.getMembershipService().getRepository();
				if(repository == null)
					logger.error(device.getIdStr() + "Repository is null!");
			} catch(Exception e)
			{
				logger.error(e.getMessage(), e);

				DPWSFramework.stop();
			}
		}
	}
}
