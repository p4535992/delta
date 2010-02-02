package ee.webmedia.alfresco.template.service;

import java.util.List;

import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.template.model.DocumentTemplate;

/**
 * @author Kaarel Jõgeva
 *
 */
public interface DocumentTemplateService {

    String BEAN_NAME = "DocumentTemplateService";

    /**
     * Returns list with all the templates
     * 
     * @return
     */
    List<DocumentTemplate> getTemplates();
    
    /**
     * Returns list with templates which match the given document type.
     * 
     * @param docType document type qname
     * @return
     */
    List<DocumentTemplate> getDocumentTemplates(QName docType);
    
    /**
     * Returns list with templates which are not mapped to any document type and are considered to be email templates.
     * 
     * @return
     */
    List<DocumentTemplate> getEmailTemplates();

    /**
     * Returns root folder for templates
     * 
     * @return
     */
    NodeRef getRoot();

    /**
     * Fills documents template file with metadata 
     * @param documentNodeRef
     * @throws FileNotFoundException throws when document has a template which has been deleted
     * @throws Exception 
     */
    void populateTemplate(NodeRef documentNodeRef) throws FileNotFoundException;

    /**
     * Fetches template content, processes the formulas and returns the processed content.
     * 
     * @param document document node ref
     * @param template template node ref
     */
    String getProcessedEmailTemplate(NodeRef document, NodeRef template);
    
    /**
     * Check if supplied document has a template that can be used
     * 
     * @param document node to check against
     * @return found DocumentTemplate or null if none found
     */
    DocumentTemplate getDocumentsTemplate(NodeRef document);
    
    
    /**
     * Scans generated Word documents and replaces patterns which are relevant to registration of the document
     * 
     * @param document  node that is being registered
     */
    void updateGeneratedFilesOnRegistration(Node document);

}
