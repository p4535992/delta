package ee.webmedia.alfresco.docadmin.bootstrap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;

/**
 * Fix for task 179069 in already existing data.
 * 
 * @author Alar Kvell
 */
public class FieldDefinitionDocTypesUpdater extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(FieldDefinitionDocTypesUpdater.class);

    private DocumentAdminService documentAdminService;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Loading all field definitions");
        Map<String, FieldDefinition> fieldDefinitions = documentAdminService.getFieldDefinitionsByFieldIds();
        Map<String, FieldDefinition> fieldDefinitionsToUpdate = new HashMap<String, FieldDefinition>();
        LOG.info("Loading all document types");
        for (DocumentType docType : documentAdminService.getDocumentTypes()) {
            for (DocumentTypeVersion docVer : docType.getDocumentTypeVersions()) {
                for (Field field : docVer.getFieldsDeeply()) {
                    FieldDefinition fieldDef = fieldDefinitions.get(field.getFieldId());
                    if (fieldDef != null) {
                        List<String> docTypesOfFieldDef = fieldDef.getDocTypes();
                        if (!docTypesOfFieldDef.contains(docType.getDocumentTypeId())) {
                            docTypesOfFieldDef.add(docType.getDocumentTypeId());
                            fieldDefinitionsToUpdate.put(fieldDef.getFieldId(), fieldDef);
                        }
                    }
                }
            }
        }
        LOG.info("Updating " + fieldDefinitionsToUpdate.size() + " field definitions");
        for (FieldDefinition fieldDef : fieldDefinitionsToUpdate.values()) {
            documentAdminService.saveOrUpdateField(fieldDef);
        }
        LOG.info("Done");
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

}
