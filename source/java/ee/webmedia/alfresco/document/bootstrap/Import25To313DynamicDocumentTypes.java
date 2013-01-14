package ee.webmedia.alfresco.document.bootstrap;

import java.io.File;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentType;

/**
 * Import dynamic document types during 2.5 -> 3.13 migration.
 * 
 * @author Riina Tens
 */
public class Import25To313DynamicDocumentTypes extends AbstractModuleComponent {

    private boolean enabled;
    private String documentTypeXmlFile;

    @Override
    protected void executeInternal() throws Throwable {
        if (!enabled) {
            return;
        }
        Assert.isTrue(StringUtils.isNotBlank(documentTypeXmlFile));
        BeanHelper.getDocumentAdminService().importDynamicTypes(new File(documentTypeXmlFile), DocumentType.class, true);
    }

    public void setDocumentTypeXmlFile(String documentTypeXmlFile) {
        this.documentTypeXmlFile = documentTypeXmlFile;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
