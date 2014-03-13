package ee.webmedia.alfresco.docconfig.generator;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.web.PropertySheetStateBean;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;

public abstract class BaseSystematicFieldGenerator implements FieldGenerator, SaveListener, BeanNameAware, InitializingBean {

    protected DocumentConfigService documentConfigService;
    private String beanName;

    @Override
    public void afterPropertiesSet() {
        documentConfigService.registerFieldGeneratorById(this, getOriginalFieldIds());
    }

    protected abstract String[] getOriginalFieldIds();

    public static String getBindingName(String suffix, String stateHolderKey) {
        return "#{" + PropertySheetStateBean.STATE_HOLDERS_BINDING_NAME + "['" + stateHolderKey + "']." + suffix + "}";
    }

    @Override
    public String getBeanName() {
        return beanName;
    }

    @Override
    public void validate(DocumentDynamic document, ValidationHelper validationHelper) {
        // Subclasses can override
    }

    @Override
    public void save(DocumentDynamic document) {
        // Subclasses can override
    }

    public void setDocumentConfigService(DocumentConfigService documentConfigService) {
        this.documentConfigService = documentConfigService;
    }

    @Override
    public void setBeanName(String name) {
        beanName = name;
    }

    public boolean handlesOriginalFieldId(String originalFieldId) {
        Assert.notNull(originalFieldId, "originalFieldId mustn't be null!");
        return ArrayUtils.contains(getOriginalFieldIds(), originalFieldId);
    }

}
