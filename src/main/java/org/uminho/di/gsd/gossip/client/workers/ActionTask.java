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
import org.uminho.di.gsd.gossip.GossipVariants;
import org.uminho.di.gsd.gossip.client.GossipClient;

public class ActionTask extends GossipWorkingTask {

	static Logger logger = Logger.getLogger(ActionTask.class);

	private GossipVariants activeVariant;

	public ActionTask(GossipClient cli) {
		super(cli);
	}

	public GossipVariants getActiveVariant() {
		return activeVariant;
	}

	public void setActiveVariant(GossipVariants activeVariant) {
		this.activeVariant = activeVariant;
	}

	@Override
	public void run() {

		long sleepTime = 1000;
		if (period > 50) {
			sleepTime = period;
		}

		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException ex) {
			logger.error(ex.getMessage(), ex);
		}

		while (!client.allMessages() && !terminate.get()) {
			logger.info("Waking up...");
			try {
				getClient().fireAction(activeVariant);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				terminate.set(true);
			}
			logger.info("Going to sleep...");
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException ex) {
				logger.error(ex.getMessage(), ex);
				terminate.set(true);
			}
		}

		logger.info(client.getDevice().getIdStr() + " : " + activeVariant + " task terminating!!!!!!!");
	}
}
