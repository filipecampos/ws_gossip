/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.application.service.operations;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.application.service.ApplicationService;
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;

/**
 *
 * @author fjoc
 */
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


            // do something?
            service.setLastValue(newTemp);
        }

        return null;
    }
}
