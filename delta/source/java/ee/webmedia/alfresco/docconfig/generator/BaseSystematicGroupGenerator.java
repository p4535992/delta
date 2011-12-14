package ee.webmedia.alfresco.docconfig.generator;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.web.PropertySheetStateBean;

/**
 * @author Alar Kvell
 */
public abstract class BaseSystematicGroupGenerator implements FieldGroupGenerator, InitializingBean {

    protected DocumentConfigService documentConfigService;

    @Override
    public void afterPropertiesSet() {
        documentConfigService.registerFieldGroupGenerator(this, getSystematicGroupNames());
    }

    protected abstract String[] getSystematicGroupNames();

    protected static String getBindingName(String suffix, String stateHolderKey) {
        return "#{" + PropertySheetStateBean.STATE_HOLDERS_BINDING_NAME + "['" + stateHolderKey + "']." + suffix + "}";
    }

    public void setDocumentConfigService(DocumentConfigService documentConfigService) {
        this.documentConfigService = documentConfigService;
    }

}
