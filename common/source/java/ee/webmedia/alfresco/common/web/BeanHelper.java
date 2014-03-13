package ee.webmedia.alfresco.common.web;

import javax.faces.context.FacesContext;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.DialogManager;

import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.cases.web.CaseDetailsDialog;
import ee.webmedia.alfresco.common.externalsession.service.ExternalSessionService;
import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.document.einvoice.web.DimensionDetailsDialog;
import ee.webmedia.alfresco.document.einvoice.web.DimensionListDialog;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.file.web.AddFileDialog;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.permissions.DocumentFileWriteDynamicAuthority;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.document.web.VisitedDocumentsBean;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.functions.web.FunctionsDetailsDialog;
import ee.webmedia.alfresco.imap.service.ImapServiceExt;
import ee.webmedia.alfresco.maais.MaaisSessionBean;
import ee.webmedia.alfresco.maais.service.MaaisService;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.privilege.web.ManagePrivilegesDialog;
import ee.webmedia.alfresco.series.web.SeriesDetailsDialog;
import ee.webmedia.alfresco.substitute.service.SubstituteService;
import ee.webmedia.alfresco.substitute.web.SubstitutionBean;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.user.web.UserDetailsDialog;
import ee.webmedia.alfresco.user.web.UserListDialog;
import ee.webmedia.alfresco.volume.web.VolumeDetailsDialog;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.xtee.client.dhl.DhlXTeeServiceImplFSStub;

/**
 * Helper class for web environment for accessing beans simply through getter. If getter for your bean is missing then just add it
 */
public class BeanHelper {

    // START: web beans

    public static DialogManager getDialogManager() {
        return getJsfBean(DialogManager.class, DialogManager.BEAN_NAME);
    }

    public static ManagePrivilegesDialog getManagePrivilegesDialog() {
        return getJsfBean(ManagePrivilegesDialog.class, ManagePrivilegesDialog.BEAN_NAME);
    }

    public static MetadataBlockBean getMetadataBlockBean() {
        return getJsfBean(MetadataBlockBean.class, MetadataBlockBean.BEAN_NAME);
    }

    public static FunctionsDetailsDialog getFunctionsDetailsDialog() {
        return getJsfBean(FunctionsDetailsDialog.class, FunctionsDetailsDialog.BEAN_NAME);
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

    public static DocumentDialog getDocumentDialog() {
        return getJsfBean(DocumentDialog.class, DocumentDialog.BEAN_NAME);
    }

    public static AddFileDialog getAddFileDialog() {
        return getJsfBean(AddFileDialog.class, AddFileDialog.BEAN_NAME);
    }

    public static VisitedDocumentsBean getVisitedDocumentsBean() {
        return getJsfBean(VisitedDocumentsBean.class, VisitedDocumentsBean.BEAN_NAME);
    }

    public static UserListDialog getUserListDialog() {
        return getJsfBean(UserListDialog.class, UserListDialog.BEAN_NAME);
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

    // END: JSF web beans

    // START: Spring web beans

    /** this bean is used by webdav, so it must be reachable outside Faces context */
    public static SubstitutionBean getSubstitutionBean() {
        return ((SubstitutionBean) AppConstants.getBeanFactory().getBean(SubstitutionBean.BEAN_NAME));
    }

    public static MaaisSessionBean getMaaisSessionBean() {
        return getSpringBean(MaaisSessionBean.class, MaaisSessionBean.BEAN_NAME);
    }

    // END: Spring web beans

    // START: alfresco services
    public static NodeService getNodeService() {
        return getAlfrescoService(NodeService.class, ServiceRegistry.NODE_SERVICE);
    }

    public static AuthorityService getAuthorityService() {
        return getAlfrescoService(AuthorityService.class, ServiceRegistry.AUTHORITY_SERVICE);
    }

    public static PersonService getPersonService() {
        return getAlfrescoService(PersonService.class, ServiceRegistry.PERSON_SERVICE);
    }

    public static SearchService getSearchService() {
        return getAlfrescoService(SearchService.class, ServiceRegistry.SEARCH_SERVICE);
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

    // END: alfresco services

    // START: delta services

    public static MaaisService getMaaisService() {
        return getSpringBean(MaaisService.class, MaaisService.BEAN_NAME);
    }

    public static ExternalSessionService getExternalSessionService() {
        return getSpringBean(ExternalSessionService.class, ExternalSessionService.BEAN_NAME);
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

    public static SubstituteService getSubstituteService() {
        return getService(SubstituteService.class, SubstituteService.BEAN_NAME);
    }

    public static DocumentTemplateService getDocumentTemplateService() {
        return getService(DocumentTemplateService.class, DocumentTemplateService.BEAN_NAME);
    }

    public static ImapServiceExt getImapServiceExt() {
        return getService(ImapServiceExt.class, ImapServiceExt.BEAN_NAME);
    }

    // END: delta services

    // START: other beans

    public static DocumentFileWriteDynamicAuthority getDocumentFileWriteDynamicAuthority() {
        return getSpringBean(DocumentFileWriteDynamicAuthority.class, DocumentFileWriteDynamicAuthority.BEAN_NAME);
    }

    // START: other beans

    // START: private methods

    @SuppressWarnings("unchecked")
    private static <T> T getSpringBean(@SuppressWarnings("unused") Class<T> beanClazz, String beanName) {
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
