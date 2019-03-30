package ee.webmedia.alfresco.common.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;

/**
 * Load data that is frequently used during application runtime.
 */
public class LoadApplicationCommonDataBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        AuthenticationUtil.runAs(new RunAsWork<Void>() {

            @Override
            public Void doWork() throws Exception {
                BeanHelper.getBulkLoadNodeService().fillQNameCache();
                DocumentAdminService documentAdminService = BeanHelper.getDocumentAdminService();
                documentAdminService.getDocumentTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN, true);
                documentAdminService.getUsedCaseFileTypes(DocumentAdminService.DONT_INCLUDE_CHILDREN);
                BeanHelper.getWorkflowService().getIndependentCompoundWorkflowDefinitions(null);

                return null;
            }
        }, AuthenticationUtil.SYSTEM_USER_NAME);
    }
}
