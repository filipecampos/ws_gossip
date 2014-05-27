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
package org.uminho.di.gsd.shadow.service;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.device.BasicDevice;
import org.uminho.di.gsd.gossip.service.GossipService;
import org.uminho.di.gsd.shadow.service.operations.ShadowOperation;
import org.ws4d.java.communication.HTTPBinding;
import org.ws4d.java.service.DefaultService;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.types.URI;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.EndpointReference;

public class ShadowService extends DefaultService {

	static Logger logger = Logger.getLogger(ShadowService.class);

	// device
	BasicDevice device;

	// mimicked service
	Service service;

	// gossip service
	GossipService gossipService;

	// mimicked operations
	Operation operation;

	public ShadowService(BasicDevice dvc, GossipService gos, Service svc)
	{
		super();

		device = dvc;
		gossipService = gos;
		service = svc;

		String serviceId = svc.getServiceId().toString();
		logger.info("Setting URI SvcId:Shadow" + serviceId);
		this.setServiceId(new URI("Shadow" + serviceId));

	}

	public void initialize()
	{
		initializeBindings();
		replicateOperations();
	}

	private void initializeBindings()
	{
		Iterator eprs = service.getEndpointReferences();
		while(eprs.hasNext())
		{
			EndpointReference epr = (EndpointReference) eprs.next();
			URI address = epr.getAddress();
			URI appendedAddress = address.append("shadow");
			URI newAddress = URI.changePath(address, "/device/shadow_svc0");
			addBinding(new HTTPBinding(appendedAddress));
			logger.info("EPR: address=" + address + "; appendedAddress=" + appendedAddress + "; newAddress=" + newAddress);
		}
	}

	private void replicateOperations()
	{
		Iterator ops = service.getOperations();

		while(ops.hasNext())
		{
			Operation op = (Operation) ops.next();
			Operation replicateOp = new ShadowOperation(gossipService, op);
			addOperation(replicateOp);
			logger.debug("Added replica operation " + op.getPortType() + "-" + op.getName());
		}
	}
}
