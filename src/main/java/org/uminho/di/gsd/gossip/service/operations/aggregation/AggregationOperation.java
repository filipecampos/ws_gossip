/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uminho.di.gsd.gossip.service.operations.aggregation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.uminho.di.gsd.common.Configuration;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.gossip.service.operations.GossipOperation;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.schema.SchemaUtil;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.QName;

/**
 *
 * @author fjoc
 */
public abstract class AggregationOperation extends GossipOperation {

    static Logger logger = Logger.getLogger(AggregationOperation.class);
    int maxFanout;
    int waitFanout;
    long timeout;

    public AggregationOperation(String operationName, QName operationType) {
        super(operationName, operationType);

        readConfig();
    }

    private void readConfig() {
        logger.debug("maxFanout: " + Configuration.getConfigParamValue(Configuration.fanout));
        maxFanout = Integer.parseInt(((Long) Configuration.getConfigParamValue(Configuration.fanout)).toString());
        logger.debug("maxFanout: " + maxFanout);

        logger.debug("waitFanout: " + Configuration.getConfigParamValue(Configuration.waitResponses));
        waitFanout = Integer.parseInt(((Long) Configuration.getConfigParamValue(Configuration.waitResponses)).toString());
        logger.debug("waitFanout: " + waitFanout);

        logger.debug("waitTime: " + Configuration.getConfigParamValue(Configuration.waitTime));
        timeout = (Long) Configuration.getConfigParamValue(Configuration.waitTime);
        logger.debug("waitTime: " + timeout);
    }

    protected Element getXsltElement() {
        ComplexType xsltType = new ComplexType(Constants.XsltTypeQName, ComplexType.CONTAINER_CHOICE);

        // uri
        Element uriElement = new Element(Constants.LocationElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_ANYURI));
        xsltType.addElement(uriElement);

        // text
        Element textElement = new Element(Constants.ContentElementQName, SchemaUtil.getSchemaType(SchemaUtil.TYPE_STRING));
        xsltType.addElement(textElement);

        Element xslt = new Element(Constants.XsltElementQName, xsltType);

        return xslt;
    }

    protected Element getXsltActionListElement() {
        ComplexType xsltActionType = new ComplexType(Constants.XsltActionTypeQName, ComplexType.CONTAINER_SEQUENCE);
        xsltActionType.addElement(getXsltElement());
        xsltActionType.addElement(getMessageInfoElement());

        Element xsltActionElement = new Element(Constants.XsltActionElementQName, xsltActionType);

        ComplexType xsltActionListType = new ComplexType(Constants.XsltActionListTypeQName, ComplexType.CONTAINER_SEQUENCE);
        xsltActionElement.setMaxOccurs(-1);
        xsltActionListType.addElement(xsltActionElement);

        Element xsltActionListElement = new Element(Constants.XsltActionListElementQName, xsltActionListType);

        return xsltActionListElement;
    }

    protected Element getXsltMessageListElement() {
        ComplexType xsltMessageType = new ComplexType(Constants.XsltMessageTypeQName, ComplexType.CONTAINER_SEQUENCE);
        xsltMessageType.addElement(getXsltElement());
        xsltMessageType.addElement(getMessageContainerElement());

        Element xsltMessageElement = new Element(Constants.XsltMessageElementQName, xsltMessageType);

        ComplexType xsltMessageListType = new ComplexType(Constants.XsltMessageListTypeQName, ComplexType.CONTAINER_SEQUENCE);
        xsltMessageElement.setMaxOccurs(-1);
        xsltMessageListType.addElement(xsltMessageElement);

        Element xsltMessageListElement = new Element(Constants.XsltMessageListElementQName, xsltMessageListType);

        return xsltMessageListElement;
    }

    protected String[] extractMessageFields(ParameterValue pv, int index) {
        String[] ret = new String[4];
        String tempPrefix = Constants.XsltMessageListElementName + "/"
                + Constants.XsltMessageElementName + "[" + index + "]/"
                + Constants.MessageContainerElementName + "/";

        //msgId
        ret[0] = pv.getValue(tempPrefix + Constants.MessageIdentifierElementName);

        // action
        ret[1] = pv.getValue(tempPrefix + Constants.ActionElementName);

        // rounds
        ret[2] = pv.getValue(tempPrefix + Constants.RoundsElementName);

        // msg
        ret[3] = pv.getValue(tempPrefix + Constants.MessageElementName);

        logger.debug("Extracted Msg[" + index + "]: Id-" + ret[0] + "; Action-" + ret[1] + "; Rounds-" + ret[2] + "; Cont-" + ret[3]);

        return ret;
    }

    protected ParameterValue extractMessage(ParameterValue pv, int index) {
        ParameterValue ret = null;
        String tempPrefix = Constants.XsltMessageListElementName + "/"
                + Constants.XsltMessageElementName + "[" + index + "]/"
                + Constants.MessageContainerElementName + "/";
        // msg
        Iterator iter = pv.getChildren(tempPrefix + Constants.MessageElementName);

        if ((iter != null) && iter.hasNext()) {
            ret = (ParameterValue) iter.next();
            logger.debug("Message: " + ret.toString());
        }

        logger.debug("Extracted Msg[" + index + "] Contents: PV-" + ret);

        return ret;
    }

    protected String extractAction(ParameterValue pv, int index) {
        String ret = null;

        String tempPrefix = Constants.XsltActionListElementName + "/"
                + Constants.XsltActionElementName + "[" + index + "]/"
                + Constants.ActionElementName;

        ret = pv.getValue(tempPrefix);

        return ret;
    }

    protected String extractXSLT(String prefix, ParameterValue pv) {
//        StringReader reader = null;
        String xslt = null;
        // extract xslt location
        String xsltLocation = pv.getValue(prefix + Constants.XsltElementName + "/" + Constants.LocationElementName);
        String xsltContent = null;
        logger.debug("XSLT[" + prefix + "]: Location-" + xsltLocation);

        if ((xsltLocation == null) || xsltLocation.isEmpty()) {
            // extract xslt content if location is empty or not present
            xsltContent = pv.getValue(prefix + Constants.XsltElementName + "/" + Constants.ContentElementName);
            logger.debug("XSLT[" + prefix + "]: Content-" + xsltContent);

            if ((xsltContent != null) && !xsltContent.isEmpty()) {
                xslt = xsltContent;
//                reader = new StringReader(xsltContent);
            } else {
                logger.warn("XSLT in " + prefix + " no location or content present!");
            }
        } else {
            // retrieve xslt
//            reader = new StringReader(retrieveXslt(xsltLocation));
            xslt = retrieveXslt(xsltLocation);
        }

        return xslt;
    }

    protected String retrieveXslt(String xsltLocation) {
        String xml = null;

        try {
            URL url = new URL(xsltLocation);
            InputStream in = url.openStream();
            BufferedReader dis = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();

            String line = null;

            while ((line = dis.readLine()) != null) {
                sb.append(line).append("\n");
            }

            in.close();

            if (sb.length() > 0) {
                xml = sb.toString();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return xml;
    }

    public static String processMessage(String xslt, String msg) {

        String ret = null;
        try {
            logger.debug("XSLT Input:" + msg);
            Processor proc = new Processor(false);
            XsltCompiler comp = proc.newXsltCompiler();
            StringReader xsltSR = new StringReader(xslt);
            StreamSource xsltReaderSS = new StreamSource(xsltSR);
            XsltExecutable exp = comp.compile(xsltReaderSS);
            StringReader msgSR = new StringReader(msg);
            StreamSource msgSS = new StreamSource(msgSR);
            XdmNode source = proc.newDocumentBuilder().build(msgSS);

            Serializer out = proc.newSerializer();
            out.setOutputProperty(Serializer.Property.METHOD, "text");
            XsltTransformer t = exp.load();
            t.setInitialContextNode(source);
            StringWriter sw = new StringWriter();
            out.setOutputWriter(sw);
            t.setDestination(out);
            t.transform();

            ret = sw.toString();
            logger.debug("XSLT Output: " + ret);
        } catch (SaxonApiException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return ret;
    }

    public static void main(String[] args)
    {
        // configure loggers
        PropertyConfigurator.configure("log4j.properties");

        String xslt = "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:gsd=\"http://gsd.di.uminho.pt/example/\">"
                    + "  <xsl:template match=\"/\">"
                    + "    <xsl:element name=\"gsd:NewTemp\">"
                    + "      <xsl:element name=\"gsd:TempValue\">"
                    + "        <xsl:value-of select=\"max(//gsd:TempValue)\" />"
                    + "      </xsl:element>"
                    + "    </xsl:element>"
                    + "  </xsl:template>"
                    + "</xsl:stylesheet>";

        String msg=
//                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
//                        + "<root>"
//                    + "<s12:Envelope "
//                    + "xmlns:dpws=\"http://docs.oasis-open.org/ws-dd/ns/dpws/2009/01\" "
//                    + "xmlns:s12=\"http://www.w3.org/2003/05/soap-envelope\" "
//                    + "xmlns:wsa=\"http://www.w3.org/2005/08/addressing\" >"
//                    + "        <s12:Header>"
//                    + "            <wsa:Action>http://gsd.di.uminho.pt/example/InfoTemp</wsa:Action>"
//                    + "            <wsa:MessageID>urn:uuid:a844ed40-96a3-11e0-bfc9-5b2cc5e01861</wsa:MessageID>"
//                    + "        </s12:Header>"
//                    + "        <s12:Body>"
                     "            <n1:NewTemp xmlns:n1=\"http://gsd.di.uminho.pt/example/\">"
                    + "                <n1:TempValue>196.3</n1:TempValue>"
                    + "                <n1:MsgId>6</n1:MsgId>"
                    + "            </n1:NewTemp>"
//                    + "        </s12:Body>"
//                    + "</s12:Envelope>"
//                    + "<s12:Envelope "
//                    + "xmlns:dpws=\"http://docs.oasis-open.org/ws-dd/ns/dpws/2009/01\" "
//                    + "xmlns:s12=\"http://www.w3.org/2003/05/soap-envelope\" "
//                    + "xmlns:wsa=\"http://www.w3.org/2005/08/addressing\" >"
//                    + "        <s12:Header>"
//                    + "            <wsa:Action>http://gsd.di.uminho.pt/example/InfoTemp</wsa:Action>"
//                    + "            <wsa:MessageID>urn:uuid:a844ed40-96a3-11e0-bfc9-5b2cc5e01861</wsa:MessageID>"
//                    + "        </s12:Header>"
//                    + "        <s12:Body>"
                    + "            <n1:NewTemp xmlns:n1=\"http://gsd.di.uminho.pt/example/\">"
                    + "                <n1:TempValue>231.6</n1:TempValue>"
                    + "                <n1:MsgId>56</n1:MsgId>"
                    + "            </n1:NewTemp>"
//                    + "        </s12:Body>"
//                    + "</s12:Envelope>"
//                    + "</root>"
                    ;
//        try {
//            Debug.sleep(5000l);
//        } catch (InterruptedException ex) {
//            logger.error(ex.getMessage(), ex);
//        }
        
        String ret = processMessage(xslt, msg);

        System.out.println("Result: " + ret);
    }
}
