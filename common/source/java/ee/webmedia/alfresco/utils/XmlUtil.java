package ee.webmedia.alfresco.utils;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.NodeList;

/**
 * @author ats.uiboupin
 */
public class XmlUtil {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(XmlUtil.class);
    private static DatatypeFactory datatypeFactory; // JAXP RI implements DatatypeFactory in a thread-safe way

    private static DatatypeFactory getDatatypeFactory() {
        if (datatypeFactory == null) {
            try {
                datatypeFactory = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        return datatypeFactory;
    }

    /**
     * @param <T> instance of this class will be returned if parsing <code>inputXml</code> is successful.
     * @param inputXml string representing xml
     * @param responseClass class of returnable instance
     * @return instance of given class T that extends XmlObject, parsed from <code>inputXml</code>
     */
    public static <T extends XmlObject> T getTypeFromXml(String inputXml, Class<T> responseClass) {
        try {
            SchemaType sType = (SchemaType) responseClass.getField("type").get(null);
            log.debug("Starting to parse '" + inputXml + "' to class: " + responseClass.getCanonicalName());
            XmlOptions replaceRootNameOpts = new XmlOptions().setLoadReplaceDocumentElement(new QName("xml-fragment"));
            final String xmlFragment = XmlObject.Factory.parse(inputXml, replaceRootNameOpts).toString();
            @SuppressWarnings("unchecked")
            T result = (T) XmlObject.Factory.parse(xmlFragment, new XmlOptions().setDocumentType(sType));
            return result;
        } catch (XmlException e) {
            throw new RuntimeException("Failed to parse '" + inputXml + "' to class: " + responseClass.getCanonicalName(), e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) { // if above exceptions were not caught it must be because of bad class
            throw new IllegalArgumentException("Failed to get value of '" + responseClass.getCanonicalName()
                    + ".type' to get corresponding SchemaType object: ", e);
        }
    }

    public static org.w3c.dom.Node findChildByQName(javax.xml.namespace.QName qName, org.w3c.dom.Node possibleDeltaNode) {
        return findChildByName(qName, possibleDeltaNode, true);
    }

    public static org.w3c.dom.Node findChildByName(javax.xml.namespace.QName qName, org.w3c.dom.Node possibleDeltaNode) {
        return findChildByName(qName, possibleDeltaNode, false);
    }

    private static org.w3c.dom.Node findChildByName(javax.xml.namespace.QName qName, org.w3c.dom.Node possibleDeltaNode, boolean useQName) {
        org.w3c.dom.Node externalReviewNode = null;
        NodeList nodeList = possibleDeltaNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (useQName ? compareNodeQName(qName, nodeList.item(i)) : compareNodeName(qName, nodeList.item(i))) {
                externalReviewNode = nodeList.item(i);
                break;
            }
        }
        return externalReviewNode;
    }

    public static Date getDate(XMLGregorianCalendar gregorianCalendar) {
        if (gregorianCalendar != null) {
            return gregorianCalendar.toGregorianCalendar().getTime();
        }
        return null;
    }

    public static XMLGregorianCalendar getXmlGregorianCalendar(Date date) {
        if (date != null) {
            GregorianCalendar gCal = new GregorianCalendar();
            gCal.setTime(date);
            return getDatatypeFactory().newXMLGregorianCalendar(gCal);
        }
        return null;
    }

    private static boolean compareNodeQName(javax.xml.namespace.QName qName, org.w3c.dom.Node node) {
        String namespaceURI = node.getNamespaceURI();
        String localName = node.getLocalName();
        if (namespaceURI != null && localName != null) {
            return qName.equals(new javax.xml.namespace.QName(namespaceURI, localName));
        }
        return false;
    }

    private static boolean compareNodeName(javax.xml.namespace.QName qName, org.w3c.dom.Node node) {
        String nodeName = node.getNodeName();
        if (nodeName != null) {
            return nodeName.equalsIgnoreCase(qName.getNamespaceURI() + ":" + qName.getLocalPart());
        }
        return false;
    }

}
