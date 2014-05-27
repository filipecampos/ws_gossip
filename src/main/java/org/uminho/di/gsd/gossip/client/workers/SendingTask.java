/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uminho.di.gsd.gossip.client.workers;

import org.apache.log4j.Logger;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.parameter.ParameterValue;

/**
 *
 * @author filipe
 */
public class SendingTask extends GossipWorkingTask {
    static Logger logger = Logger.getLogger(SendingTask.class);

    Operation op;
    ParameterValue pv;
    String msg;

    public SendingTask(Operation op, ParameterValue pv, String msg) {
        this.op = op;
        this.pv = pv;
        this.msg = msg;
    }

    @Override
    public void run() {
        try {
            op.invoke(pv);
            logger.debug(msg);
        } catch (InvocationException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (TimeoutException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
