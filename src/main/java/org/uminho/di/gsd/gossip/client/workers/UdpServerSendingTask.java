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

package org.uminho.di.gsd.gossip.client.workers;

import org.apache.log4j.Logger;
import org.ws4d.java.communication.protocol.soap.server.SOAPoverUDPServer;

public class UdpServerSendingTask extends GossipWorkingTask {
	static Logger logger = Logger.getLogger(UdpServerSendingTask.class);

	SOAPoverUDPServer sender;
	String targetIp;
	int targetPort;
	byte[] msg;
	int length;

	public UdpServerSendingTask(SOAPoverUDPServer sender, String targetIp, int targetPort, byte[] msg, int length) {
		super();
		this.sender = sender;
		this.targetIp = targetIp;
		this.targetPort = targetPort;
		this.msg = msg;
		this.length = length;
	}

	@Override
	public void run() {
		try {
			logger.debug("Will send message with " + length + " bytes.");
			sender.send(targetIp, targetPort, msg, length);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		} 
	}
}
