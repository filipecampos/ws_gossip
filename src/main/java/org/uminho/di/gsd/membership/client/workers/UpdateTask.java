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

package org.uminho.di.gsd.membership.client.workers;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.membership.client.MembershipRepositoryClient;

public class UpdateTask extends MembershipWorkingTask {
	static Logger logger = Logger.getLogger(UpdateTask.class);
	long initialWaitingPeriod;
	long period;

	public UpdateTask(MembershipRepositoryClient cli)
	{
		super(cli);
	}

	public UpdateTask(MembershipRepositoryClient client, Long initialWaitingPeriod, Long period) {
		super(client);

		this.initialWaitingPeriod = initialWaitingPeriod;
		this.period = period;
	}

	@Override
	public void run() {
		try {
			Thread.sleep(initialWaitingPeriod);

			while(true)
			{
				logger.debug("Waking up...");
				try
				{
					if(getClient() == null)
						logger.error("Client is NULL! Couldn't initialize update...");
					else
						getClient().initUpdate();
				}
				catch(Exception e)
				{
					logger.error(e.getMessage(), e);
				}
				logger.debug("Going to sleep...");
				Thread.sleep(period);
			}
		} catch (InterruptedException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}
}
