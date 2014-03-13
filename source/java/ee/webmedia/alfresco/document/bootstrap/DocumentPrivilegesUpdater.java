package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
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
 * Update document privileges according to new document privileges system
 */
public class DocumentPrivilegesUpdater extends AbstractNodeUpdater {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentPrivilegesUpdater.class);
    private PrivilegeService privilegeService;
    private DocumentService documentService;
    private FileService fileService;
    private WorkflowService workflowService;
    public static HashSet<String> SERIES_GROUPMEMBERS_PRIVILEGES;
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
        String query = SearchUtil.joinQueryPartsAnd(Arrays.asList(
                SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                SearchUtil.generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE)));
        return Arrays.asList(
                searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query),
                searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
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
        final Map<String, Object> docProps = RepoUtil.toStringProperties(origDocProps);
        Set<QName> aspects = nodeService.getAspects(docRef);

        Pair<Boolean, String> result = updatePrivileges(docRef, aspects, docProps);

        if (result.getFirst()) {
            docProps.put(ContentModel.PROP_MODIFIER.toString(), origDocProps.get(ContentModel.PROP_MODIFIER));
            docProps.put(ContentModel.PROP_MODIFIED.toString(), origDocProps.get(ContentModel.PROP_MODIFIED));
            nodeService.setProperties(docRef, generalService.getPropertiesIgnoringSystem(docProps));
        }

        return new String[] { result.getSecond() };
    }

    public Pair<Boolean, String> updatePrivileges(final NodeRef docRef, final Set<QName> aspects, final Map<String, Object> docProps) {
        if (!aspects.contains(DocumentCommonModel.Aspects.SEARCHABLE)) {
            return new Pair<Boolean, String>(false, "doesNotHaveSearchableAspect");
        }
        final String docOwner = (String) docProps.get(DocumentCommonModel.Props.OWNER_ID.toString());
        documentService.addPrivilegesBasedOnSeries(docRef, docProps, null);
        final QName addPrivListener = DocumentCommonModel.Types.DOCUMENT;

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

        if (addViewDocMetaToRelatedDocs[0] != null && addViewDocMetaToRelatedDocs[0] && StringUtils.isNotBlank(docOwner)) {
            List<DocAssocInfo> assocInfos = documentService.getAssocInfos(new Node(docRef));
            for (DocAssocInfo docAssocInfo : assocInfos) {
                if (!docAssocInfo.isCase()) {
                    NodeRef relatedDocRef = docAssocInfo.getNodeRef();
                    privilegeService.addPrivilege(relatedDocRef, docProps, addPrivListener, docOwner, Privileges.VIEW_DOCUMENT_META_DATA);
                }
            }
        }

        // This is not done in 1.10
        // permissionService.setInheritParentPermissions(docRef, false);
        return new Pair<Boolean, String>(true, "privilegesUpdated");
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

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

}
