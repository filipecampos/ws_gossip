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

package org.uminho.di.gsd.gossip.service.operations.aggregation;

import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.uminho.di.gsd.common.CommunicationProtocol;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.gossip.service.repo.aggregation.AggregationMessage;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.io.xml.XmlSerializer;
import org.ws4d.java.io.xml.XmlSerializerImplementation;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.types.URI;

public class AggOperation extends AggregationOperation {

	static Logger logger = Logger.getLogger(AggOperation.class);

	public AggOperation() {
		super(Constants.AggOperationName, Constants.AggregationPortQName);

		initInput();
		initOutput();
	}

	@Override
	protected void initInput() {
		ComplexType req = new ComplexType(Constants.AggRequestTypeQName, ComplexType.CONTAINER_SEQUENCE);

		req.addElement(getSvcEprElement());
		req.addElement(getXsltMessageListElement());

		Element in = new Element(Constants.AggRequestElementQName, req);
		setInput(in);
	}

	@Override
	protected void initOutput() {
		ComplexType resp = new ComplexType(Constants.AggResponseTypeQName, ComplexType.CONTAINER_SEQUENCE);

		resp.addElement(getMessagesListElement());

		Element out = new Element(Constants.AggResponseElementQName, resp);
		setOutput(out);
	}

	@Override
	public ParameterValue invoke(ParameterValue parameterValue) throws InvocationException, TimeoutException {
		long nanoTime = System.nanoTime();

		// pick xslts and messages
		ParameterValue ret = null;

		// check number of XsltMessage elements
		String prefix = Constants.XsltMessageListElementName + "/" + Constants.XsltMessageElementName;
		int num = parameterValue.getChildrenCount(prefix);

		logger.debug("Got " + num + " XsltMessages!");

		prefix += "[";

		if (num > 0) {
			// if there are messages
			// initialize serializer
			XmlSerializer serializer = new XmlSerializerImplementation();
			StringWriter sw = new StringWriter();
			serializer.setOutput(sw);

			String reader = null;

			ret = createOutputValue();
			int counter = 0;

			String src = parameterValue.getValue(Constants.SvcEprElementName);

			// cycle messages and extract each xslt
			// WARNING: Assuming a single message
			for (int i = 0; i < num; i++) {
				String tempPrefix = prefix + i + "]/";

				// extract xslt
				reader = extractXSLT(tempPrefix, parameterValue);

				if (reader != null) {
					String value = parameterValue.getValue(tempPrefix + Constants.MessageContainerElementName + "/" + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName);
					logger.debug("Got request value: " + value);

					String responsePrefix = Constants.MessagesListElementName + "/"
							+ Constants.MessageContainerElementName + "[" + counter + "]/";

					// set rounds decremented
					Integer rounds = Integer.parseInt(parameterValue.getValue(tempPrefix + Constants.MessageContainerElementName + "/" + Constants.RoundsElementName));
					String roundsStr = Integer.toString(--rounds);
					ret.setValue(responsePrefix + Constants.RoundsElementName, roundsStr);

					// set action
					String action = parameterValue.getValue(tempPrefix + Constants.MessageContainerElementName + "/" + Constants.ActionElementName);
					ret.setValue(responsePrefix + Constants.ActionElementName, action);

					// set msgid
					String msgId = parameterValue.getValue(tempPrefix + Constants.MessageContainerElementName + "/" + Constants.MessageIdentifierElementName);
					ret.setValue(responsePrefix + Constants.MessageIdentifierElementName, msgId);

					// invoke agg on peers assuming that only one XSLTMessage is being sent
					AggregationMessage agg = (AggregationMessage) processor.getMessage(new URI(msgId));
					String val = null;
					if (agg == null) {
						String current = Double.toString(processor.getService().getAppService().getValue());
						// new message, so invoke on peers
						agg = new AggregationMessage(new URI(msgId), new URI(action), 0, current, 0, current, maxFanout, waitFanout, timeout, reader);
						agg.addAggValue(value);
						logger.debug("Added to AggregationMessage object the received value : " + value);
						processor.addMessage(agg);

						// set my own epr
						parameterValue.setValue(Constants.SvcEprElementName, processor.getService().getSvcEPR());

						// set rounds decremented
						parameterValue.setValue(Constants.RoundsElementName, roundsStr);

						if (rounds == 0)
						{
							agg.timeout();
							val = agg.getResponseValue();
						}
						else
						{
							val = processor.process_agg(parameterValue, nanoTime, msgId, agg, src);
						}
					} else {
						logger.debug("Duplicate msgId for an aggregation message. Will wait for response...");
						if (rounds == 0)
						{
							agg.timeout();
							val = agg.getResponseValue();
						}
						else
						{
							// duplicate message, so just wait for response to return reply
							val = agg.getResponseValue();
						}
					}

					if ((val != null) && (!val.isEmpty())) {
						logger.debug("Got XSLTed value: " + val);
						processor.getService().getAppService().setLastValue(Double.parseDouble(val));

					} else {
						logger.warn("Some error occurred processing the XSLT. Using last value...");
						val = Double.toString(processor.getService().getAppService().getLastValue());
					}

					// set resulting message
					ret.setValue(responsePrefix + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName, val);
					counter++;

					processor.removeMessage(new URI(msgId));
				}
			}
		}

		return ret;

	}

	public ParameterValue common_invoke(CommunicationProtocol communicationProtocol, long UDP, long nanoTime, URI millisTime, ParameterValue parameterValue) {
		// pick xslts and messages
		ParameterValue ret = null;

		// check number of XsltMessage elements
		String prefix = Constants.XsltMessageListElementName + "/" + Constants.XsltMessageElementName;
		int num = parameterValue.getChildrenCount(prefix);

		logger.debug("Got " + num + " XsltMessages!");

		prefix += "[";

		if (num > 0) {
			// if there are messages
			// initialize serializer
			XmlSerializer serializer = new XmlSerializerImplementation();
			StringWriter sw = new StringWriter();
			serializer.setOutput(sw);

			String reader = null;

			ret = createOutputValue();
			int counter = 0;

			String src = parameterValue.getValue(Constants.SvcEprElementName);

			// cycle messages and extract each xslt
			// WARNING: Assuming a single message
			for (int i = 0; i < num; i++) {
				String tempPrefix = prefix + i + "]/";

				// extract xslt
				reader = extractXSLT(tempPrefix, parameterValue);

				if (reader != null) {
					String value = parameterValue.getValue(tempPrefix + Constants.MessageContainerElementName + "/" + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName);
					logger.debug("Got request value: " + value);

					String responsePrefix = Constants.MessagesListElementName + "/"
							+ Constants.MessageContainerElementName + "[" + counter + "]/";

					// set rounds decremented
					Integer rounds = Integer.parseInt(parameterValue.getValue(tempPrefix + Constants.MessageContainerElementName + "/" + Constants.RoundsElementName));
					String roundsStr = Integer.toString(--rounds);
					ret.setValue(responsePrefix + Constants.RoundsElementName, roundsStr);

					// set action
					String action = parameterValue.getValue(tempPrefix + Constants.MessageContainerElementName + "/" + Constants.ActionElementName);
					ret.setValue(responsePrefix + Constants.ActionElementName, action);

					// set msgid
					String msgId = parameterValue.getValue(tempPrefix + Constants.MessageContainerElementName + "/" + Constants.MessageIdentifierElementName);
					ret.setValue(responsePrefix + Constants.MessageIdentifierElementName, msgId);

					// invoke agg on peers assuming that only one XSLTMessage is being sent
					AggregationMessage agg = (AggregationMessage) processor.getMessage(new URI(msgId));
					String val = null;
					if (agg == null) {
						String current = Double.toString(processor.getService().getAppService().getValue());
						// new message, so invoke on peers
						agg = new AggregationMessage(new URI(msgId), new URI(action), 0, current, 0, current, maxFanout, waitFanout, timeout, reader);
						agg.addAggValue(value);
						logger.debug("Added to AggregationMessage object the received value : " + value);
						processor.addMessage(agg);

						// set my own epr
						parameterValue.setValue(Constants.SvcEprElementName, processor.getService().getSvcEPR());

						// set rounds decremented
						parameterValue.setValue(Constants.RoundsElementName, roundsStr);

						if (rounds == 0)
						{
							agg.timeout();
							val = agg.getResponseValue();
						}
						else
						{
							val = processor.process_agg(parameterValue, nanoTime, msgId, agg, src);
						}
					} else {
						logger.debug("Duplicate msgId for an aggregation message. Will wait for response...");
						if (rounds == 0)
						{
							agg.timeout();
							val = agg.getResponseValue();
						}
						else
						{
							// duplicate message, so just wait for response to return reply
							val = agg.getResponseValue();
						}
					}

					if ((val != null) && (!val.isEmpty())) {
						logger.debug("Got XSLTed value: " + val);
						processor.getService().getAppService().setLastValue(Double.parseDouble(val));

					} else {
						logger.warn("Some error occurred processing the XSLT. Using last value...");
						val = Double.toString(processor.getService().getAppService().getLastValue());
					}

					// set resulting message
					ret.setValue(responsePrefix + Constants.MessageElementName + "/" + ApplicationServiceConstants.infoTempValueElementName, val);
					counter++;

					processor.removeMessage(new URI(msgId));
				}
			}
		}

		return ret;
	}
}
