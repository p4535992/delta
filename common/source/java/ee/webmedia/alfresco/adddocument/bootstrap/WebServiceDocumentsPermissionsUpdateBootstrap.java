package ee.webmedia.alfresco.adddocument.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Add permissions to web service documents' folder
 * 
 * @author Riina Tens
 */
public class WebServiceDocumentsPermissionsUpdateBootstrap extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Executing " + getName());
        NodeRef folderRef = BeanHelper.getAddDocumentService().getWebServiceDocumentsRoot();
        BeanHelper.getPrivilegeService().setPermissions(folderRef, UserService.AUTH_DOCUMENT_MANAGERS_GROUP, Privileges.EDIT_DOCUMENT);
    }

}
