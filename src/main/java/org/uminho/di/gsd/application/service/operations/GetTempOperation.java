/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.application.service.operations;

import org.uminho.di.gsd.application.service.ApplicationService;
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.schema.Element;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;

/**
 *
 * @author fjoc
 */
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
