package ee.webmedia.alfresco.utils;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

/**
 * @author ats.uiboupin
 */
public class XmlUtil {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(XmlUtil.class);

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
}