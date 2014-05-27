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
import org.ws4d.java.client.SearchParameter;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.URI;
import org.ws4d.java.util.Log;

public class DeviceSearchingClient extends DefaultClient {

	static Logger logger = Logger.getLogger(DeviceSearchingClient.class);

	Service membershipService = null;

	public DeviceSearchingClient() {
		super();
	}

	@Override
	public void deviceFound(DeviceReference devRef, SearchParameter searchParams) {
		logger.info("deviceFound:Found Device with Reference " + devRef.getEndpointReference());

		logger.debug("deviceFound:Search Parameter was...");
		// devices
		QNameSet dvcTypes = searchParams.getDeviceTypes();
		if ((dvcTypes != null) && (!dvcTypes.isEmpty())) {
			logger.debug("DeviceTypes:");
			Iterator iterator = dvcTypes.iterator();

			while (iterator.hasNext()) {
				logger.debug(iterator.next());
			}
		}

		Device device = null;
		ServiceReference svcRef = null;
		EndpointReference epr = null;

		try {
			device = devRef.getDevice();
			DeviceInfo devInfo = new DeviceInfo(device);
			logger.info("Created devInfo with ref : " + devInfo.getEndpointReference());
			Iterator services = null;

			services = device.getServiceReferences();
			while (services.hasNext()) {
				svcRef = (ServiceReference) services.next();
				logger.debug("deviceFound:ServiceId " + svcRef.getServiceId());

				ServiceInfo si = new ServiceInfo(svcRef, devInfo.getEndpointReference());
				logger.debug("deviceFound:Created ServiceInfo with preferredXAddress= " + si.getPreferredXAddress() + "; endpointAddresses: ");
				Iterator iterator = si.getEndpointAddresses().iterator();
				while (iterator.hasNext()) {
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

	public void probe() {
		// we define the service to search.
		SearchParameter params = new SearchParameter();
		params.setDeviceTypes(new QNameSet(QName.construct("{http://gsd.di.uminho.pt/ws/2010/06/membership}MembershipDevice")));

		searchDevice(params);
		logger.info("probe:Searching for Device...");
	}

	public void useGetTargetsOperation() {
		Operation getTargetsOperation = null;

		try {
			//We need to get the operation from the service.
			//getAnyOperation returns the first Operation that fits the specification in the parameters.
			getTargetsOperation = membershipService.getAnyOperation(Constants.MembershipServiceQName, "GetTargets");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		if (getTargetsOperation != null) {
			ParameterValue pValue = getTargetsOperation.createInputValue();

			pValue.setValue("ServiceType", String.valueOf("Gossip"));
			pValue.setValue("Fanout", String.valueOf(3));

			logger.debug("GetTargets invocation parameters set!");

			ParameterValue returnMessagePV = null;
			//now lets invoke our first operation
			try {
				returnMessagePV = getTargetsOperation.invoke(pValue);
				logger.debug("GetTargets invoked!");
			} catch (InvocationException e) {
				logger.error(e.getMessage(), e);
			} catch (TimeoutException e) {
				logger.error(e.getMessage(), e);
			}

			if (returnMessagePV != null) {
				Iterator targets = returnMessagePV.getChildren("TargetsList");

				if (targets.hasNext()) {
					ParameterValue targetsList = (ParameterValue) targets.next();

					logger.debug("TargetsList size " + targetsList.getChildrenCount("Endpoint"));
				}
				String firstTarget = returnMessagePV.getValue("TargetsList/Endpoint[0]");

				logger.debug("GetTargets returned " + firstTarget);
			}
		} else {
			logger.warn("Operation is still null!");
		}
	}

	public static void main(String[] args) {
		DPWSFramework.start(args);
		Log.setLogLevel(Log.DEBUG_LEVEL_INFO);
		DeviceSearchingClient client = null;

		try {
			client = new DeviceSearchingClient();

			client.probe();
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
