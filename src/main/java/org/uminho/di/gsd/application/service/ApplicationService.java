package org.uminho.di.gsd.application.service;

import org.uminho.di.gsd.application.service.operations.GetTempOperation;
import org.uminho.di.gsd.application.service.operations.InfoTempOperation;
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.service.DefaultService;
import org.ws4d.java.service.Operation;
import org.ws4d.java.types.URI;

public class ApplicationService extends DefaultService
{
    private String identifier;

    private InfoTempOperation infoTempOperation;
    private GetTempOperation getTempOperation;

    private double lastValue = -1.0;

    private double value = -1.0;

    public ApplicationService()
    {
        super();

        //the optional-to-set ServiceId
        this.setServiceId(new URI(ApplicationServiceConstants.applicationServiceName));
        infoTempOperation = new InfoTempOperation(this);
        this.addOperation(infoTempOperation);
        getTempOperation = new GetTempOperation(this);
        this.addOperation(getTempOperation);
    }

    public ApplicationService(String id)
    {
        this();

        identifier = id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
    
    public double getLastValue() {
        if(lastValue == -1.0)
            lastValue = value;
        
        return lastValue;
    }

    public void setLastValue(double lastValue) {
        this.lastValue = lastValue;
    }

    public String getIdentifier() {
        return identifier;
    }

    public static Element buildInfoTempElement()
    {
        Element infoTemp = new Element(ApplicationServiceConstants.infoTempElementQName);

        ComplexType complexType = new ComplexType(ApplicationServiceConstants.infoTempComplexTypeElementQName, ComplexType.CONTAINER_SEQUENCE);

        complexType.addElement(new Element(ApplicationServiceConstants.infoTempValueElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_DOUBLE)));
        complexType.addElement(new Element(ApplicationServiceConstants.msgIdValueElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_ANYURI)));

        infoTemp.setType(complexType);

        return infoTemp;
    }

    public Operation getInfoTempOperation() {
        return infoTempOperation;
    }

    public Operation getGetTempOperation() {
        return getTempOperation;
    }
}
