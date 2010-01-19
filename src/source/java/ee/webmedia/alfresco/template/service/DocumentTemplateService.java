package ee.webmedia.alfresco.template.service;

import java.util.List;

import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;

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
     * Check if supplied document has a template that can be used
     * 
     * @param document node to check against
     * @return found DocumentTemplate or null if none found
     */
    DocumentTemplate getDocumentsTemplate(NodeRef document);

}
