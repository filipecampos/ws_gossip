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
package org.uminho.di.gsd.membership.client;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.membership.info.DeviceInfo;
import org.uminho.di.gsd.membership.info.ServiceInfo;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.client.DefaultClient;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.dispatch.HelloData;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;

public class HelloListeningClient extends DefaultClient {
	static Logger logger = Logger.getLogger(HelloListeningClient.class);

	Service membershipService = null;

	public HelloListeningClient() {
		this.init();
	}

	private void init() {
		this.initDiscoveryListening();
	}

	private void initDiscoveryListening() {
		// makes helloReceived catch Hello messages
		this.registerHelloListening();
	}

	/**
	 * Callback method, if device hello was received
	 * @param hd
	 */
	@Override
	public void helloReceived(HelloData hd) {
		boolean query = true;

		EndpointReference endpointRef = hd.getEndpointReference();

		logger.info("Received Hello from " + endpointRef);

		ServiceReference svcRef;
		Device device;
		EndpointReference epr;

		// if device announced Gossip or MembershipType or if, according to configuration, it is supposed to be queried
		if (query) {
			try {
				DeviceReference deviceRef = getDeviceReference(endpointRef);
				device = deviceRef.getDevice();
				DeviceInfo devInfo = new DeviceInfo(device);
				logger.info("Created devInfo with ref : " + devInfo.getEndpointReference());
				Iterator services = null;

				services = device.getServiceReferences();
				while (services.hasNext()) {
					svcRef = (ServiceReference) services.next();
					logger.debug("helloReceived:ServiceId " + svcRef.getServiceId());

					ServiceInfo si = new ServiceInfo(svcRef, devInfo.getEndpointReference());
					logger.debug("Created ServiceInfo with preferredXAddress= " + si.getPreferredXAddress() + "; endpointAddresses: ");
					Iterator iterator = si.getEndpointAddresses().iterator();
					while(iterator.hasNext())
					{
						epr = (EndpointReference) iterator.next();
						logger.debug("EPR: " + epr);
					}

					if (membershipService == null) {
						catchService(svcRef, Constants.MembershipServiceId);
					}
					if (membershipService != null) {
						useGetTargetsOperation();
					}
				}
			} catch (TimeoutException ex) {
				logger.error(ex.getMessage(), ex);
			}
		}
	}

	private void catchService(ServiceReference svcRef, URI svcId) {
		if (svcRef.getServiceId().equals(svcId)) {
			try {
				membershipService = (Service) svcRef.getService();
			} catch (TimeoutException e) {
				logger.error(e.getMessage(), e);
			}

			logger.info("catchService:Found " + svcId);
		} else {
			logger.warn("catchService:" + svcRef.getServiceId() + " not equal to " + svcId);
		}

	}

	public void useGetTargetsOperation() {
		Operation getTargetsOperation = null;

		try {
			//We need to get the operation from the service.
			//getAnyOperation returns the first Operation that fits the specification in the parameters.
			getTargetsOperation = membershipService.getAnyOperation(Constants.MembershipPortTypeQName, "GetTargets");
			Thread.sleep(1000);
		} catch (InterruptedException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		if (getTargetsOperation != null) {
			ParameterValue pValue = getTargetsOperation.createInputValue();

			pValue.setValue("ServiceType", String.valueOf("Gossip"));
			pValue.setValue("Fanout", String.valueOf(3));

			logger.info("GetTargets invocation parameters set!");

			ParameterValue returnMessagePV = null;
			//now lets invoke our first operation
			try {
				returnMessagePV = getTargetsOperation.invoke(pValue);
				logger.info("GetTargets invoked!");

			} catch (InvocationException e) {
				logger.error(e.getMessage(), e);
			} catch (TimeoutException e) {
				logger.error(e.getMessage(), e);
			}

			if (returnMessagePV != null) {
				Iterator targets = returnMessagePV.getChildren("TargetsList");

				if (targets.hasNext()) {
					ParameterValue targetsList = (ParameterValue) targets.next();

					int size = targetsList.getChildrenCount("Endpoint");
					logger.info("TargetsList size " + size);

					int i = 0;

					while(i < size)
					{
						String target = returnMessagePV.getValue("TargetsList/Endpoint[" + i + "]");

						logger.info("GetTargets returned number " + i + " : " + target);
						i++;
					}
				}
			}
		} else {
			logger.warn("Operation is still null!");
		}

	}

	public static void main(String[] args) {
		DPWSFramework.start(args);
		Log.setLogLevel(Log.DEBUG_LEVEL_INFO);
		HelloListeningClient client = null;

		try {
			client = new HelloListeningClient();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			if (client != null) {
				logger.warn("Shutting down...");
			}

			DPWSFramework.stop();
			System.exit(0);

		}

	}
}
