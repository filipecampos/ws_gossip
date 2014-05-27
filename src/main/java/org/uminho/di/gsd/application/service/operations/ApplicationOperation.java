package org.uminho.di.gsd.application.service.operations;

import org.uminho.di.gsd.application.service.ApplicationService;
import org.ws4d.java.service.Operation;
import org.ws4d.java.types.QName;

/**
 *
 * @author fcampos
 */
public abstract class ApplicationOperation extends Operation {

    public ApplicationService service;

    public ApplicationOperation(String opName, QName qname, ApplicationService svc)
    {
        super(opName, qname);

        service = svc;
    }
}
