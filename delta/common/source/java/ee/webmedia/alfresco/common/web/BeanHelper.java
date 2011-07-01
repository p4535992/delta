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
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.DialogManager;

import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.cases.web.CaseDetailsDialog;
import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.assignresponsibility.web.AssignResponsibilityBean;
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
import ee.webmedia.alfresco.document.sendout.web.DocumentSendOutDialog;
import ee.webmedia.alfresco.document.service.DocLockService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.document.web.VisitedDocumentsBean;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.functions.web.FunctionsDetailsDialog;
import ee.webmedia.alfresco.imap.service.ImapServiceExt;
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

/**
 * Helper class for web environment for accessing beans simply through getter. If getter for your bean is missing then just add it
 * 
 * @author Ats Uiboupin
 */
public class BeanHelper {
    // START: web beans
    public static DialogManager getDialogManager() {
        return (DialogManager) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), DialogManager.BEAN_NAME);
    }

    public static ManagePrivilegesDialog getManagePrivilegesDialog() {
        return (ManagePrivilegesDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), ManagePrivilegesDialog.BEAN_NAME);
    }

    public static DocumentSendOutDialog getDocumentSendOutDialog() {
        return (DocumentSendOutDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), DocumentSendOutDialog.BEAN_NAME);
    }

    public static MetadataBlockBean getMetadataBlockBean() {
        return (MetadataBlockBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MetadataBlockBean.BEAN_NAME);
    }

    public static FunctionsDetailsDialog getFunctionsDetailsDialog() {
        return (FunctionsDetailsDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), FunctionsDetailsDialog.BEAN_NAME);
    }

    public static SeriesDetailsDialog getSeriesDetailsDialog() {
        return (SeriesDetailsDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), SeriesDetailsDialog.BEAN_NAME);
    }

    public static VolumeDetailsDialog getVolumeDetailsDialog() {
        return (VolumeDetailsDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), VolumeDetailsDialog.BEAN_NAME);
    }

    public static CaseDetailsDialog getCaseDetailsDialog() {
        return (CaseDetailsDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), CaseDetailsDialog.BEAN_NAME);
    }

    public static DocumentDialog getDocumentDialog() {
        return (DocumentDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), DocumentDialog.BEAN_NAME);
    }

    public static AddFileDialog getAddFileDialog() {
        return (AddFileDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), AddFileDialog.BEAN_NAME);
    }

    public static VisitedDocumentsBean getVisitedDocumentsBean() {
        return (VisitedDocumentsBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), VisitedDocumentsBean.BEAN_NAME);
    }

    public static UserListDialog getUserListDialog() {
        return (UserListDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UserListDialog.BEAN_NAME);
    }

    public static ClearStateNotificationHandler getClearStateNotificationHandler() {
        return (ClearStateNotificationHandler) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), ClearStateNotificationHandler.BEAN_NAME);
    }

    public static UserDetailsDialog getUserDetailsDialog() {
        return (UserDetailsDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UserDetailsDialog.BEAN_NAME);
    }

    public static SubstitutionBean getSubstitutionBean() {
        return ((SubstitutionBean) AppConstants.getBeanFactory().getBean(SubstitutionBean.BEAN_NAME));
    }

    public static DimensionDetailsDialog getDimensionDetailsDialog() {
        return (DimensionDetailsDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), DimensionDetailsDialog.BEAN_NAME);
    }

    public static DimensionListDialog getDimensionListDialog() {
        return (DimensionListDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), DimensionListDialog.BEAN_NAME);
    }

    public static AssignResponsibilityBean getAssignResponsibilityBean() {
        return (AssignResponsibilityBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), AssignResponsibilityBean.BEAN_NAME);
    }

    // END: web beans

    // START: alfresco services
    public static NodeService getNodeService() {
        return (NodeService) AppConstants.getBeanFactory().getBean(ServiceRegistry.NODE_SERVICE.getLocalName());
    }

    public static AuthorityService getAuthorityService() {
        return (AuthorityService) AppConstants.getBeanFactory().getBean(ServiceRegistry.AUTHORITY_SERVICE.getLocalName());
    }

    public static SearchService getSearchService() {
        return (SearchService) AppConstants.getBeanFactory().getBean(ServiceRegistry.SEARCH_SERVICE.getLocalName());
    }

    public static TransactionService getTransactionService() {
        return (TransactionService) AppConstants.getBeanFactory().getBean(ServiceRegistry.TRANSACTION_SERVICE.getLocalName());
    }

    public static PermissionService getPermissionService() {
        return (PermissionService) AppConstants.getBeanFactory().getBean(ServiceRegistry.PERMISSIONS_SERVICE.getLocalName());
    }

    public static DictionaryService getDictionaryService() {
        return (DictionaryService) AppConstants.getBeanFactory().getBean(ServiceRegistry.DICTIONARY_SERVICE.getLocalName());
    }

    public static FileFolderService getFileFolderService() {
        return (FileFolderService) AppConstants.getBeanFactory().getBean(ServiceRegistry.FILE_FOLDER_SERVICE.getLocalName());
    }

    public static NamespaceService getNamespaceService() {
        return (NamespaceService) AppConstants.getBeanFactory().getBean(ServiceRegistry.NAMESPACE_SERVICE.getLocalName());
    }

    public static PersonService getPersonService() {
        return (PersonService) AppConstants.getBeanFactory().getBean(ServiceRegistry.PERSON_SERVICE.getLocalName());
    }

    // END: alfresco services

    // START: delta services
    public static PrivilegeService getPrivilegeService() {
        return (PrivilegeService) AppConstants.getBeanFactory().getBean(PrivilegeService.BEAN_NAME);
    }

    public static DocumentSearchService getDocumentSearchService() {
        return (DocumentSearchService) AppConstants.getBeanFactory().getBean(DocumentSearchService.BEAN_NAME);
    }

    public static UserService getUserService() {
        return (UserService) AppConstants.getBeanFactory().getBean(UserService.BEAN_NAME);
    }

    public static GeneralService getGeneralService() {
        return (GeneralService) AppConstants.getBeanFactory().getBean(GeneralService.BEAN_NAME);
    }

    public static AddressbookService getAddressbookService() {
        return (AddressbookService) AppConstants.getBeanFactory().getBean(AddressbookService.BEAN_NAME);
    }

    public static DocumentService getDocumentService() {
        return (DocumentService) AppConstants.getBeanFactory().getBean(DocumentService.BEAN_NAME);
    }

    public static ApplicationService getApplicationService() {
        return (ApplicationService) AppConstants.getBeanFactory().getBean(ApplicationService.BEAN_NAME);
    }

    public static WorkflowService getWorkflowService() {
        return (WorkflowService) AppConstants.getBeanFactory().getBean(WorkflowService.BEAN_NAME);
    }

    public static FileService getFileService() {
        return (FileService) AppConstants.getBeanFactory().getBean(FileService.BEAN_NAME);
    }

    public static EInvoiceService getEInvoiceService() {
        return (EInvoiceService) AppConstants.getBeanFactory().getBean(EInvoiceService.BEAN_NAME);
    }

    public static ParametersService getParametersService() {
        return (ParametersService) AppConstants.getBeanFactory().getBean(ParametersService.BEAN_NAME);
    }

    public static SendOutService getSendOutService() {
        return (SendOutService) AppConstants.getBeanFactory().getBean(SendOutService.BEAN_NAME);
    }

    public static DvkService getDvkService() {
        return (DvkService) AppConstants.getBeanFactory().getBean(DvkService.BEAN_NAME);
    }

    public static DocumentLogService getDocumentLogService() {
        return (DocumentLogService) AppConstants.getBeanFactory().getBean(DocumentLogService.BEAN_NAME);
    }

    public static DocLockService getDocLockService() {
        return (DocLockService) AppConstants.getBeanFactory().getBean(DocLockService.BEAN_NAME);
    }

    public static SubstituteService getSubstituteService() {
        return (SubstituteService) AppConstants.getBeanFactory().getBean(SubstituteService.BEAN_NAME);
    }

    public static DocumentTemplateService getDocumentTemplateService() {
        return (DocumentTemplateService) AppConstants.getBeanFactory().getBean(DocumentTemplateService.BEAN_NAME);
    }

    public static ImapServiceExt getImapServiceExt() {
        return (ImapServiceExt) AppConstants.getBeanFactory().getBean(ImapServiceExt.BEAN_NAME);
    }

    // END: delta services

    public static DocumentFileWriteDynamicAuthority getDocumentFileWriteDynamicAuthority() {
        return (DocumentFileWriteDynamicAuthority) AppConstants.getBeanFactory().getBean(DocumentFileWriteDynamicAuthority.BEAN_NAME);
    }

}
