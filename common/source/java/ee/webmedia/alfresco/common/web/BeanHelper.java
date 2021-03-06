package ee.webmedia.alfresco.common.web;

import javax.faces.context.FacesContext;
import javax.sql.DataSource;

import ee.smit.adit.AditAdapterSearches;
import ee.smit.adit.AditAdapterService;
import ee.smit.alfresco.plumbr.PlumbrService;
import ee.smit.alfresco.visual.VisualService;
import ee.smit.digisign.DigiSignSearches;
import ee.smit.digisign.DigiSignService;
import ee.smit.tera.TeraService;
import org.alfresco.repo.node.db.NodeDaoService;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.webdav.WebDAVLockService;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespacePrefixResolverProvider;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.BrowseBean;
import org.alfresco.web.bean.dialog.DialogManager;
import org.alfresco.web.bean.groups.GroupsDialog;
import org.alfresco.web.bean.users.UsersDialog;

import ee.webmedia.alfresco.adddocument.service.AddDocumentService;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.addressbook.web.bean.AddressbookGroupsManagerBean;
import ee.webmedia.alfresco.addressbook.web.bean.AddressbookSearchBean;
import ee.webmedia.alfresco.addressbook.web.dialog.AddressbookAddEditDialog;
import ee.webmedia.alfresco.adit.service.AditService;
import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.archivals.service.ArchivalsService;
import ee.webmedia.alfresco.archivals.web.ArchivalActivitiesListDialog;
import ee.webmedia.alfresco.archivals.web.ConfirmVolumeArchiveActionDialog;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.casefile.log.service.CaseFileLogService;
import ee.webmedia.alfresco.casefile.service.CaseFileFavoritesService;
import ee.webmedia.alfresco.casefile.service.CaseFileService;
import ee.webmedia.alfresco.casefile.web.CaseFileDialog;
import ee.webmedia.alfresco.casefile.web.CaseFileLogBlockBean;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.cases.web.CaseDetailsDialog;
import ee.webmedia.alfresco.cases.web.CaseDocumentListDialog;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.classificator.web.ClassificatorDetailsDialog;
import ee.webmedia.alfresco.classificator.web.ClassificatorsImportDialog;
import ee.webmedia.alfresco.common.service.ApplicationConstantsBean;
import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.ConstantNodeRefsBean;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.service.OpenOfficeService;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.docadmin.web.AssociationModelDetailsDialog;
import ee.webmedia.alfresco.docadmin.web.CaseFileTypeDetailsDialog;
import ee.webmedia.alfresco.docadmin.web.DocTypeDetailsDialog;
import ee.webmedia.alfresco.docadmin.web.DocumentTypesImportDialog;
import ee.webmedia.alfresco.docadmin.web.DynamicTypeDetailsDialog;
import ee.webmedia.alfresco.docadmin.web.DynamicTypeDetailsDialog.DynTypeDialogSnapshot;
import ee.webmedia.alfresco.docadmin.web.FieldDetailsDialog;
import ee.webmedia.alfresco.docadmin.web.FieldGroupDetailsDialog;
import ee.webmedia.alfresco.docconfig.generator.systematic.UserContactRelatedGroupGenerator;
import ee.webmedia.alfresco.docconfig.generator.systematic.UserContactTableGenerator;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.UserContactMappingService;
import ee.webmedia.alfresco.docconfig.web.PropertySheetStateBean;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.docdynamic.web.DocumentDialogHelperBean;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog;
import ee.webmedia.alfresco.docdynamic.web.DocumentLockHelperBean;
import ee.webmedia.alfresco.doclist.service.DocumentListService;
import ee.webmedia.alfresco.document.assignresponsibility.service.AssignResponsibilityService;
import ee.webmedia.alfresco.document.assignresponsibility.web.AssignResponsibilityBean;
import ee.webmedia.alfresco.document.associations.web.AssocsBlockBean;
import ee.webmedia.alfresco.document.assocsdyn.service.DocumentAssociationsService;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.document.einvoice.web.DimensionDetailsDialog;
import ee.webmedia.alfresco.document.einvoice.web.DimensionListDialog;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.file.web.AddFileDialog;
import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.lock.service.DocLockService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.search.service.DocumentReportFilterService;
import ee.webmedia.alfresco.document.search.service.DocumentSearchFilterService;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.search.web.DocumentSearchBean;
import ee.webmedia.alfresco.document.search.web.DocumentSearchResultsDialog;
import ee.webmedia.alfresco.document.search.web.SearchBlockBean;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.sendout.web.DocumentSendOutDialog;
import ee.webmedia.alfresco.document.sendout.web.ForwardDecDocumentDialog;
import ee.webmedia.alfresco.document.sendout.web.SendOutBlockBean;
import ee.webmedia.alfresco.document.service.DocumentFavoritesService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.document.web.DocumentListDialog;
import ee.webmedia.alfresco.document.web.VisitedDocumentsBean;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.email.service.EmailService;
import ee.webmedia.alfresco.eventplan.service.EventPlanService;
import ee.webmedia.alfresco.eventplan.web.EventPlanDialog;
import ee.webmedia.alfresco.eventplan.web.EventPlanLogBlockBean;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.functions.web.FunctionsDetailsDialog;
import ee.webmedia.alfresco.help.service.HelpTextService;
import ee.webmedia.alfresco.help.web.HelpTextEditDialog;
import ee.webmedia.alfresco.imap.service.ImapServiceExt;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.log.web.ApplicationLogListDialog;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.menu.web.MenuItemCountBean;
import ee.webmedia.alfresco.mso.service.MsoService;
import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.orgstructure.amr.service.RSService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.orgstructure.web.RsAccessStatusBean;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.parameters.web.ParametersImportDialog;
import ee.webmedia.alfresco.person.bootstrap.PersonAndOrgStructPropertiesCacheUpdater;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.privilege.web.ManageInheritablePrivilegesDialog;
import ee.webmedia.alfresco.register.service.RegisterService;
import ee.webmedia.alfresco.report.service.ReportService;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.series.web.SeriesDetailsDialog;
import ee.webmedia.alfresco.signature.service.DigiDoc4JSignatureService;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.signature.service.SkLdapService;
import ee.webmedia.alfresco.substitute.service.SubstituteService;
import ee.webmedia.alfresco.substitute.web.SubstitutionBean;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.thesaurus.service.ThesaurusService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.user.web.GroupUsersListDialog;
import ee.webmedia.alfresco.user.web.PermissionsListDialog;
import ee.webmedia.alfresco.user.web.UserDetailsDialog;
import ee.webmedia.alfresco.user.web.UserListDialog;
import ee.webmedia.alfresco.versions.service.VersionsService;
import ee.webmedia.alfresco.volume.search.service.VolumeReportFilterService;
import ee.webmedia.alfresco.volume.search.service.VolumeSearchFilterService;
import ee.webmedia.alfresco.volume.search.web.VolumeSearchResultsDialog;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.volume.web.VolumeDetailsDialog;
import ee.webmedia.alfresco.workflow.search.service.CompoundWorkflowSearchFilterService;
import ee.webmedia.alfresco.workflow.search.service.TaskReportFilterService;
import ee.webmedia.alfresco.workflow.search.service.TaskSearchFilterService;
import ee.webmedia.alfresco.workflow.search.web.CompoundWorkflowSearchResultsDialog;
import ee.webmedia.alfresco.workflow.search.web.TaskSearchResultsDialog;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflowFavoritesService;
import ee.webmedia.alfresco.workflow.service.WorkflowConstantsBean;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.web.CommentListBlock;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowAssocListDialog;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowAssocSearchBlock;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowDialog;
import ee.webmedia.alfresco.workflow.web.CompoundWorkflowLogBlockBean;
import ee.webmedia.alfresco.workflow.web.DelegationBean;
import ee.webmedia.alfresco.workflow.web.MyTasksBean;
import ee.webmedia.alfresco.workflow.web.RelatedUrlListBlock;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;
import ee.webmedia.xtee.client.dhl.DhlFSStubXTeeServiceImpl;

/**
 * Helper class for web environment for accessing beans simply through getter. If getter for your bean is missing then just add it
 */
public class BeanHelper implements NamespacePrefixResolverProvider {
    private static final long serialVersionUID = 1L;

    private static BeanHelper self = new BeanHelper();

    private BeanHelper() {
        // private singleton constructor
    }

    public static BeanHelper getInstance() {
        return self;
    }

    @Override
    public NamespacePrefixResolver getNamespacePrefixResolver() {
        return getNamespaceService();
    }

    public static DataSource getDataSource() {
        return getSpringBean(DataSource.class, "dataSource");
    }

    public static NodeDaoService getNodeDaoService() {
        return getSpringBean(NodeDaoService.class, "nodeDaoService");
    }

    // START: web beans

    public static DialogManager getDialogManager() {
        return getJsfBean(DialogManager.class, DialogManager.BEAN_NAME);
    }

    public static ManageInheritablePrivilegesDialog getManageInheritablePrivilegesDialog() {
        return getJsfBean(ManageInheritablePrivilegesDialog.class, ManageInheritablePrivilegesDialog.BEAN_NAME);
    }

    public static DocumentSendOutDialog getDocumentSendOutDialog() {
        return getJsfBean(DocumentSendOutDialog.class, DocumentSendOutDialog.BEAN_NAME);
    }

    public static MetadataBlockBean getMetadataBlockBean() {
        return getJsfBean(MetadataBlockBean.class, MetadataBlockBean.BEAN_NAME);
    }

    public static FunctionsDetailsDialog getFunctionsDetailsDialog() {
        return getJsfBean(FunctionsDetailsDialog.class, FunctionsDetailsDialog.BEAN_NAME);
    }

    public static DocumentSearchResultsDialog getDocumentSearchResultsDialog() {
        return getJsfBean(DocumentSearchResultsDialog.class, DocumentSearchResultsDialog.BEAN_NAME);
    }

    public static VolumeSearchResultsDialog getVolumeSearchResultsDialog() {
        return getJsfBean(VolumeSearchResultsDialog.class, VolumeSearchResultsDialog.BEAN_NAME);
    }

    public static SeriesDetailsDialog getSeriesDetailsDialog() {
        return getJsfBean(SeriesDetailsDialog.class, SeriesDetailsDialog.BEAN_NAME);
    }

    public static VolumeDetailsDialog getVolumeDetailsDialog() {
        return getJsfBean(VolumeDetailsDialog.class, VolumeDetailsDialog.BEAN_NAME);
    }

    public static CaseDetailsDialog getCaseDetailsDialog() {
        return getJsfBean(CaseDetailsDialog.class, CaseDetailsDialog.BEAN_NAME);
    }

    public static CaseDocumentListDialog getCaseDocumentListDialog() {
        return getJsfBean(CaseDocumentListDialog.class, CaseDocumentListDialog.BEAN_NAME);
    }

    public static ClassificatorDetailsDialog getClassificatorDetailsDialog() {
        return getJsfBean(ClassificatorDetailsDialog.class, ClassificatorDetailsDialog.BEAN_NAME);
    }

    public static ParametersImportDialog getParametersImportDialog() {
        return getJsfBean(ParametersImportDialog.class, ParametersImportDialog.BEAN_NAME);
    }

    public static ClassificatorsImportDialog getClassificatorsImportDialog() {
        return getJsfBean(ClassificatorsImportDialog.class, ClassificatorsImportDialog.BEAN_NAME);
    }

    public static DocumentTypesImportDialog getDocumentTypesImportDialog() {
        return getJsfBean(DocumentTypesImportDialog.class, DocumentTypesImportDialog.BEAN_NAME);
    }

    public static DocumentDialog getDocumentDialog() {
        return getJsfBean(DocumentDialog.class, DocumentDialog.BEAN_NAME);
    }

    public static AddFileDialog getAddFileDialog() {
        return getJsfBean(AddFileDialog.class, AddFileDialog.BEAN_NAME);
    }

    public static FieldDetailsDialog getFieldDetailsDialog() {
        return getJsfBean(FieldDetailsDialog.class, FieldDetailsDialog.BEAN_NAME);
    }

    public static FieldGroupDetailsDialog getFieldGroupDetailsDialog() {
        return getJsfBean(FieldGroupDetailsDialog.class, FieldGroupDetailsDialog.BEAN_NAME);
    }

    public static AssociationModelDetailsDialog getAssociationModelDetailsDialog() {
        return getJsfBean(AssociationModelDetailsDialog.class, AssociationModelDetailsDialog.BEAN_NAME);
    }

    public static DocTypeDetailsDialog getDocTypeDetailsDialog() {
        return getJsfBean(DocTypeDetailsDialog.class, DocTypeDetailsDialog.BEAN_NAME);
    }

    public static ApplicationLogListDialog getAppLogListDialog() {
        return getJsfBean(ApplicationLogListDialog.class, ApplicationLogListDialog.BEAN_NAME);
    }

    public static HelpTextEditDialog getHelpTextEditDialog(String type) {
        return getJsfBean(HelpTextEditDialog.class, type + HelpTextEditDialog.BEAN_NAME_SUFFIX);
    }

    public static ConfirmVolumeArchiveActionDialog getConfirmVolumeArchiveActionDialog() {
        return getJsfBean(ConfirmVolumeArchiveActionDialog.class, ConfirmVolumeArchiveActionDialog.BEAN_NAME);
    }

    public static ArchivalActivitiesListDialog getArchivalActivitiesListDialog() {
        return getJsfBean(ArchivalActivitiesListDialog.class, ArchivalActivitiesListDialog.BEAN_NAME);
    }

    @SuppressWarnings({ "cast", "rawtypes", "unchecked" })
    public static <D extends DynamicType, S extends DynTypeDialogSnapshot<D>> DynamicTypeDetailsDialog<D, S> getDynamicTypeDetailsDialog(Class<D> dynTypeClass) {
        if (DocumentType.class.equals(dynTypeClass)) {
            // not using getDocTypeDetailsDialog() as unlike smarter eclipse compiler javac can't handle complicated generics
            DynamicTypeDetailsDialog tmp = getJsfBean(DocTypeDetailsDialog.class, DocTypeDetailsDialog.BEAN_NAME);
            return tmp;
        } else if (CaseFileType.class.equals(dynTypeClass)) {
            DynamicTypeDetailsDialog tmp = getJsfBean(CaseFileTypeDetailsDialog.class, CaseFileTypeDetailsDialog.BEAN_NAME);
            return tmp;
        } else {
            throw new RuntimeException("Returning details dialog for " + dynTypeClass.getSimpleName() + " is unimplemented");
        }
    }

    public static VisitedDocumentsBean getVisitedDocumentsBean() {
        return getJsfBean(VisitedDocumentsBean.class, VisitedDocumentsBean.BEAN_NAME);
    }

    public static DocumentSearchBean getDocumentSearchBean() {
        return getJsfBean(DocumentSearchBean.class, DocumentSearchBean.BEAN_NAME);
    }

    public static UserListDialog getUserListDialog() {
        return getJsfBean(UserListDialog.class, UserListDialog.BEAN_NAME);
    }

    public static TaskSearchResultsDialog getTaskSearchResultsDialog() {
        return getJsfBean(TaskSearchResultsDialog.class, TaskSearchResultsDialog.BEAN_NAME);
    }

    public static CompoundWorkflowSearchResultsDialog getCompoundWorkflowSearchResultsDialog() {
        return getJsfBean(CompoundWorkflowSearchResultsDialog.class, CompoundWorkflowSearchResultsDialog.BEAN_NAME);
    }

    public static ConfirmDialog getConfirmDialog() {
        return getJsfBean(ConfirmDialog.class, ConfirmDialog.BEAN_NAME);
    }

    public static ClearStateNotificationHandler getClearStateNotificationHandler() {
        return getJsfBean(ClearStateNotificationHandler.class, ClearStateNotificationHandler.BEAN_NAME);
    }

    public static UserDetailsDialog getUserDetailsDialog() {
        return getJsfBean(UserDetailsDialog.class, UserDetailsDialog.BEAN_NAME);
    }

    public static UsersDialog getUsersDialog() {
        return getJsfBean(UsersDialog.class, UsersDialog.BEAN_NAME);
    }

    public static DimensionDetailsDialog getDimensionDetailsDialog() {
        return getJsfBean(DimensionDetailsDialog.class, DimensionDetailsDialog.BEAN_NAME);
    }

    public static DimensionListDialog getDimensionListDialog() {
        return getJsfBean(DimensionListDialog.class, DimensionListDialog.BEAN_NAME);
    }

    public static MenuBean getMenuBean() {
        return getJsfBean(MenuBean.class, MenuBean.BEAN_NAME);
    }

    public static AddressbookSearchBean getAddressbookSearchBean() {
        return getJsfBean(AddressbookSearchBean.class, AddressbookSearchBean.BEAN_NAME);
    }

    public static AddressbookGroupsManagerBean getAddressbookGroupsManagerBeanBean() {
        return getJsfBean(AddressbookGroupsManagerBean.class, AddressbookGroupsManagerBean.BEAN_NAME);
    }

    public static AssignResponsibilityBean getAssignResponsibilityBean() {
        return getJsfBean(AssignResponsibilityBean.class, AssignResponsibilityBean.BEAN_NAME);
    }

    public static DocumentDynamicDialog getDocumentDynamicDialog() {
        return getJsfBean(DocumentDynamicDialog.class, DocumentDynamicDialog.BEAN_NAME);
    }

    public static CaseFileDialog getCaseFileDialog() {
        return getJsfBean(CaseFileDialog.class, CaseFileDialog.BEAN_NAME);
    }

    public static PropertySheetStateBean getPropertySheetStateBean() {
        return getJsfBean(PropertySheetStateBean.class, PropertySheetStateBean.BEAN_NAME);
    }

    public static PermissionsListDialog getPermissionsListDialog() {
        return getJsfBean(PermissionsListDialog.class, PermissionsListDialog.BEAN_NAME);
    }

    public static DocumentDialogHelperBean getDocumentDialogHelperBean() {
        return getJsfBean(DocumentDialogHelperBean.class, DocumentDialogHelperBean.BEAN_NAME);
    }

    public static DocumentLockHelperBean getDocumentLockHelperBean() {
        return getSpringBean(DocumentLockHelperBean.class, DocumentLockHelperBean.BEAN_NAME);
    }

    public static FileBlockBean getFileBlockBean() {
        return getJsfBean(FileBlockBean.class, FileBlockBean.BEAN_NAME);
    }

    public static LogBlockBean getLogBlockBean() {
        return getJsfBean(LogBlockBean.class, LogBlockBean.BEAN_NAME);
    }

    public static WorkflowBlockBean getWorkflowBlockBean() {
        return getJsfBean(WorkflowBlockBean.class, WorkflowBlockBean.BEAN_NAME);
    }

    public static DelegationBean getDelegationBean() {
        return getJsfBean(DelegationBean.class, DelegationBean.BEAN_NAME);
    }

    public static SendOutBlockBean getSendOutBlockBean() {
        return getJsfBean(SendOutBlockBean.class, SendOutBlockBean.BEAN_NAME);
    }

    public static AssocsBlockBean getAssocsBlockBean() {
        return getJsfBean(AssocsBlockBean.class, AssocsBlockBean.BEAN_NAME);
    }

    public static SearchBlockBean getSearchBlockBean() {
        return getJsfBean(SearchBlockBean.class, SearchBlockBean.BEAN_NAME);
    }

    public static AddressbookAddEditDialog getAddressbookAddEditDialog() {
        return getJsfBean(AddressbookAddEditDialog.class, AddressbookAddEditDialog.BEAN_NAME);
    }

    public static UserContactGroupSearchBean getUserContactGroupSearchBean() {
        return getJsfBean(UserContactGroupSearchBean.class, UserContactGroupSearchBean.BEAN_NAME);
    }

    public static GroupsDialog getGroupsDialog() {
        return getJsfBean(GroupsDialog.class, GroupsDialog.BEAN_NAME);
    }

    public static GroupUsersListDialog getGroupUsersListDialog() {
        return getJsfBean(GroupUsersListDialog.class, GroupUsersListDialog.BEAN_NAME);
    }

    public static DocumentListDialog getDocumentListDialog() {
        return getJsfBean(DocumentListDialog.class, DocumentListDialog.BEAN_NAME);
    }

    public static UserConfirmHelper getUserConfirmHelper() {
        return getSpringBean(UserConfirmHelper.class, UserConfirmHelper.BEAN_NAME);
    }

    public static JsfBindingHelper getJsfBindingHelper() {
        return getSpringBean(JsfBindingHelper.class, JsfBindingHelper.BEAN_NAME);
    }

    public static RsAccessStatusBean getRsAccessStatusBean() {
        return getSpringBean(RsAccessStatusBean.class, RsAccessStatusBean.BEAN_NAME);
    }

    public static CompoundWorkflowDialog getCompoundWorkflowDialog() {
        return getJsfBean(CompoundWorkflowDialog.class, CompoundWorkflowDialog.BEAN_NAME);
    }

    public static CompoundWorkflowAssocListDialog getCompoundWorkflowAssocListDialog() {
        return getJsfBean(CompoundWorkflowAssocListDialog.class, CompoundWorkflowAssocListDialog.BEAN_NAME);
    }

    public static CompoundWorkflowAssocSearchBlock getCompoundWorkflowAssocSearchBlock() {
        return getJsfBean(CompoundWorkflowAssocSearchBlock.class, CompoundWorkflowAssocSearchBlock.BEAN_NAME);
    }

    public static CompoundWorkflowLogBlockBean getCompoundWorkflowLogBlockBean() {
        return getJsfBean(CompoundWorkflowLogBlockBean.class, CompoundWorkflowLogBlockBean.BEAN_NAME);
    }

    public static RelatedUrlListBlock getRelatedUrlListBlock() {
        return getJsfBean(RelatedUrlListBlock.class, RelatedUrlListBlock.BEAN_NAME);
    }

    public static CommentListBlock getCommentListBlock() {
        return getJsfBean(CommentListBlock.class, CommentListBlock.BEAN_NAME);
    }

    public static CaseFileLogBlockBean getCaseFileLogBlockBean() {
        return getJsfBean(CaseFileLogBlockBean.class, CaseFileLogBlockBean.BEAN_NAME);
    }

    public static MyTasksBean getMyTasksBean() {
        return getJsfBean(MyTasksBean.class, MyTasksBean.BEAN_NAME);
    }

    public static DisableFocusingBean getDisableFocusingBean() {
        return getSpringBean(DisableFocusingBean.class, DisableFocusingBean.BEAN_NAME);
    }

    public static UserContactRelatedGroupGenerator getUserContactRelatedGroupGenerator() {
        return getSpringBean(UserContactRelatedGroupGenerator.class, UserContactRelatedGroupGenerator.BEAN_NAME);
    }

    public static UserContactTableGenerator getUserContactTableGenerator() {
        return getSpringBean(UserContactTableGenerator.class, UserContactTableGenerator.BEAN_NAME);
    }

    public static PersonAndOrgStructPropertiesCacheUpdater getPersonAndOrgStructPropertiesCacheUpdater() {
        return getSpringBean(PersonAndOrgStructPropertiesCacheUpdater.class, PersonAndOrgStructPropertiesCacheUpdater.BEAN_NAME);
    }

    public static MenuItemCountBean getMenuItemCountBean() {
        return getJsfBean(MenuItemCountBean.class, MenuItemCountBean.BEAN_NAME);
    }

    public static BeanCleanupHelper getBeanCleanupHelper() {
        return getJsfBean(BeanCleanupHelper.class, BeanCleanupHelper.BEAN_NAME);
    }

    public static BrowseBean getBrowseBean() {
        return getJsfBean(BrowseBean.class, BrowseBean.BEAN_NAME);
    }

    public static ForwardDecDocumentDialog getForwardDecDocumentDialog() {
        return getJsfBean(ForwardDecDocumentDialog.class, ForwardDecDocumentDialog.BEAN_NAME);
    }

    // END: JSF web beans

    // START: Spring web beans

    /** this bean is used by webdav, so it must be reachable outside Faces context */
    public static SubstitutionBean getSubstitutionBean() {
        return getSpringBean(SubstitutionBean.class, SubstitutionBean.BEAN_NAME);
    }

    // END: Spring web beans

    // START: alfresco services
    public static NodeService getNodeService() {
        return getAlfrescoService(NodeService.class, ServiceRegistry.NODE_SERVICE);
    }

    public static AuthorityService getAuthorityService() {
        return getAlfrescoService(AuthorityService.class, ServiceRegistry.AUTHORITY_SERVICE);
    }

    public static AuthenticationService getAuthenticationService() {
        return getAlfrescoService(AuthenticationService.class, ServiceRegistry.AUTHENTICATION_SERVICE);
    }

    public static PersonService getPersonService() {
        return getAlfrescoService(PersonService.class, ServiceRegistry.PERSON_SERVICE);
    }

    public static SearchService getSearchService() {
        return getAlfrescoService(SearchService.class, ServiceRegistry.SEARCH_SERVICE);
    }

    public static MimetypeService getMimetypeService() {
        return getAlfrescoService(MimetypeService.class, ServiceRegistry.MIMETYPE_SERVICE);
    }

    public static ExporterService getExporterService() {
        return getAlfrescoService(ExporterService.class, ServiceRegistry.EXPORTER_SERVICE);
    }

    public static ImporterService getImporterService() {
        return getAlfrescoService(ImporterService.class, ServiceRegistry.IMPORTER_SERVICE);
    }

    public static TransactionService getTransactionService() {
        return getAlfrescoService(TransactionService.class, ServiceRegistry.TRANSACTION_SERVICE);
    }

    public static PermissionService getPermissionService() {
        return getAlfrescoService(PermissionService.class, ServiceRegistry.PERMISSIONS_SERVICE);
    }

    public static DictionaryService getDictionaryService() {
        return getAlfrescoService(DictionaryService.class, ServiceRegistry.DICTIONARY_SERVICE);
    }

    public static FileFolderService getFileFolderService() {
        return getAlfrescoService(FileFolderService.class, ServiceRegistry.FILE_FOLDER_SERVICE);
    }

    public static NamespaceService getNamespaceService() {
        return getAlfrescoService(NamespaceService.class, ServiceRegistry.NAMESPACE_SERVICE);
    }

    public static ContentService getContentService() {
        return getAlfrescoService(ContentService.class, ServiceRegistry.CONTENT_SERVICE);
    }

    public static DescriptorService getDescriptorService() {
        return getAlfrescoService(DescriptorService.class, ServiceRegistry.DESCRIPTOR_SERVICE);
    }

    public static BehaviourFilter getPolicyBehaviourFilter() {
        return getSpringBean(BehaviourFilter.class, "policyBehaviourFilter");
    }

    public static CaseFileLogService getCaseFileLogService() {
        return getSpringBean(CaseFileLogService.class, CaseFileLogService.BEAN_NAME);
    }

    public static WebDAVLockService getWebDAVLockService() {
        return getSpringBean(WebDAVLockService.class, WebDAVLockService.BEAN_NAME);
    }

    // END: alfresco services

    // START: delta services

    public static OpenOfficeService getOpenOfficeService() {
        return getSpringBean(OpenOfficeService.class, OpenOfficeService.BEAN_NAME);
    }

    public static MsoService getMsoService() {
        return getSpringBean(MsoService.class, MsoService.BEAN_NAME);
    }

    public static BaseService getBaseService() {
        return getService(BaseService.class, BaseService.BEAN_NAME);
    }

    public static PrivilegeService getPrivilegeService() {
        return getService(PrivilegeService.class, PrivilegeService.BEAN_NAME);
    }

    public static DocumentSearchService getDocumentSearchService() {
        return getService(DocumentSearchService.class, DocumentSearchService.BEAN_NAME);
    }

    public static UserService getUserService() {
        return getService(UserService.class, UserService.BEAN_NAME);
    }

    public static GeneralService getGeneralService() {
        return getService(GeneralService.class, GeneralService.BEAN_NAME);
    }

    public static AddressbookService getAddressbookService() {
        return getService(AddressbookService.class, AddressbookService.BEAN_NAME);
    }

    public static DocumentService getDocumentService() {
        return getService(DocumentService.class, DocumentService.BEAN_NAME);
    }

    public static ClassificatorService getClassificatorService() {
        return getService(ClassificatorService.class, ClassificatorService.BEAN_NAME);
    }

    public static RegisterService getRegisterService() {
        return getService(RegisterService.class, RegisterService.BEAN_NAME);
    }

    public static ApplicationService getApplicationService() {
        return getService(ApplicationService.class, ApplicationService.BEAN_NAME);
    }

    public static WorkflowService getWorkflowService() {
        return getService(WorkflowService.class, WorkflowService.BEAN_NAME);
    }

    public static FileService getFileService() {
        return getService(FileService.class, FileService.BEAN_NAME);
    }

    public static VersionsService getVersionsService() {
        return getService(VersionsService.class, VersionsService.BEAN_NAME);
    }

    public static EInvoiceService getEInvoiceService() {
        return getService(EInvoiceService.class, EInvoiceService.BEAN_NAME);
    }

    public static AdrService getAdrService() {
        return getService(AdrService.class, AdrService.BEAN_NAME);
    }

    public static ParametersService getParametersService() {
        return getService(ParametersService.class, ParametersService.BEAN_NAME);
    }

    public static SendOutService getSendOutService() {
        return getService(SendOutService.class, SendOutService.BEAN_NAME);
    }

    public static DvkService getDvkService() {
        return getService(DvkService.class, DvkService.BEAN_NAME);
    }

    public static AditService getAditService() {
        return getService(AditService.class, AditService.BEAN_NAME);
    }

    public static DvkService getStubDvkService() {
        return getService(DvkService.class, "StubDvkService"); // same class, but with stub xteeService implementation
    }

    public static DhlFSStubXTeeServiceImpl getDhlXTeeServiceImplFSStub() {
        return getService(DhlFSStubXTeeServiceImpl.class, DhlFSStubXTeeServiceImpl.BEAN_NAME);
    }

    public static DocumentLogService getDocumentLogService() {
        return getService(DocumentLogService.class, DocumentLogService.BEAN_NAME);
    }

    public static DocLockService getDocLockService() {
        return getService(DocLockService.class, DocLockService.BEAN_NAME);
    }

    public static SubstituteService getSubstituteService() {
        return getService(SubstituteService.class, SubstituteService.BEAN_NAME);
    }

    public static DocumentTemplateService getDocumentTemplateService() {
        return getService(DocumentTemplateService.class, DocumentTemplateService.BEAN_NAME);
    }

    public static ImapServiceExt getImapServiceExt() {
        return getService(ImapServiceExt.class, ImapServiceExt.BEAN_NAME);
    }

    public static DocumentTypeService getDocumentTypeService() {
        return getService(DocumentTypeService.class, DocumentTypeService.BEAN_NAME);
    }

    public static DocumentDynamicService getDocumentDynamicService() {
        return getService(DocumentDynamicService.class, DocumentDynamicService.BEAN_NAME);
    }

    public static CaseFileService getCaseFileService() {
        return getService(CaseFileService.class, CaseFileService.BEAN_NAME);
    }

    public static ThesaurusService getThesaurusService() {
        return getService(ThesaurusService.class, ThesaurusService.BEAN_NAME);
    }

    public static DocumentAdminService getDocumentAdminService() {
        return getService(DocumentAdminService.class, DocumentAdminService.BEAN_NAME);
    }

    public static DocumentAssociationsService getDocumentAssociationsService() {
        return getService(DocumentAssociationsService.class, DocumentAssociationsService.BEAN_NAME);
    }

    public static DocumentConfigService getDocumentConfigService() {
        return getService(DocumentConfigService.class, DocumentConfigService.BEAN_NAME);
    }

    public static MenuService getMenuService() {
        return getService(MenuService.class, MenuService.BEAN_NAME);
    }

    public static FunctionsService getFunctionsService() {
        return getService(FunctionsService.class, FunctionsService.BEAN_NAME);
    }

    public static SeriesService getSeriesService() {
        return getService(SeriesService.class, SeriesService.BEAN_NAME);
    }

    public static VolumeService getVolumeService() {
        return getService(VolumeService.class, VolumeService.BEAN_NAME);
    }

    public static CaseService getCaseService() {
        return getService(CaseService.class, CaseService.BEAN_NAME);
    }

    public static OrganizationStructureService getOrganizationStructureService() {
        return getService(OrganizationStructureService.class, OrganizationStructureService.BEAN_NAME);
    }

    public static EmailService getEmailService() {
        return getService(EmailService.class, EmailService.BEAN_NAME);
    }

    public static AssignResponsibilityService getAssignResponsibilityService() {
        return getService(AssignResponsibilityService.class, AssignResponsibilityService.BEAN_NAME);
    }

    public static UserContactMappingService getUserContactMappingService() {
        return getService(UserContactMappingService.class, UserContactMappingService.BEAN_NAME);
    }

    public static ArchivalsService getArchivalsService() {
        return getService(ArchivalsService.class, ArchivalsService.BEAN_NAME);
    }

    public static DocumentSearchFilterService getDocumentSearchFilterService() {
        return getService(DocumentSearchFilterService.class, DocumentSearchFilterService.BEAN_NAME);
    }

    public static VolumeSearchFilterService getVolumeSearchFilterService() {
        return getService(VolumeSearchFilterService.class, VolumeSearchFilterService.BEAN_NAME);
    }

    public static TaskSearchFilterService getTaskSearchFilterService() {
        return getService(TaskSearchFilterService.class, TaskSearchFilterService.BEAN_NAME);
    }

    public static VolumeReportFilterService getVolumeReportFilterService() {
        return getService(VolumeReportFilterService.class, VolumeReportFilterService.BEAN_NAME);
    }

    public static CompoundWorkflowSearchFilterService getCompoundWorkflowSearchFilterService() {
        return getService(CompoundWorkflowSearchFilterService.class, CompoundWorkflowSearchFilterService.BEAN_NAME);
    }

    public static NotificationService getNotificationService() {
        return getService(NotificationService.class, NotificationService.BEAN_NAME);
    }

    public static RSService getRSService() {
        return getService(RSService.class, RSService.BEAN_NAME);
    }

    public static DocumentListService getDocumentListService() {
        return getService(DocumentListService.class, DocumentListService.BEAN_NAME);
    }

    public static LogService getLogService() {
        return getService(LogService.class, LogService.BEAN_NAME);
    }

    public static HelpTextService getHelpTextService() {
        return getService(HelpTextService.class, HelpTextService.BEAN_NAME);
    }

    public static SkLdapService getSkLdapService() {
        return getService(SkLdapService.class, SkLdapService.BEAN_NAME);
    }

    public static SignatureService getSignatureService() {
        return getService(SignatureService.class, SignatureService.BEAN_NAME);
    }
    
    public static DigiDoc4JSignatureService getDigiDoc4JSignatureService() {
        return getService(DigiDoc4JSignatureService.class, DigiDoc4JSignatureService.BEAN_NAME);
    }

    public static ReportService getReportService() {
        return getService(ReportService.class, ReportService.BEAN_NAME);
    }

    public static TaskReportFilterService getTaskReportFilterService() {
        return getService(TaskReportFilterService.class, TaskReportFilterService.BEAN_NAME);
    }

    public static DocumentReportFilterService getDocumentReportFilterService() {
        return getService(DocumentReportFilterService.class, DocumentReportFilterService.BEAN_NAME);
    }

    public static DocumentFavoritesService getDocumentFavoritesService() {
        return getService(DocumentFavoritesService.class, DocumentFavoritesService.BEAN_NAME);
    }

    public static CaseFileFavoritesService getCaseFileFavoritesService() {
        return getService(CaseFileFavoritesService.class, CaseFileFavoritesService.BEAN_NAME);
    }

    public static CompoundWorkflowFavoritesService getCompoundWorkflowFavoritesService() {
        return getService(CompoundWorkflowFavoritesService.class, CompoundWorkflowFavoritesService.BEAN_NAME);
    }

    public static WorkflowDbService getWorkflowDbService() {
        return getService(WorkflowDbService.class, WorkflowDbService.BEAN_NAME);
    }

    public static AddDocumentService getAddDocumentService() {
        return getService(AddDocumentService.class, AddDocumentService.BEAN_NAME);
    }

    public static EventPlanService getEventPlanService() {
        return getService(EventPlanService.class, EventPlanService.BEAN_NAME);
    }

    public static EventPlanDialog getEventPlanDialog() {
        return getJsfBean(EventPlanDialog.class, EventPlanDialog.BEAN_NAME);
    }

    public static EventPlanLogBlockBean getEventPlanLogBlock() {
        return getJsfBean(EventPlanLogBlockBean.class, EventPlanLogBlockBean.BEAN_NAME);
    }

    public static BulkLoadNodeService getBulkLoadNodeService() {
        return getService(BulkLoadNodeService.class, BulkLoadNodeService.BEAN_NAME);
    }

    public static BulkLoadNodeService getNonTxBulkLoadNodeService() {
        return getService(BulkLoadNodeService.class, BulkLoadNodeService.NON_TX_BEAN_NAME);
    }

    public static ApplicationConstantsBean getApplicationConstantsBean() {
        return getService(ApplicationConstantsBean.class, ApplicationConstantsBean.BEAN_NAME);
    }

    public static WorkflowConstantsBean getWorkflowConstantsBean() {
        return getService(WorkflowConstantsBean.class, WorkflowConstantsBean.BEAN_NAME);
    }

    public static ConstantNodeRefsBean getConstantNodeRefsBean() {
        return getService(ConstantNodeRefsBean.class, ConstantNodeRefsBean.BEAN_NAME);
    }

    public static PlumbrService getPlumbrService(){
        return getService(PlumbrService.class, PlumbrService.BEAN_NAME);
    }

    public static VisualService getVisualService(){
        return getService(VisualService.class, VisualService.BEAN_NAME);
    }

    public static DigiSignService getDigiSignService(){
        return getService(DigiSignService.class, DigiSignService.BEAN_NAME);
    }

    public static DigiSignSearches getDigiSignSearches(){
        return getService(DigiSignSearches.class, DigiSignSearches.BEAN_NAME);
    }

    public static TeraService getTeraService(){
        return getService(TeraService.class, TeraService.BEAN_NAME);
    }

    public static AditAdapterService getAditAdapterService(){
        return getService(AditAdapterService.class, AditAdapterService.BEAN_NAME);
    }

    public static AditAdapterSearches getAditAdapterSearches(){
        return getService(AditAdapterSearches.class, AditAdapterSearches.BEAN_NAME);
    }
    // END: delta services

    // START: other beans

    // END: other beans

    // START: private methods

    @SuppressWarnings("unchecked")
    public static <T> T getSpringBean(@SuppressWarnings("unused") Class<T> beanClazz, String beanName) {
        return (T) AppConstants.getBeanFactory().getBean(beanName);
    }

    private static <T> T getService(Class<T> beanClazz, String beanName) {
        return getSpringBean(beanClazz, beanName);
    }

    private static <T> T getAlfrescoService(Class<T> beanClazz, QName serviceQName) {
        return getSpringBean(beanClazz, serviceQName.getLocalName());
    }

    @SuppressWarnings("unchecked")
    private static <T> T getJsfBean(@SuppressWarnings("unused") Class<T> beanClazz, String beanName) {
        return (T) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), beanName);
    }

    // END: private methods

}
