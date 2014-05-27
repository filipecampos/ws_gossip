/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.membership.service;

import org.uminho.di.gsd.membership.service.operations.UpdateOperation;
import org.uminho.di.gsd.membership.service.operations.GetTargetsOperation;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.membership.info.MembershipRepository;
import org.ws4d.java.service.DefaultService;
import org.ws4d.java.types.URI;

/**
 *
 * @author fcampos
 */
public class MembershipService extends DefaultService {
    static Logger logger = Logger.getLogger(MembershipService.class);

    private MembershipRepository repository;
    
    private UpdateOperation updateOp;
    private GetTargetsOperation getTargetsOp;

    public MembershipService()
    {
        super();

        this.setServiceId(new URI(Constants.MembershipServiceName));

        initializeOperations();
    }

    public MembershipRepository getRepository()
    {
        return repository;
    }

    public void setRepository(MembershipRepository repository)
    {
        this.repository = repository;

        getTargetsOp.setRepository(repository);
        updateOp.setRepository(repository);
    }
    
    private void initializeOperations()
    {
        updateOp = new UpdateOperation();
        this.addOperation(updateOp);

        getTargetsOp = new GetTargetsOperation();
        this.addOperation(getTargetsOp);
    }

}
