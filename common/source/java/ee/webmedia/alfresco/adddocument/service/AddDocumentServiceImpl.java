package ee.webmedia.alfresco.adddocument.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.bind.DatatypeConverter;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.FileCopyUtils;

import ee.webmedia.alfresco.adddocument.AddDocumentException;
import ee.webmedia.alfresco.adddocument.generated.AddDocumentRequest;
import ee.webmedia.alfresco.adddocument.generated.AddDocumentResponse;
import ee.webmedia.alfresco.adddocument.generated.Field;
import ee.webmedia.alfresco.adddocument.generated.Fields;
import ee.webmedia.alfresco.adddocument.generated.File;
import ee.webmedia.alfresco.adddocument.generated.Files;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.docadmin.web.DocAdminUtil;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docconfig.service.PropDefCacheKey;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.service.DocumentServiceImpl;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TreeNode;

/**
 * @author Riina Tens
 */
public class AddDocumentServiceImpl implements AddDocumentService {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AddDocumentServiceImpl.class);

    private static List<String> forbiddenFieldIds = Arrays.asList(DocumentCommonModel.Props.FUNCTION.getLocalName(), DocumentCommonModel.Props.SERIES.getLocalName(),
            DocumentCommonModel.Props.VOLUME.getLocalName(), DocumentCommonModel.Props.CASE.getLocalName(), DocumentDynamicModel.Props.DOCUMENT_TEMPLATE.getLocalName());
    private static List<String> allowedHiddenFields = Arrays.asList(DocumentCommonModel.Props.SHORT_REG_NUMBER.getLocalName(),
            DocumentCommonModel.Props.INDIVIDUAL_NUMBER.getLocalName(), DocumentCommonModel.Props.OWNER_ID.getLocalName(), DocumentDynamicModel.Props.SIGNER_ID.getLocalName(),
            DocumentDynamicModel.Props.SUBSTITUTE_ID.getLocalName());
    private static Map<String, Boolean> registrationGroupFields = new HashMap<String, Boolean>();
    private static Map<String, Boolean> ownerGroupFields = new HashMap<String, Boolean>();
    private static Map<String, Boolean> signerGroupFields = new HashMap<String, Boolean>();
    private static Map<String, Boolean> substituteGroupFields = new HashMap<String, Boolean>();

    static {
        registrationGroupFields.put(DocumentCommonModel.Props.REG_NUMBER.getLocalName(), true);
        registrationGroupFields.put(DocumentCommonModel.Props.REG_DATE_TIME.getLocalName(), true);
        registrationGroupFields.put(DocumentCommonModel.Props.SHORT_REG_NUMBER.getLocalName(), true);
        registrationGroupFields.put(DocumentCommonModel.Props.INDIVIDUAL_NUMBER.getLocalName(), false);

        ownerGroupFields.put(DocumentCommonModel.Props.OWNER_ID.getLocalName(), true);
        ownerGroupFields.put(DocumentCommonModel.Props.OWNER_NAME.getLocalName(), true);
        ownerGroupFields.put(DocumentDynamicModel.Props.OWNER_SERVICE_RANK.getLocalName(), false);
        ownerGroupFields.put(DocumentCommonModel.Props.OWNER_JOB_TITLE.getLocalName(), false);
        ownerGroupFields.put(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT.getLocalName(), false);
        ownerGroupFields.put(DocumentDynamicModel.Props.OWNER_WORK_ADDRESS.getLocalName(), false);
        ownerGroupFields.put(DocumentCommonModel.Props.OWNER_EMAIL.getLocalName(), false);
        ownerGroupFields.put(DocumentCommonModel.Props.OWNER_PHONE.getLocalName(), false);

        signerGroupFields.put(DocumentDynamicModel.Props.SIGNER_ID.getLocalName(), true);
        signerGroupFields.put(DocumentCommonModel.Props.SIGNER_NAME.getLocalName(), true);
        signerGroupFields.put(DocumentDynamicModel.Props.SIGNER_SERVICE_RANK.getLocalName(), false);
        signerGroupFields.put(DocumentCommonModel.Props.SIGNER_JOB_TITLE.getLocalName(), false);
        signerGroupFields.put(DocumentCommonModel.Props.SIGNER_ORG_STRUCT_UNIT.getLocalName(), false);
        signerGroupFields.put(DocumentDynamicModel.Props.SIGNER_WORK_ADDRESS.getLocalName(), false);
        signerGroupFields.put(DocumentDynamicModel.Props.SIGNER_EMAIL.getLocalName(), false);
        signerGroupFields.put(DocumentDynamicModel.Props.SIGNER_PHONE.getLocalName(), false);

        substituteGroupFields.put(DocumentDynamicModel.Props.SUBSTITUTE_ID.getLocalName(), true);
        substituteGroupFields.put(DocumentSpecificModel.Props.SUBSTITUTE_NAME.getLocalName(), true);
        substituteGroupFields.put(DocumentSpecificModel.Props.SUBSTITUTE_JOB_TITLE.getLocalName(), false);
        substituteGroupFields.put(DocumentSpecificModel.Props.SUBSTITUTION_BEGIN_DATE.getLocalName(), false);
        substituteGroupFields.put(DocumentSpecificModel.Props.SUBSTITUTION_END_DATE.getLocalName(), false);
    }

    private String webServiceDocumentsMenuItemTitle;
    private String webServiceDocumentsListTitle;

    @Override
    public AddDocumentResponse importDocument(AddDocumentRequest request) throws IOException {
        AddDocumentResponse result = new AddDocumentResponse();
        ee.webmedia.alfresco.adddocument.generated.Document documentToAdd = request.getDocument();

        String documentTypeId = documentToAdd.getDocumentType();
        DocumentType documentType = BeanHelper.getDocumentAdminService().getUsedDocumentType(documentTypeId);
        if (documentType == null) {
            throw new AddDocumentException(MessageUtil.getMessage("addDocument_error_doc_type_not_found", documentTypeId));
        }

        DocumentTypeVersion documentTypeVersion = documentType.getLatestDocumentTypeVersion();
        PropDefCacheKey propDefCacheKey = DocAdminUtil.getPropDefCacheKey(DocumentType.class, documentTypeVersion);
        Map<String, Pair<DynamicPropertyDefinition, ee.webmedia.alfresco.docadmin.service.Field>> propDefinitions = BeanHelper.getDocumentConfigService().getPropertyDefinitions(
                propDefCacheKey);
        Fields fields = documentToAdd.getFields();
        validateDocumentFields(documentTypeId, documentTypeVersion, propDefinitions, fields);

        DocumentDynamic importedDocument = BeanHelper.getDocumentDynamicService()
                .createNewDocument(documentTypeVersion, BeanHelper.getAddDocumentService().getWebServiceDocumentsRoot(), false).getFirst();

        Map<QName, Serializable> docProps = collectPropertyValues(propDefinitions, fields);
        if (StringUtils.isBlank((String) docProps.get(DocumentCommonModel.Props.DOC_STATUS))) {
            docProps.put(DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.WORKING.getValueName());
        }
        importedDocument.getNode().getProperties().putAll(RepoUtil.toStringProperties(docProps));
        TreeNode<QName> childAssocTypeQNamesRoot = BeanHelper.getDocumentConfigService().getChildAssocTypeQNameTree(importedDocument.getNode());

        NodeRef docRef = importedDocument.getNodeRef();
        for (Map.Entry<String, Pair<DynamicPropertyDefinition, ee.webmedia.alfresco.docadmin.service.Field>> entry : propDefinitions.entrySet()) {
            QName fieldId = entry.getValue().getFirst().getName();
            QName[] childQNameHierarchy = entry.getValue().getFirst().getChildAssocTypeQNameHierarchy();
            if (!isSubnode(childQNameHierarchy)) {
                continue;
            }
            List<Object> valueList = (List<Object>) docProps.get(fieldId);
            if (valueList == null || valueList.isEmpty()) {
                continue;
            }
            QName childQName = childQNameHierarchy[0];
            List<Node> childNodes = importedDocument.getNode().getAllChildAssociations(childQName);
            int childrenToAdd = valueList.size() - childNodes.size();
            for (int i = 0; i < childrenToAdd; i++) {
                BeanHelper.getDocumentDynamicService().createChildNodesHierarchyAndSetDefaultPropertyValues(importedDocument.getNode(), childQNameHierarchy,
                        documentTypeVersion);
            }

            int propCounter = 0;
            for (Node childNode : importedDocument.getNode().getAllChildAssociations(childQName)) {
                childNode.getProperties().put(entry.getValue().getFirst().getName().toString(), valueList.get(propCounter));
                propCounter++;
            }
        }

        DocumentServiceImpl.PropertyChangesMonitorHelper propertyChangesMonitorHelper = new DocumentServiceImpl.PropertyChangesMonitorHelper();
        BeanHelper.getDocumentDynamicService().saveThisNodeAndChildNodes(null, importedDocument.getNode(), childAssocTypeQNamesRoot.getChildren(), null,
                propertyChangesMonitorHelper, null);

        addFiles(documentToAdd, docRef);

        String senderApplication = documentToAdd.getSenderApplication();
        String logMsgKey = "document_log_status_imported_web_service";
        BeanHelper.getLogService().addLogEntry(LogEntry.create(LogObject.DOCUMENT, BeanHelper.getUserService(), docRef, logMsgKey, senderApplication));
        BeanHelper.getDocumentLogService().addDocumentLog(docRef, MessageUtil.getMessage(logMsgKey, senderApplication), senderApplication);
        result.setStatus("OK");
        return result;
    }

    private void addFiles(ee.webmedia.alfresco.adddocument.generated.Document documentToAdd, NodeRef docRef) throws IOException {
        Files files = documentToAdd.getFiles();
        if (files != null) {
            FileFolderService fileFolderService = BeanHelper.getFileFolderService();
            GeneralService generalService = BeanHelper.getGeneralService();
            MimetypeService mimetypeService = BeanHelper.getMimetypeService();
            for (File file : files.getFile()) {
                DataHandler emptyContent = file.getContent();
                if (emptyContent == null && StringUtils.isBlank(file.getName())) {
                    continue;
                }

                if (file.getContent() == null) {
                    throw new AddDocumentException(MessageUtil.getMessage("addDocument_error_doc_files_empty_name_or_content"));
                }
                String importedFileName = file.getName();
                boolean hasImportName = StringUtils.isNotBlank(importedFileName);
                String mimetype = hasImportName ? mimetypeService.guessMimetype(importedFileName) : null;
                if (StringUtils.isBlank(mimetype)) {
                    mimetype = MimetypeMap.MIMETYPE_BINARY;
                }
                String fileName = hasImportName ? importedFileName : MessageUtil.getMessage("addDocument_imported_file_default_name") + "."
                        + mimetypeService.getExtension(mimetype);
                FileInfo createdFile = fileFolderService.create(docRef, generalService.getUniqueFileName(docRef, fileName), ContentModel.TYPE_CONTENT);
                ContentWriter writer = fileFolderService.getWriter(createdFile.getNodeRef());
                writer.setMimetype(mimetype);
                writer.setEncoding(createdFile.getContentData().getEncoding());
                OutputStream os = writer.getContentOutputStream();
                FileCopyUtils.copy(file.getContent().getInputStream(), os);
            }
        }
    }

    private Map<QName, Serializable> collectPropertyValues(Map<String, Pair<DynamicPropertyDefinition, ee.webmedia.alfresco.docadmin.service.Field>> propDefinitions, Fields fields) {
        Map<QName, Serializable> docProps = new HashMap<QName, Serializable>();
        if (fields != null) {
            for (Field importField : fields.getField()) {
                String fieldId = importField.getId();
                DynamicPropertyDefinition docPropDefinition = propDefinitions.get(fieldId).getFirst();
                List<String> importValue = importField.getValues().getValue();
                Serializable value;
                QName dataTypeQName = docPropDefinition.getDataTypeQName();
                if (!docPropDefinition.isMultiValued() && !isSubnode(docPropDefinition.getChildAssocTypeQNameHierarchy())) {
                    String rawValue = importValue == null ? null : importValue.get(0);
                    value = convertValue(rawValue, dataTypeQName, fieldId);
                } else {
                    if (importValue == null) {
                        value = null;
                    } else {
                        List<Object> valueList = new ArrayList<Object>();
                        List<String> rawValues = importValue;
                        for (String rawValue : rawValues) {
                            valueList.add(convertValue(rawValue, dataTypeQName, fieldId));
                        }
                        value = (Serializable) valueList;
                    }
                }
                docProps.put(docPropDefinition.getName(), value);
            }
        }
        return docProps;
    }

    private void validateDocumentFields(String documentTypeId, DocumentTypeVersion documentTypeVersion,
            Map<String, Pair<DynamicPropertyDefinition, ee.webmedia.alfresco.docadmin.service.Field>> propDefinitions,
            Fields fields) {
        Map<String, Boolean> registrationFields = new HashMap<String, Boolean>();
        Map<String, Boolean> ownerFields = new HashMap<String, Boolean>();
        Map<String, Boolean> signerFields = new HashMap<String, Boolean>();
        Map<QName, Pair<String, Integer>> subnodePropsSize = new HashMap<QName, Pair<String, Integer>>();
        Map<String, Pair<String, Integer>> groupPropsSize = new HashMap<String, Pair<String, Integer>>();
        Map<String, List<Boolean>> substituteFields = new HashMap<String, List<Boolean>>();
        Boolean substitutionSingleValued = null;
        if (fields != null) {
            for (Field importField : fields.getField()) {
                String fieldId = importField.getId();
                // hidden fields are not allowed
                if (!propDefinitions.containsKey(fieldId) || (propDefinitions.get(fieldId).getSecond() == null && !allowedHiddenFields.contains(fieldId))) {
                    throw new AddDocumentException(MessageUtil.getMessage("addDocument_error_doc_field_not_found", documentTypeId, fieldId));
                }
                if (forbiddenFieldIds.contains(fieldId)) {
                    throw new AddDocumentException(MessageUtil.getMessage("addDocument_error_doc_field_not_allowed", fieldId));
                }
                DynamicPropertyDefinition docPropDefinition = propDefinitions.get(fieldId).getFirst();
                List<String> importValue = importField.getValues().getValue();
                boolean singleValued = !docPropDefinition.isMultiValued();
                QName[] childHierarchy = docPropDefinition.getChildAssocTypeQNameHierarchy();
                // grandchildren are not allowed; children can have only single valued values
                boolean isSubnode = isSubnode(childHierarchy);
                if (singleValued && !isSubnode && importValue != null && importValue.size() > 1) {
                    throw new AddDocumentException(MessageUtil.getMessage("addDocument_error_doc_field_multivalue_not_allowed", fieldId));
                }
                if (!(childHierarchy == null || childHierarchy.length == 0 || (isSubnode && childHierarchy.length == 1 && singleValued))) {
                    throw new AddDocumentException(MessageUtil.getMessage("addDocument_error_doc_field_not_allowed", fieldId));
                }
                if (isSubnode) {
                    QName childType = childHierarchy[0];
                    Pair<String, Integer> groupFieldAndSize = subnodePropsSize.get(childType);
                    Integer groupSize = groupFieldAndSize != null ? groupFieldAndSize.getSecond() : null;
                    int importValueSize = importValue == null ? 0 : importValue.size();
                    if (groupSize == null) {
                        subnodePropsSize.put(childType, new Pair<String, Integer>(fieldId, importValueSize));
                    } else if (groupSize != importValueSize) {
                        throw new AddDocumentException(MessageUtil.getMessage("addDocument_error_doc_fields_multivalued_not_same_length", groupFieldAndSize.getFirst(), fieldId));
                    }
                } else {
                    String groupId = getFieldGroupId(documentTypeVersion, fieldId);
                    if (groupId != null) {
                        Pair<String, Integer> groupFieldAndSize = groupPropsSize.get(groupId);
                        Integer groupSize = groupFieldAndSize != null ? groupFieldAndSize.getSecond() : null;
                        int importValueSize = importValue == null ? 0 : importValue.size();
                        if (groupSize == null) {
                            groupPropsSize.put(groupId, new Pair<String, Integer>(fieldId, importValueSize));
                        } else if (groupSize != importValueSize) {
                            throw new AddDocumentException(
                                    MessageUtil.getMessage("addDocument_error_doc_fields_multivalued_not_same_length", groupFieldAndSize.getFirst(), fieldId));
                        }
                    }
                }
                if (registrationGroupFields.containsKey(fieldId)) {
                    addNotEmptyCheck(registrationFields, fieldId, importValue);
                } else if (ownerGroupFields.containsKey(fieldId)) {
                    addNotEmptyCheck(ownerFields, fieldId, importValue);
                } else if (signerGroupFields.containsKey(fieldId)) {
                    addNotEmptyCheck(signerFields, fieldId, importValue);
                } else if (substituteGroupFields.containsKey(fieldId)) {
                    if (substitutionSingleValued == null) {
                        substitutionSingleValued = singleValued;
                    }
                    if (singleValued != substitutionSingleValued) {
                        throw new AddDocumentException("addDocument_error_doc_fields_substitute_data_incomplete");
                    }
                    if (singleValued) {
                        substituteFields.put(fieldId, Arrays.asList(isValueNotBlank(importValue)));
                    } else {
                        List<Boolean> isValueNotBlank = new ArrayList<Boolean>();
                        substituteFields.put(fieldId, isValueNotBlank);
                        if (importValue != null) {
                            for (String value : importValue) {
                                isValueNotBlank.add(StringUtils.isNotBlank((value)));
                            }
                        }
                    }
                }

            }
        }
        throwIfMandatoryValueMissing(registrationGroupFields, registrationFields, "addDocument_error_doc_fields_registration_data_incomplete");
        throwIfMandatoryValueMissing(ownerGroupFields, ownerFields, "addDocument_error_doc_fields_owner_data_incomplete");
        throwIfMandatoryValueMissing(signerGroupFields, signerFields, "addDocument_error_doc_fields_signer_data_incomplete");
        // for substitute data, check each row for consistency
        int substGroupSize = substituteFields.size() == 0 ? 0 : (substituteFields.values().isEmpty() ? 0 : substituteFields.values().iterator().next().size());
        for (int i = 0; i < substGroupSize; i++) {
            Map<String, Boolean> substFields = new HashMap<String, Boolean>();
            for (Map.Entry<String, List<Boolean>> entry : substituteFields.entrySet()) {
                substFields.put(entry.getKey(), entry.getValue().get(i));
            }
            throwIfMandatoryValueMissing(substituteGroupFields, substFields, "addDocument_error_doc_fields_substitute_data_incomplete");
        }
    }

    private String getFieldGroupId(DocumentTypeVersion documentTypeVersion, String fieldId) {
        for (MetadataItem metadataItem : documentTypeVersion.getMetadata().getList()) {
            if (metadataItem instanceof ee.webmedia.alfresco.docadmin.service.Field) {
                if (((ee.webmedia.alfresco.docadmin.service.Field) metadataItem).getFieldId().equals(fieldId)) {
                    return null;
                }
            } else if (metadataItem instanceof FieldGroup) {
                FieldGroup fieldGroup = (FieldGroup) metadataItem;
                if (fieldGroup.getFieldById(fieldId) != null) {
                    return fieldGroup.getName();
                }
            }
        }
        return null;
    }

    private boolean isSubnode(QName[] childHierarchy) {
        return childHierarchy != null && childHierarchy.length > 0;
    }

    private Serializable convertValue(String rawValue, QName dataType, String fieldId) {
        if (DataTypeDefinition.TEXT.equals(dataType)) {
            return rawValue;
        } else if (DataTypeDefinition.DATE.equals(dataType)) {
            // jaxb parser allows date with only year and month part (like "2012-06"), but this is not valid value here
            if (StringUtils.countMatches(rawValue, "-") < 2) {
                throw new AddDocumentException(MessageUtil.getMessage("addDocument_error_doc_field_type_not_date", fieldId));
            }
            try {
                return new Date(DatatypeConverter.parseDateTime(rawValue).getTimeInMillis());
            } catch (IllegalArgumentException e) {
                throw new AddDocumentException(MessageUtil.getMessage("addDocument_error_doc_field_type_not_date", fieldId));
            }
        } else if (DataTypeDefinition.BOOLEAN.equals(dataType)) {
            if (!"true".equalsIgnoreCase(rawValue) && !"false".equalsIgnoreCase(rawValue)) {
                throw new AddDocumentException(MessageUtil.getMessage("addDocument_error_doc_field_type_not_boolean", fieldId));
            }
            return Boolean.parseBoolean(rawValue);
        } else {
            boolean isDouble = DataTypeDefinition.DOUBLE.equals(dataType);
            if (DataTypeDefinition.LONG.equals(dataType) || isDouble) {
                DataTypeDefinition dataTypeDefinition = BeanHelper.getDictionaryService().getDataType(dataType);
                try {
                    return (Serializable) DefaultTypeConverter.INSTANCE.convert(dataTypeDefinition, rawValue);
                } catch (Exception e) {
                    if (isDouble) {
                        try {
                            return (Serializable) DefaultTypeConverter.INSTANCE.convert(dataTypeDefinition,
                                    StringUtils.replace(rawValue, ",", "."));
                        } catch (Exception ex) {
                            throw new AddDocumentException(MessageUtil.getMessage("addDocument_error_doc_field_type_not_double", fieldId));
                        }
                    }
                    throw new AddDocumentException(MessageUtil.getMessage("addDocument_error_doc_field_type_not_integer", fieldId));
                }
            }
            throw new AddDocumentException(MessageUtil.getMessage("addDocument_error_doc_field_type_not_importable", fieldId, dataType.toString()));
        }
    }

    private void throwIfMandatoryValueMissing(Map<String, Boolean> fieldsWithMandatory, Map<String, Boolean> fieldsWithEmptyCheck, String errorMessageKey) {
        // if some value in given group is filled
        if (fieldsWithEmptyCheck.containsValue(Boolean.TRUE)) {
            // check all fields belonging to this group
            for (Map.Entry<String, Boolean> entry : fieldsWithMandatory.entrySet()) {
                String fieldId = entry.getKey();
                // if field is mandatory, but no value is provided, throw error
                if (entry.getValue() && (!fieldsWithEmptyCheck.containsKey(fieldId) || !fieldsWithEmptyCheck.get(fieldId))) {
                    throw new AddDocumentException(MessageUtil.getMessage(errorMessageKey));
                }
            }
        }
    }

    private void addNotEmptyCheck(Map<String, Boolean> fields, String fieldId, List<String> importValue) {
        fields.put(fieldId, isValueNotBlank(importValue));
    }

    private boolean isValueNotBlank(List<String> importValue) {
        return importValue == null ? false : StringUtils.isNotBlank(importValue.get(0));
    }

    @Override
    public List<Document> getAllDocumentFromWebService() {
        List<Document> documents = BeanHelper.getDocumentService().getIncomingDocuments(getWebServiceDocumentsRoot());
        Collections.sort(documents);
        return documents;
    }

    @Override
    public int getAllDocumentFromWebServiceCount() {
        return BeanHelper.getDocumentService().getAllDocumentsFromFolderCount(getWebServiceDocumentsRoot());
    }

    @Override
    public NodeRef getWebServiceDocumentsRoot() {
        return BeanHelper.getGeneralService().getNodeRef(DocumentCommonModel.Repo.WEB_SERVICE_SPACE);
    }

    @Override
    public String getWebServiceDocumentsMenuItemTitle() {
        return webServiceDocumentsMenuItemTitle;
    }

    public void setWebServiceDocumentsMenuItemTitle(String webServiceDocumentsMenuItemTitle) {
        this.webServiceDocumentsMenuItemTitle = webServiceDocumentsMenuItemTitle;
    }

    @Override
    public String getWebServiceDocumentsListTitle() {
        return webServiceDocumentsListTitle;
    }

    public void setWebServiceDocumentsListTitle(String webServiceDocumentsListTitle) {
        this.webServiceDocumentsListTitle = webServiceDocumentsListTitle;
    }

}
