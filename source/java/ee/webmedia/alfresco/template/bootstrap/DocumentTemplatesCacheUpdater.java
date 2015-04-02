package ee.webmedia.alfresco.template.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.common.web.BeanHelper;

public class DocumentTemplatesCacheUpdater extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        BeanHelper.getDocumentTemplateService().getUnmodifiableTemplates();
    }

}
