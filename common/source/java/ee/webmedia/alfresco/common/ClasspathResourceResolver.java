package ee.webmedia.alfresco.common;

import java.io.InputStream;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * This is an implementation of LSResourceResolver that can resolve XML schemas from the classpath.
 * Example base is copied from http://www.java.net/node/666263.
 */
public class ClasspathResourceResolver implements LSResourceResolver {
    private final DOMImplementationLS domImplementationLS;

    private ClasspathResourceResolver() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        System.setProperty(DOMImplementationRegistry.PROPERTY, "org.apache.xerces.dom.DOMImplementationSourceImpl");
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        domImplementationLS = (DOMImplementationLS) registry.getDOMImplementation("LS");
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId,
            String baseURI) {
        LSInput lsInput = domImplementationLS.createLSInput();
        InputStream is = getClass().getResourceAsStream("/" + systemId);
        lsInput.setByteStream(is);
        lsInput.setSystemId(systemId);
        return lsInput;
    }
}
