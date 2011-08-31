package ee.webmedia.alfresco.docadmin.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.classificator.constant.FieldChangeableIf;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.web.MetadataItemCompareUtil;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * @author Ats Uiboupin
 */
public class DocumentAdminServiceImpl implements DocumentAdminService, InitializingBean {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentAdminServiceImpl.class);

    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private GeneralService generalService;
    private BaseService baseService;
    private MenuService menuService;
    private UserService userService;

    private NodeRef documentTypesRoot;
    private NodeRef fieldDefinitionsRoot;

    @Override
    public void afterPropertiesSet() throws Exception {
        baseService.addTypeMapping(DocumentAdminModel.Types.DOCUMENT_TYPE, DocumentType.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.DOCUMENT_TYPE_VERSION, DocumentTypeVersion.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.FIELD, Field.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.FIELD_GROUP, FieldGroup.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.SEPARATION_LINE, SeparatorLine.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.FIELD_DEFINITION, FieldDefinition.class);
    }

    @Override
    public List<DocumentType> getDocumentTypes() {
        return getAllDocumentTypes(null);
    }

    @Override
    public List<DocumentType> getDocumentTypes(boolean used) {
        return getAllDocumentTypes(Boolean.valueOf(used));
    }

    @Override
    public DocumentType getDocumentType(String id) {
        if ("type1".equals(id)) {
            return getTestDocumentType1();
        }
        if ("type2".equals(id)) {
            return getTestDocumentType2();
        }
        if ("type3".equals(id)) {
            return getTestDocumentType3();
        }
        String xPath = DocumentAdminModel.Repo.DOCUMENT_TYPES_SPACE + "/" + DocumentType.getAssocName(id);
        return getDocumentType(generalService.getNodeRef(xPath));
    }

    @Override
    public DocumentType getDocumentType(NodeRef docTypeRef) {
        return baseService.getObject(docTypeRef, DocumentType.class);
    }

    private DocumentType getTestDocumentType1() {
        DocumentType docType = new DocumentType(getDocumentTypesRoot());
        docType.setDocumentTypeId("type1");
        docType.setName("Tüüp 1");

        DocumentTypeVersion docVer = docType.getDocumentTypeVersions().add();
        docVer.setVersionNr(1);
        addSystematicMetadataItems(docVer);

        Field field1 = docVer.getMetadata().add(Field.class);
        field1.setOrder(3);
        field1.setFieldId(QName.createQName("docdyn:field1", namespaceService));
        field1.setName("Väli 1");
        field1.setFieldTypeEnum(FieldType.TEXT_FIELD);

        return docType;
    }

    private DocumentType getTestDocumentType2() {
        DocumentType docType = new DocumentType(getDocumentTypesRoot());
        docType.setDocumentTypeId("type2");
        docType.setName("Tüüp 2");

        DocumentTypeVersion docVer = docType.getDocumentTypeVersions().add();
        docVer.setVersionNr(1);
        addSystematicMetadataItems(docVer);

        Field field1 = docVer.getMetadata().add(Field.class);
        field1.setOrder(3);
        field1.setFieldId(QName.createQName("docdyn:field2", namespaceService));
        field1.setName("Väli 2");
        field1.setFieldTypeEnum(FieldType.TEXT_FIELD);

        Field field2 = docVer.getMetadata().add(Field.class);
        field2.setOrder(4);
        field2.setFieldId(QName.createQName("docdyn:field3", namespaceService));
        field2.setName("Väli 3");
        field2.setFieldTypeEnum(FieldType.TEXT_FIELD);

        return docType;
    }

    private DocumentType getTestDocumentType3() {
        DocumentType docType = new DocumentType(getDocumentTypesRoot());
        docType.setDocumentTypeId("type3");
        docType.setName("Tüüp 3");

        DocumentTypeVersion docVer = docType.getDocumentTypeVersions().add();
        docVer.setVersionNr(1);
        addSystematicMetadataItems(docVer);

        Field field1 = docVer.getMetadata().add(Field.class);
        field1.setOrder(3);
        field1.setFieldId(QName.createQName("docdyn:field3", namespaceService));
        field1.setName("Väli 3");
        field1.setFieldTypeEnum(FieldType.TEXT_FIELD);

        Field field2 = docVer.getMetadata().add(Field.class);
        field2.setOrder(4);
        field2.setFieldId(QName.createQName("docdyn:field4", namespaceService));
        field2.setName("Väli 4");
        field2.setFieldTypeEnum(FieldType.TEXT_FIELD);

        return docType;
    }

    @Override
    public void addSystematicMetadataItems(DocumentTypeVersion docVer) {
        FieldGroup group = docVer.getMetadata().add(FieldGroup.class);
        group.setOrder(1);
        group.setName("Dokumendi asukoht");
        group.setSystematic(true);
        group.setMandatoryForDoc(true);
        group.setRemovableFromSystemDocType(false);

        Field field = group.getFields().add();
        field.setOrder(1);
        field.setFieldId(QName.createQName("docdyn:function", namespaceService));
        field.setName("Funktsioon");
        field.setSystematic(true);
        field.setMandatoryForDoc(true);
        field.setMandatory(true);
        field.setFieldTypeEnum(FieldType.COMBOBOX);
        field.setChangeableIfEnum(FieldChangeableIf.CHANGEABLE_IF_WORKING_DOC);
        field.setRemovableFromSystemDocType(false);
        field.setOnlyInGroup(true);

        Field series = group.getFields().add();
        series.setOrder(2);
        series.setFieldId(QName.createQName("docdyn:series", namespaceService));
        series.setName("Sari");
        series.setSystematic(true);
        series.setMandatoryForDoc(true);
        series.setMandatory(true);
        series.setFieldTypeEnum(FieldType.COMBOBOX);
        series.setChangeableIfEnum(FieldChangeableIf.CHANGEABLE_IF_WORKING_DOC);
        series.setRemovableFromSystemDocType(false);
        series.setOnlyInGroup(true);

        Field volume = group.getFields().add();
        volume.setOrder(3);
        volume.setFieldId(QName.createQName("docdyn:volume", namespaceService));
        volume.setName("Toimik");
        volume.setSystematic(true);
        volume.setMandatoryForDoc(true);
        volume.setMandatory(true);
        volume.setFieldTypeEnum(FieldType.COMBOBOX);
        volume.setChangeableIfEnum(FieldChangeableIf.CHANGEABLE_IF_WORKING_DOC);
        volume.setRemovableFromSystemDocType(false);
        volume.setOnlyInGroup(true);

        Field caseField = group.getFields().add();
        caseField.setOrder(4);
        caseField.setFieldId(QName.createQName("docdyn:case", namespaceService));
        caseField.setName("Asi");
        caseField.setSystematic(true);
        caseField.setMandatoryForDoc(true);
        caseField.setMandatory(true);
        caseField.setFieldTypeEnum(FieldType.COMBOBOX_EDITABLE);
        caseField.setChangeableIfEnum(FieldChangeableIf.CHANGEABLE_IF_WORKING_DOC);
        caseField.setRemovableFromSystemDocType(false);
        caseField.setOnlyInGroup(true);

        Field docName = docVer.getMetadata().add(Field.class);
        docName.setOrder(2);
        docName.setFieldId(QName.createQName("docdyn:docName", namespaceService));
        docName.setName("Pealkiri");
        docName.setSystematic(true);
        docName.setMandatoryForDoc(true);
        docName.setMandatory(true);
        docName.setFieldTypeEnum(FieldType.COMBOBOX_EDITABLE);
        docName.setChangeableIfEnum(FieldChangeableIf.CHANGEABLE_IF_WORKING_DOC);
        docName.setRemovableFromSystemDocType(false);
    }

    private List<DocumentType> getAllDocumentTypes(final Boolean used) {
        return baseService.getChildren(getDocumentTypesRoot(), DocumentType.class, new Predicate<DocumentType>() {
            @Override
            public boolean evaluate(DocumentType documentType) {
                return used == null || documentType.isUsed() == used;
            }
        });
    }

    @Override
    public DocumentType createNewUnSaved() {
        return new DocumentType(getDocumentTypesRoot());
    }

    @Override
    public FieldDefinition createNewUnSavedFieldDefinition() {
        return new FieldDefinition(getFieldDefinitionsRoot());
    }

    @Override
    public void deleteDocumentType(NodeRef docTypeRef) {
        DocumentType documentType = getDocumentType(docTypeRef);
        if (documentType.isSystematic()) {
            throw new IllegalArgumentException("docType_list_action_delete_failed_systematic"); // shouldn't happen, because systematic can't be changed at runtime
        }
        boolean docTypeInUse = false; // TODO DLSeadist: CLTASK 166371 determine if type is in use
        if (docTypeInUse) {
            throw new UnableToPerformException("docType_delete_failed_inUse");
        }
        nodeService.deleteNode(docTypeRef);
    }

    @Override
    public DocumentType saveOrUpdateDocumentType(DocumentType docTypeOriginal) {
        DocumentType docType = docTypeOriginal.clone();
        boolean wasUnsaved = docType.isUnsaved();

        updateOrRemoveLatestDocTypeVersion(docType);
        baseService.saveObject(docType);

        updatePublicAdr(docType, wasUnsaved);
        menuService.menuUpdated();
        return docType;
    }

    @Override
    public <F extends Field> F saveOrUpdateField(F originalFieldDef) {
        @SuppressWarnings("unchecked")
        F fieldDef = (F) originalFieldDef.clone();
        baseService.saveObject(fieldDef);
        return fieldDef;
    }

    @Override
    public List<FieldDefinition> saveOrUpdateFieldDefinitions(List<FieldDefinition> fieldDefinitions) {
        ArrayList<FieldDefinition> saved = new ArrayList<FieldDefinition>();
        for (FieldDefinition fieldDefinition : fieldDefinitions) {
            saved.add(saveOrUpdateField(fieldDefinition));
        }
        // TODO DLSeadist task 166391:
        // 4.1.11.2. Kui mõnel andmevälja real on muudetud parameterOrderInDocSearch väärtust,
        // siis teostatakse andmeväljade ümberjärjestamine veeru parameterOrderInDocSearch alusel (vt Üldised reeglid.docx punkt 3).
        // 4.1.10.1.4.1.11.3. Kui mõnel andmevälja real on muudetud parameterOrderInVolSearch väärtust,
        // siis teostatakse andmeväljade ümberjärjestamine veeru parameterOrderInVolSearch alusel (vt Üldised reeglid.docx punkt 3).
        return saved;
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        // createFieldDefinitionsTestData(); // FIXME DLSeadist test data
        return baseService.getChildren(getFieldDefinitionsRoot(), FieldDefinition.class);
    }

    @Override
    public FieldDefinition getFieldDefinition(QName fieldId) {
        return baseService.getChild(getFieldDefinitionsRoot(), fieldId, FieldDefinition.class);
    }

    @Override
    public Field getField(NodeRef fieldDefRef) {
        return baseService.getObject(fieldDefRef, FieldDefinition.class);
    }

    @Override
    public void deleteFieldDefinition(NodeRef fieldDefRef) {
        nodeService.deleteNode(fieldDefRef);
    }

    /** FIXME DLSeadist test data */
    private List<FieldDefinition> createFieldDefinitionsTestData() {
        FieldDefinition fd1 = new FieldDefinition(getFieldDefinitionsRoot());
        fd1.setName("testFieldDefName");
        fd1.setFieldId(QName.createQName(DocumentAdminModel.URI, "testFieldId"));
        fd1.setSystematic(true);
        fd1.setDocTypes(Arrays.asList("type1", "type2"));
        fd1.setParameterOrderInDocSearch(1);
        fd1.setParameterOrderInVolSearch(2);
        fd1.setVolTypes(Arrays.asList("volType1", "volType2"));
        fd1.setParameterInDocSearch(true);
        fd1.setParameterInVolSearch(false);

        FieldDefinition fd2 = new FieldDefinition(getFieldDefinitionsRoot());
        fd2.setName("testFieldDefName2");
        fd2.setFieldId(QName.createQName(DocumentAdminModel.URI, "testFieldId2"));
        fd2.setSystematic(false);
        fd2.setDocTypes(Arrays.asList("type2"));
        fd2.setParameterOrderInDocSearch(3);
        fd2.setParameterOrderInVolSearch(4);
        fd2.setVolTypes(Arrays.asList("volType2"));
        fd2.setParameterInDocSearch(false);
        fd2.setParameterInVolSearch(true);

        FieldDefinition fd3 = new FieldDefinition(getFieldDefinitionsRoot());
        fd3.setName("testFieldDefName3");
        fd3.setFieldId(QName.createQName(DocumentAdminModel.URI, "testFieldId3"));
        fd3.setSystematic(true);
        fd3.setDocTypes(Collections.<String> emptyList());
        fd3.setParameterOrderInDocSearch(1);
        fd3.setParameterOrderInVolSearch(2);
        // fd3.setVolTypes(null);
        fd3.setVolTypes(Collections.<String> emptyList());
        fd3.setParameterInDocSearch(true);
        fd3.setParameterInVolSearch(false);

        FieldDefinition fd4 = new FieldDefinition(getFieldDefinitionsRoot());
        fd4.setName("testFieldDefName4");
        fd4.setFieldId(QName.createQName(DocumentAdminModel.URI, "testFieldId4"));
        fd4.setSystematic(false);
        fd4.setDocTypes(null);
        fd4.setParameterOrderInDocSearch(3);
        fd4.setParameterOrderInVolSearch(4);
        fd4.setVolTypes(Arrays.asList("volType2"));
        fd4.setParameterInDocSearch(false);
        fd4.setParameterInVolSearch(true);

        List<FieldDefinition> fieldDefinitions = Arrays.asList(fd1, fd2, fd3, fd4);
        for (FieldDefinition fDef : fieldDefinitions) {
            saveOrUpdateField(fDef);
        }
        return fieldDefinitions;
    }

    private void updatePublicAdr(DocumentType docType, boolean wasUnsaved) {
        Boolean oldPublicAdr = wasUnsaved ? false : (Boolean) nodeService.getProperty(docType.getNodeRef(), DocumentAdminModel.Props.PUBLIC_ADR);
        if (oldPublicAdr == null) {
            oldPublicAdr = Boolean.FALSE;
        }
        Boolean newPublicAdr = docType.isPublicAdr();

        if (oldPublicAdr.booleanValue() != newPublicAdr.booleanValue()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Changing publicAdr of DocumentType " + docType.getDocumentTypeId() + " from "
                        + oldPublicAdr.toString().toUpperCase() + " to " + newPublicAdr.toString().toUpperCase());
            }
            QName id = docType.getAssocName();
            if (newPublicAdr) {
                getAdrService().addDocumentType(id);
            } else {
                getAdrService().deleteDocumentType(id);
            }
        }
    }

    private void updateOrRemoveLatestDocTypeVersion(DocumentType docType) {
        int versionNr = 1;
        boolean saved = docType.isSaved();
        if (saved) {
            Integer latestVersion = docType.getLatestVersion();
            ChildrenList<DocumentTypeVersion> documentTypeVersions = docType.getDocumentTypeVersions();
            ChildrenList<MetadataItem> savedMetadata = documentTypeVersions.get(latestVersion - 2).getMetadata();
            DocumentTypeVersion latestDocumentTypeVersion = docType.getLatestDocumentTypeVersion();
            ChildrenList<MetadataItem> unSavedMetadata = latestDocumentTypeVersion.getMetadata();
            if (!MetadataItemCompareUtil.clidrenListChanged(savedMetadata, unSavedMetadata)) {
                // metaData list is not changed, don't save new DocumentTypeVersion (currently as new latestDocumentTypeVersion)
                documentTypeVersions.remove(latestDocumentTypeVersion);
                docType.restoreProp(DocumentAdminModel.Props.LATEST_VERSION); // don't overwrite latest version number
                return;
            }
            // someone might have saved meanwhile new version of the same docType
            DocumentType latestDocTypeInRepo = getDocumentType(docType.getNodeRef());
            versionNr = latestDocTypeInRepo.getLatestVersion() + 1;
        }
        String userId = AuthenticationUtil.getFullyAuthenticatedUser();
        DocumentTypeVersion docVer = docType.getLatestDocumentTypeVersion();
        docVer.setCreatorId(userId);
        docVer.setCreatorName(userService.getUserFullName(userId));
        docVer.setVersionNr(versionNr);
        docVer.setCreatedDateTime(new Date(AlfrescoTransactionSupport.getTransactionStartTime()));
    }

    private NodeRef getDocumentTypesRoot() {
        if (documentTypesRoot == null) {
            String xPath = DocumentAdminModel.Repo.DOCUMENT_TYPES_SPACE;
            documentTypesRoot = generalService.getNodeRef(xPath);
        }
        return documentTypesRoot;
    }

    private NodeRef getFieldDefinitionsRoot() {
        if (fieldDefinitionsRoot == null) {
            String xPath = DocumentAdminModel.Repo.FIELD_DEFINITIONS_SPACE;
            fieldDefinitionsRoot = generalService.getNodeRef(xPath);
        }
        return fieldDefinitionsRoot;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setBaseService(BaseService baseService) {
        this.baseService = baseService;
    }

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /** To break Circular dependency */
    private AdrService getAdrService() {
        return BeanHelper.getAdrService();
    }

}
