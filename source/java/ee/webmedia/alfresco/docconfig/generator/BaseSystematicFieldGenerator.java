package ee.webmedia.alfresco.docconfig.generator;

<<<<<<< HEAD
import org.apache.commons.lang.StringUtils;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

<<<<<<< HEAD
import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.web.PropertySheetStateBean;

/**
 * @author Alar Kvell
 */
=======
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.web.PropertySheetStateBean;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public abstract class BaseSystematicFieldGenerator implements FieldGenerator, SaveListener, BeanNameAware, InitializingBean {

    protected DocumentConfigService documentConfigService;
    private String beanName;
<<<<<<< HEAD
    protected boolean useAdditionalStateHolders;
    protected String additionalStateHolderKey;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    @Override
    public void afterPropertiesSet() {
        documentConfigService.registerFieldGeneratorById(this, getOriginalFieldIds());
    }

    protected abstract String[] getOriginalFieldIds();

    public static String getBindingName(String suffix, String stateHolderKey) {
        return "#{" + PropertySheetStateBean.STATE_HOLDERS_BINDING_NAME + "['" + stateHolderKey + "']." + suffix + "}";
    }

<<<<<<< HEAD
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

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    @Override
    public String getBeanName() {
        return beanName;
    }

    @Override
<<<<<<< HEAD
    public void validate(DynamicBase dynamicObject, ValidationHelper validationHelper) {
=======
    public void validate(DocumentDynamic document, ValidationHelper validationHelper) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        // Subclasses can override
    }

    @Override
<<<<<<< HEAD
    public void save(DynamicBase dynamicObject) {
=======
    public void save(DocumentDynamic document) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
