package org.uminho.di.gsd.common;


import org.apache.log4j.Logger;
import org.ws4d.java.communication.DPWSProtocolData;
import org.ws4d.java.communication.protocol.soap.server.SOAPoverUDPServer.SOAPoverUDPDatagramHandler;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.discovery.ByeMessage;
import org.ws4d.java.message.discovery.HelloMessage;
import org.ws4d.java.message.discovery.ProbeMatchesMessage;
import org.ws4d.java.message.discovery.ProbeMessage;
import org.ws4d.java.message.discovery.ResolveMatchesMessage;
import org.ws4d.java.message.discovery.ResolveMessage;
import org.ws4d.java.message.eventing.GetStatusMessage;
import org.ws4d.java.message.eventing.GetStatusResponseMessage;
import org.ws4d.java.message.eventing.RenewMessage;
import org.ws4d.java.message.eventing.RenewResponseMessage;
import org.ws4d.java.message.eventing.SubscribeMessage;
import org.ws4d.java.message.eventing.SubscribeResponseMessage;
import org.ws4d.java.message.eventing.SubscriptionEndMessage;
import org.ws4d.java.message.eventing.UnsubscribeMessage;
import org.ws4d.java.message.eventing.UnsubscribeResponseMessage;
import org.ws4d.java.message.metadata.GetMessage;
import org.ws4d.java.message.metadata.GetMetadataMessage;
import org.ws4d.java.message.metadata.GetMetadataResponseMessage;
import org.ws4d.java.message.metadata.GetResponseMessage;
import org.ws4d.java.structures.MessageIdBuffer;

/**
 *
 * @author fjoc
 */
public class OutSOAPoverUDPDatagramHandlerImpl extends SOAPoverUDPDatagramHandler {
    static final Logger logger = Logger.getLogger(OutSOAPoverUDPDatagramHandlerImpl.class);

    final static String prefix = Constants.MessagesListElementName + "/" + Constants.MessageContainerElementName + "[0]/" + Constants.MessageIdentifierElementName;
    int counter = 0;

    public OutSOAPoverUDPDatagramHandlerImpl(MessageIdBuffer messageIdBuffer) {
        super(messageIdBuffer);
    }

    public void receive(HelloMessage hm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(ByeMessage bm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(ProbeMessage pm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(ProbeMatchesMessage pmm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(ResolveMessage rm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(ResolveMatchesMessage rmm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(GetMessage gm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(GetResponseMessage grm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(GetMetadataMessage gmm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(GetMetadataResponseMessage gmrm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(SubscribeMessage sm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(SubscribeResponseMessage srm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(GetStatusMessage gsm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(GetStatusResponseMessage gsrm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(RenewMessage rm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(RenewResponseMessage rrm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(UnsubscribeMessage um, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(UnsubscribeResponseMessage urm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receive(SubscriptionEndMessage sem, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void receive(InvokeMessage im, DPWSProtocolData dpwspd) {
        long currentTime = System.nanoTime();

        logger.debug("Sending UDP InvokeMessage at " + currentTime);
    }

    public void receive(FaultMessage fm, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receiveFailed(Exception excptn, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void sendFailed(Exception excptn, DPWSProtocolData dpwspd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void receiveFailed(Exception excptn) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void sendFailed(Exception excptn) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
