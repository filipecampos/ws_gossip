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
import org.uminho.di.gsd.gossip.service.repo.aggregation.AggregationMessage;

public class TimeoutTask extends Thread {

	static final Logger logger = Logger.getLogger(TimeoutTask.class);

	AggregationMessage msg;
	long time;

	boolean cancelled;

	public TimeoutTask(AggregationMessage agg, long period)
	{
		msg = agg;
		time = period;
		cancelled = false;
	}

	@Override
	public void run() {
		logger.debug("Going to sleep for " + time + " ms...");
		try {
			Thread.sleep(time);
		} catch (InterruptedException ex) {
			logger.error(ex.getMessage(), ex);
		}
		logger.debug("Wokeup from " + time + " ms sleep.");

		if(!cancelled)
			msg.timeout();
	}

	public void cancel()
	{
		cancelled = true;
		interrupt();
		logger.debug("Was cancelled!");
	}
}
