package ee.webmedia.alfresco.docdynamic.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.event.DocumentWorkflowStatusEventListener;
import ee.webmedia.alfresco.privilege.model.UserPrivileges;
import ee.webmedia.alfresco.privilege.web.AbstractInheritingPrivilegesHandler;
import ee.webmedia.alfresco.privilege.web.PrivilegesHandler;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * {@link PrivilegesHandler} for nodes of type {@link DocumentCommonModel.Types#DOCUMENT}
 * 
 * @author Ats Uiboupin
 */
public class DocumentTypePrivilegesHandler extends AbstractInheritingPrivilegesHandler {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentTypePrivilegesHandler.class);

    protected DocumentTypePrivilegesHandler() {
        super(DocumentCommonModel.Types.DOCUMENT, Arrays.asList("viewDocumentMetaData", "viewDocumentFiles", "editDocument"));
    }

    @Override
    public String getObjectOwner() {
        return (String) getNodeService().getProperty(state.getManageableRef(), DocumentCommonModel.Props.OWNER_ID);
    }

    @Override
    // when document is public, then everybody implicit rights to viewDocumentMetaData and viewDocumentFiles
    // (but showing those privileges only on rows already added because of some other reason)
    protected void addDynamicPrivileges() {
        String accessRestriction = (String) getNodeService().getProperty(state.getManageableRef(), DocumentCommonModel.Props.ACCESS_RESTRICTION);
        if (StringUtils.equals(accessRestriction, AccessRestriction.OPEN.getValueName())) {
            String docIsPublic = MessageUtil.getMessage("document_manage_permissions_extraInfo_documentIsPublic");
            for (UserPrivileges privs : state.getUserPrivileges()) {
                // FIXME PRIV2 Ats VIEW_DOCUMENT_META_DATA lisamine vist pole vajalik - selle tulba kuvamise peaks hiljem eemaldama
                privs.addDynamicPrivilege(DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA, docIsPublic);
                privs.addDynamicPrivilege(DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES, docIsPublic);
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
        Set<Task> tasks = ws.getTasks(docRef, new Predicate<Task>() {
            @Override
            public boolean eval(Task task) {
                if (!task.isStatus(Status.IN_PROGRESS)) {
                    return false;
                }
                if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK, WorkflowSpecificModel.Types.REVIEW_TASK, WorkflowSpecificModel.Types.SIGNATURE_TASK)) {
                    return true;
                }
                return false;
            }
        });
        if (tasks.isEmpty()) {
            return false;
        }
        FileService fileService = BeanHelper.getFileService();
        Map<String, Set<String>> missingPrivsByUser = new HashMap<String, Set<String>>();
        for (Task task : tasks) {
            String ownerId = task.getOwnerId();
            UserPrivileges userPrivileges = loosingPrivileges.get(ownerId);
            if (userPrivileges == null) {
                continue;
            }
            Set<String> requiredPrivileges = DocumentWorkflowStatusEventListener.getRequiredPrivsForInprogressTask(task, docRef, fileService);
            requiredPrivileges.removeAll(userPrivileges.getActivePrivileges());
            if (!requiredPrivileges.isEmpty()) {
                Set<String> missingPrivileges = missingPrivsByUser.get(userPrivileges.getUserName());
                if (missingPrivileges == null) {
                    missingPrivileges = requiredPrivileges;
                } else {
                    missingPrivileges.addAll(requiredPrivileges);
                }
                LOG.debug("User " + userPrivileges.getUserName() + " is missing required privileges '" + missingPrivileges + "' for task " + task.getNodeRef());
                missingPrivsByUser.put(userPrivileges.getUserName(), missingPrivileges);
            }

        }
        boolean removedWFPrivilege = !missingPrivsByUser.isEmpty();
        if (removedWFPrivilege) {
            List<MessageData> missingUserPrivilegeMessages = new ArrayList<MessageData>();
            for (Entry<String, Set<String>> entry : missingPrivsByUser.entrySet()) {
                String userName = entry.getKey();
                String userDisplayName = loosingPrivileges.get(userName).getUserDisplayName();
                List<String> missingPrivileges = new ArrayList<String>();
                for (String privilege : missingPrivsByUser.get(userName)) {
                    FacesContext context = FacesContext.getCurrentInstance();
                    missingPrivileges.add(MessageUtil.getMessage(context, "permission_" + privilege));
                }
                missingUserPrivilegeMessages.add(new MessageDataImpl("document_manage_permissions_save_error_removedWfPrivileges_missingUserPrivileges"
                        , userDisplayName, missingPrivileges));
            }
            MessageUtil.addErrorMessage("document_manage_permissions_save_error_removedWfPrivileges", missingUserPrivilegeMessages);
        }
        return removedWFPrivilege;
    }
}