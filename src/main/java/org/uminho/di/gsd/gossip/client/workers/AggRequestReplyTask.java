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
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.gossip.service.repo.aggregation.AggregationMessage;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.parameter.ParameterValue;

public class AggRequestReplyTask extends SendingTask {
	static Logger logger = Logger.getLogger(AggRequestReplyTask.class);

	ParameterValue ret;

	AggregationMessage agg;

	public AggRequestReplyTask(Operation op, ParameterValue pv, String msg, AggregationMessage ag) {
		super(op, pv, msg);

		agg = ag;
	}

	@Override
	public void run() {
		try {
			ret = op.invoke(pv);
			logger.debug(msg);

			if(ret != null)
			{
				logger.debug("Received reply: " + ret);
				String value = ret.getValue(Constants.MessagesListElementName + "/"
						+ Constants.MessageContainerElementName + "[" + 0 + "]/"
						+ Constants.MessageElementName + "/"
						+ ApplicationServiceConstants.infoTempValueElementName);
				agg.addResponse(value);
			}
		} catch (InvocationException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (TimeoutException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}
}
