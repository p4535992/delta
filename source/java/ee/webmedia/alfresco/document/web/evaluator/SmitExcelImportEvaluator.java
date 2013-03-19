package ee.webmedia.alfresco.document.web.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.importer.excel.service.DocumentImportServiceImpl;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * @author Ats Uiboupin
 */
public class SmitExcelImportEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        final FacesContext ctx = FacesContext.getCurrentInstance();
        UserService userService = (UserService) FacesHelper.getManagedBean(ctx, UserService.BEAN_NAME);
        final boolean isAdmin = userService.isAdministrator();
        if (!isAdmin) {
            return false;
        }
        return !isDocImportCompleted() && isSmit();
    }

    private boolean isDocImportCompleted() {
        NodeService nodeService = (NodeService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                .getBean(ServiceRegistry.NODE_SERVICE.getLocalName());
        FunctionsService functionsService = (FunctionsService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                .getBean(FunctionsService.BEAN_NAME);
        Boolean smitDocListImported = (Boolean) nodeService.getProperty(functionsService.getFunctionsRoot(), DocumentImportServiceImpl.smitDocListImported);
        return smitDocListImported != null && smitDocListImported == true;
    }

    @Override
    public boolean evaluate(Node node) {
        return evaluate((Object) node);
    }

    private boolean isSmit() {
        DocumentTypeService documentTypeService = (DocumentTypeService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(),
                DocumentTypeService.BEAN_NAME);
        final DocumentType contractSmitDocType = documentTypeService.getDocumentType(DocumentSubtypeModel.Types.CONTRACT_SMIT);
        return contractSmitDocType.isUsed();
    }

}
