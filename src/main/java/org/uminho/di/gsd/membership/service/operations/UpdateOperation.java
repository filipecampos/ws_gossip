/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.membership.service.operations;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.membership.info.MembershipRepository;
import org.uminho.di.gsd.membership.info.ServiceInfo;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;

/**
 *
 * @author fcampos
 */
public class UpdateOperation extends MembershipOperation {
    static Logger logger = Logger.getLogger(UpdateOperation.class);

    public UpdateOperation()
    {
        super(Constants.UpdateOperationName, Constants.MembershipPortTypeQName);

        initInput();

        initOutput();
    }

    @Override
    public ParameterValue invoke(ParameterValue pv) throws InvocationException, TimeoutException
    {
        ParameterValue response = createOutputValue();
        
        // request
        if(pv != null)
        {
            List returnList = null;
            try
            {
                List sis = ServiceInfo.fromPVToServiceInfoList(pv);
                MembershipRepository repo = getRepository();
                returnList = repo.update(sis);
            }
            catch(Exception e)
            {
                logger.error(e.getMessage(), e);
            }
            
            if((returnList != null) && (!returnList.isEmpty()))
            {
                logger.debug("Will return this list with " + returnList.size() + " SIs.");
                
                // if there is any ServiceInfo to return
                if(logger.isDebugEnabled())
                {
                    Iterator iter = returnList.iterator();
                    StringBuilder sb = new StringBuilder();

                    while(iter.hasNext())
                    {
                        sb.append(((ServiceInfo) iter.next()).toString());
                    }

                    logger.debug(sb.toString());
                }

                // set response
                response = ServiceInfo.fromServiceInfoListToPV(returnList, response);
            }
        }
        
        return response;
    }

    @Override
    protected void initInput()
    {
        ComplexType updateElementType = new ComplexType(Constants.UpdateElementTypeQName, ComplexType.CONTAINER_SEQUENCE);
        updateElementType.addElement(initializeServiceElement());

        Element updateIn = new Element(Constants.UpdateInMessageQName);
        updateIn.setType(updateElementType);

        this.setInput(updateIn);
    }

    @Override
    protected void initOutput()
    {
        ComplexType updateResponseElementType = new ComplexType(Constants.UpdateResponseElementTypeQName, ComplexType.CONTAINER_SEQUENCE);
        updateResponseElementType.addElement(initializeServiceElement());

        Element updateOut = new Element(Constants.UpdateOutMessageQName);
        updateOut.setType(updateResponseElementType);
        
	this.setOutput(updateOut);
    }

    private Element initializeServiceElement()
    {
        Element devRef = new Element(Constants.DeviceRefElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_ANYURI));
        devRef.setMinOccurs(0);

        ComplexType complexType = new ComplexType(Constants.ServiceComplexTypeElementQName, ComplexType.CONTAINER_SEQUENCE);
        complexType.addElement(devRef);
        complexType.addElement(new Element(Constants.ServiceTypeElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_QNAME)));
        complexType.addElement(new Element(Constants.ServiceRefElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_ANYURI)));
        complexType.addElement(new Element(Constants.ServiceEndpointAddressElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_ANYURI)));
        complexType.addElement(new Element(Constants.ServiceHeartbeatElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_UNSIGNED_LONG)));

        Element serviceElement = new Element(Constants.ServiceElementQName);
        serviceElement.setType(complexType);
        serviceElement.setMaxOccurs(-1);
        
        return serviceElement;
    }
}
