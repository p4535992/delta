package ee.webmedia.alfresco.docdynamic.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.web.evaluator.IsOwnerEvaluator;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.privilege.web.AbstractInheritingPrivilegesHandler;
import ee.webmedia.alfresco.privilege.web.PrivilegesHandler;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * {@link PrivilegesHandler} for nodes of type {@link DocumentCommonModel.Types#DOCUMENT}
 */
public class DocumentTypePrivilegesHandler extends AbstractInheritingPrivilegesHandler {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentTypePrivilegesHandler.class);

    protected DocumentTypePrivilegesHandler() {
        this(DocumentCommonModel.Types.DOCUMENT, Arrays.asList(Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES, Privilege.EDIT_DOCUMENT));
    }

    protected DocumentTypePrivilegesHandler(QName nodeType, Collection<Privilege> manageablePermissions) {
        super(nodeType, manageablePermissions);
    }

    @Override
    public String getObjectOwner() {
        return (String) getNodeService().getProperty(state.getManageableRef(), DocumentCommonModel.Props.OWNER_ID);
    }

    @Override
    public boolean isEditable() {
        return super.isEditable() || new IsOwnerEvaluator().evaluate(state.getManageableRef());
    }

    @Override
    // when document is public, then everybody implicit rights to viewDocumentMetaData and viewDocumentFiles
    // (but showing those privileges only on rows already added because of some other reason)
    protected void addDynamicPrivileges() {
        String accessRestriction = (String) getNodeService().getProperty(state.getManageableRef(), DocumentCommonModel.Props.ACCESS_RESTRICTION);
        if (StringUtils.equals(accessRestriction, AccessRestriction.OPEN.getValueName())) {
            String docIsPublic = MessageUtil.getMessage("document_manage_permissions_extraInfo_documentIsPublic");
            for (UserPrivileges privs : state.getUserPrivileges()) {
                privs.addPrivilegeDynamic(Privilege.VIEW_DOCUMENT_FILES, docIsPublic);
            }
            for (UserPrivileges groupPrivs : state.getPrivMappings().getPrivilegesByGroup().values()) {
                groupPrivs.addPrivilegeDynamic(Privilege.VIEW_DOCUMENT_FILES, docIsPublic);
            }
        }
    }

    @Override
    protected boolean validate(Map<String, UserPrivileges> loosingPrivileges) {
        return !removedWFPrivilege(loosingPrivileges);
    }

    private boolean removedWFPrivilege(Map<String, UserPrivileges> loosingPrivileges) {
        if (loosingPrivileges.isEmpty()) {
            return false; // no privilege will be lost(if static is removed, then there is still dynamic privilege)
        }
        WorkflowService ws = BeanHelper.getWorkflowService();
        NodeRef docRef = state.getManageableRef();
        Set<Task> tasks = ws.getTasksInProgress(docRef);
        NodeRef caseFileRef = BeanHelper.getGeneralService().getAncestorNodeRefWithType(docRef, CaseFileModel.Types.CASE_FILE);
        if (caseFileRef != null) {
            tasks.addAll(ws.getTasksInProgress(caseFileRef));
        }
        if (tasks.isEmpty()) {
            return false;
        }
        FileService fileService = BeanHelper.getFileService();
        Map<String, Set<Privilege>> missingPrivsByUser = new HashMap<String, Set<Privilege>>();
        for (Task task : tasks) {
            String ownerId = task.getOwnerId();
            UserPrivileges userPrivileges = loosingPrivileges.get(ownerId);
            if (userPrivileges == null) {
                continue;
            }
            Set<Privilege> requiredPrivileges = PrivilegeUtil.getPrivsWithDependencies(PrivilegeUtil.getRequiredPrivsForInprogressTask(task, docRef, fileService,
                    CaseFileModel.Types.CASE_FILE.equals(getNodeType())));
            requiredPrivileges.removeAll(userPrivileges.getActivePrivileges());
            if (!requiredPrivileges.isEmpty()) {
                Set<Privilege> missingPrivileges = missingPrivsByUser.get(userPrivileges.getUserName());
                if (missingPrivileges == null) {
                    missingPrivileges = requiredPrivileges;
                } else {
                    missingPrivileges.addAll(requiredPrivileges);
                }
                missingPrivsByUser.put(userPrivileges.getUserName(), missingPrivileges);
            }

        }
        boolean removedWFPrivilege = !missingPrivsByUser.isEmpty();
        if (removedWFPrivilege) {
            List<MessageData> missingUserPrivilegeMessages = new ArrayList<MessageData>();
            for (Entry<String, Set<Privilege>> entry : missingPrivsByUser.entrySet()) {
                String userName = entry.getKey();
                String userDisplayName = loosingPrivileges.get(userName).getUserDisplayName();
                List<String> missingPrivileges = new ArrayList<String>();
                for (Privilege privilege : missingPrivsByUser.get(userName)) {
                    FacesContext context = FacesContext.getCurrentInstance();
                    missingPrivileges.add(MessageUtil.getMessage(context, "permission_" + privilege.getPrivilegeName()));
                }
                missingUserPrivilegeMessages.add(new MessageDataImpl("document_manage_permissions_save_error_removedWfPrivileges_missingUserPrivileges"
                        , userDisplayName, StringUtils.join(missingPrivileges, ", ")));
            }
            MessageUtil.addErrorMessage("document_manage_permissions_save_error_removedWfPrivileges", missingUserPrivilegeMessages);
        }
        return removedWFPrivilege;
    }
}