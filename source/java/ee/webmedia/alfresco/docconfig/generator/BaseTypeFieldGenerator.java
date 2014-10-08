package ee.webmedia.alfresco.docconfig.generator;

import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public abstract class BaseTypeFieldGenerator implements FieldGenerator, InitializingBean {

    private DocumentConfigService documentConfigService;

    @Override
    public void afterPropertiesSet() {
        documentConfigService.registerFieldGeneratorByType(this, getFieldTypes());
    }

    protected abstract FieldType[] getFieldTypes();

    public void setDocumentConfigService(DocumentConfigService documentConfigService) {
        this.documentConfigService = documentConfigService;
    }

}
