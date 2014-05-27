package org.uminho.di.gsd.common;

import org.ws4d.java.message.InvokeMessage;
import org.ws4d.java.message.SOAPHeader;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

public abstract class ApplicationServiceConstants {
    public final static String IP = "192.168.1.70";

    public final static String MY_NAMESPACE = "http://gsd.di.uminho.pt/example/";

    public final static QName receiverType = new QName("Receiver", MY_NAMESPACE);

    public final static QName senderType = new QName("Sender", MY_NAMESPACE);

    public final static String receiverDeviceName = "ReceiverDevice";

    public final static String senderDeviceName = "SenderDevice";

    public final static String notificationServiceName = "NotificationService";
    public final static QName notificationServiceQName = new QName(notificationServiceName, MY_NAMESPACE);

    public final static String infoTempEventName = "InfoTempEvent";
    public final static URI infoTempEventActionURI = new URI(MY_NAMESPACE, notificationServiceName + "/" + infoTempEventName);

    
    public final static String applicationServiceName = "ApplicationService";
    public final static QName applicationServiceQName = new QName(applicationServiceName, MY_NAMESPACE);
    
    public final static String infoTempOpName = "InfoTemp";
    public final static URI infoTempOpActionURI = new URI(MY_NAMESPACE, applicationServiceName + "/" + infoTempOpName);

    public final static String infoTempElementName = "NewTemp";
    public final static QName infoTempElementQName = new QName(infoTempElementName, MY_NAMESPACE);

    public final static String infoTempComplexTypeElementName = "NewTempCType";
    public final static QName infoTempComplexTypeElementQName = new QName(infoTempComplexTypeElementName, MY_NAMESPACE);

    public final static String infoTempValueElementName = "TempValue";
    public final static QName infoTempValueElementQName = new QName(infoTempValueElementName, MY_NAMESPACE);

    public final static String msgIdValueElementName = "MsgId";
    public final static QName msgIdValueElementQName = new QName(msgIdValueElementName, MY_NAMESPACE);


    public final static String getTempOpName = "GetTemp";
    public final static URI getTempOpActionURI = new URI(MY_NAMESPACE, applicationServiceName + "/" + getTempOpName);

    public final static QName getTempElementQName = new QName(getTempOpName, MY_NAMESPACE);
    
    // time in milliseconds
    public final static long devicePollingPeriod = 5000;
    public final static long searchPollingPeriod = 1000;
    public final static long serviceSearchPollingPeriod = 2000;


    public static StringBuffer messageToString(InvokeMessage msg)
    {
        StringBuffer lsb = new StringBuffer();

        lsb.append("\nMessage: ");

        SOAPHeader soapHeader = msg.getHeader();
        lsb.append("Header: ").append(soapHeader);

        URI targetAddress = msg.getTargetAddress();
        lsb.append("; TargetAddress: ").append(targetAddress);

        int type = msg.getType();
        lsb.append("; Type: ").append(type);

        InvokeMessage iMsg = (InvokeMessage) msg;
        ParameterValue content = iMsg.getContent();
        lsb.append("; Content: ").append(content);
        lsb.append("\n");

        return lsb;
    }
}
