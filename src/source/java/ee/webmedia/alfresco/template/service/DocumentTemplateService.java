package ee.webmedia.alfresco.template.service;

import java.util.List;

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

}
