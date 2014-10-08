package ee.webmedia.alfresco.docconfig.bootstrap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldAndGroupBase;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Fix for task 182456 in already existing deployments
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> develop-5.1
 */
public class SystematicDocumentTypesFixBootstrap extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SystematicDocumentTypesFixBootstrap.class);

    private DocumentAdminService documentAdminService;

    @Override
    protected void executeInternal() throws Throwable {
        executeInternalImpl();
    }

    public void executeInternalImpl() {
        Map<String, Pair<String, Pair<Set<String>, Set<QName>>>> systematicDocumentTypes = new HashMap<String, Pair<String, Pair<Set<String>, Set<QName>>>>();

        addSystematicDocumentType(systematicDocumentTypes, SystematicDocumentType.INCOMING_LETTER.getId(), "Sissetulev kiri",
                new String[] {
                        SystematicFieldGroupNames.SENDER_NAME_AND_EMAIL,
                        SystematicFieldGroupNames.SENDER_REG_NUMBER_AND_DATE,
                        SystematicFieldGroupNames.COMPLIENCE
        },
                new QName[] {
                        DocumentSpecificModel.Props.TRANSMITTAL_MODE,
                        DocumentSpecificModel.Props.DUE_DATE,
        });

        addSystematicDocumentType(systematicDocumentTypes, SystematicDocumentType.OUTGOING_LETTER.getId(), "Väljaminev kiri",
                new String[] {
                        SystematicFieldGroupNames.SENDER_REG_NUMBER_AND_DATE,
                        SystematicFieldGroupNames.SIGNER,
                        SystematicFieldGroupNames.RECIPIENTS,
                        SystematicFieldGroupNames.ADDITIONAL_RECIPIENTS
         },
                null);

        createSystematicDocumentTypes(systematicDocumentTypes);
    }

    private void addSystematicDocumentType(Map<String, Pair<String, Pair<Set<String>, Set<QName>>>> systematicDocumentTypes, String documentTypeId, String documentTypeName,
            String[] fieldGroupDefinitionNamesArray, QName[] fieldDefinitionIdsArray) {
        Set<String> fieldGroupDefinitionNames = fieldGroupDefinitionNamesArray == null ? new HashSet<String>() :
                new HashSet<String>(Arrays.asList(fieldGroupDefinitionNamesArray));
        Set<QName> fieldDefinitionIds = fieldDefinitionIdsArray == null ? new HashSet<QName>() :
                new HashSet<QName>(Arrays.asList(fieldDefinitionIdsArray));
        systematicDocumentTypes.put(
                documentTypeId,
                new Pair<String, Pair<Set<String>, Set<QName>>>(documentTypeName,
                        new Pair<Set<String>, Set<QName>>(fieldGroupDefinitionNames, fieldDefinitionIds)));
    }

    private void createSystematicDocumentTypes(Map<String, Pair<String, Pair<Set<String>, Set<QName>>>> systematicDocumentTypes) {

        systematicDocumentTypes.keySet().removeAll(documentAdminService.getNonExistingDocumentTypes(systematicDocumentTypes.keySet()));
        if (systematicDocumentTypes.isEmpty()) {
            LOG.info("No systematic document types to fix");
            return;
        }
        LOG.info("Following systematic document types are not yet fixed: " + systematicDocumentTypes.keySet());

        fixSystematicDocumentTypes(systematicDocumentTypes);
    }

    private void fixSystematicDocumentTypes(Map<String, Pair<String, Pair<Set<String>, Set<QName>>>> systematicDocumentTypes) {
        Set<MessageData> messages = new LinkedHashSet<MessageData>();
        for (Entry<String, Pair<String, Pair<Set<String>, Set<QName>>>> systematicDocumentType : systematicDocumentTypes.entrySet()) {
            String documentTypeId = systematicDocumentType.getKey();
            DocumentType docType = documentAdminService.getDocumentType(documentTypeId, DocumentAdminService.DOC_TYPE_WITHOUT_OLDER_DT_VERSION_CHILDREN);
            final DocumentTypeVersion ver = docType.addNewLatestDocumentTypeVersion();
            final Set<String> fieldGroupNames = systematicDocumentType.getValue().getSecond().getFirst();
            final List<String> fieldDefinitionIds = Field.getLocalNames(systematicDocumentType.getValue().getSecond().getSecond());

            boolean needSave = false;
            for (MetadataItem metadataItem : ver.getMetadata()) {
                String info = null;
                if (metadataItem instanceof Field) {
                    Field field = (Field) metadataItem;
                    String fieldId = field.getFieldId();
                    if (!fieldDefinitionIds.contains(fieldId)) {
                        continue;
                    }
                    if (!field.isSystematic() || !field.getOriginalFieldId().equals(fieldId)) {
                        LOG.error("!!!\n\nERROR: Field is not correct, skipping: " + field + "\n\n");
                        continue;
                    }
                    info = "field fieldId=" + fieldId;
                    fieldDefinitionIds.remove(fieldId);
                }
                if (metadataItem instanceof FieldGroup) {
                    FieldGroup group = (FieldGroup) metadataItem;
                    String groupName = group.getName();
                    if (!fieldGroupNames.contains(groupName)) {
                        continue;
                    }
                    if (!group.isSystematic()) {
                        LOG.error("!!!\n\nERROR: Group is not correct, skipping: " + group + "\n\n");
                        continue;
                    }
                    info = "group name=" + groupName;
                    fieldGroupNames.remove(groupName);
                    if (SystematicFieldGroupNames.SENDER_NAME_AND_EMAIL.equals(groupName)) {
                        String fieldOriginalId = DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL.getLocalName();
                        if (group.getFieldsByOriginalId().get(fieldOriginalId) == null) {
                            LOG.error("!!!\n\nERROR: Group is not correct, field with originalId=" + fieldOriginalId + " is missing: " + group + "\n\n");
                        }
                    }
                }
                if (metadataItem instanceof FieldAndGroupBase) {
                    FieldAndGroupBase fieldAndGroupBase = (FieldAndGroupBase) metadataItem;
                    if (fieldAndGroupBase.isRemovableFromSystematicDocType()) {
                        LOG.info("Setting removableFromSystematicDocType=false on " + info);
                        fieldAndGroupBase.setRemovableFromSystematicDocType(false);
                        needSave = true;
                    }
                }
            }
            if (!fieldDefinitionIds.isEmpty()) {
                LOG.error("!!!\n\nERROR: Document type is not correct, the following systematic fields are missing: " + fieldDefinitionIds + "\n\n");
            }
            if (!fieldGroupNames.isEmpty()) {
                LOG.error("!!!\n\nERROR: Document type is not correct, the following systematic groups are missing: " + fieldGroupNames + "\n\n");
            }
            if (needSave) {
                LOG.info("Saving document type: " + documentTypeId);
                Pair<DocumentType, MessageData> result = documentAdminService.saveOrUpdateDocumentType(docType);
                MessageData messageData = result.getSecond();
                if (messageData != null) {
                    messages.add(messageData);
                }
            } else {
                LOG.info("Skipping document type, all is well: " + documentTypeId);
            }
        }
        for (MessageData messageData : messages) {
            MessageUtil.addStatusMessage(messageData);
        }
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

}
