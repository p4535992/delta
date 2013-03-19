package ee.webmedia.alfresco.docadmin.bootstrap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;

/**
 * Deleting {@link DocumentType}s was implemented before updating {@link FieldDefinition#getDocTypes()} - this updater removes references to missing {@link DocumentType}s
 * 
 * @author Ats Uiboupin
 */
public class FieldDefinitionDeletedDocTypesUpdater extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(FieldDefinitionDocTypesUpdater.class);

    private DocumentAdminService documentAdminService;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Loading all field definitions");
        List<FieldDefinition> fieldDefinitions = documentAdminService.getFieldDefinitions();
        LOG.info("Loading all document types");
        List<DocumentType> documentTypes = documentAdminService.getDocumentTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN);
        LOG.info("found " + documentTypes.size() + " document types");
        Set<String> existingDocTypes = new HashSet<String>(documentTypes.size());
        for (DocumentType docType : documentTypes) {
            existingDocTypes.add(docType.getId());
        }

        Set<FieldDefinition> fieldDefinitionsToUpdate = new HashSet<FieldDefinition>();
        for (FieldDefinition fieldDef : fieldDefinitions) {
            List<String> docTypesOfFieldDef = fieldDef.getDocTypes();
            for (Iterator<String> it = docTypesOfFieldDef.iterator(); it.hasNext();) {
                String docTypeId = it.next();
                if (!existingDocTypes.contains(docTypeId)) {
                    LOG.info("fieldDefinition " + fieldDef.getFieldId() + " contained missing document type " + docTypeId);
                    it.remove();
                    fieldDefinitionsToUpdate.add(fieldDef);
                }
            }
        }
        LOG.info("Updating " + fieldDefinitionsToUpdate.size() + " field definitions");
        documentAdminService.saveOrUpdateFieldDefinitions(fieldDefinitionsToUpdate);
        LOG.info("Done");
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

}
