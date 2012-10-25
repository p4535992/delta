package ee.webmedia.alfresco.common.web;

import javax.faces.context.FacesContext;

import org.alfresco.repo.policy.BehaviourFilter;
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
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespacePrefixResolverProvider;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.DialogManager;
import org.alfresco.web.bean.groups.GroupsDialog;

import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.addressbook.web.bean.AddressbookGroupsManagerBean;
import ee.webmedia.alfresco.addressbook.web.bean.AddressbookSearchBean;
import ee.webmedia.alfresco.addressbook.web.dialog.AddressbookAddEditDialog;
import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.archivals.service.ArchivalsService;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.cases.web.CaseDetailsDialog;
import ee.webmedia.alfresco.cases.web.CaseListDialog;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.classificator.web.ClassificatorDetailsDialog;
import ee.webmedia.alfresco.classificator.web.ClassificatorsImportDialog;
import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.common.service.GeneralService;
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
import ee.webmedia.alfresco.document.sendout.web.SendOutBlockBean;
import ee.webmedia.alfresco.document.service.DocLockService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.document.web.DocumentListDialog;
import ee.webmedia.alfresco.document.web.VisitedDocumentsBean;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.email.service.EmailService;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.functions.web.FunctionsDetailsDialog;
import ee.webmedia.alfresco.help.service.HelpTextService;
import ee.webmedia.alfresco.help.web.HelpTextEditDialog;
import ee.webmedia.alfresco.imap.service.ImapServiceExt;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.log.web.ApplicationLogListDialog;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.mso.service.MsoService;
import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.orgstructure.amr.service.RSService;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.orgstructure.web.RsAccessStatusBean;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.parameters.web.ParametersImportDialog;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.privilege.web.ManageInheritablePrivilegesDialog;
import ee.webmedia.alfresco.register.service.RegisterService;
import ee.webmedia.alfresco.report.service.ReportService;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.series.web.SeriesDetailsDialog;
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
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.volume.web.VolumeDetailsDialog;
import ee.webmedia.alfresco.workflow.search.service.TaskReportFilterService;
import ee.webmedia.alfresco.workflow.search.service.TaskSearchFilterService;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;
import ee.webmedia.xtee.client.dhl.DhlXTeeServiceImplFSStub;

/**
 * Helper class for web environment for accessing beans simply through getter. If getter for your bean is missing then just add it
 * 
 * @author Ats Uiboupin
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

    public static SeriesDetailsDialog getSeriesDetailsDialog() {
        return getJsfBean(SeriesDetailsDialog.class, SeriesDetailsDialog.BEAN_NAME);
    }

    public static VolumeDetailsDialog getVolumeDetailsDialog() {
        return getJsfBean(VolumeDetailsDialog.class, VolumeDetailsDialog.BEAN_NAME);
    }

    public static CaseDetailsDialog getCaseDetailsDialog() {
        return getJsfBean(CaseDetailsDialog.class, CaseDetailsDialog.BEAN_NAME);
    }

    public static CaseListDialog getCaseListDialog() {
        return getJsfBean(CaseListDialog.class, CaseListDialog.BEAN_NAME);
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

    @SuppressWarnings({ "cast", "rawtypes", "unchecked" })
    public static <D extends DynamicType, S extends DynTypeDialogSnapshot<D>> DynamicTypeDetailsDialog<D, S> getDynamicTypeDetailsDialog(Class<D> dynTypeClass) {
        if (DocumentType.class.equals(dynTypeClass)) {
            // not using getDocTypeDetailsDialog() as unlike smarter eclipse compiler javac can't handle complicated generics
            DynamicTypeDetailsDialog tmp = (DynamicTypeDetailsDialog) getJsfBean(DocTypeDetailsDialog.class, DocTypeDetailsDialog.BEAN_NAME);
            return tmp;
        } else if (CaseFileType.class.equals(dynTypeClass)) {
            DynamicTypeDetailsDialog tmp = (DynamicTypeDetailsDialog) getJsfBean(CaseFileTypeDetailsDialog.class, CaseFileTypeDetailsDialog.BEAN_NAME);
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

    public static ConfirmDialog getConfirmDialog() {
        return getJsfBean(ConfirmDialog.class, ConfirmDialog.BEAN_NAME);
    }

    public static ClearStateNotificationHandler getClearStateNotificationHandler() {
        return getJsfBean(ClearStateNotificationHandler.class, ClearStateNotificationHandler.BEAN_NAME);
    }

    public static UserDetailsDialog getUserDetailsDialog() {
        return getJsfBean(UserDetailsDialog.class, UserDetailsDialog.BEAN_NAME);
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

    public static RsAccessStatusBean getRsAccessStatusBean() {
        return getSpringBean(RsAccessStatusBean.class, RsAccessStatusBean.BEAN_NAME);
    }

    public static DisableFocusingBean getDisableFocusingBean() {
        return getSpringBean(DisableFocusingBean.class, DisableFocusingBean.BEAN_NAME);
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

    public static BehaviourFilter getPolicyBehaviourFilter() {
        return getSpringBean(BehaviourFilter.class, "policyBehaviourFilter");
    }

    // END: alfresco services

    // START: delta services

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

    public static DvkService getStubDvkService() {
        return getService(DvkService.class, "StubDvkService"); // same class, but with stub xteeService implementation
    }

    public static DhlXTeeServiceImplFSStub getDhlXTeeServiceImplFSStub() {
        return getService(DhlXTeeServiceImplFSStub.class, DhlXTeeServiceImplFSStub.BEAN_NAME);
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

    public static TaskSearchFilterService getTaskSearchFilterService() {
        return getService(TaskSearchFilterService.class, TaskSearchFilterService.BEAN_NAME);
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

    public static ReportService getReportService() {
        return getService(ReportService.class, ReportService.BEAN_NAME);
    }

    public static TaskReportFilterService getTaskReportFilterService() {
        return getService(TaskReportFilterService.class, TaskReportFilterService.BEAN_NAME);
    }

    public static DocumentReportFilterService getDocumentReportFilterService() {
        return getService(DocumentReportFilterService.class, DocumentReportFilterService.BEAN_NAME);
    }

    public static WorkflowDbService getWorkflowDbService() {
        return getService(WorkflowDbService.class, WorkflowDbService.BEAN_NAME);
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
