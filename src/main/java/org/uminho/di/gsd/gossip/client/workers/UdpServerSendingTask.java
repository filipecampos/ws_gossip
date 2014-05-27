/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uminho.di.gsd.gossip.client.workers;

import org.apache.log4j.Logger;
import org.ws4d.java.communication.protocol.soap.server.SOAPoverUDPServer;

/**
 *
 * @author filipe
 */
public class UdpServerSendingTask extends GossipWorkingTask {
    static Logger logger = Logger.getLogger(UdpServerSendingTask.class);

    SOAPoverUDPServer sender;
    String targetIp;
    int targetPort;
    byte[] msg;
    int length;

    public UdpServerSendingTask(SOAPoverUDPServer sender, String targetIp, int targetPort, byte[] msg, int length) {
        super();
        this.sender = sender;
        this.targetIp = targetIp;
        this.targetPort = targetPort;
        this.msg = msg;
        this.length = length;
    }

    @Override
    public void run() {
        try {
            logger.debug("Will send message with " + length + " bytes.");
            sender.send(targetIp, targetPort, msg, length);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } 
    }
}
