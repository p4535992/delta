package ee.webmedia.alfresco.common.web;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.bean.repository.Repository;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;

/**
 * Helper class for web environment for accessing beans simply through getter. If getter for your bean is missing then just add it
 * 
 * @author Ats Uiboupin
 */
public class BeanHelper {
    public static NodeService getNodeService() {
        return Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
    }

    public static GeneralService getGeneralService() {
        return (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
    }

    public static SearchService getSearchService() {
        return Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getSearchService();
    }

    public static TransactionService getTransactionService() {
        return Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getTransactionService();
    }
}
