package ee.webmedia.alfresco.webdav;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.webdav.WebDAV;
import org.dom4j.DocumentHelper;
import org.dom4j.io.XMLWriter;
import org.xml.sax.helpers.AttributesImpl;

public class PropFindMethod extends org.alfresco.repo.webdav.PropFindMethod {

    public static final String XML_LOCK_ENTRY = "lockentry";
    public static final String XML_NS_LOCK_ENTRY = WebDAV.DAV_NS_PREFIX + XML_LOCK_ENTRY;

    /**
     * Output the supported lock types XML element
     * 
     * @param xml XMLWriter
     */
    @Override
    protected void writeLockTypes(XMLWriter xml)
    {
        try
        {
            AttributesImpl nullAttr = getDAVHelper().getNullAttributes();

            xml.startElement(WebDAV.DAV_NS, WebDAV.XML_SUPPORTED_LOCK, WebDAV.XML_NS_SUPPORTED_LOCK, nullAttr);

            xml.startElement(WebDAV.DAV_NS, XML_LOCK_ENTRY, XML_NS_LOCK_ENTRY, nullAttr);

            xml.startElement(WebDAV.DAV_NS, WebDAV.XML_LOCK_SCOPE, WebDAV.XML_NS_LOCK_SCOPE, nullAttr);
            xml.write(DocumentHelper.createElement(WebDAV.XML_NS_EXCLUSIVE));
            xml.endElement(WebDAV.DAV_NS, WebDAV.XML_LOCK_SCOPE, WebDAV.XML_NS_LOCK_SCOPE);

            xml.startElement(WebDAV.DAV_NS, WebDAV.XML_LOCK_TYPE, WebDAV.XML_NS_LOCK_TYPE, nullAttr);
            xml.write(DocumentHelper.createElement(WebDAV.XML_NS_WRITE));
            xml.endElement(WebDAV.DAV_NS, WebDAV.XML_LOCK_TYPE, WebDAV.XML_NS_LOCK_TYPE);

            xml.endElement(WebDAV.DAV_NS, XML_LOCK_ENTRY, XML_NS_LOCK_ENTRY);

            xml.endElement(WebDAV.DAV_NS, WebDAV.XML_SUPPORTED_LOCK, WebDAV.XML_NS_SUPPORTED_LOCK);
        } catch (Exception ex)
        {
            throw new AlfrescoRuntimeException("XML write error", ex);
        }
    }
}