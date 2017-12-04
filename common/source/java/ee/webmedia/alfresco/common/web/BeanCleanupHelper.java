package ee.webmedia.alfresco.common.web;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.NavigationBean;
import org.alfresco.web.bean.dialog.DialogManager;
import org.alfresco.web.bean.dialog.IDialogBean;
import org.alfresco.web.bean.groups.GroupsDialog;
import org.alfresco.web.config.DialogsConfigElement.DialogConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.addressbook.web.bean.AddressbookGroupsManagerBean;
import ee.webmedia.alfresco.addressbook.web.bean.AddressbookSearchBean;
import ee.webmedia.alfresco.addressbook.web.dialog.AddressbookAddEditDialog;
import ee.webmedia.alfresco.addressbook.web.dialog.AddressbookListDialog;
import ee.webmedia.alfresco.addressbook.web.dialog.ConfirmAddDuplicateDialog;
import ee.webmedia.alfresco.addressbook.web.dialog.ContactGroupContactsDialog;
import ee.webmedia.alfresco.addressbook.web.dialog.ContactGroupListDialog;
import ee.webmedia.alfresco.archivals.web.ArchivalActivitiesListDialog;
import ee.webmedia.alfresco.archivals.web.ArchivedFunctionsListDialog;
import ee.webmedia.alfresco.archivals.web.ConfirmVolumeArchiveActionDialog;
import ee.webmedia.alfresco.casefile.web.CaseFileDialog;
import ee.webmedia.alfresco.casefile.web.CaseFileListDialog;
import ee.webmedia.alfresco.cases.web.CaseDetailsDialog;
import ee.webmedia.alfresco.cases.web.CaseDocumentListDialog;
import ee.webmedia.alfresco.classificator.web.ClassificatorDetailsDialog;
import ee.webmedia.alfresco.classificator.web.ClassificatorListDialog;
import ee.webmedia.alfresco.docadmin.web.FieldDefinitionListDialog;
import ee.webmedia.alfresco.docadmin.web.FieldDetailsDialog;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog;
import ee.webmedia.alfresco.document.forum.web.InviteUsersDialog;
import ee.webmedia.alfresco.document.search.web.DocumentDynamicReportDialog;
import ee.webmedia.alfresco.document.search.web.DocumentDynamicSearchDialog;
import ee.webmedia.alfresco.document.search.web.DocumentQuickSearchResultsDialog;
import ee.webmedia.alfresco.document.search.web.DocumentSearchResultsDialog;
import ee.webmedia.alfresco.document.web.DiscussionDocumentListDialog;
import ee.webmedia.alfresco.document.web.DocumentListDialog;
import ee.webmedia.alfresco.document.web.DvkDocumentListDialog;
import ee.webmedia.alfresco.document.web.FavoritesDocumentListDialog;
import ee.webmedia.alfresco.document.web.OutboxDocumentListDialog;
import ee.webmedia.alfresco.document.web.UnsentDocumentListDialog;
import ee.webmedia.alfresco.eventplan.web.VolumeEventPlanDialog;
import ee.webmedia.alfresco.functions.web.FunctionsDetailsDialog;
import ee.webmedia.alfresco.functions.web.FunctionsListDialog;
import ee.webmedia.alfresco.imap.web.IncomingEInvoiceListDialog;
import ee.webmedia.alfresco.log.web.ApplicationLogListDialog;
import ee.webmedia.alfresco.orgstructure.web.OrganizationStructureListDialog;
import ee.webmedia.alfresco.privilege.web.ManageInheritablePrivilegesDialog;
import ee.webmedia.alfresco.register.web.RegisterDetailsDialog;
import ee.webmedia.alfresco.register.web.RegisterListDialog;
import ee.webmedia.alfresco.report.web.ReportListDialog;
import ee.webmedia.alfresco.series.web.SeriesDetailsDialog;
import ee.webmedia.alfresco.series.web.SeriesListDialog;
import ee.webmedia.alfresco.substitute.web.SubstituteListDialog;
import ee.webmedia.alfresco.substitute.web.SubstitutionBean;
import ee.webmedia.alfresco.thesaurus.web.ThesaurusDetailsDialog;
import ee.webmedia.alfresco.thesaurus.web.ThesaurusListDialog;
import ee.webmedia.alfresco.user.web.GroupUsersListDialog;
import ee.webmedia.alfresco.user.web.PermissionsAddDialog;
import ee.webmedia.alfresco.user.web.PermissionsDeleteDialog;
import ee.webmedia.alfresco.user.web.PermissionsListDialog;
import ee.webmedia.alfresco.user.web.UserDetailsDialog;
import ee.webmedia.alfresco.user.web.UserSyncDialog;
import ee.webmedia.alfresco.volume.search.web.VolumeDynamicReportDialog;
import ee.webmedia.alfresco.volume.search.web.VolumeDynamicSearchDialog;
import ee.webmedia.alfresco.volume.search.web.VolumeSearchResultsDialog;
import ee.webmedia.alfresco.volume.web.VolumeDetailsDialog;
import ee.webmedia.alfresco.volume.web.VolumeListDialog;
import ee.webmedia.alfresco.workflow.search.web.TaskReportDialog;
import ee.webmedia.alfresco.workflow.search.web.TaskSearchDialog;
import ee.webmedia.alfresco.workflow.search.web.TaskSearchResultsDialog;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowDefinitionDialog;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowDefinitionListDialog;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowDialog;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowListDialog;
import ee.webmedia.alfresco.workflow.web.MyTasksBean;

public class BeanCleanupHelper implements Serializable {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(BeanCleanupHelper.class);

    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "BeanCleanupHelper";

    private final Set<String> dialogsResetLater = new HashSet<>();
    private final Set<String> allVisitedBeans = new HashSet<>();
    private boolean myAlfrescoScreenVisited;

    /**
     * keys = dialogs to clean (i.e. call clean() method) </br>
     * value = list of dialogs when clean() should not be called when moving from key to any value
     */
    private static final Map<String, List<String>> DIALOGS_TO_CLEAN = new HashMap<>();

    static {
        DIALOGS_TO_CLEAN.put(CompoundWorkflowListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(DiscussionDocumentListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(CaseFileListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(DvkDocumentListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(FavoritesDocumentListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(IncomingEInvoiceListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(OutboxDocumentListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(UnsentDocumentListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(ReportListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(DocumentDynamicSearchDialog.BEAN_NAME, Collections.singletonList(DocumentSearchResultsDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(TaskSearchDialog.BEAN_NAME, Collections.singletonList(TaskSearchResultsDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(VolumeDynamicSearchDialog.BEAN_NAME, Collections.singletonList(VolumeSearchResultsDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(DocumentDynamicReportDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(TaskReportDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(VolumeDynamicReportDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(ApplicationLogListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(ClassificatorListDialog.BEAN_NAME, Arrays.asList(ClassificatorDetailsDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(AddressbookListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(AddressbookAddEditDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(ConfirmAddDuplicateDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(ContactGroupContactsDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(ContactGroupListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(AddressbookGroupsManagerBean.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(AddressbookSearchBean.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(UserContactGroupSearchBean.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(UserConfirmHelper.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(ClearStateNotificationHandler.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(OrganizationStructureListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(SubstituteListDialog.BEAN_NAME, Collections.singletonList("dialog:close"));
        DIALOGS_TO_CLEAN.put(SubstitutionBean.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(ThesaurusDetailsDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(ThesaurusListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(UserSyncDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(GroupUsersListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(PermissionsListDialog.BEAN_NAME, Arrays.asList(PermissionsAddDialog.BEAN_NAME, InviteUsersDialog.BEAN_NAME, PermissionsDeleteDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(PermissionsAddDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(PermissionsDeleteDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(ConfirmVolumeArchiveActionDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(ArchivalActivitiesListDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(ArchivedFunctionsListDialog.BEAN_NAME, Arrays.asList(SeriesListDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(FunctionsDetailsDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(RegisterListDialog.BEAN_NAME, Collections.singletonList(RegisterDetailsDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(RegisterDetailsDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(VolumeListDialog.BEAN_NAME,
                Arrays.asList(CaseDocumentListDialog.DIALOG_NAME, CaseFileDialog.BEAN_NAME, VolumeDetailsDialog.BEAN_NAME, VolumeEventPlanDialog.BEAN_NAME,
                        SeriesListDialog.BEAN_NAME, DocumentListDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(SeriesListDialog.BEAN_NAME, Arrays.asList(VolumeListDialog.BEAN_NAME, SeriesDetailsDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(FunctionsListDialog.BEAN_NAME, Arrays.asList(SeriesListDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(ManageInheritablePrivilegesDialog.BEAN_NAME, Collections.singletonList(GroupUsersListDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(CompoundWorkflowDefinitionListDialog.BEAN_NAME, Arrays.asList(CompoundWorkflowDefinitionDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(CompoundWorkflowDefinitionDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(FieldDefinitionListDialog.BEAN_NAME, Arrays.asList(FieldDetailsDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(FieldDetailsDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(GroupsDialog.BEAN_NAME, null);
        DIALOGS_TO_CLEAN.put(CaseDocumentListDialog.BEAN_NAME, Arrays.asList(DocumentListDialog.BEAN_NAME, CaseDetailsDialog.BEAN_NAME, DocumentDynamicDialog.BEAN_NAME));
        DIALOGS_TO_CLEAN.put(UserDetailsDialog.BEAN_NAME, Arrays.asList(GroupUsersListDialog.BEAN_NAME, SubstituteListDialog.BEAN_NAME));
    }

    public void clean(FacesContext context, String outcome, boolean canResetMyTasks) {
        DialogManager dialogManager = Application.getDialogManager();
        if (dialogManager != null && dialogManager.getState() != null) {
            DialogConfig dialogConfig = dialogManager.getCurrentDialog();
            String managedBean = dialogConfig.getManagedBean();
            boolean resetTasks = canResetMyTasks && myAlfrescoScreenVisited && outcome != null
                    && !NavigationBean.LOCATION_MYALFRESCO.equals(outcome) && !MyTasksBean.TASK_LIST_DIALOG.equals(outcome);
            if (resetTasks) {
                myAlfrescoScreenVisited = false;
                if (!CompoundWorkflowDialog.DIALOG_NAME.equals(outcome) && !DocumentDynamicDialog.DIALOG_NAME.equals(outcome) && !CaseFileDialog.DIALOG_NAME.equals(outcome)) {
                    BeanHelper.getMyTasksBean().clean();
                }
            }

            if (DIALOGS_TO_CLEAN.containsKey(managedBean)) {
                if (postponeClean(managedBean, outcome) || outcome == null || MyTasksBean.TASK_LIST_DIALOG.equals(outcome)
                        || StringUtils.containsIgnoreCase(outcome, "dialog:delete") || StringUtils.equals(outcome, "search")) {
                    // Bean cannot be reset right now beacause user might return to this dialog. Reset them later when it's safe.
                    dialogsResetLater.add(managedBean);
                } else {
                    if (!dialogsResetLater.contains(managedBean)) {
                        clean(context, managedBean);
                        resetDialogsInQueue(context, outcome);
                    }
                }
            } else {
                allVisitedBeans.add(managedBean);
            }
        }
    }

    public void cleanAll() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (CollectionUtils.isNotEmpty(dialogsResetLater)) {
            cleanAll(context, dialogsResetLater, false);
        }
        if (CollectionUtils.isNotEmpty(allVisitedBeans)) {
            cleanAll(context, allVisitedBeans, true);
        }
    }

    private void cleanAll(FacesContext context, Collection<String> dialogs, boolean isAllVisitedBeans) {
        Iterator<String> iterator = dialogs.iterator();
        DialogConfig dialogConfig = Application.getDialogManager().getCurrentDialog();
        if (dialogConfig == null) {
            return;
        }
        String currentDialogName = dialogConfig.getManagedBean();
        while (iterator.hasNext()) {
            String managedBean = iterator.next();
            if (DocumentQuickSearchResultsDialog.BEAN_NAME.equals(managedBean)) {
                if (isAllVisitedBeans) {
                    dialogsResetLater.add(managedBean);
                    iterator.remove();
                }
            }
            else if (!managedBean.equals(currentDialogName)) {
                clean(context, managedBean);
                iterator.remove();
            }
        }
    }

    public boolean isMyAlfrescoScreenVisited() {
        return myAlfrescoScreenVisited;
    }

    public void setMyAlfrescoScreenVisited(boolean visited) {
        myAlfrescoScreenVisited = visited;
    }

    private void resetDialogsInQueue(FacesContext context, String outcome) {
        if (outcome == null || dialogsResetLater.isEmpty() || AlfrescoNavigationHandler.isDialogOrWizardClosing(outcome)) {
            return;
        }
        Iterator<String> iterator = dialogsResetLater.iterator();
        while (iterator.hasNext()) {
            String managedBean = iterator.next();
            if (postponeClean(managedBean, outcome)) {
                continue;
            }
            clean(context, managedBean);
            iterator.remove();
        }
    }

    private void clean(FacesContext context, String managedBean) {
        IDialogBean bean = (IDialogBean) FacesHelper.getManagedBean(context, managedBean);
        if (bean == null) {
            LOG.warn("Unable to find " + managedBean);
            return;
        }
        bean.clean();
    }

    /**
     * Returns true if <code>clean()</code> should be called later if moving from managedBean to outcome
     */
    private static boolean postponeClean(String managedBean, String outcome) {
        return StringUtils.containsIgnoreCase(outcome, managedBean) || containsException(managedBean, outcome)
                || "dialog:userConsole".equals(outcome) && UserDetailsDialog.BEAN_NAME.equals(managedBean);
    }

    private static boolean containsException(String managedBean, String outcome) {
        List<String> exceptions = DIALOGS_TO_CLEAN.get(managedBean);
        if (exceptions != null && outcome != null) {
            for (String bean : exceptions) {
                if (StringUtils.containsIgnoreCase(outcome, bean)) {
                    return true;
                }
            }
        }
        return false;
    }

}
