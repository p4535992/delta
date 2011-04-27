package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.event.DocumentWorkflowStatusEventListener;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Update document privileges according to new document privileges system. Updates only SpacesStore.
 * 
 * @author Ats Uiboupin
 */
public class DocumentPrivilegesUpdater extends AbstractNodeUpdater {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentPrivilegesUpdater.class);
    private BehaviourFilter behaviourFilter;
    private AuthorityService authorityService;
    private PermissionService permissionService;
    private PrivilegeService privilegeService;
    private DocumentService documentService;
    private FileService fileService;
    private WorkflowService workflowService;
    private SearchService searchService;
    protected GeneralService generalService;
    private static HashSet<String> SERIES_GROUPMEMBERS_PRIVILEGES;
    private static HashSet<String> SIGNATURE_OWNER_PRIVILEGES;
    private static HashSet<String> ASSIGNMENT_OWNER_PRIVILEGES;
    private static HashSet<String> REVIEW_OWNER_PRIVILEGES;
    static {
        SERIES_GROUPMEMBERS_PRIVILEGES = new HashSet<String>(Arrays.asList(Privileges.VIEW_DOCUMENT_META_DATA
                , Privileges.VIEW_DOCUMENT_FILES));

        SIGNATURE_OWNER_PRIVILEGES = new HashSet<String>(SERIES_GROUPMEMBERS_PRIVILEGES);
        SIGNATURE_OWNER_PRIVILEGES.add(Privileges.EDIT_DOCUMENT_FILES);

        ASSIGNMENT_OWNER_PRIVILEGES = new HashSet<String>(SERIES_GROUPMEMBERS_PRIVILEGES);
        ASSIGNMENT_OWNER_PRIVILEGES.add(Privileges.EDIT_DOCUMENT_META_DATA);

        REVIEW_OWNER_PRIVILEGES = new HashSet<String>(ASSIGNMENT_OWNER_PRIVILEGES);
        REVIEW_OWNER_PRIVILEGES.add(Privileges.EDIT_DOCUMENT_FILES);
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(Arrays.asList(SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT)
                , generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE)));
        Set<StoreRef> stores = getStores();
        List<ResultSet> result = new ArrayList<ResultSet>(stores.size());
        for (StoreRef storeRef : stores) {
        	result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
		}
        return result;
    }

    protected Set<StoreRef> getStores() {
    	return Collections.singleton(generalService.getStore());
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own ContentModel PROP_MODIFIER and PROP_MODIFIED values
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "documentRef", "action performed" };
    }

    @Override
    protected String[] updateNode(final NodeRef docRef) throws Exception {
        LOG.debug("starting to update privileges on document: " + docRef);

        // ChildAssociationRef primaryParent = nodeService.getPrimaryParent(cWorkflowRef);
        Map<QName, Serializable> origDocProps = nodeService.getProperties(docRef);
        final String docOwner = (String) origDocProps.get(DocumentCommonModel.Props.OWNER_ID);
        final Map<String, Object> docProps = RepoUtil.toStringProperties(origDocProps);
        Set<String> groups = documentService.addPrivilegesBasedOnSeries(docRef, docProps, null);
        LOG.debug(groups.size() + " groups added to series authoirites:" + groups);
        final QName addPrivListener = DocumentCommonModel.Types.DOCUMENT;
        for (String group : groups) {
            Set<String> authorities = authorityService.getContainedAuthorities(AuthorityType.USER, group, true);
            for (String authority : authorities) {
                privilegeService.addPrivilege(docRef, docProps, addPrivListener, authority, group, SERIES_GROUPMEMBERS_PRIVILEGES);
            }
        }

        final Boolean[] addViewDocMetaToRelatedDocs = new Boolean[1];
        workflowService.getTasks(docRef, new Predicate<Task>() {

            @Override
            public boolean evaluate(Task task) {
                boolean inProgress = task.isStatus(Status.IN_PROGRESS);
                String ownerId = task.getOwnerId();
                if (!inProgress || StringUtils.isEmpty(ownerId)) {
                    return false;
                }

                if (DocumentWorkflowStatusEventListener.isFirstSignatureTask(task, docRef, fileService)) {
                    privilegeService.addPrivilege(docRef, docProps, addPrivListener, ownerId, null, SIGNATURE_OWNER_PRIVILEGES);
                    addViewDocMetaToRelatedDocs[0] = true;
                    return true;
                }
                if (inProgress || task.isStatus(Status.FINISHED, Status.STOPPED)) {
                    if (task.isType(WorkflowSpecificModel.Types.REVIEW_TASK)) {
                        privilegeService.addPrivilege(docRef, docProps, addPrivListener, ownerId, null, REVIEW_OWNER_PRIVILEGES);
                        addViewDocMetaToRelatedDocs[0] = true;
                        return true;
                    }
                    if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
                        if (WorkflowUtil.isActiveResponsible(task) && !StringUtils.equals(ownerId, docOwner)) {
                            privilegeService.addPrivilege(docRef, docProps, addPrivListener, ownerId, null, REVIEW_OWNER_PRIVILEGES);
                        } else {
                            privilegeService.addPrivilege(docRef, docProps, addPrivListener, ownerId, null, ASSIGNMENT_OWNER_PRIVILEGES);
                        }
                        addViewDocMetaToRelatedDocs[0] = true;
                        return true;
                    }
                }
                privilegeService.addPrivilege(docRef, docProps, addPrivListener, ownerId, null, SERIES_GROUPMEMBERS_PRIVILEGES);
                return false;
            }
        });

        if (addViewDocMetaToRelatedDocs[0] != null && addViewDocMetaToRelatedDocs[0]) {
            List<DocAssocInfo> assocInfos = documentService.getAssocInfos(new Node(docRef));
            for (DocAssocInfo docAssocInfo : assocInfos) {
                if (!docAssocInfo.isCase()) {
                    NodeRef relatedDocRef = docAssocInfo.getNodeRef();
                    privilegeService.addPrivilege(relatedDocRef, docProps, addPrivListener, docOwner, Privileges.VIEW_DOCUMENT_META_DATA);
                }
            }
        }

        docProps.put(ContentModel.PROP_MODIFIER.toString(), origDocProps.get(ContentModel.PROP_MODIFIER));
        docProps.put(ContentModel.PROP_MODIFIED.toString(), origDocProps.get(ContentModel.PROP_MODIFIED));

        nodeService.setProperties(docRef, RepoUtil.toQNameProperties(docProps));
        permissionService.setInheritParentPermissions(docRef, false);

        return new String[] { "Privileges updated" };
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
        this.behaviourFilter = behaviourFilter;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setPrivilegeService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

}
