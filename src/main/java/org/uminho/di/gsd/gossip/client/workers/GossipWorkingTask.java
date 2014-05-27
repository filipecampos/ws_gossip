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

import java.util.concurrent.atomic.AtomicBoolean;

import org.uminho.di.gsd.gossip.client.GossipClient;

public abstract class GossipWorkingTask implements Runnable {

	protected GossipClient client;
	protected AtomicBoolean terminate;
	protected long period;

	public GossipWorkingTask()
	{
		terminate = new AtomicBoolean(false);
	}

	public GossipWorkingTask(GossipClient cli)
	{
		this();
		this.client = cli;
	}

	public GossipClient getClient() {
		return client;
	}

	public void setClient(GossipClient client) {
		this.client = client;
	}

	public boolean isTerminate() {
		return terminate.get();
	}

	public void setTerminate(boolean terminate) {
		this.terminate.set(terminate);
	}

	public long getPeriod() {
		return period;
	}

	public void setPeriod(long period) {
		this.period = period;
	}

	@Override
	public abstract void run();
}
