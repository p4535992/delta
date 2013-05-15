package ee.webmedia.alfresco.docdynamic.bootstrap;

import static ee.webmedia.alfresco.classificator.enums.DocumentStatus.FINISHED;
import static ee.webmedia.alfresco.classificator.enums.DocumentStatus.WORKING;
import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.removePermission;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.bootstrap.StructUnitFieldTypeUpdater;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.bootstrap.FileEncodingUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.privilege.model.PrivilegeModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * This updater does multiple things with document:
 * 1) Update properties of type STRUCT_UNIT to multiValued properties (String -> List<String>)
 * 2) Set searchableHasAllFinishedCompoundWorkflows property value
 * 3) Remove old permissions from all documents and replace them with new ones if needed
 * 4) Update searchableFileContents if necessary (because of {@link FileEncodingUpdater})
 * 5) Always call addProperties, to trigger re-indexing of document (ADMLuceneIndexerImpl writes special fields VALUES and xxx)
 * 
 * @author Riina Tens - STRUCT_UNIT update
 * @author Alar Kvell - setting searchableHasAllFinishedCompoundWorkflows
 * @author Ats Uiboupin - replacing permissions
 */
public class DocumentUpdater extends AbstractNodeUpdater {

    // START: old permissions
    private static final String EDIT_DOCUMENT_META_DATA = "editDocumentMetaData";
    private static final String EDIT_DOCUMENT_FILES = "editDocumentFiles";
    private static final String DELETE_DOCUMENT_META_DATA = "deleteDocumentMetaData";
    private static final String DELETE_DOCUMENT_FILES = "deleteDocumentFiles";
    // END: old permissions

    // START: old aspects
    public static final QName EMAIL_DATE_TIME = QName.createQName(DocumentCommonModel.DOCCOM_URI, "emailDateTime");
    // END: old aspects

    private WorkflowService workflowService;
    private DocumentService documentService;
    private FileEncodingUpdater fileEncodingUpdater;
    private final Map<NodeRef /* documentParentRef */, List<String /* regNumber */>> documentRegNumbers = new HashMap<NodeRef, List<String>>();

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
        return new String[] { "docRef", "searchableHasAllFinishedCompoundWorkflows", "updatedStructUnitProps", "removed authorities by permission", "removePrivilegeMappings" };
    }

    @Override
    protected String[] updateNode(NodeRef docRef) throws Exception {
        QName type = nodeService.getType(docRef);
        if (!DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            return new String[] { "isNotDocument",
                    type.toPrefixString(serviceRegistry.getNamespaceService()) };
        }
        if (nodeService.hasAspect(docRef, EMAIL_DATE_TIME)) {
            ChildAssociationRef assoc = nodeService.getPrimaryParent(docRef);
            return new String[] { "hasEmailDateTimeAspectAndIgnored",
                    assoc.getTypeQName().toPrefixString(serviceRegistry.getNamespaceService()),
                    assoc.getQName().toPrefixString(serviceRegistry.getNamespaceService()),
                    type.toPrefixString(serviceRegistry.getNamespaceService()) };
        }
        ChildAssociationRef primaryParentAssoc = nodeService.getPrimaryParent(docRef);
        if (DocumentCommonModel.Types.DRAFTS.equals(primaryParentAssoc.getQName())) {
            nodeService.deleteNode(docRef);
            return new String[] { "isDraftAndDeleted",
                    primaryParentAssoc.getTypeQName().toPrefixString(serviceRegistry.getNamespaceService()),
                    primaryParentAssoc.getQName().toPrefixString(serviceRegistry.getNamespaceService()),
                    type.toPrefixString(serviceRegistry.getNamespaceService()) };
        }

        Map<QName, Serializable> origProps = nodeService.getProperties(docRef);
        Map<QName, Serializable> updatedProps = new HashMap<QName, Serializable>();

        addParentRegNumber(docRef, (String) origProps.get(DocumentCommonModel.Props.REG_NUMBER));

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

        // Always update document node to trigger an update of document data in Lucene index.
        nodeService.addProperties(docRef, updatedProps);

        String removePermissionLog = updatePermission(docRef);
        String removePrivilegeMappingsLog = removePrivilegeMappings(docRef, origProps);
        return new String[] { hasAllFinishedCompoundWorkflowsUpdaterLog, structUnitPropertiesToMultivaluedUpdaterLog, removePermissionLog, removePrivilegeMappingsLog,
                fileContentsLog, updateMetadataInFilesUpdaterLog };
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

    private String removePrivilegeMappings(NodeRef docRef, Map<QName, Serializable> props) {
        @SuppressWarnings("unchecked")
        Collection<String> privUsers = (Collection<String>) props.get(PrivilegeModel.Props.USER);
        @SuppressWarnings("unchecked")
        Collection<String> privGroups = (Collection<String>) props.get(PrivilegeModel.Props.GROUP);
        RepoUtil.validateSameSize(privUsers, privGroups, "users", "groups");
        if (privUsers != null) {
            nodeService.removeProperty(docRef, PrivilegeModel.Props.USER);
            nodeService.removeProperty(docRef, PrivilegeModel.Props.GROUP);
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
                    List<String> newValue = new ArrayList<String>();
                    if (StringUtils.isNotBlank((String) currentValue)) {
                        newValue.add((String) currentValue);
                    }
                    updatedProps.put(propQName, (Serializable) newValue);
                    resultLog.add(propQName.toPrefixString(serviceRegistry.getNamespaceService()));
                }
            }
        }
        return StringUtils.join(resultLog, ", ");
    }

    public String updatePermission(NodeRef docRef) {
        Set<AccessPermission> allSetPermissions = serviceRegistry.getPermissionService().getAllSetPermissions(docRef);
        Map<String, Set<String>> replacePermissions = new HashMap<String, Set<String>>();
        replacePermissions.put(DELETE_DOCUMENT_META_DATA, null);
        replacePermissions.put(DELETE_DOCUMENT_FILES, null);
        replacePermissions.put(EDIT_DOCUMENT_FILES, Collections.singleton(DocumentCommonModel.Privileges.EDIT_DOCUMENT));
        replacePermissions.put(EDIT_DOCUMENT_META_DATA, Collections.singleton(DocumentCommonModel.Privileges.EDIT_DOCUMENT));
        Map<String, List<String>> removedAuthorities = removePermission(docRef, replacePermissions, allSetPermissions);
        List<String> removedPermissionInfo = new ArrayList<String>(removedAuthorities.keySet().size());
        for (Entry<String, List<String>> entry : removedAuthorities.entrySet()) {
            removedPermissionInfo.add(entry.getKey() + " [ " + StringUtils.join(entry.getValue(), " ") + " ]");
        }
        return StringUtils.join(removedPermissionInfo, ", ");
    }

    public static String updateHasAllFinishedCompoundWorkflows(NodeRef docRef, Map<QName, Serializable> origProps, Map<QName, Serializable> updatedProps,
            WorkflowService workflowService) {
        Serializable origValueReal = origProps.get(DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS);
        boolean origValue = Boolean.TRUE.equals(origValueReal);
        boolean newValue = workflowService.hasAllFinishedCompoundWorkflows(docRef);
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
