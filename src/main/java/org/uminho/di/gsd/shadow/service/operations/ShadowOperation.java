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

package org.uminho.di.gsd.shadow.service.operations;

import org.uminho.di.gsd.gossip.service.GossipService;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.parameter.ParameterValue;

public class ShadowOperation extends Operation {

	GossipService gossipService;

	Operation mimickedOperation;

	public ShadowOperation(GossipService gos, Operation op)
	{
		super(op.getName(), op.getPortType());
		gossipService = gos;
		mimickedOperation = op;

		setInput(mimickedOperation.getInput());
		setOutput(mimickedOperation.getOutput());
	}

	@Override
	public ParameterValue invoke(ParameterValue parameterValue) throws InvocationException, TimeoutException {
		long nanoTime = System.nanoTime();
		long millisTime = System.currentTimeMillis();
		// call gossip service to propagate received invocation. perhaps create a threaded task for this?
		gossipService.disseminateShadowInvocation(mimickedOperation.getInputAction(), parameterValue, nanoTime, millisTime);

		return mimickedOperation.invoke(parameterValue);
	}

}
