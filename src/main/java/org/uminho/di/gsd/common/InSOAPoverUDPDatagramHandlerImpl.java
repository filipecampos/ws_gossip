package org.uminho.di.gsd.common;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.gossip.service.GossipService;
import org.uminho.di.gsd.gossip.service.operations.FetchOperation;
import org.uminho.di.gsd.gossip.service.operations.PullIdsOperation;
import org.uminho.di.gsd.gossip.service.operations.PullOperation;
import org.uminho.di.gsd.gossip.service.operations.PushIdsOperation;
import org.uminho.di.gsd.gossip.service.operations.PushOperation;
import org.uminho.di.gsd.gossip.service.operations.PushPullOperation;
import org.uminho.di.gsd.gossip.service.operations.aggregation.AggOperation;
import org.uminho.di.gsd.gossip.service.operations.aggregation.AggPullOperation;
import org.uminho.di.gsd.gossip.service.operations.aggregation.AggPushOperation;
import org.ws4d.java.communication.DPWSProtocolData;
import org.ws4d.java.communication.protocol.soap.server.SOAPoverUDPServer.SOAPoverUDPDatagramHandler;
import org.ws4d.java.message.FaultMessage;
import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.SOAPHeader;
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
import org.ws4d.java.schema.Type;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.MessageIdBuffer;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

public class InSOAPoverUDPDatagramHandlerImpl extends SOAPoverUDPDatagramHandler {

    static final Logger logger = Logger.getLogger(InSOAPoverUDPDatagramHandlerImpl.class);
    private PushOperation pushOp;
    private PullOperation pullOp;
    private PushPullOperation pushPullOp;
    private PushIdsOperation pushIdsOp;
    private PullIdsOperation pullIdsOp;
    private FetchOperation fetchOp;
    private AggPushOperation aggPushOp;
    private AggPullOperation aggPullOp;
    private AggOperation aggOp;
    final static String prefix = Constants.MessagesListElementName + "/" + Constants.MessageContainerElementName + "[0]/" + Constants.MessageIdentifierElementName;
    int counter = 0;
    ArrayList ids;

    public InSOAPoverUDPDatagramHandlerImpl(MessageIdBuffer messageIdBuffer, GossipService svc) {
        super(messageIdBuffer);
        pushOp = svc.getPushOperation();
        pullOp = svc.getPullOperation();
        pushPullOp = svc.getPushPullOperation();
        pushIdsOp = svc.getPushIdsOperation();
        pullIdsOp = svc.getPullIdsOperation();
        fetchOp = svc.getFetchOperation();

        aggPushOp = svc.getAggPushOperation();
        aggPullOp = svc.getAggPullOperation();
        aggOp = svc.getAggOperation();
        ids = new ArrayList();
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
        long nanoTime = System.nanoTime();
        long millisTime = System.currentTimeMillis();

        SOAPHeader header = im.getHeader();
        logger.debug("SOAP Header: " + header);
        ParameterValue pv = im.getContent();
        QName name = pv.getName();

        logger.debug("PV name: " + name);
        if(name.equals(Constants.PushOperationQName))
        {
            pushOp.common_invoke(CommunicationProtocol.UDP, nanoTime, millisTime, pv, header);
        }
        else if(name.equals(Constants.PullOperationQName))
        {
            pullOp.common_invoke(CommunicationProtocol.UDP, millisTime, pv, header);
        }
        else if(name.equals(Constants.PushPullOperationQName))
        {
            pushPullOp.common_invoke(CommunicationProtocol.UDP, nanoTime, millisTime, pv, header);
        }
        else if(name.equals(Constants.PushIdsOperationQName))
        {
            pushIdsOp.common_invoke(CommunicationProtocol.UDP, millisTime, pv, header);
        }
        else if(name.equals(Constants.PullIdsOperationQName))
        {
            pullIdsOp.common_invoke(CommunicationProtocol.UDP, millisTime, pv, header);
        }
        else if(name.equals(Constants.FetchOperationQName))
        {
            fetchOp.common_invoke(CommunicationProtocol.UDP, millisTime, pv, header);
        }
        else if(name.equals(Constants.AggPushOperationQName))
        {
            // peer who sent message
            URI sender = new URI("http://" + dpwspd.getSourceHost() + ":" + dpwspd.getSourcePort() + "/device/gossip/service");
            
            aggPushOp.common_invoke(CommunicationProtocol.UDP, nanoTime, millisTime, sender, pv);
        }
        else if(name.equals(Constants.AggPullRequestElementQName))
        {
            // peer who sent message
            URI sender = new URI("http://" + dpwspd.getSourceHost() + ":" + dpwspd.getSourcePort() + "/device/gossip/service");
            
            logger.debug("Received AggPullRequest message " + pv + " from " + sender);

            
            aggPullOp.invoke_request(nanoTime, millisTime, sender, pv);
        }
        else if(name.equals(Constants.AggPullResponseElementQName))
        {
            // peer who sent message
            URI sender = new URI("http://" + dpwspd.getSourceHost() + ":" + dpwspd.getSourcePort() + "/device/gossip/service");

            logger.debug("Received AggPullResponse message " + pv + " from " + sender);


            aggPullOp.invoke_response(nanoTime, millisTime, sender, pv);
        }
        else if(name.equals(Constants.AggOperationQName))
        {
            // peer who sent message
            URI sender = new URI("http://" + dpwspd.getSourceHost() + ":" + dpwspd.getSourcePort() + "/device/gossip/service");

            aggOp.common_invoke(CommunicationProtocol.UDP, nanoTime, millisTime, sender, pv);
        }
        else
        {
            // else
            logger.debug("Didn't match message with any operation");
        }

        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("Received InvokeMessage at ");
            sb.append(millisTime);
            sb.append("\nSource address: ");
            sb.append(dpwspd.getSourceAddress());
            sb.append("\nSource host: ");
            sb.append(dpwspd.getSourceHost());
            sb.append("\nSource port: ");
            sb.append(dpwspd.getSourcePort());
            sb.append("\nDestination host: ");
            sb.append(dpwspd.getDestinationHost());
            sb.append("\nDestination port: ");
            sb.append(dpwspd.getDestinationPort());
            sb.append(ApplicationServiceConstants.messageToString(im));
            logger.debug(sb.toString());
        }
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
