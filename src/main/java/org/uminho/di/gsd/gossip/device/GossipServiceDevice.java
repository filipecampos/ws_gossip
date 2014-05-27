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

import java.io.IOException;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.device.BasicDevice;
import org.uminho.di.gsd.gossip.service.GossipService;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.communication.HTTPBinding;
import org.ws4d.java.service.Service;
import org.ws4d.java.types.URI;

public class GossipServiceDevice extends BasicDevice implements GossipDevice {
	static Logger logger = Logger.getLogger(GossipServiceDevice.class);

	protected GossipService gossipService;

	public void initializeGossipService() {
		gossipService = new GossipService();
		gossipService.addBinding(new HTTPBinding(new URI("http://" + IP + ":" + PORT + "/gossip/service")));

		this.addService(gossipService);
	}

	@Override
	public void startDevice() {
		this.startGossipService();

		super.startDevice();
	}

	private void startGossipService() {
		try {
			gossipService.start();
		} catch (IOException ex) {
			logger.error(idStr + ex.getMessage(), ex);
		}
	}

	@Override
	public GossipService getGossipService() {
		return gossipService;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	 public static void main(String[] args) throws Exception {
		 // always start the framework first
		 DPWSFramework.start(args);

		 // create a simple device ...
		 GossipServiceDevice device = new GossipServiceDevice();
		 device.initializeBinding();

		 // ... and a service
		 device.initializeGossipService();

		 // add service to device in order to support automatic discovery ...
		 device.startDevice();
	 }

	 @Override
	 public void setMonitoringMembershipService(Service svc) {
		 throw new UnsupportedOperationException("Not supported yet.");
	 }

	 @Override
	 public Service getMonitoringMembershipService() {
		 throw new UnsupportedOperationException("Not supported yet.");
	 }

	 @Override
	 public void initializeWorkers()
	 {
		 throw new UnsupportedOperationException("Not supported yet.");
	 }
}
