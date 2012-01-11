package ee.webmedia.alfresco.document.assocsdyn.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.DOC_NAME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_DATE_TIME;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.base.BaseService.Effort;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
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
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService.AssocType;
import ee.webmedia.alfresco.utils.MessageUtil;

public class DocumentAssociationsServiceImpl implements DocumentAssociationsService {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentAssociationsServiceImpl.class);

    private DocumentAdminService documentAdminService;
    private DocumentDynamicService documentDynamicService;
    private DocumentConfigService documentConfigService;
    private BaseService baseService;
    private NodeService nodeService;
    private DictionaryService dictionaryService;

    @Override
    public List<AssociationModel> getAssocs(String documentTypeId, QName typeQNamePattern) {
        NodeRef docTypeRef = documentAdminService.getDocumentTypeRef(documentTypeId);
        return baseService.getChildren(docTypeRef, AssociationModel.class, typeQNamePattern, RegexQNamePattern.MATCH_ALL, Effort.DONT_INCLUDE_CHILDREN);
    }

    @Override
    public DocumentDynamic createAssociatedDocFromModel(NodeRef baseDocRef, NodeRef assocModelRef) {
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
        createAssoc(newDocNode.getNodeRef(), baseDocNode.getNodeRef(), replyOrF.getAssocBetweenDocs());

        // On first rendering of document metadata block, initial access restriction properties would be set from series data -- disable this
        newDoc.setDisableUpdateInitialAccessRestrictionProps(true);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Created " + replyOrF + " " + newDocTypeId + ": from " + baseDoc.getDocumentTypeId());
        }
        return newDoc;
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
        List<ClassificatorValue> classificatorValues = BeanHelper.getClassificatorService().getAllClassificatorValues(classificator);
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
        nodeService.createAssociation(sourceNodeRef, targetNodeRef, assocQName);
        updateModifiedDateTime(sourceNodeRef, targetNodeRef);
    }

    /*
     * NOTE: association with case is defined differently
     */
    @Override
    public void deleteAssoc(final NodeRef sourceNodeRef, final NodeRef targetNodeRef, QName assocQName) {
        if (assocQName == null) {
            assocQName = DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT;
        }
        LOG.debug("Deleting " + assocQName + " association from document " + sourceNodeRef + " that points to " + targetNodeRef);
        if (assocQName.equals(CaseModel.Associations.CASE_DOCUMENT)) {
            nodeService.removeAssociation(targetNodeRef, sourceNodeRef, assocQName);
        } else {
            nodeService.removeAssociation(sourceNodeRef, targetNodeRef, assocQName);
        }
        updateModifiedDateTime(sourceNodeRef, targetNodeRef);
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
        DocAssocInfo assocInf = getDocAssocInfo(assocRef, isSourceAssoc);
        if (assocInf != null) {
            assocInfos.add(assocInf);
        }
    }

    @Override
    public DocAssocInfo getDocAssocInfo(AssociationRef assocRef, boolean isSourceAssoc) {
        DocAssocInfo assocInf = new DocAssocInfo();
        if (isSourceAssoc) {
            final NodeRef sourceRef = assocRef.getSourceRef();
            assocInf.setNodeRef(sourceRef);
            if (!nodeService.hasAspect(sourceRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
                if (CaseModel.Associations.CASE_DOCUMENT.equals(assocRef.getTypeQName())) {
                    assocInf.setCaseNodeRef(sourceRef);
                    assocInf.setAssocType(AssocType.DEFAULT);
                    assocInf.setType(MessageUtil.getMessage(VolumeType.CASE_FILE));
                    assocInf.setTitle((String) nodeService.getProperty(sourceRef, CaseModel.Props.TITLE));
                } else {
                    LOG.debug("not searchable: " + assocRef);
                    return null;
                }
            }
            if (DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.equals(assocRef.getTypeQName())) {
                assocInf.setAssocType(AssocType.FOLLOWUP);
            } else if (DocumentCommonModel.Assocs.DOCUMENT_REPLY.equals(assocRef.getTypeQName())) {
                assocInf.setAssocType(AssocType.REPLY);
            } else if (DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT.equals(assocRef.getTypeQName())) {
                assocInf.setAssocType(AssocType.DEFAULT);
            } else if (assocInf.getAssocType() == null) {
                throw new RuntimeException("Unexpected document type: " + assocRef.getTypeQName());
            }
            if (!assocInf.isCase()) {// document association, not case
                final Node otherDocNode = new Node(sourceRef);
                assocInf.setTitle((String) nodeService.getProperty(sourceRef, DOC_NAME));
                assocInf.setType(getDocumentAdminService().getDocumentTypeName(otherDocNode));
                assocInf.setRegNumber((String) nodeService.getProperty(sourceRef, REG_NUMBER));
                assocInf.setRegDateTime((Date) nodeService.getProperty(sourceRef, REG_DATE_TIME));
            }
        } else {
            final NodeRef targetRef = assocRef.getTargetRef();
            if (!nodeService.hasAspect(targetRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
                return null;
            }
            if (DocumentCommonModel.Assocs.DOCUMENT_REPLY.equals(assocRef.getTypeQName())//
                    || DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.equals(assocRef.getTypeQName())) {
                assocInf.setAssocType(AssocType.INITIAL);
            } else if (DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT.equals(assocRef.getTypeQName())) {
                assocInf.setAssocType(AssocType.DEFAULT);
            }
            final Node otherDocNode = new Node(targetRef);
            final Map<String, Object> otherDocProps = otherDocNode.getProperties();
            assocInf.setTitle((String) otherDocProps.get(DOC_NAME));
            assocInf.setRegNumber((String) otherDocProps.get(REG_NUMBER));
            assocInf.setRegDateTime((Date) otherDocProps.get(REG_DATE_TIME));
            assocInf.setType(getDocumentAdminService().getDocumentTypeName(otherDocNode));
            assocInf.setNodeRef(assocRef.getTargetRef());
        }
        assocInf.setSource(isSourceAssoc);
        return assocInf;
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

}
