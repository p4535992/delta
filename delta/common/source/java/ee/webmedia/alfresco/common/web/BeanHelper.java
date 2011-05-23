package ee.webmedia.alfresco.common.web;

import javax.faces.context.FacesContext;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.DialogManager;
import org.alfresco.web.bean.repository.Repository;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.cases.web.CaseDetailsDialog;
import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.file.web.AddFileDialog;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.permissions.DocumentFileWriteDynamicAuthority;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.document.web.VisitedDocumentsBean;
import ee.webmedia.alfresco.functions.web.FunctionsDetailsDialog;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.privilege.web.ManagePrivilegesDialog;
import ee.webmedia.alfresco.series.web.SeriesDetailsDialog;
import ee.webmedia.alfresco.user.service.UserService;
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

    // END: web beans

    // START: alfresco services
    public static NodeService getNodeService() {
        return (NodeService) AppConstants.getBeanFactory().getBean(ServiceRegistry.NODE_SERVICE.getLocalName());
    }

    public static AuthorityService getAuthorityService() {
        return Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getAuthorityService();
    }

    public static SearchService getSearchService() {
        return Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getSearchService();
    }

    public static TransactionService getTransactionService() {
        return Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getTransactionService();
    }

    public static PermissionService getPermissionService() {
        return (PermissionService) AppConstants.getBeanFactory().getBean(ServiceRegistry.PERMISSIONS_SERVICE.getLocalName());
    }

    public static DictionaryService getDictionaryService() {
        return (DictionaryService) AppConstants.getBeanFactory().getBean(ServiceRegistry.DICTIONARY_SERVICE.getLocalName());
    }

    public static FileFolderService getFileFolderService() {
        return Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getFileFolderService();
    }

    public static NamespaceService getNamespaceService() {
        return (NamespaceService) AppConstants.getBeanFactory().getBean(ServiceRegistry.NAMESPACE_SERVICE.getLocalName());
    }

    // END: alfresco services

    // START: delta services
    public static PrivilegeService getPrivilegeService() {
        return (PrivilegeService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(PrivilegeService.BEAN_NAME);
    }

    public static DocumentSearchService getDocumentSearchService() {
        return (DocumentSearchService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(DocumentSearchService.BEAN_NAME);
    }

    public static UserService getUserService() {
        return (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(UserService.BEAN_NAME);
    }

    public static GeneralService getGeneralService() {
        return (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
    }

    public static DocumentService getDocumentService() {
        return (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(DocumentService.BEAN_NAME);
    }

    public static ApplicationService getApplicationService() {
        return (ApplicationService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(ApplicationService.BEAN_NAME);
    }

    public static WorkflowService getWorkflowService() {
        return (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(WorkflowService.BEAN_NAME);
    }

    public static FileService getFileService() {
        return (FileService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(FileService.BEAN_NAME);
    }

    public static EInvoiceService getEInvoiceService() {
        return (EInvoiceService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(EInvoiceService.BEAN_NAME);
    }

    public static ParametersService getParametersService() {
        return (ParametersService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(ParametersService.BEAN_NAME);
    }

    public static SendOutService getSendOutService() {
        return (SendOutService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(SendOutService.BEAN_NAME);
    }

    public static DvkService getDvkService() {
        return (DvkService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(DvkService.BEAN_NAME);
    }

    // END: delta services

    public static DocumentFileWriteDynamicAuthority getDocumentFileWriteDynamicAuthority() {
        return (DocumentFileWriteDynamicAuthority) AppConstants.getBeanFactory().getBean(DocumentFileWriteDynamicAuthority.BEAN_NAME);
    }

}
