/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.shadow.service.operations;

import org.uminho.di.gsd.gossip.service.GossipService;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.parameter.ParameterValue;

/**
 *
 * @author fjoc
 */
public class ShadowOperation extends Operation {

    GossipService gossipService;

    Operation mimickedOperation;

    public ShadowOperation(GossipService gos, Operation op)
    {
        super(op.getName(), op.getPortType());
        gossipService = gos;
        mimickedOperation = op;

        setInput(mimickedOperation.getInput());
        setOutput(mimickedOperation.getOutput());
    }

    @Override
    public ParameterValue invoke(ParameterValue parameterValue) throws InvocationException, TimeoutException {
        long nanoTime = System.nanoTime();
        long millisTime = System.currentTimeMillis();
        // call gossip service to propagate received invocation. perhaps create a threaded task for this?
        gossipService.disseminateShadowInvocation(mimickedOperation.getInputAction(), parameterValue, nanoTime, millisTime);

        return mimickedOperation.invoke(parameterValue);
    }

}
