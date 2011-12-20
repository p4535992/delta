package ee.webmedia.alfresco.docdynamic.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.bootstrap.StructUnitFieldTypeUpdater;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.model.PrivilegeModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * This updater does multiple things with document:
 * 1) Update properties of type STRUCT_UNIT to multiValued properties (String -> List<String>)
 * 2) Remove old permissions from all documents and replace them with new ones if needed
 * 
 * @author Riina Tens - STRUCT_UNIT update
 * @author Ats Uiboupin - replacing permissions
 */
public class DocumentUpdater extends AbstractNodeUpdater {

    // START: old permissions
    private static final String EDIT_DOCUMENT_META_DATA = "editDocumentMetaData";
    private static final String EDIT_DOCUMENT_FILES = "editDocumentFiles";
    private static final String DELETE_DOCUMENT_META_DATA = "deleteDocumentMetaData";
    private static final String DELETE_DOCUMENT_FILES = "deleteDocumentFiles";

    // END: old permissions

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        // Need to go through all documents
        String query = SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT);
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query),
                searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "docRef", "updatedStructUnitProps", "removed authorities by permission" };
    }

    @Override
    protected String[] updateNode(NodeRef docRef) throws Exception {
        String structUnitPropertiesToMultivaluedUpdaterLog = updateStructUnitPropertiesToMultivalued(docRef);
        String removePermissionLog = updatePermission(docRef);
        removePrivilegeMappings(docRef);
        return new String[] { structUnitPropertiesToMultivaluedUpdaterLog, removePermissionLog };
    }

    private void removePrivilegeMappings(NodeRef docRef) {
        @SuppressWarnings("unchecked")
        Collection<String> privUsers = (Collection<String>) nodeService.getProperty(docRef, PrivilegeModel.Props.USER);
        @SuppressWarnings("unchecked")
        Collection<String> privGroups = (Collection<String>) nodeService.getProperty(docRef, PrivilegeModel.Props.GROUP);
        RepoUtil.validateSameSize(privUsers, privGroups, "users", "groups");
        if (privUsers != null) {
            nodeService.removeProperty(docRef, PrivilegeModel.Props.USER);
            nodeService.removeProperty(docRef, PrivilegeModel.Props.GROUP);
            nodeService.removeAspect(docRef, PrivilegeModel.Aspects.USER_GROUP_MAPPING);
        }
    }

    /**
     * XXX: This method is not needed when updating from 2.5 to 3.x
     * 
     * @param docRef
     * @return
     */
    private String updateStructUnitPropertiesToMultivalued(NodeRef docRef) {
        List<String> resultLog = new ArrayList<String>();
        Map<QName, Serializable> props = nodeService.getProperties(docRef);
        Map<QName, Serializable> updatedProps = new HashMap<QName, Serializable>();
        for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
            QName propQName = entry.getKey();
            if (DocumentDynamicModel.URI.equals(propQName.getNamespaceURI()) && propQName.getLocalName().contains(StructUnitFieldTypeUpdater.ORG_STRUCT_UNIT)) {
                Serializable currentValue = entry.getValue();
                if (!(currentValue instanceof List)) {
                    List<String> newValue = new ArrayList<String>();
                    if (StringUtils.isNotBlank((String) currentValue)) {
                        newValue.add((String) currentValue);
                    }
                    updatedProps.put(propQName, (Serializable) newValue);
                    resultLog.add(propQName.toString());
                }
            }
        }
        nodeService.addProperties(docRef, updatedProps);
        String structUnitPropertiesToMultivaluedUpdaterLog = StringUtils.join(resultLog, ", ");
        return structUnitPropertiesToMultivaluedUpdaterLog;
    }

    private String updatePermission(NodeRef docRef) {
        PermissionService permissionService = BeanHelper.getPermissionService();
        Set<AccessPermission> allSetPermissions = permissionService.getAllSetPermissions(docRef);
        Map<String, String> replacePermissions = new HashMap<String, String>();
        replacePermissions.put(DELETE_DOCUMENT_META_DATA, null);
        replacePermissions.put(DELETE_DOCUMENT_FILES, null);
        replacePermissions.put(EDIT_DOCUMENT_FILES, DocumentCommonModel.Privileges.EDIT_DOCUMENT);
        replacePermissions.put(EDIT_DOCUMENT_META_DATA, DocumentCommonModel.Privileges.EDIT_DOCUMENT);
        Map<String, List<String>> removedAuthorities = removePermission(docRef, replacePermissions, allSetPermissions, permissionService);
        List<String> removedPermissionInfo = new ArrayList<String>(removedAuthorities.keySet().size());
        for (Entry<String, List<String>> entry : removedAuthorities.entrySet()) {
            removedPermissionInfo.add(entry.getKey() + " [ " + StringUtils.join(entry.getValue(), " ") + " ]");
        }
        return StringUtils.join(removedPermissionInfo, "; ");
    }

    private Map<String, List<String>> removePermission(NodeRef nodeRef, Map<String, String> replacements
            , Set<AccessPermission> allSetPermissions, PermissionService permissionService) {
        Map<String, List<String>> hashMap = new HashMap<String, List<String>>();
        for (AccessPermission accessPermission : allSetPermissions) {
            String existingPermission = accessPermission.getPermission();
            if (accessPermission.isSetDirectly() && replacements.containsKey(existingPermission)) {
                String authority = accessPermission.getAuthority();
                permissionService.deletePermission(nodeRef, authority, existingPermission);
                String replacementPermission = replacements.get(existingPermission);
                if (replacementPermission != null) {
                    permissionService.setPermission(nodeRef, authority, replacementPermission, true);
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
}
