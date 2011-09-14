package ee.webmedia.alfresco.docconfig.generator;

import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.web.PropertySheetStateBean;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;

/**
 * @author Alar Kvell
 */
public abstract class BaseSystematicFieldGenerator implements FieldGenerator, SaveListener, BeanNameAware, InitializingBean {

    private DocumentConfigService documentConfigService;
    private String beanName;

    @Override
    public void afterPropertiesSet() {
        documentConfigService.registerFieldGeneratorById(this, getFieldIds());
    }

    protected abstract QName[] getFieldIds();

    protected String getStateHolderKey() {
        return getClass().getName();
    }

    protected String getBindingName(String suffix) {
        return getBindingName(suffix, getStateHolderKey());
    }

    protected String getBindingName(String suffix, String stateHolderKey) {
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

}
