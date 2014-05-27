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

import org.uminho.di.gsd.gossip.service.GossipService;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.Service;

public interface GossipDevice extends Device {

	public String getIdStr();

	public void setMonitoringMembershipService(Service svc);
	public Service getMonitoringMembershipService();

	public void initializeGossipService();
	public GossipService getGossipService();

	public void initializeWorkers();
}