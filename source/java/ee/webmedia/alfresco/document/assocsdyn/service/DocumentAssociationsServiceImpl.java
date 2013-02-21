package ee.webmedia.alfresco.document.assocsdyn.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getClassificatorService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.DOC_NAME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_DATE_TIME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.base.BaseService.Effort;
import ee.webmedia.alfresco.casefile.log.service.CaseFileLogService;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.AssociationModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldMapping;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService.AssocType;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

public class DocumentAssociationsServiceImpl implements DocumentAssociationsService {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentAssociationsServiceImpl.class);

    /** QName order in this list is used for sorting associations in associations block */
    public static List<QName> ASSOCS_BETWEEN_DOC_LIST_UNIT_ITEMS = Arrays.asList(DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP,
            DocumentCommonModel.Assocs.DOCUMENT_REPLY, CaseModel.Associations.CASE_DOCUMENT, VolumeModel.Associations.VOLUME_DOCUMENT, CaseFileModel.Assocs.CASE_FILE_DOCUMENT,
            VolumeModel.Associations.VOLUME_CASE, CaseFileModel.Assocs.CASE_FILE_CASE, VolumeModel.Associations.VOLUME_VOLUME, CaseFileModel.Assocs.CASE_FILE_VOLUME,
            CaseFileModel.Assocs.CASE_FILE_CASE_FILE);

    private DocumentAdminService documentAdminService;
    private DocumentDynamicService documentDynamicService;
    private DocumentConfigService documentConfigService;
    private BaseService baseService;
    private NodeService nodeService;
    private DictionaryService dictionaryService;
    private WorkflowService workflowService;
    private LogService logService;
    private DocumentLogService documentLogService;
    private UserService userService;
    private PrivilegeService privilegeService;
    private CaseFileLogService caseFileLogService;

    @Override
    public List<AssociationModel> getAssocs(String documentTypeId, QName typeQNamePattern) {
        NodeRef docTypeRef = documentAdminService.getDocumentTypeRef(documentTypeId);
        return baseService.getChildren(docTypeRef, AssociationModel.class, typeQNamePattern, RegexQNamePattern.MATCH_ALL, Effort.DONT_INCLUDE_CHILDREN);
    }

    @Override
    public Pair<DocumentDynamic, AssociationModel> createAssociatedDocFromModel(NodeRef baseDocRef, NodeRef assocModelRef) {
        ChildAssociationRef primaryParent = nodeService.getPrimaryParent(assocModelRef);
        String newDocTypeId = primaryParent.getQName().getLocalName();
        QName replyOrFollowUp = primaryParent.getTypeQName();

        DocumentDynamic baseDoc = documentDynamicService.getDocument(baseDocRef);
        WmNode baseDocNode = baseDoc.getNode();

        Pair<DocumentDynamic, DocumentTypeVersion> newDocAndVer = documentDynamicService.createNewDocumentInDrafts(newDocTypeId);
        DocumentDynamic newDoc = newDocAndVer.getFirst();
        WmNode newDocNode = newDoc.getNode();

        String baseDocTypeId = baseDoc.getDocumentTypeId();
        Integer baseDocTypeVersionNr = baseDoc.getDocumentTypeVersionNr();
        Integer newDocTypeVersionNr = newDoc.getDocumentTypeVersionNr();

        Pair<DocumentType, DocumentTypeVersion> baseDocTypeAndVersion = documentAdminService.getDocumentTypeAndVersion(baseDocTypeId, baseDocTypeVersionNr);
        DocumentType baseDocType = baseDocTypeAndVersion.getFirst();

        DocTypeAssocType replyOrF = DocTypeAssocType.valueOf(replyOrFollowUp);
        AssociationModel assocModel = getAssocModel(assocModelRef, baseDocType, replyOrF);
        if (!newDocTypeId.equals(assocModel.getDocType())) {
            throw new IllegalArgumentException("docTypeId to be created should be stored in assocModel and in associationType between documentType and AssociationModel" +
                    ", but they are not equal!\n'" + assocModel.getDocType() + "' according to model, but\n'" + newDocTypeId + "' according to association");
        }
        if (!baseDocType.getNodeRef().equals(primaryParent.getParentRef())) {
            throw new IllegalArgumentException("baseDocType.getNodeRef!=primaryParent.getParentRef()! " +
                    "primaryParent.parentRef=" + primaryParent.getParentRef() + ", baseDocType.nodeRef=" + baseDocType.getNodeRef());
        }

        // FIXME: kas source prop def. on mitmeväärtuseline või kas property ise on mitmeväärtuseline - võib vist ainult viimase järgi kontrollida?
        Map<String, Pair<DynamicPropertyDefinition, Field>> baseDocPropDefinitions = documentConfigService.getPropertyDefinitions(baseDocNode);
        Map<String, Pair<DynamicPropertyDefinition, Field>> newDocPropDefinitions = documentConfigService.getPropertyDefinitions(newDocNode);

        for (FieldMapping fieldMapping : assocModel.getFieldMappings()) {
            String fromFieldId = fieldMapping.getFromField();
            String toFieldId = fieldMapping.getToField();
            Pair<DynamicPropertyDefinition, Field> baseDocPropDefAndField = baseDocPropDefinitions.get(fromFieldId);
            Pair<DynamicPropertyDefinition, Field> newDocPropDefAndField = newDocPropDefinitions.get(toFieldId);
            if (baseDocPropDefAndField == null || baseDocPropDefAndField.getSecond() == null) {
                LOG.warn("Creating assoc to " + newDocTypeId + ". Found mapping " + fromFieldId + "->" + toFieldId + " but " + baseDocTypeId + " ver " + baseDocTypeVersionNr
                        + " doesn't seem to have source field " + fromFieldId + ". Mapping: " + fieldMapping.getNodeRef());
                continue;
            }
            if (newDocPropDefAndField == null || newDocPropDefAndField.getSecond() == null) {
                LOG.warn("Creating assoc from " + baseDocTypeId + ". Found mapping " + fromFieldId + "->" + toFieldId + " but " + newDocTypeId + " ver " + newDocTypeVersionNr
                        + " doesn't seem to have target field " + toFieldId + ". Mapping: " + fieldMapping.getNodeRef());
                continue;
            }
            Field baseDocTypeField = baseDocPropDefAndField.getSecond();
            Field newDocTypeField = newDocPropDefAndField.getSecond();
            DynamicPropertyDefinition baseDocPropDef = baseDocPropDefAndField.getFirst();
            DynamicPropertyDefinition newDocPropDef = newDocPropDefAndField.getFirst();
            QName[] baseHierarchy = baseDocPropDef.getChildAssocTypeQNameHierarchy();
            QName[] newHierarchy = newDocPropDef.getChildAssocTypeQNameHierarchy();
            if (baseHierarchy == null) {
                baseHierarchy = new QName[] {};
            }
            if (newHierarchy == null) {
                newHierarchy = new QName[] {};
            }
            copyPropertyRecursively(0, baseHierarchy, newHierarchy, baseDocNode, newDocNode, baseDocTypeField, newDocTypeField, newDocPropDef, newDocAndVer.getSecond());
        }
        NodeRef baseDocumentRef = baseDocNode.getNodeRef();
        NodeRef newDocumentRef = newDocNode.getNodeRef();
        createAssoc(newDocumentRef, baseDocumentRef, replyOrF.getAssocBetweenDocs());
        // On first rendering of document metadata block, initial access restriction properties would be set from series data -- disable this
        newDoc.setDisableUpdateInitialAccessRestrictionProps(true);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Created " + replyOrF + " " + newDocTypeId + ": from " + baseDoc.getDocumentTypeId());
        }
        return Pair.newInstance(newDoc, assocModel);
    }

    private void copyPropertyRecursively(int i, QName[] baseHierarchy, QName[] newHierarchy, Node baseDocNode, Node newDocNode, Field baseDocTypeField, Field newDocTypeField,
            DynamicPropertyDefinition newDocPropDef, DocumentTypeVersion newDocTypeVer) {
        if (i >= baseHierarchy.length && i >= newHierarchy.length) {
            // direct
            copyPropertyValue(baseDocNode, newDocNode, baseDocTypeField, newDocTypeField, newDocPropDef);

        } else if (i < baseHierarchy.length && i < newHierarchy.length) {
            // recurse
            List<Node> baseChildNodes = baseDocNode.getAllChildAssociations(baseHierarchy[i]);
            if (baseChildNodes == null || baseChildNodes.isEmpty()) {
                return;
            }
            List<Node> newChildNodes = newDocNode.getAllChildAssociations(newHierarchy[i]);
            Assert.isTrue(newChildNodes != null && !newChildNodes.isEmpty()); // createNewDocument/createNewChild should have created one child node for each child assoc
            for (int j = 0; j < baseChildNodes.size(); j++) {
                if (j >= newChildNodes.size()) {
                    QName[] newChildHierarchy = (QName[]) ArrayUtils.subarray(newHierarchy, 0, i + 1);
                    documentDynamicService.createChildNodesHierarchyAndSetDefaultPropertyValues(newDocNode, newChildHierarchy, newDocTypeVer);
                    // create
                }
                copyPropertyRecursively(i + 1, baseHierarchy, newHierarchy, baseChildNodes.get(j), newChildNodes.get(j), baseDocTypeField, newDocTypeField, newDocPropDef,
                        newDocTypeVer);
            }

        } else if (i >= baseHierarchy.length) {
            Assert.isTrue(i < newHierarchy.length);
            List<Node> newChildNodes = newDocNode.getAllChildAssociations(newHierarchy[i]);
            Assert.isTrue(newChildNodes != null && !newChildNodes.isEmpty()); // createNewDocument/createNewChild should have created one child node for each child assoc
            copyPropertyRecursively(i + 1, baseHierarchy, newHierarchy, baseDocNode, newChildNodes.get(0), baseDocTypeField, newDocTypeField, newDocPropDef, newDocTypeVer);

        } else if (i >= newHierarchy.length) {
            Assert.isTrue(i < baseHierarchy.length);
            List<Node> baseChildNodes = baseDocNode.getAllChildAssociations(baseHierarchy[i]);
            if (baseChildNodes == null || baseChildNodes.isEmpty()) {
                return;
            }
            copyPropertyRecursively(i + 1, baseHierarchy, newHierarchy, baseChildNodes.get(0), newDocNode, baseDocTypeField, newDocTypeField, newDocPropDef, newDocTypeVer);

        } else {
            Assert.isTrue(false);
        }
    }

    private void copyPropertyValue(Node baseDocNode, Node newDocNode, Field baseDocTypeField, Field newDocTypeField, DynamicPropertyDefinition newDocPropDef) {
        Map<String, Object> docProps = baseDocNode.getProperties();
        Map<String, Object> followupProps = newDocNode.getProperties();
        FieldType baseDocFieldType = baseDocTypeField.getFieldTypeEnum();
        FieldType newDocFieldType = newDocTypeField.getFieldTypeEnum();

        QName fromField = baseDocTypeField.getQName();
        QName toField = newDocTypeField.getQName();
        if (!docProps.containsKey(fromField.toString())) {
            return; // if value is missing then don't overwrite with null
        }
        Object existingProp = docProps.get(fromField.toString());
        if (existingProp != null) {
            boolean toPropIsMultivalued = newDocPropDef.isMultiValued();
            if (existingProp instanceof Collection<?>) {
                Collection<?> existingPropCol = (Collection<?>) existingProp;
                boolean existingPropNotEmpty = !existingPropCol.isEmpty();
                if (!toPropIsMultivalued) {
                    // multivalued -> singlevalued
                    existingProp = existingPropNotEmpty ? existingPropCol.iterator().next() : null;
                } else if (existingPropNotEmpty) {
                    if (!toPropIsMultivalued) {
                        // only first value is used when target is not multiValued
                        existingProp = existingPropCol.iterator().next();
                    } else if (FieldType.LISTBOX.equals(newDocFieldType) && FieldType.LISTBOX.equals(baseDocFieldType)) {
                        existingProp = filterExistingClassificatorValues(newDocTypeField, existingPropCol);
                    }
                }
            } else {
                // existing prop is singleValued
                if (toPropIsMultivalued) {
                    // singlevalued -> multivalued
                    existingProp = new ArrayList<Object>(Collections.singleton(existingProp));
                } else {
                    // both singleValued
                    if (FieldType.COMBOBOX.equals(newDocFieldType) && FieldType.COMBOBOX.equals(baseDocFieldType)) {
                        // value should be copied only if classificator of toField has that value
                        List<Object> classificatorValue = filterExistingClassificatorValues(newDocTypeField, Arrays.asList(existingProp));
                        existingProp = classificatorValue.isEmpty() ? null : classificatorValue.get(0);
                    }
                }
            }
        }
        followupProps.put(toField.toString(), existingProp);
        // copy special properties related to owner and signer
        if (DocumentDynamicModel.Props.OWNER_NAME.equals(fromField) && DocumentDynamicModel.Props.OWNER_NAME.equals(toField)) {
            QName prop = DocumentDynamicModel.Props.OWNER_ID;
            Object existingOwnerId = docProps.get(prop);
            followupProps.put(prop.toString(), existingOwnerId);
        } else if (DocumentDynamicModel.Props.SIGNER_NAME.equals(fromField) && DocumentDynamicModel.Props.SIGNER_NAME.equals(toField)) {
            QName prop = DocumentDynamicModel.Props.SIGNER_ID;
            Object existingOwnerId = docProps.get(prop);
            followupProps.put(prop.toString(), existingOwnerId);
        }
    }

    private List<Object> filterExistingClassificatorValues(Field newDocTypeField, Collection<?> col) {
        String classificator = newDocTypeField.getClassificator();
        if (classificator == null && newDocTypeField.isSystematic()) {
            return new ArrayList<Object>(col); // some systematic fields don't have classificator - values are dynamically generated
        }
        List<ClassificatorValue> classificatorValues = getClassificatorService().getActiveClassificatorValues(getClassificatorService().getClassificatorByName(classificator));
        Set<String> classificatorValueNames = new HashSet<String>(classificatorValues.size());
        for (ClassificatorValue classificatorValue : classificatorValues) {
            classificatorValueNames.add(classificatorValue.getValueName());
        }
        ArrayList<Object> newProp = new ArrayList<Object>(col.size());
        for (Object existingPropPart : col) {
            if (classificatorValueNames.contains(existingPropPart) || classificatorValueNames.contains(existingPropPart.toString())) {
                newProp.add(existingPropPart);
            }
        }
        return newProp;
    }

    private AssociationModel getAssocModel(NodeRef assocModelRef, DocumentType baseDocType, DocTypeAssocType replyOrF) {
        for (AssociationModel associationModel : (List<? extends AssociationModel>) baseDocType.getAssociationModels(replyOrF)) {
            if (associationModel.getNodeRef().equals(assocModelRef)) {
                baseService.loadChildren(associationModel, null); // load all children of the association
                return associationModel;
            }
        }
        throw new IllegalArgumentException("didn't find associationModel by nodeRef=" + assocModelRef);
    }

    @Override
    /** Add association from new to original doc */
    public void createAssoc(final NodeRef sourceNodeRef, final NodeRef targetNodeRef, QName assocQName) {
        if (getDocLockService().getLockStatus(sourceNodeRef) == LockStatus.LOCKED) {// lock owned by other user
            throw new NodeLockedException(sourceNodeRef);
        }
        if (getDocLockService().getLockStatus(targetNodeRef) == LockStatus.LOCKED) {// lock owned by other user
            throw new NodeLockedException(targetNodeRef);
        }
        nodeService.createAssociation(sourceNodeRef, targetNodeRef, assocQName);
        updateModifiedDateTime(sourceNodeRef, targetNodeRef);
        createLog(sourceNodeRef, targetNodeRef, false);
    }

    @Override
    public boolean createWorkflowAssoc(NodeRef docRef, NodeRef workflowRef, boolean updateMainDoc, boolean setOwnerProps) {
        createAssoc(docRef, workflowRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
        logDocumentWorkflowAssocAction(docRef, workflowRef, "applog_compoundWorkflow_document_added", "applog_document_compoundWorkflow_added",
                "document_log_compoundWorkflow_added");
        setIndependentWorkflowDocPermissions(docRef, workflowRef, setOwnerProps);
        workflowService.updateDocumentCompWorkflowSearchProps(docRef);
        if (updateMainDoc && workflowService.getCompoundWorkflowDocumentCount(workflowRef) == 1) {
            workflowService.updateMainDocument(workflowRef, docRef);
            return true;
        }
        return false;
    }

    private void setIndependentWorkflowDocPermissions(NodeRef docRef, NodeRef workflowRef, boolean setOwnerProps) {
        CompoundWorkflow compoundWorkflow = workflowService.getCompoundWorkflow(workflowRef);
        Set<String> defaultPrivileges = WorkflowUtil.getIndependentWorkflowDefaultDocPermissions();
        Map<String, Set<String>> userPrivileges = new HashMap<String, Set<String>>();
        boolean isFirstConfirmationTask = true;
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                String ownerId = task.getOwnerId();
                if (StringUtils.isBlank(ownerId)) {
                    continue;
                }
                if (!userPrivileges.containsKey(ownerId)) {
                    userPrivileges.put(ownerId, new HashSet<String>());
                }
                Set<String> userPriv = userPrivileges.get(ownerId);
                if (task.isStatus(Status.IN_PROGRESS, Status.FINISHED, Status.STOPPED, Status.UNFINISHED)) {
                    userPriv.addAll(defaultPrivileges);
                }
                if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
                    userPriv.add(DocumentCommonModel.Privileges.EDIT_DOCUMENT);
                }
                if (isFirstConfirmationTask && task.isType(WorkflowSpecificModel.Types.CONFIRMATION_TASK)) {
                    userPriv.add(DocumentCommonModel.Privileges.EDIT_DOCUMENT);
                    isFirstConfirmationTask = false;
                }
            }
        }
        for (Map.Entry<String, Set<String>> entry : userPrivileges.entrySet()) {
            Set<String> privilegesToAdd = entry.getValue();
            if (privilegesToAdd != null && !privilegesToAdd.isEmpty()) {
                privilegeService.setPermissions(docRef, entry.getKey(), privilegesToAdd);
            }
        }
        if (setOwnerProps) {
            Map<String, Object> documentProps = new HashMap<String, Object>();
            documentDynamicService.setOwnerFromActiveResponsibleTask(compoundWorkflow, docRef, documentProps);
            nodeService.addProperties(docRef, RepoUtil.toQNameProperties(documentProps));
        }
    }

    @Override
    public boolean isAddCompoundWorkflowAssoc(NodeRef baseDocumentRef, String associatiedDocTypeId, QName documentAssocType) {
        if (associatiedDocTypeId == null) {
            return false;
        }
        String baseDocTypeId = documentDynamicService.getDocumentType(baseDocumentRef);
        List<? extends AssociationModel> associationModels = getAssocs(baseDocTypeId, documentAssocType);
        for (AssociationModel associationModel : associationModels) {
            if (associatiedDocTypeId.equals(associationModel.getDocType())) {
                return Boolean.TRUE.equals(associationModel.getAssociateWithSourceDocument());
            }
        }
        return false;
    }

    @Override
    public void logDocumentWorkflowAssocRemove(final NodeRef docRef, final NodeRef workflowRef) {
        logDocumentWorkflowAssocAction(docRef, workflowRef, "applog_compoundWorkflow_document_removed", "applog_document_compoundWorkflow_removed",
                "document_log_compoundWorkflow_removed");
    }

    private void logDocumentWorkflowAssocAction(NodeRef docRef, NodeRef workflowRef, String appWorkflowLogMsgKey, String appDocumentLogMsgKey, String documentLogMsgKey) {
        Map<QName, Serializable> docProps = nodeService.getProperties(docRef);
        String typeName = documentAdminService.getDocumentTypeName((String) docProps.get(DocumentAdminModel.Props.OBJECT_TYPE_ID));
        logService.addLogEntry(LogEntry.create(LogObject.COMPOUND_WORKFLOW, userService, workflowRef, appWorkflowLogMsgKey, typeName,
                docProps.get(DocumentCommonModel.Props.DOC_NAME)));
        String compoundWorkflowTitle = (String) nodeService.getProperty(workflowRef, WorkflowCommonModel.Props.TITLE);
        logService.addLogEntry(LogEntry.create(LogObject.DOCUMENT, userService, docRef, appDocumentLogMsgKey, typeName,
                compoundWorkflowTitle));
        documentLogService.addDocumentLog(docRef, MessageUtil.getMessage(documentLogMsgKey, compoundWorkflowTitle));
    }

    @Override
    public void deleteWorkflowAssoc(final NodeRef docRef, final NodeRef workflowRef) {
        deleteAssoc(docRef, workflowRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
        logDocumentWorkflowAssocRemove(docRef, workflowRef);
        Map<QName, Serializable> compoundWorkflowProps = nodeService.getProperties(workflowRef);
        NodeRef mainDocumentRef = (NodeRef) compoundWorkflowProps.get(WorkflowCommonModel.Props.MAIN_DOCUMENT);
        if (docRef.equals(mainDocumentRef)) {
            mainDocumentRef = null;
        }
        List<NodeRef> documentsToSign = (List<NodeRef>) compoundWorkflowProps.get(WorkflowCommonModel.Props.DOCUMENTS_TO_SIGN);
        if (documentsToSign != null) {
            documentsToSign.remove(docRef);
        }
        workflowService.updateIndependentWorkflowDocumentData(workflowRef, mainDocumentRef, documentsToSign);
        workflowService.updateDocumentCompWorkflowSearchProps(docRef);
    }

    @Override
    public void deleteAssoc(final NodeRef sourceNodeRef, final NodeRef targetNodeRef, QName assocQName) {
        if (assocQName == null) {
            assocQName = DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT;
        }
        LOG.debug("Deleting " + assocQName + " association from document " + sourceNodeRef + " that points to " + targetNodeRef);
        nodeService.removeAssociation(sourceNodeRef, targetNodeRef, assocQName);
        updateModifiedDateTime(sourceNodeRef, targetNodeRef);
        createLog(sourceNodeRef, targetNodeRef, true);
    }

    private void createLog(NodeRef sourceNodeRef, NodeRef targetNodeRef, boolean delete) {
        if (CaseFileModel.Types.CASE_FILE.equals(nodeService.getType(sourceNodeRef))) {
            caseFileLogService.addAssociationLog(sourceNodeRef, targetNodeRef, delete);
        }
        if (DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(targetNodeRef))) {
            documentLogService.addAssociationLog(targetNodeRef, sourceNodeRef, delete);
        }
    }

    /*
     * If associations between two documents are added/deleted, then update modified time of both documents.
     * Because we need ADR to detect changes based on modified time.
     */
    @Override
    public void updateModifiedDateTime(final NodeRef firstDocNodeRef, final NodeRef secondDocNodeRef) {
        if (dictionaryService.isSubClass(nodeService.getType(firstDocNodeRef), DocumentCommonModel.Types.DOCUMENT)
                && dictionaryService.isSubClass(nodeService.getType(secondDocNodeRef), DocumentCommonModel.Types.DOCUMENT)) {
            AuthenticationUtil.runAs(new RunAsWork<Void>() {
                @Override
                public Void doWork() throws Exception {
                    nodeService.setProperty(firstDocNodeRef, ContentModel.PROP_MODIFIED, null);
                    nodeService.setProperty(secondDocNodeRef, ContentModel.PROP_MODIFIED, null);
                    return null;
                }
            }, AuthenticationUtil.getSystemUserName());
        }
    }

    @Override
    public List<DocAssocInfo> getAssocInfos(Node docNode) {
        final ArrayList<DocAssocInfo> assocInfos = new ArrayList<DocAssocInfo>();
        final List<AssociationRef> targetAssocs = nodeService.getTargetAssocs(docNode.getNodeRef(), RegexQNamePattern.MATCH_ALL);
        for (AssociationRef targetAssocRef : targetAssocs) {
            LOG.debug("targetAssocRef=" + targetAssocRef.getTypeQName());
            addDocAssocInfo(targetAssocRef, false, assocInfos);
        }
        final List<AssociationRef> sourceAssocs = nodeService.getSourceAssocs(docNode.getNodeRef(), RegexQNamePattern.MATCH_ALL);
        for (AssociationRef srcAssocRef : sourceAssocs) {
            LOG.debug("srcAssocRef=" + srcAssocRef.getTypeQName());
            addDocAssocInfo(srcAssocRef, true, assocInfos);
        }
        final Map<String, Map<String, AssociationRef>> addedAssocs = docNode.getAddedAssociations();
        for (Map<String, AssociationRef> typedAssoc : addedAssocs.values()) {
            for (AssociationRef addedAssoc : typedAssoc.values()) {
                LOG.debug("addedAssoc=" + addedAssoc.getTypeQName());
                addDocAssocInfo(addedAssoc, false, assocInfos);
            }
        }
        return assocInfos;
    }

    @Override
    public List<NodeRef> getDocumentIndependentWorkflowAssocs(NodeRef docRef) {
        List<AssociationRef> workflowAssocs = nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
        List<NodeRef> workflowRefs = new ArrayList<NodeRef>();
        for (AssociationRef assocRef : workflowAssocs) {
            workflowRefs.add(assocRef.getTargetRef());
        }
        return workflowRefs;
    }

    @Override
    public boolean isBaseOrReplyOrFollowUpDocument(NodeRef docRef, Map<String, Map<String, AssociationRef>> addedAssociations) {
        if (addedAssociations != null) {
            Map<String, AssociationRef> addedAssocs = addedAssociations.get(DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.toString());
            if (addedAssocs != null && hasValidAssocs(addedAssocs.values(), docRef)) {
                return true;
            }
            addedAssocs = addedAssociations.get(DocumentCommonModel.Assocs.DOCUMENT_REPLY.toString());
            if (addedAssocs != null && hasValidAssocs(addedAssocs.values(), docRef)) {
                return true;
            }
        }
        return hasValidAssocs(nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY), docRef)
                || hasValidAssocs(nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP), docRef)
                || hasValidAssocs(nodeService.getSourceAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY), docRef)
                || hasValidAssocs(nodeService.getSourceAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP), docRef);
    }

    private boolean hasValidAssocs(Iterable<AssociationRef> addedAssocs, NodeRef currentDocRef) {
        for (AssociationRef assocRef : addedAssocs) {
            NodeRef sourceRef = assocRef.getSourceRef();
            if (isValidAssocRef(currentDocRef, sourceRef)) {
                return true;
            }
            NodeRef targetRef = assocRef.getTargetRef();
            if (isValidAssocRef(currentDocRef, targetRef)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidAssocRef(NodeRef currentDocRef, NodeRef sourceRef) {
        return !sourceRef.equals(currentDocRef) && nodeService.hasAspect(sourceRef, DocumentCommonModel.Aspects.SEARCHABLE);
    }

    private void addDocAssocInfo(AssociationRef assocRef, boolean isSourceAssoc, ArrayList<DocAssocInfo> assocInfos) {
        DocAssocInfo assocInf = getDocListUnitAssocInfo(assocRef, isSourceAssoc);
        if (assocInf != null) {
            assocInfos.add(assocInf);
        }
    }

    @Override
    public DocAssocInfo getDocListUnitAssocInfo(AssociationRef assocRef, boolean isSourceAssoc) {
        return getDocListUnitAssocInfo(assocRef, isSourceAssoc, true);
    }

    @Override
    public DocAssocInfo getDocListUnitAssocInfo(AssociationRef assocRef, boolean isSourceAssoc, boolean skipNotSearchable) {
        DocAssocInfo assocInf = new DocAssocInfo();
        QName assocTypeQName = assocRef.getTypeQName();
        boolean isDocumentWorkflowAssociation = DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT.equals(assocTypeQName);
        if (skipNotSearchable && skipAssoc(isSourceAssoc, assocTypeQName, isDocumentWorkflowAssociation)) {
            return null;
        }
        NodeRef objectRef = isSourceAssoc ? assocRef.getSourceRef() : assocRef.getTargetRef();
        if (skipNotSearchable && isNotSearchableDocument(objectRef)) {
            LOG.debug("not searchable: " + assocRef);
            return null;
        }
        assocInf.setSource(isSourceAssoc);
        assocInf.setOtherNodeRef(objectRef);
        assocInf.setThisNodeRef(isSourceAssoc ? assocRef.getTargetRef() : assocRef.getSourceRef());
        assocInf.setAssocTypeQName(assocTypeQName);
        getAssocInfoType(isSourceAssoc, assocInf, assocTypeQName);
        // default condition for allowing delete
        assocInf.setAllowDelete(AssocType.DEFAULT == assocInf.getAssocType());
        // retrieve data for association other end object
        QName objectType = nodeService.getType(objectRef);
        if (WorkflowCommonModel.Types.COMPOUND_WORKFLOW.equals(objectType)) {
            assocInf.setType(MessageUtil.getMessage("compoundWorklfow_assoc_object_type"));
            assocInf.setTitle((String) nodeService.getProperty(objectRef, WorkflowCommonModel.Props.TITLE));
        } else if (DocumentCommonModel.Types.DOCUMENT.equals(objectType)) {
            final Node otherDocNode = new Node(objectRef);
            final Map<String, Object> otherDocProps = otherDocNode.getProperties();
            assocInf.setTitle((String) otherDocProps.get(DOC_NAME));
            assocInf.setRegNumber((String) otherDocProps.get(REG_NUMBER));
            assocInf.setRegDateTime((Date) otherDocProps.get(REG_DATE_TIME));
            Pair<String, String> documentTypeNameAndId = getDocumentAdminService().getDocumentTypeNameAndId(otherDocNode);
            assocInf.setType(documentTypeNameAndId.getFirst());
            assocInf.setTypeId(documentTypeNameAndId.getSecond());
        } else if (CaseModel.Types.CASE.equals(objectType)) {
            assocInf.setCaseNodeRef(objectRef);
            assocInf.setType(MessageUtil.getMessage("case"));
            assocInf.setTitle((String) nodeService.getProperty(objectRef, CaseModel.Props.TITLE));
        } else {
            boolean isCaseFile = CaseFileModel.Types.CASE_FILE.equals(objectType);
            if (isCaseFile || VolumeModel.Types.VOLUME.equals(objectType)) {
                final Node volumeNode = new Node(objectRef);
                assocInf.setVolumeNodeRef(objectRef);
                Map<String, Object> volumeProps = volumeNode.getProperties();
                assocInf.setTitle(TextUtil.joinNonBlankStrings(Arrays.asList((String) volumeProps.get(VolumeModel.Props.VOLUME_MARK),
                        (String) volumeProps.get(DocumentDynamicModel.Props.TITLE)), " "));
                if (isCaseFile) {
                    assocInf.setType(BeanHelper.getDocumentAdminService().getCaseFileTypeName(volumeNode));
                    assocInf.setCaseFileVolume(true);
                } else {
                    assocInf.setType(MessageUtil.getMessage(VolumeType.valueOf((String) volumeNode.getProperties().get(VolumeModel.Props.VOLUME_TYPE))));
                }
            }
        }
        return assocInf;
    }

    private void getAssocInfoType(boolean isSourceAssoc, DocAssocInfo assocInf, QName assocTypeQName) {
        if (DocumentCommonModel.Assocs.DOCUMENT_REPLY.equals(assocTypeQName) || DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.equals(assocTypeQName)) {
            if (isSourceAssoc) {
                if (DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.equals(assocTypeQName)) {
                    assocInf.setAssocType(AssocType.FOLLOWUP);
                } else if (DocumentCommonModel.Assocs.DOCUMENT_REPLY.equals(assocTypeQName)) {
                    assocInf.setAssocType(AssocType.REPLY);
                }
            } else {
                assocInf.setAssocType(AssocType.INITIAL);
            }
        } else if (DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT.equals(assocTypeQName)) {
            assocInf.setAssocType(AssocType.WORKFLOW);
        } else {
            assocInf.setAssocType(AssocType.DEFAULT);
        }
    }

    private boolean skipAssoc(boolean isSourceAssoc, QName assocTypeQName, boolean isDocumentWorkflowAssociation) {
        // Compound workflow association block doesn't use this method,
        // so retrieving workflow association info is not implemented for "from" associations
        return !ASSOCS_BETWEEN_DOC_LIST_UNIT_ITEMS.contains(assocTypeQName)
                && (!isDocumentWorkflowAssociation
                || (isDocumentWorkflowAssociation
                && (isSourceAssoc || !workflowService.isIndependentWorkflowEnabled() || !workflowService.isWorkflowTitleEnabled())));
    }

    private boolean isNotSearchableDocument(final NodeRef targetRef) {
        QName targetType = nodeService.getType(targetRef);
        return (DocumentCommonModel.Types.DOCUMENT.equals(targetType) || CaseFileModel.Types.CASE_FILE.equals(targetType))
                && !nodeService.hasAspect(targetRef, DocumentCommonModel.Aspects.SEARCHABLE);
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
    }

    public void setDocumentConfigService(DocumentConfigService documentConfigService) {
        this.documentConfigService = documentConfigService;
    }

    public void setBaseService(BaseService baseService) {
        this.baseService = baseService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    public void setPrivilegeService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    public void setCaseFileLogService(CaseFileLogService caseFileLogService) {
        this.caseFileLogService = caseFileLogService;
    }

}
