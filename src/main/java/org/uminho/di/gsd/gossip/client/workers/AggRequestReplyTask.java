/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uminho.di.gsd.gossip.client.workers;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.ApplicationServiceConstants;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.gossip.service.repo.aggregation.AggregationMessage;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.parameter.ParameterValue;

/**
 *
 * @author filipe
 */
public class AggRequestReplyTask extends SendingTask {
    static Logger logger = Logger.getLogger(AggRequestReplyTask.class);

    ParameterValue ret;

    AggregationMessage agg;

    public AggRequestReplyTask(Operation op, ParameterValue pv, String msg, AggregationMessage ag) {
       super(op, pv, msg);

       agg = ag;
    }

    @Override
    public void run() {
        try {
            ret = op.invoke(pv);
            long nanoTime = System.nanoTime();
            long millisTime = System.currentTimeMillis();
            logger.debug(msg);

            if(ret != null)
            {
                logger.debug("Received reply: " + ret);
                String value = ret.getValue(Constants.MessagesListElementName + "/"
                                        + Constants.MessageContainerElementName + "[" + 0 + "]/"
                                        + Constants.MessageElementName + "/"
                                        + ApplicationServiceConstants.infoTempValueElementName);
                agg.addResponse(value);
            }
        } catch (InvocationException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (TimeoutException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
