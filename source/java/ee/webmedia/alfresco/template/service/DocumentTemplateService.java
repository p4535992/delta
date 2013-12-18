package ee.webmedia.alfresco.template.service;

import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;

import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.template.exception.ExistingFileFromTemplateException;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.volume.model.Volume;

/**
 * @author Kaarel Jõgeva
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
    List<DocumentTemplate> getDocumentTemplates(String docTypeId);

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
     * 
     * @param documentNodeRef
     * @return display name(that doesn't have OS-dependent filename length constraints) of generated word file
     * @throws FileNotFoundException throws when document has a template which has been deleted
     * @throws Exception
     */
    String populateTemplate(NodeRef documentNodeRef, boolean overWritingGranted) throws FileNotFoundException, ExistingFileFromTemplateException;

    String getProcessedVolumeDispositionTemplate(List<Volume> volumes, NodeRef template);

    String getProcessedAccessRestrictionEndDateTemplate(List<Document> documents, NodeRef template);

    /**
     * Fetches template content, processes the formulas and returns the processed content.
     * 
     * @param dataNodeRefs Map where key is prefix in the formulas and value is NodeRef that has properties for that formula group. Prefix may also be null or
     *            empty, in that case this formula group is used without prefix.
     * @param templatetemplate file NodeRef
     * @return processed content, where formulas are replaced with their values (if formula has a non-empty value)
     */
    String getProcessedEmailTemplate(Map<String, NodeRef> dataNodeRefs, NodeRef template);

    /**
     * Fetches template content, processes the formulas and returns the processed content.
     * 
     * @param dataNodeRefs Map where key is prefix in the formulas and value is NodeRef that has properties for that formula group. Prefix may also be null or
     *            empty, in that case this formula group is used without prefix.
     * @param templatetemplate file NodeRef
     * @param additionalFormulas additional formulas that can be used. Can be null.
     * @return processed content, where formulas are replaced with their values (if formula has a non-empty value)
     */
    String getProcessedEmailTemplate(Map<String, NodeRef> dataNodeRefs, NodeRef template, Map<String, String> additionalFormulas);

    /**
     * Check if supplied document has a template that can be used
     * 
     * @param document node to check against
     * @return found DocumentTemplate or null if none found
     */
    DocumentTemplate getDocumentsTemplate(NodeRef document);

    boolean hasDocumentsTemplate(NodeRef document);

    /**
     * Scans generated Word documents and replaces patterns which are relevant to registration of the document
     * 
     * @param nodeRef nodeRef that is being registered
     * @param isRegistering if document is also registered the generated document is finalized (/* {emptyField} *\/ markings are removed)
     */
    void updateGeneratedFiles(NodeRef nodeRef, boolean isRegistering);

    /**
     * Fetches template NodeRef for system templates that are specified by file name
     * 
     * @param templateName file name (e.g. foobar.html)
     * @return template template NodeRef or {@code null} if template not found
     */
    NodeRef getNotificationTemplateByName(String templateName);

    String getDocumentUrl(NodeRef document);

    ServletContext getServletContext();

    void updateDocTemplate(Node docTemplNode);

    List<SelectItem> getReportTemplates(TemplateReportType typeId);

    List<Pair<SelectItem, String>> getReportTemplatesWithOutputTypes(TemplateReportType typeId);

    NodeRef getReportTemplateByName(String templateName, TemplateReportType reportType);

    String getDocumentServerUrlPrefix();

    String getServerUrl();

}