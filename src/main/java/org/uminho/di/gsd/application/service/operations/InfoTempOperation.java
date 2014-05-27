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

import org.apache.log4j.Logger;
import org.uminho.di.gsd.application.service.ApplicationService;
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;

public class InfoTempOperation extends ApplicationOperation
{
    static Logger logger = Logger.getLogger(InfoTempOperation.class);
    
    public InfoTempOperation(ApplicationService svc)
    {
        super(ApplicationServiceConstants.infoTempOpName, ApplicationServiceConstants.applicationServiceQName, svc);

        //We define the input for this method.
        setInput(ApplicationService.buildInfoTempElement());
    }

    /**
     * We don't want to answer - therefore null is returned.
     */
    public ParameterValue invoke(ParameterValue parameterValues) throws InvocationException, TimeoutException
    {
        if(parameterValues != null)
        {
            logger.debug("[" + ApplicationServiceConstants.applicationServiceName + service.getIdentifier() + "] Received PV: " + parameterValues);
            double newTemp = Double.parseDouble(parameterValues.getValue(ApplicationServiceConstants.infoTempValueElementName));
            logger.debug("[" + ApplicationServiceConstants.applicationServiceName + service.getIdentifier() + "] New Temperature Information: " + newTemp);


            service.setLastValue(newTemp);
        }

        return null;
    }
}
