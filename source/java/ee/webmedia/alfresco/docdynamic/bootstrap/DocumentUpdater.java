package ee.webmedia.alfresco.docdynamic.bootstrap;

import static ee.webmedia.alfresco.classificator.enums.DocumentStatus.FINISHED;
import static ee.webmedia.alfresco.classificator.enums.DocumentStatus.WORKING;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.bootstrap.StructUnitFieldTypeUpdater;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.web.DocAdminUtil;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docconfig.service.PropDefCacheKey;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.bootstrap.FileEncodingUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.privilege.model.PrivilegeModel;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * This updater does multiple things with document:
 * 1) Update properties of type STRUCT_UNIT to multiValued properties (String -> List<String>)
 * 2) Set searchableHasAllFinishedCompoundWorkflows property value
 * * 4) Update searchableFileContents if necessary (because of {@link FileEncodingUpdater})
 * 5) Always call addProperties, to trigger re-indexing of document (ADMLuceneIndexerImpl writes special fields VALUES and xxx)
 */
public class DocumentUpdater extends AbstractNodeUpdater {

    // START: old aspects
    public static final QName EMAIL_DATE_TIME = QName.createQName(DocumentCommonModel.DOCCOM_URI, "emailDateTime");
    // END: old aspects

    private WorkflowService workflowService;
    private DocumentService documentService;
    private FileEncodingUpdater fileEncodingUpdater;

    private boolean regNumberCollectingNeeded;
    private final Map<NodeRef /* documentParentRef */, List<String /* regNumber */>> documentRegNumbers = new HashMap<>();
    private Set<NodeRef> drafts;
    private Set<NodeRef> documentsImportedFromImap;
    Map<PropDefCacheKey, Map<String, Pair<DynamicPropertyDefinition, Field>>> docTypePropertyDefintions = new HashMap<>();

    @Override
    protected void executeUpdater() throws Exception {
        NodeRef draftsRoot = BeanHelper.getConstantNodeRefsBean().getDraftsRoot();
        drafts = BeanHelper.getBulkLoadNodeService().loadChildRefs(draftsRoot, null, null, DocumentCommonModel.Types.DOCUMENT);
        // FIXME add parameter or get correct xpath value
        regNumberCollectingNeeded = false; // BeanHelper.getGeneralService().getNodeRef(
        // "/sys:system-registry/module:modules/module:simdhs/module:components/module:documentRegNumbersUpdater2") == null;
        //
        log.info("regNumberCollectingNeeded = " + regNumberCollectingNeeded);
        documentsImportedFromImap = new HashSet<>(BeanHelper.getLogService().getDocumentsWithImapImportLog());
        super.executeUpdater();
        drafts.clear();
        documentsImportedFromImap.clear();
        docTypePropertyDefintions.clear();
    }

    @Override
    protected boolean usePreviousState() {
        return false;
    }

    public Map<NodeRef, List<String>> getDocumentRegNumbers() {
        return documentRegNumbers;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        // Need to go through all documents
        String query = SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "docRef", "searchableHasAllFinishedCompoundWorkflows", "updatedStructUnitProps", "removePrivilegeMappings" };
    }

    @Override
    protected String[] updateNode(NodeRef docRef) throws Exception {
        if (drafts.contains(docRef)) {
            nodeService.deleteNode(docRef);
            return new String[] { "isDraftAndDeleted" };
        }
        QName type = nodeService.getType(docRef);
        if (!DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            return new String[] { "isNotDocument", type.toString() };
        }
        if (nodeService.hasAspect(docRef, EMAIL_DATE_TIME)) {
            return new String[] { "hasEmailDateTimeAspectAndIgnored" };
        }

        Map<QName, Serializable> origProps = nodeService.getProperties(docRef);
        Map<QName, Serializable> updatedProps = new HashMap<>();

        if (regNumberCollectingNeeded) {
            addParentRegNumber(docRef, (String) origProps.get(DocumentCommonModel.Props.REG_NUMBER));
        }

        String accessRestriction = (String) origProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
        if (AccessRestriction.OPEN.equals(accessRestriction) || AccessRestriction.INTERNAL.equals(accessRestriction)) {
            updatedProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON, null);
            updatedProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, null);
            updatedProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE, null);
            updatedProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC, null);
        }

        String hasAllFinishedCompoundWorkflowsUpdaterLog = updateHasAllFinishedCompoundWorkflows(docRef, origProps, updatedProps, workflowService);

        String structUnitPropertiesToMultivaluedUpdaterLog = updateStructUnitPropertiesToMultivalued(origProps, updatedProps);

        String updateMetadataInFilesUpdaterLog = updateMetadataInFiles(origProps, updatedProps);

        String fileContentsLog;
        if (fileEncodingUpdater.getDocumentsToUpdate().contains(docRef)) { // searchable aspect has been checked in fileEncodingUpdater
            updatedProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
            // Update modified time on document, so ADR would detect up changes
            updatedProps.put(ContentModel.PROP_MODIFIED, new Date(AlfrescoTransactionSupport.getTransactionStartTime()));
            updateFileContentsProp(docRef, updatedProps);
            fileContentsLog = "searchableFileContentsUpdated";
        } else {
            updatedProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
            fileContentsLog = "searchableFileContentsSkipped";
        }

        String removePrivilegeMappingsLog = removePrivilegeMappings(docRef, origProps, updatedProps);
        // Always update document node to trigger an update of document data in Lucene index.
        nodeService.addProperties(docRef, updatedProps);

        String updateImapDocProps = "";
        if (documentsImportedFromImap.contains(docRef)) {
            PropDefCacheKey propDefCacheKey = DocAdminUtil.getPropDefCacheKey(DocumentType.class, origProps);

            Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = docTypePropertyDefintions.get(propDefCacheKey);
            if (propertyDefinitions == null && !docTypePropertyDefintions.containsKey(propDefCacheKey)) {
                propertyDefinitions = BeanHelper.getDocumentConfigService().getPropertyDefinitions(propDefCacheKey);
                docTypePropertyDefintions.put(propDefCacheKey, propertyDefinitions);
            }
            if (propertyDefinitions != null) {
                Set<String> typeVersionFields = propertyDefinitions.keySet();
                List<QName> propsToNull = new ArrayList<>();
                for (Map.Entry<QName, Serializable> entry : origProps.entrySet()) {
                    QName propQName = entry.getKey();
                    String localName = propQName.getLocalName();
                    if (DocumentDynamicModel.URI.equals(propQName.getNamespaceURI())
                            && (!typeVersionFields.contains(localName) || isChildNodeProperty(propertyDefinitions, localName))
                            && entry.getValue() != null) {
                        nodeService.removeProperty(docRef, propQName);
                        propsToNull.add(propQName);
                    }
                }
                updateImapDocProps = "Removed properties: " + StringUtils.join(propsToNull, ", ");
            }
        }

        return new String[] { hasAllFinishedCompoundWorkflowsUpdaterLog, structUnitPropertiesToMultivaluedUpdaterLog, removePrivilegeMappingsLog,
                fileContentsLog, updateMetadataInFilesUpdaterLog, updateImapDocProps };
    }

    private boolean isChildNodeProperty(Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions, String localName) {
        Pair<DynamicPropertyDefinition, Field> propDefAndField = propertyDefinitions.get(localName);
        if (propDefAndField == null) {
            return false;
        }
        QName[] childAssocTypeQNAmeHierarchy = propDefAndField.getFirst().getChildAssocTypeQNameHierarchy();
        return childAssocTypeQNAmeHierarchy != null && childAssocTypeQNAmeHierarchy.length > 0;
    }

    public void updateFileContentsProp(NodeRef docRef, Map<QName, Serializable> updatedProps) {
        updatedProps.put(DocumentCommonModel.Props.FILE_CONTENTS, documentService.getSearchableFileContents(docRef));
    }

    public void addParentRegNumber(NodeRef docRef, String regNumber) {
        if (StringUtils.isNotBlank(regNumber)) {
            NodeRef parentRef = nodeService.getPrimaryParent(docRef).getParentRef();
            List<String> regNumbers = documentRegNumbers.get(parentRef);
            if (regNumbers == null) {
                regNumbers = new ArrayList<String>();
                documentRegNumbers.put(parentRef, regNumbers);
            }
            regNumbers.add(regNumber);
        }
    }

    public String updateMetadataInFiles(Map<QName, Serializable> origProps, Map<QName, Serializable> updatedProps) {
        String status = (String) origProps.get(DocumentCommonModel.Props.DOC_STATUS);
        if (!WORKING.getValueName().equals(status) && !FINISHED.getValueName().equals(status)) {
            return "updateMetadatainFilesSkipped";
        }
        Boolean updateMetadataInFiles = WORKING.getValueName().equals(status);
        updatedProps.put(DocumentCommonModel.Props.UPDATE_METADATA_IN_FILES, updateMetadataInFiles);
        return status + "->" + updateMetadataInFiles.toString();
    }

    private String removePrivilegeMappings(NodeRef docRef, Map<QName, Serializable> props, Map<QName, Serializable> updatedProps) {
        if (props.get(PrivilegeModel.Props.USER) != null) {
            updatedProps.put(PrivilegeModel.Props.USER, null);
            updatedProps.put(PrivilegeModel.Props.GROUP, null);
            nodeService.removeAspect(docRef, PrivilegeModel.Aspects.USER_GROUP_MAPPING);
            return "removedUserGroupMappingAspectAndProps";
        }
        return "";
    }

    /**
     * XXX: This method is not needed when updating from 2.5 to 3.x
     *
     * @param docRef
     * @return
     */
    private String updateStructUnitPropertiesToMultivalued(Map<QName, Serializable> origProps, Map<QName, Serializable> updatedProps) {
        List<String> resultLog = new ArrayList<String>();
        for (Map.Entry<QName, Serializable> entry : origProps.entrySet()) {
            QName propQName = entry.getKey();
            if (DocumentDynamicModel.URI.equals(propQName.getNamespaceURI()) && propQName.getLocalName().contains(StructUnitFieldTypeUpdater.ORG_STRUCT_UNIT)) {
                Serializable currentValue = entry.getValue();
                if (!(currentValue instanceof List)) {
                    List<String> newValue = new ArrayList<>();
                    if (StringUtils.isNotBlank((String) currentValue)) {
                        newValue.add((String) currentValue);
                    }
                    updatedProps.put(propQName, (Serializable) newValue);
                    resultLog.add(propQName.toString());
                }
            }
        }
        return StringUtils.join(resultLog, ", ");
    }

    public static String updateHasAllFinishedCompoundWorkflows(NodeRef docRef, Map<QName, Serializable> origProps, Map<QName, Serializable> updatedProps,
            WorkflowService workflowService) {
        Serializable origValueReal = origProps.get(DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS);
        boolean origValue = Boolean.TRUE.equals(origValueReal);
        boolean newValue = workflowService.hasCompoundWorkflowsAndAllAreFinished(docRef);
        if (origValue != newValue) {
            updatedProps.put(DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS, newValue);
            return ObjectUtils.toString(origValueReal, "null") + ", " + newValue;
        }
        return "";
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setFileEncodingUpdater(FileEncodingUpdater fileEncodingUpdater) {
        this.fileEncodingUpdater = fileEncodingUpdater;
    }

    public FileEncodingUpdater getFileEncodingUpdater() {
        return fileEncodingUpdater;
    }

}
