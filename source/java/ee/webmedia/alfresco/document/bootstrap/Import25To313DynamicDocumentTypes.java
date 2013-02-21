package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.io.File;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.bootstrap.IndexIntegrityCheckerBootstrap;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentType;

/**
 * Import dynamic document types during 2.5 -> 3.13 migration.
 * 
 * @author Riina Tens
 */
public class Import25To313DynamicDocumentTypes extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(Import25To313DynamicDocumentTypes.class);

    private boolean enabled;
    private String documentTypeXmlFile;

    @Override
    public boolean isRequiresNewTransaction() {
        return false;
    }

    @Override
    protected void executeInternal() throws Throwable {
        if (!enabled) {
            return;
        }
        Assert.isTrue(StringUtils.isNotBlank(documentTypeXmlFile));
        File importFile = new File(documentTypeXmlFile);
        getDocumentAdminService().getImportHelper().importDynamicTypes(importFile, DocumentType.class);

        IndexIntegrityCheckerBootstrap indexIntegrityCheckerBootstrap = BeanHelper.getSpringBean(IndexIntegrityCheckerBootstrap.class, "indexIntegrityCheckerBootstrap");
        indexIntegrityCheckerBootstrap.execute(true, null);
    }

    public void setDocumentTypeXmlFile(String documentTypeXmlFile) {
        this.documentTypeXmlFile = documentTypeXmlFile;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
