package ee.webmedia.alfresco.privilege.service;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isResponsible;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;

/**
 * @author Ats Uiboupin
 */
public class PrivilegeUtil {

    public static boolean isAdminOrDocmanagerWithViewDocPermission(Node docNode) {
        return isAdminOrDocmanagerWithPermission(docNode, Privileges.VIEW_DOCUMENT_META_DATA);
    }

    public static boolean isAdminOrDocmanagerWithPermission(Node docNode, String... permissions) {
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("no permissions given for permissions check");
        }
        UserService userService = BeanHelper.getUserService();
        return userService.isAdministrator() || (userService.isDocumentManager() && docNode.hasPermissions(permissions));
    }

    public static Set<String> getPrivsWithDependencies(Set<String> permissions) {
        Set<String> permissionsWithDependencies = new HashSet<String>(permissions);
        for (String permission : permissions) {
            Set<String> privilegeDependencies = DocumentCommonModel.Privileges.PRIVILEGE_DEPENDENCIES.get(permission);
            if (privilegeDependencies != null) {
                permissionsWithDependencies.addAll(privilegeDependencies);
            }
        }
        return permissionsWithDependencies;
    }

    public static Set<String> getRequiredPrivsForInprogressTask(Task task, NodeRef docRef, FileService fileService) {
        String taskOwnerId = task.getOwnerId();
        Set<String> requiredPrivileges = new HashSet<String>(4);
        if (!StringUtils.isBlank(taskOwnerId) && isStatus(task, Status.IN_PROGRESS)) {
            // give permissions to task owner
            boolean isSignatureTaskWith1Digidoc = false;
            boolean isSignatureTaskWithFiles = false;
            if (task.isType(WorkflowSpecificModel.Types.SIGNATURE_TASK)) {
                List<File> allFiles = fileService.getAllActiveFiles(docRef);
                if (allFiles.size() == 1 && allFiles.get(0).getName().toLowerCase().endsWith(".ddoc")) {
                    isSignatureTaskWith1Digidoc = true;
                } else if (!allFiles.isEmpty()) {
                    isSignatureTaskWithFiles = true;
                }
            }
            boolean isResponsible = isResponsible(task);
            if (isSignatureTaskWith1Digidoc
                        || (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && !isResponsible)
                        || (task.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK) && !isResponsible)
                        || task.isType(WorkflowSpecificModel.Types.OPINION_TASK, WorkflowSpecificModel.Types.INFORMATION_TASK, WorkflowSpecificModel.Types.CONFIRMATION_TASK,
                                WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK, WorkflowSpecificModel.Types.SIGNATURE_TASK)) {
                requiredPrivileges.add(Privileges.VIEW_DOCUMENT_FILES); // with dependencies
            } else if (isSignatureTaskWithFiles
                    || (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && isResponsible)
                    || task.isType(WorkflowSpecificModel.Types.REVIEW_TASK, WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)
                    || (task.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK) && isResponsible)) {
                requiredPrivileges.add(Privileges.EDIT_DOCUMENT); // with dependencies
            } else {
                MessageUtil.addWarningMessage("task " + task.getType().getLocalName()
                        + ": failed to determine required permissions for the owner of task that is now in progress. Task ownerId=" + taskOwnerId);
            }
        }
        return requiredPrivileges;
    }

}
