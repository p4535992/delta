package ee.webmedia.alfresco.docdynamic.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.model.PrivilegeModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * This updater does multiple things with document:
 * 1) Update properties of type STRUCT_UNIT to multiValued properties (String -> List<String>)
 * 2) Set searchableHasAllFinishedCompoundWorkflows property value
 * 3) Remove old permissions from all documents and replace them with new ones if needed
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
    QName EMAIL_DATE_TIME = QName.createQName(DocumentCommonModel.DOCCOM_URI, "emailDateTime");
    // END: old aspects

    private WorkflowService workflowService;
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
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
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

        String regNumber = (String) origProps.get(DocumentCommonModel.Props.REG_NUMBER);
        if (StringUtils.isNotBlank(regNumber)) {
            NodeRef parentRef = nodeService.getPrimaryParent(docRef).getParentRef();
            List<String> regNumbers = documentRegNumbers.get(parentRef);
            if (regNumbers == null) {
                regNumbers = new ArrayList<String>();
                documentRegNumbers.put(parentRef, regNumbers);
            }
            regNumbers.add(regNumber);
        }

        String hasAllFinishedCompoundWorkflowsUpdaterLog = updateHasAllFinishedCompoundWorkflows(docRef, origProps, updatedProps);

        String structUnitPropertiesToMultivaluedUpdaterLog = updateStructUnitPropertiesToMultivalued(origProps, updatedProps);

        // Always update document node to trigger an update of document data in Lucene index.
        nodeService.addProperties(docRef, updatedProps);

        String removePermissionLog = updatePermission(docRef);
        String removePrivilegeMappingsLog = removePrivilegeMappings(docRef, origProps);
        return new String[] { hasAllFinishedCompoundWorkflowsUpdaterLog, structUnitPropertiesToMultivaluedUpdaterLog, removePermissionLog, removePrivilegeMappingsLog };
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

    private String updatePermission(NodeRef docRef) {
        Set<AccessPermission> allSetPermissions = serviceRegistry.getPermissionService().getAllSetPermissions(docRef);
        Map<String, String> replacePermissions = new HashMap<String, String>();
        replacePermissions.put(DELETE_DOCUMENT_META_DATA, null);
        replacePermissions.put(DELETE_DOCUMENT_FILES, null);
        replacePermissions.put(EDIT_DOCUMENT_FILES, DocumentCommonModel.Privileges.EDIT_DOCUMENT);
        replacePermissions.put(EDIT_DOCUMENT_META_DATA, DocumentCommonModel.Privileges.EDIT_DOCUMENT);
        Map<String, List<String>> removedAuthorities = removePermission(docRef, replacePermissions, allSetPermissions);
        List<String> removedPermissionInfo = new ArrayList<String>(removedAuthorities.keySet().size());
        for (Entry<String, List<String>> entry : removedAuthorities.entrySet()) {
            removedPermissionInfo.add(entry.getKey() + " [ " + StringUtils.join(entry.getValue(), " ") + " ]");
        }
        return StringUtils.join(removedPermissionInfo, ", ");
    }

    private Map<String, List<String>> removePermission(NodeRef nodeRef, Map<String, String> replacements, Set<AccessPermission> allSetPermissions) {
        Map<String, List<String>> hashMap = new HashMap<String, List<String>>();
        for (AccessPermission accessPermission : allSetPermissions) {
            String existingPermission = accessPermission.getPermission();
            if (accessPermission.isSetDirectly() && replacements.containsKey(existingPermission)) {

                String authority = accessPermission.getAuthority();
                serviceRegistry.getPermissionService().deletePermission(nodeRef, authority, existingPermission);
                String replacementPermission = replacements.get(existingPermission);
                if (replacementPermission != null) {
                    serviceRegistry.getPermissionService().setPermission(nodeRef, authority, replacementPermission, true);
                }
                List<String> authoritiesByFormerPermission = hashMap.get(existingPermission);
                if (authoritiesByFormerPermission == null) {
                    authoritiesByFormerPermission = new ArrayList<String>();
                    hashMap.put(existingPermission, authoritiesByFormerPermission);
                }
                authoritiesByFormerPermission.add(authority);
            }
        }
        return hashMap;
    }

    private String updateHasAllFinishedCompoundWorkflows(NodeRef docRef, Map<QName, Serializable> origProps, Map<QName, Serializable> updatedProps) {
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

}
