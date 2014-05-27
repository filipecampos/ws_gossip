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

package org.uminho.di.gsd.gossip.service.repo.aggregation;

import java.io.StringReader;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.gossip.client.workers.TimeoutTask;
import org.uminho.di.gsd.gossip.service.operations.aggregation.AggregationOperation;
import org.uminho.di.gsd.gossip.service.repo.Message;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.URI;

public class AggregationMessage extends Message {

	static Logger logger = Logger.getLogger(AggregationMessage.class);
	// contains all the received responses
	List responses;
	StringBuilder sb;
	// timeoutTask
	TimeoutTask timeout;

	List invokers;

	// mutex
	final Lock lock = new ReentrantLock();
	// condition
	boolean aggregated = false;
	final Condition aggr = lock.newCondition();

	final Semaphore processed = new Semaphore(1);

	// number of responses to wait for
	int wait;
	// maximum number of responses (fanout)
	int max;
	// XSLT
	String xslt;
	StringReader reader = null;

	String value = null;

	int diff = 1;

	public AggregationMessage(URI identifier, URI action, long rounds, String message, long millis, String ownValue, int fanout, int wait, long time, String xslt) {
		super(identifier, action, rounds, message, millis);

		// initialize
		sb = new StringBuilder();
		responses = new ArrayList();

		invokers = new ArrayList();

		// add own value
		responses.add(ownValue);
		addResponseToXML(ownValue);

		this.xslt = xslt;

		this.wait = wait;
		max = fanout;

		logger.debug("Id:" + getIdentifier() + " wait:" + wait + "; max:" + fanout + "; time:" + time + "; aggregated: " + aggregated);

		if(time > 0)
		{
			logger.debug("Id:" + getIdentifier() + " Starting timeout task...");
			timeout = new TimeoutTask(this, time);
			DPWSFramework.getThreadPool().execute(timeout);
		}
	}

	public void setMax(int max) {
		this.max = max;
	}

	public void setWait(int wait) {
		this.wait = wait;
	}

	public synchronized void addAggValue(String value) {
		responses.add(value);
		addResponseToXML(value);
		diff = 2;

		logger.debug("Id:" + getIdentifier() + " wait:" + wait + "; max:" + max + "; aggregated: " + aggregated);
		logger.debug("Id:" + getIdentifier() + " Current xml: " + sb.toString());
	}

	public synchronized void addResponse(String value) {
		logger.debug("Id:" + getIdentifier() + " wait:" + wait + "; max:" + max + "; aggregated: " + aggregated);
		if(!aggregated)
		{
			logger.debug("Id:" + getIdentifier() + " Adding received value: " + value);
			responses.add(value);
			addResponseToXML(value);

			checkReceivedResponses();
		}
	}

	public boolean checkReceivedResponses() {
		logger.debug("Id:" + getIdentifier() + " wait:" + wait + "; max:" + max + "; aggregated: " + aggregated);
		int num = responses.size() - diff;

		logger.debug("Id:" + getIdentifier() + " Responses: " + responses.size() + "; diff: " + diff + "; wait: " + wait + "; max: " + max);

		boolean cond = (num == wait) || (num == max);
		if (cond) {

			logger.debug("Id:" + getIdentifier() + " Going to unlock...");
			// kill timer task
			if((timeout != null) && (timeout.isAlive()))
				timeout.cancel();

			// if lock is still closed, open it so response is returned
			unlock();
		}

		return cond;
	}

	private void addResponseToXML(String value) {
		sb.append("<n1:NewTemp xmlns:n1=\"http://gsd.di.uminho.pt/example/\"><n1:TempValue>");
		sb.append(value).append("</n1:TempValue></n1:NewTemp>");
	}

	public synchronized void timeout() {
		logger.debug("Id:" + getIdentifier() + " timeouting...");
		// if mutex is still closed, open it so response is returned
		unlock();
		logger.debug("Id:" + getIdentifier() + " timeouted.");
	}

	public String getResponseValue() {
		logger.debug("Id:" + getIdentifier() + " wait:" + wait + "; max:" + max + "; aggregated: " + aggregated);
		// block on mutex while condition not satisfied
		logger.debug("Id:" + getIdentifier() + " Blocked waiting for Aggregation Responses...");
		lock.lock();

		long init = System.currentTimeMillis();

		try {
			while (!aggregated) {
				aggr.await();
			}

		} catch (InterruptedException ex) {
			logger.error(ex.getMessage(), ex);
		} finally {
			lock.unlock();
		}

		logger.debug("Id:" + getIdentifier() + " wait:" + wait + "; max:" + max + "; aggregated: " + aggregated);

		long end = System.currentTimeMillis();

		logger.debug("Id:" + getIdentifier() + " Waited for " + (end - init) + " ms");
		try {
			// acquire semaphore
			processed.acquire();
		} catch (InterruptedException ex) {
			logger.error(ex.getMessage(), ex);
		}

		if(value == null)
		{
			if((xslt != null) && (!xslt.isEmpty()))
			{
				sb.insert(0, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>");
				sb.append("</root>");
				String xml = sb.toString();
				logger.debug("Id:" + getIdentifier() + " Going to process " + xml);
				// processed xml with xslt
				value = AggregationOperation.processMessage(xslt, sb.toString());
			}
			else
			{
				logger.error("Id:" + getIdentifier() + " Could not process " + sb.toString() + " because stylesheet reader is null!");
			}
		}

		// release semaphore
		processed.release();

		return value;
	}

	private void unlock() {
		logger.debug("Id:" + getIdentifier() + " Unlocking... wait:" + wait + "; max:" + max + "; aggregated: " + aggregated);
		lock.lock();
		try {
			aggregated = true;
			aggr.signal();
		} finally {
			lock.unlock();
		}

		logger.debug("Id:" + getIdentifier() + " Unlocked!");
	}

	public void addInvoker(URI inv)
	{
		invokers.add(inv);
	}

	public List getInvokers() {
		return invokers;
	}

	public String getCurrentResponses()
	{
		StringBuilder sb = new StringBuilder();
		Iterator iter = responses.iterator();
		while(iter.hasNext())
		{
			sb.append((String) iter.next());
			sb.append(";");
		}

		return sb.toString();
	}
}
