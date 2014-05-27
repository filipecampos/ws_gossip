/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uminho.di.gsd.membership.service.operations;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.membership.info.MembershipRepository;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

/**
 *
 * @author fcampos
 */
public class GetTargetsOperation extends MembershipOperation {

    static Logger logger = Logger.getLogger(GetTargetsOperation.class);

    public GetTargetsOperation() {
        super(Constants.GetTargetsOperationName, Constants.MembershipPortTypeQName);

        initInput();
        initOutput();
    }

    @Override
    public ParameterValue invoke(ParameterValue request) throws InvocationException {

        // create empty response
        ParameterValue response = createOutputValue();
        
        if (request == null)
        {
            logger.error("Received request is null!");
        }
        else
        {
            // extract the expected input args...
            String serviceTypeStr = request.getValue(Constants.ServiceTypeElementName);
            String fanoutStr = request.getValue(Constants.FanoutElementName);
            String requestingGossipServiceEPRStr = request.getValue(Constants.SvcEprElementName);

            if (serviceTypeStr == null)
            {
                serviceTypeStr = "No Type Received";
                logger.warn("Received ServiceType is null!");
            }

            if (fanoutStr == null)
            {
                logger.error("Received Fanout is null!");
            }

            if (requestingGossipServiceEPRStr == null)
            {
                logger.error("Received Requesting Gossip Service EPR is null!");
            }
            else if(fanoutStr != null)
            {
                QName svcType = QName.construct(serviceTypeStr);
                int fanout = Integer.parseInt(fanoutStr);
                URI reqSvcEPR = new URI(requestingGossipServiceEPRStr);

                MembershipRepository repository = getRepository();
                if(repository != null)
                {
                    List ret = repository.getTargets(fanout, svcType, reqSvcEPR);
                    int retSize = ret.size();
                    logger.debug("Returned list has " + retSize + " elements.");
                    if((ret != null) && (retSize > 0))
                    {
                        for (int i = 0; i < retSize; i++) {
                            response.setValue("TargetsList/Endpoint[" + i + "]", ((URI) ret.get(i)).toString());
                        }

                    }
                }

                // Used as mock response
                /*
                for (int i = 0; i < fanout; i++) {
                    response.setValue("TargetsList/Endpoint[" + i + "]", serviceTypeStr + " serviceURL " + i);
                }
                 */
            }
        }
        // ... and send it back
        return response;
    }

    @Override
    protected void initInput() {
        ComplexType getTargetsRequestType = new ComplexType(Constants.GetTargetsRequestTypeQName, ComplexType.CONTAINER_SEQUENCE);
        Element serviceTypeElement = new Element(Constants.ServiceTypeElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_QNAME));
        serviceTypeElement.setMinOccurs(0);
        getTargetsRequestType.addElement(serviceTypeElement);
        getTargetsRequestType.addElement(new Element(Constants.FanoutElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_INTEGER)));
        getTargetsRequestType.addElement(new Element(Constants.SvcEprElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_ANYURI)));
        Element in = new Element(Constants.GetTargetsOperationQName, getTargetsRequestType);
        this.setInput(in);
    }

    @Override
    protected void initOutput() {
        Element endpoint = new Element(Constants.EndpointElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_ANYURI));
        // set unlimited number of endpoint elements
        endpoint.setMaxOccurs(-1);
        ComplexType targetsListType = new ComplexType(Constants.TargetsListTypeQName, ComplexType.CONTAINER_SEQUENCE);
        targetsListType.addElement(endpoint);

        ComplexType getTargetsResponseType = new ComplexType(Constants.GetTargetsReponseTypeQName, ComplexType.CONTAINER_SEQUENCE);
        getTargetsResponseType.addElement(new Element(Constants.TargetsListElementQName, targetsListType));

        Element out = new Element(Constants.GetTargetsReponseMessageQName, getTargetsResponseType);
        this.setOutput(out);
    }
}
