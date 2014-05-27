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

package org.uminho.di.gsd.application.service.operations;

import org.uminho.di.gsd.application.service.ApplicationService;
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.schema.Element;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;

public class GetTempOperation extends ApplicationOperation
{
    public GetTempOperation(ApplicationService svc)
    {
        super(ApplicationServiceConstants.getTempOpName, ApplicationServiceConstants.applicationServiceQName, svc);

        Element getTempIn = new Element(ApplicationServiceConstants.getTempElementQName);
        setInput(getTempIn);

        setOutput(ApplicationService.buildInfoTempElement());
    }

    /**
     * If the method is invoked by the client this method will be called.
     * The returned ParameterValue is the answer. It will be sent to the
     * client.
     */
    public ParameterValue invoke(ParameterValue parameterValue) throws InvocationException, TimeoutException
    {
        ParameterValue returnValue = createOutputValue();
        returnValue.setValue(ApplicationServiceConstants.infoTempValueElementName, Double.toString(service.getLastValue()));

        return returnValue;
    }
}
