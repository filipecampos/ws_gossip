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

package org.uminho.di.gsd.membership.info;

import org.apache.log4j.Logger;
import org.ws4d.java.service.Device;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.URI;

public class DeviceInfo extends AbstractInfo {

	static Logger logger = Logger.getLogger(DeviceInfo.class);

	// device
	private List deviceTypes;
	private URI endpointReference; // used to uniquely identify the device
	private List xAddresses;
	private List scopes;
	private long metadataVersion;

	// services
	private List hostedServices;

	public DeviceInfo(Device device)
	{
		deviceTypes = new ArrayList(device.getPortTypes());
		endpointReference = device.getEndpointReference().getAddress();
		xAddresses = new ArrayList(device.getXAddresses());
		scopes = new ArrayList(device.getScopes());
		metadataVersion = device.getMetadataVersion();
		hostedServices = new ArrayList();

		// hostedServices initialized in MembershipRepository.initializeHostedServices(MembershipDevice device)
		setHeartbeat(0);
	}

	public void setEndpointReference(URI endpointReference) {
		this.endpointReference = endpointReference;
	}

	public void setxAddresses(List xAddresses) {
		this.xAddresses = xAddresses;
	}

	public List getDeviceTypes() {
		return deviceTypes;
	}

	public URI getEndpointReference() {
		return endpointReference;
	}

	public List getHostedServices() {
		return hostedServices;
	}

	public long getMetadataVersion() {
		return metadataVersion;
	}

	public List getScopes() {
		return scopes;
	}

	public List getxAddresses() {
		return xAddresses;
	}

	public void addServiceAddress(URI addr)
	{
		if(!hostedServices.contains(addr))
			hostedServices.add(addr);

		updated = System.currentTimeMillis();
	}

	public void removeService(URI serviceAddress) {
		if(hostedServices.contains(serviceAddress))
			hostedServices.remove(serviceAddress);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("\n\nDevice:\nEPR-");
		sb.append(endpointReference.toString());

		sb.append("\nDevice Types:\n");
		Iterator iter = deviceTypes.iterator();
		while(iter.hasNext())
		{
			sb.append(iter.next());
			sb.append('\n');
		}

		sb.append("xAddresses:\n");
		iter = xAddresses.iterator();
		while(iter.hasNext())
		{
			sb.append(iter.next());
			sb.append('\n');
		}

		sb.append("Scopes:");
		iter = scopes.iterator();
		while(iter.hasNext())
		{
			sb.append(iter.next());
			sb.append('\n');
		}

		sb.append("\nMetadataVersion-");
		sb.append(metadataVersion);

		sb.append(super.toString());

		return sb.toString();
	}
}
