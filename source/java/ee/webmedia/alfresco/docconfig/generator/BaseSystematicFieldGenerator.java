package ee.webmedia.alfresco.docconfig.generator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.web.PropertySheetStateBean;

public abstract class BaseSystematicFieldGenerator implements FieldGenerator, SaveListener, BeanNameAware, InitializingBean {

    protected DocumentConfigService documentConfigService;
    private String beanName;
    protected boolean useAdditionalStateHolders;
    protected String additionalStateHolderKey;

    @Override
    public void afterPropertiesSet() {
        documentConfigService.registerFieldGeneratorById(this, getOriginalFieldIds());
    }

    protected abstract String[] getOriginalFieldIds();

    public static String getBindingName(String suffix, String stateHolderKey) {
        return "#{" + PropertySheetStateBean.STATE_HOLDERS_BINDING_NAME + "['" + stateHolderKey + "']." + suffix + "}";
    }

    protected String getAdditionalStateHolderBindingName(String suffix, String stateHolderKey) {
        return "#{" + PropertySheetStateBean.ADDITIONAL_STATE_HOLDERS_BINDING_NAME + "['" + additionalStateHolderKey + "']" + "['" + stateHolderKey + "']." + suffix + "}";
    }

    public void setUseAdditionalStateHolders(String additionalStateHolderKey) {
        useAdditionalStateHolders = StringUtils.isNotBlank(additionalStateHolderKey);
        this.additionalStateHolderKey = additionalStateHolderKey;
    }

    public String getAdditionalStateHolderKey() {
        return additionalStateHolderKey;
    }

    public String getStateHolderBindingName(String suffix, String stateHolderKey) {
        if (useAdditionalStateHolders) {
            return getAdditionalStateHolderBindingName(suffix, stateHolderKey);
        }
        return getBindingName(suffix, stateHolderKey);
    }

    @Override
    public String getBeanName() {
        return beanName;
    }

    @Override
    public void validate(DynamicBase dynamicObject, ValidationHelper validationHelper) {
        // Subclasses can override
    }

    @Override
    public void save(DynamicBase dynamicObject) {
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
