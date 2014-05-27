/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.membership.service.operations;

import org.uminho.di.gsd.membership.info.MembershipRepository;
import org.ws4d.java.service.Operation;
import org.ws4d.java.types.QName;

/**
 *
 * @author fjoc
 */
public abstract class MembershipOperation extends Operation {
    private MembershipRepository repository;

    public MembershipOperation(String operationName, QName portType)
    {
        super(operationName, portType);
    }
    
    public void setRepository(MembershipRepository repository) {
        this.repository = repository;
    }

    public MembershipRepository getRepository() {
        return repository;
    }

    protected abstract void initInput();
    protected abstract void initOutput();
}
