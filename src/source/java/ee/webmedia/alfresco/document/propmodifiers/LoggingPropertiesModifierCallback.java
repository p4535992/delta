package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentService.PropertiesModifierCallback;

/**
 * @author Ats Uiboupin
 * Just a logging callback, that shouldn't alter properties, but just log them.
 */
public class LoggingPropertiesModifierCallback implements PropertiesModifierCallback, InitializingBean {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(LoggingPropertiesModifierCallback.class);
    private DocumentService documentService;

    @Override
    public void doWithProperties(Map<QName, Serializable> properties, NodeRef nodeRef) {
        if(log.isDebugEnabled()) {
            log.debug("Created document with noderef '"+nodeRef+"' has "+properties.size()+" properties:");
            for (QName qName : properties.keySet()) {
                log.debug("\t" + qName);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
//        documentService.addPropertiesModifierCallback(QName.createQName("http://alfresco.webmedia.ee/model/document/1.0", "test"), this);
    }

    // START: getters/setters
    @Override
    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }
    // END: getters/setters

}
