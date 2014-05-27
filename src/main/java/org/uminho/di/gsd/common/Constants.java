/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.common;

import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

/**
 *
 * @author fcampos
 */
public abstract class Constants {

    // measuring parameters
    public final static long terminateAtUpdate = 5;
    public final static int numberOfDevices = 30;

    // ms
    public final static long updateInitialDelay = 3000;
    public final static long updatePeriod = 1000;


    public final static String NameSpace = "http://gsd.di.uminho.pt/ws/2010/06/gossip";
//    public final static String AggregationNameSpace  = "http://gsd.di.uminho.pt/ws/2011/07/aggregation";

    // Aggregation Service
    public final static String AggregationServiceName = "AggregationService";
    public final static QName AggregationServiceQName = new QName(AggregationServiceName, NameSpace);

    public final static String AggregationPortName = "AggregationPortType";
    public final static QName AggregationPortQName = new QName(AggregationPortName, NameSpace);


    // Operations

    // AggPush
    public final static String AggPushOperationName = "AggPush";
    public final static QName AggPushOperationQName = new QName(AggPushOperationName, NameSpace);

    public final static String AggPushRequestTypeName = "AggPushRequestType";
    public final static QName AggPushRequestTypeQName = new QName(AggPushRequestTypeName, NameSpace);

    public final static String AggPushRequestElementName = "AggPush";
    public final static QName AggPushRequestElementQName = new QName(AggPushRequestElementName, NameSpace);

    
    // AggPull
    public final static String AggPullOperationName = "AggPull";
    public final static QName AggPullOperationQName = new QName(AggPullOperationName, NameSpace);

    public final static String AggPullRequestTypeName = "AggPullRequestType";
    public final static QName AggPullRequestTypeQName = new QName(AggPullRequestTypeName, NameSpace);

    public final static String AggPullRequestElementName = "AggPullRequest";
    public final static QName AggPullRequestElementQName = new QName(AggPullRequestElementName, NameSpace);

    public final static String AggPullResponseTypeName = "AggPullResponseType";
    public final static QName AggPullResponseTypeQName = new QName(AggPullResponseTypeName, NameSpace);

    public final static String AggPullResponseElementName = "AggPullResponse";
    public final static QName AggPullResponseElementQName = new QName(AggPullResponseElementName, NameSpace);

    
    // Agg
    public final static String AggOperationName = "Agg";
    public final static QName AggOperationQName = new QName(AggOperationName, NameSpace);

    public final static String AggRequestTypeName = "AggRequestType";
    public final static QName AggRequestTypeQName = new QName(AggRequestTypeName, NameSpace);

    public final static String AggRequestElementName = "AggRequest";
    public final static QName AggRequestElementQName = new QName(AggRequestElementName, NameSpace);

    public final static String AggResponseTypeName = "AggResponseType";
    public final static QName AggResponseTypeQName = new QName(AggResponseTypeName, NameSpace);

    public final static String AggResponseElementName = "AggResponse";
    public final static QName AggResponseElementQName = new QName(AggResponseElementName, NameSpace);

    // XSLT common element
    public final static String XsltElementName = "Xslt";
    public final static QName XsltElementQName = new QName(XsltElementName, NameSpace);

    public final static String XsltTypeName = "XsltType";
    public final static QName XsltTypeQName = new QName(XsltTypeName, NameSpace);

    public final static String XsltActionTypeName = "XsltActionType";
    public final static QName XsltActionTypeQName = new QName(XsltActionTypeName, NameSpace);
    
    public final static String XsltActionElementName = "XsltAction";
    public final static QName XsltActionElementQName = new QName(XsltActionElementName, NameSpace);

    public final static String XsltActionListTypeName = "XsltActionListType";
    public final static QName XsltActionListTypeQName = new QName(XsltActionListTypeName, NameSpace);

    public final static String XsltActionListElementName = "XsltActionList";
    public final static QName XsltActionListElementQName = new QName(XsltActionListElementName, NameSpace);

    public final static String XsltMessageTypeName = "XsltMessageType";
    public final static QName XsltMessageTypeQName = new QName(XsltMessageTypeName, NameSpace);

    public final static String XsltMessageElementName = "XsltMessage";
    public final static QName XsltMessageElementQName = new QName(XsltMessageElementName, NameSpace);

    public final static String XsltMessageListTypeName = "XsltMessageListType";
    public final static QName XsltMessageListTypeQName = new QName(XsltMessageListTypeName, NameSpace);

    public final static String XsltMessageListElementName = "XsltMessageList";
    public final static QName XsltMessageListElementQName = new QName(XsltMessageListElementName, NameSpace);


    public final static String LocationElementName = "Location";
    public final static QName LocationElementQName = new QName(LocationElementName, NameSpace);

    public final static String ContentElementName = "Cont";
    public final static QName ContentElementQName = new QName(ContentElementName, NameSpace);

    // Gossip Service
    public final static String GossipServiceName = "GossipService";
    public final static QName GossipServiceQName = new QName(GossipServiceName, NameSpace);

    public final static String GossipPushPortName = "GossipPushPortType";
    public final static QName GossipPushPortQName = new QName(GossipPushPortName, NameSpace);
    public final static String GossipPullPortName = "GossipPullPortType";
    public final static QName GossipPullPortQName = new QName(GossipPullPortName, NameSpace);
    public final static String GossipLazyPortName = "GossipLazyPortType";
    public final static QName GossipLazyPortQName = new QName(GossipLazyPortName, NameSpace);


    // Operations
    
    // Push Operation
    public final static String PushOperationName = "Push";
    public final static QName PushOperationQName = new QName(PushOperationName, NameSpace);

    public final static String PushMessageTypeName = "PushType";
    public final static QName PushMessageTypeQName = new QName(PushMessageTypeName, NameSpace);

    public final static String MessagesListElementName = "MessagesList";
    public final static QName MessagesListElementQName = new QName(MessagesListElementName, NameSpace);
//    public final static QName AggMessagesListElementQName = new QName(MessagesListElementName, AggregationNameSpace);

    public final static String MessagesListTypeName = "MessagesListType";
    public final static QName MessagesListTypeQName = new QName(MessagesListTypeName, NameSpace);
//    public final static QName AggMessagesListTypeQName = new QName(MessagesListTypeName, AggregationNameSpace);

    public final static String MessageContainerElementName = "MessageContainer";
    public final static QName MessageContainerElementQName = new QName(MessageContainerElementName, NameSpace);

    public final static String MessageActionContainerElementName = "MessageActionContainer";
    public final static QName MessageActionContainerElementQName = new QName(MessageActionContainerElementName, NameSpace);

    public final static String MessageContainerTypeName = "MessageContainerType";
    public final static QName MessageContainerTypeQName = new QName(MessageContainerTypeName, NameSpace);

    public final static String MessageActionContainerTypeName = "MessageActionContainerType";
    public final static QName MessageActionContainerTypeQName = new QName(MessageActionContainerTypeName, NameSpace);

    public final static String MessageElementName = "Message";
    public final static QName MessageElementQName = new QName(MessageElementName, NameSpace);
//    public final static QName AggMessageElementQName = new QName(MessageElementName, AggregationNameSpace);

    public final static String RoundsElementName = "Rounds";
    public final static QName RoundsElementQName = new QName(RoundsElementName, NameSpace);
//    public final static QName AggRoundsElementQName = new QName(RoundsElementName, AggregationNameSpace);

    public final static String MessageIdentifierElementName = "Id";
    public final static QName MessageIdentifierElementQName = new QName(MessageIdentifierElementName, NameSpace);
//    public final static QName AggMessageIdentifierElementQName = new QName(MessageIdentifierElementName, AggregationNameSpace);

    public final static String ActionElementName = "Action";
    public final static QName ActionElementQName = new QName(ActionElementName, NameSpace);
//    public final static QName AggActionElementQName = new QName(ActionElementName, AggregationNameSpace);

    // Pull Operation
    public final static String PullOperationName = "Pull";
    public final static QName PullOperationQName = new QName(PullOperationName, NameSpace);

    public final static String PullSyncOperationName = "PullSync";
    public final static QName PullSyncOperationQName = new QName(PullSyncOperationName, NameSpace);

    public final static String TimeIntervalElementName = "TimeInterval";
    public final static QName TimeIntervalElementQName = new QName(TimeIntervalElementName, NameSpace);

    public final static String MillisecondsAttributeName = "Millis";
    public final static QName MillisecondsAttributeQName = new QName(MillisecondsAttributeName, NameSpace);

    
    // Pull Ids Operation
    public final static String PullIdsOperationName = "PullIds";
    public final static QName PullIdsOperationQName = new QName(PullIdsOperationName, NameSpace);

    public final static String PullIdsSyncOperationName = "PullIdsSync";
    public final static QName PullIdsSyncOperationQName = new QName(PullIdsSyncOperationName, NameSpace);

    public final static String IdentifiersListElementName = "Ids";
    public final static QName IdentifiersListElementQName = new QName(IdentifiersListElementName, NameSpace);

    // Pull Messages Operation
    public final static String FetchOperationName = "Fetch";
    public final static QName FetchOperationQName = new QName(FetchOperationName, NameSpace);

    public final static String FetchSyncOperationName = "Fetch";
    public final static QName FetchSyncOperationQName = new QName(FetchSyncOperationName, NameSpace);

    // PushIds Operation
    public final static String PushIdsOperationName = "PushIds";
    public final static QName PushIdsOperationQName = new QName(PushIdsOperationName, NameSpace);

    public final static String MessagesInfoListElementName = "MsgInfos";
    public final static QName MessagesInfoListElementQName = new QName(MessagesInfoListElementName, NameSpace);

    public final static String MessageInfoElementName = "MsgInfo";
    public final static QName MessageInfoElementQName = new QName(MessageInfoElementName, NameSpace);

    public final static String MessageInfoTypeName = "MessageInfoType";
    public final static QName MessageInfoTypeQName = new QName(MessageInfoTypeName, NameSpace);


    // PushPull Operation
    public final static String PushPullOperationName = "PushPull";
    public final static QName PushPullOperationQName = new QName(PushPullOperationName, NameSpace);

    // Membership Service
    public final static String MembershipServiceName = "MembershipService";
    public final static QName MembershipServiceQName = new QName(MembershipServiceName, NameSpace);

    public final static String MembershipPortTypeName = "MembershipPortType";
    public final static QName MembershipPortTypeQName = new QName(MembershipPortTypeName, NameSpace);

    public final static URI MembershipServiceId = new URI(MembershipServiceName);



    // Operations

    // GetTargets Operation
    public final static String GetTargetsOperationName = "GetTargets";
    public final static QName GetTargetsOperationQName = new QName(GetTargetsOperationName, Constants.NameSpace);

    public final static String GetTargetsRequestTypeName = "GetTargetsRequestType";
    public final static QName GetTargetsRequestTypeQName = new QName(GetTargetsRequestTypeName, Constants.NameSpace);

    public final static String FanoutElementName = "Fanout";
    public final static QName FanoutElementQName = new QName(FanoutElementName, Constants.NameSpace);

    public final static String EndpointElementName = "Endpoint";
    public final static QName EndpointElementQName = new QName(EndpointElementName, Constants.NameSpace);

    public final static String GetTargetsResponseTypeName = "GetTargetsResponseType";
    public final static QName GetTargetsReponseTypeQName = new QName(GetTargetsResponseTypeName, Constants.NameSpace);

    public final static String GetTargetsResponseMessageName = "GetTargetsResponse";
    public final static QName GetTargetsReponseMessageQName = new QName(GetTargetsResponseMessageName, Constants.NameSpace);

    public final static String TargetsListElementName = "TargetsList";
    public final static QName TargetsListElementQName = new QName(TargetsListElementName, Constants.NameSpace);

    public final static String TargetsListTypeName = "TargetsListType";
    public final static QName TargetsListTypeQName = new QName(TargetsListTypeName, Constants.NameSpace);

    // Update Operation
    public final static String UpdateOperationName = "Update";

    public final static String UpdateInMessage = "Update";
    public final static QName UpdateInMessageQName = new QName(UpdateInMessage, NameSpace);

    public final static String UpdateElementTypeName = "UpdateType";
    public final static QName UpdateElementTypeQName = new QName(UpdateElementTypeName, NameSpace);

    public final static String UpdateOutMessage = "UpdateResponse";
    public final static QName UpdateOutMessageQName = new QName(UpdateOutMessage, NameSpace);

    public final static String UpdateResponseElementTypeName = "UpdateResponseType";
    public final static QName UpdateResponseElementTypeQName = new QName(UpdateResponseElementTypeName, NameSpace);

    public final static String ServiceElementName = "Svc";
    public final static QName ServiceElementQName = new QName(ServiceElementName, NameSpace);

    public final static String ServiceComplexTypeElementName = "SvcType";
    public final static QName ServiceComplexTypeElementQName = new QName(ServiceComplexTypeElementName, NameSpace);

    public final static String DeviceRefElementName = "DevRef";
    public final static QName DeviceRefElementQName = new QName(DeviceRefElementName, NameSpace);

    public final static String ServiceTypeElementName = "Type";
    public final static QName ServiceTypeElementQName = new QName(ServiceTypeElementName, NameSpace);

    public final static String ServiceRefElementName = "Ref";
    public final static QName ServiceRefElementQName = new QName(ServiceRefElementName, NameSpace);

    public final static String ServiceEndpointAddressElementName = "Addr";
    public final static QName ServiceEndpointAddressElementQName = new QName(ServiceEndpointAddressElementName, NameSpace);

    public final static String ServiceHeartbeatElementName = "Heartbeat";
    public final static QName ServiceHeartbeatElementQName = new QName(ServiceHeartbeatElementName, NameSpace);

    public final static String SvcEprElementName = "SvcEPR";
    public final static QName SvcEprElementQName = new QName(SvcEprElementName, NameSpace);



    // Management Service
    public final static String ManagementServiceName = "ManagementService";
    public final static QName ManagementServiceQName = new QName(ManagementServiceName, NameSpace);

    public final static String ManagementPortName = "ManagementPortType";
    public final static QName ManagementPortQName = new QName(ManagementPortName, NameSpace);

    public final static URI ManagementServiceId = new URI(ManagementServiceName);

    // Operations
    public final static String GetEndpointOperationName = "GetEndpoint";
    public final static QName GetEndpointOperationQName = new QName(GetEndpointOperationName, NameSpace);

    public final static String SetMembershipOperationName = "SetMembership";
    public final static QName SetMembershipOperationQName = new QName(SetMembershipOperationName, NameSpace);

    public final static String StartDisseminationOperationName = "StartDissemination";
    public final static QName StartDisseminationOperationQName = new QName(StartDisseminationOperationName, NameSpace);

    public final static String EndDisseminationNotificationName = "EndDissemination";
    public final static QName EndDisseminationNotificationQName = new QName(EndDisseminationNotificationName, NameSpace);

    public final static String StartWorkersNotificationName = "StartWorkers";
    public final static QName StartWorkersNotificationQName = new QName(StartWorkersNotificationName, NameSpace);

    public final static String StopOperationName = "Stop";
    public final static QName StopOperationQName = new QName(StopOperationName, NameSpace);

    public final static String WriteStatsOperationName = "WriteStats";
    public final static QName WriteStatsOperationQName = new QName(WriteStatsOperationName, NameSpace);

    public final static String GetStatsOperationName = "GetStats";
    public final static QName GetStatsOperationQName = new QName(GetStatsOperationName, NameSpace);

    public final static String SetPublisherOperationName = "SetPublisher";
    public final static QName SetPublisherOperationQName = new QName(SetPublisherOperationName, NameSpace);


    // elements
    public final static String FilenameElementName = "Filename";
    public final static QName FilenameElementQName = new QName(FilenameElementName, NameSpace);

    public final static String WriteStatsResponseName = "WroteStats";
    public final static QName WriteStatsResponseQName = new QName(WriteStatsResponseName, NameSpace);

    public final static String GetStatsResponseName = "GotStats";
    public final static QName GetStatsResponseQName = new QName(GetStatsResponseName, NameSpace);

    public final static String SetMembershipRequestMessageName = "NewMembership";
    public final static QName SetMembershipRequestMessageQName = new QName(SetMembershipRequestMessageName, NameSpace);

    public final static String SetMembershipResponseMessageName = "MembershipSet";
    public final static QName SetMembershipResponseMessageQName = new QName(SetMembershipResponseMessageName, NameSpace);

    public final static String SetMembershipRequestTypeName = "NewMembershipType";
    public final static QName SetMembershipRequestTypeQName = new QName(SetMembershipRequestTypeName, NameSpace);

    public final static String EndDisseminationElementName = "EndDissemination";
    public final static QName EndDisseminationElementQName = new QName(EndDisseminationElementName, NameSpace);

    public final static String StartWorkersElementName = "StartWorkers";
    public final static QName StartWorkersElementQName = new QName(StartWorkersElementName, NameSpace);

    public final static String StopRequestName = "Stop";
    public final static QName StopRequestQName = new QName(StopRequestName, NameSpace);
}
