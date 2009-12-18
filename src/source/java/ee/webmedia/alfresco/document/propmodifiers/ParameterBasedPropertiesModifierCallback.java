package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentService.PropertiesModifierCallback;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;

/**
 * @author Ats Uiboupin
 */
public class ParameterBasedPropertiesModifierCallback implements PropertiesModifierCallback, InitializingBean {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ParameterBasedPropertiesModifierCallback.class);
    private DocumentService documentService;
    private ParametersService parametersService;

    @Override
    public void doWithProperties(Map<QName, Serializable> properties, NodeRef nodeRef) {
//        QName propertyQName = QName.createQName("http://alfresco.webmedia.ee/model/document/1.0", "testDate");
//        String oldValue = (String) properties.get(propertyQName);
//        log.debug("Value of "+propertyQName+" was '"+oldValue+"'");
//        Long dvkRetainPeriod = parametersService.getLongParameter(Parameters.DVK_RETAIN_PERIOD);
//        Calendar dvkRetainPeriodDeadLine = Calendar.getInstance();
//        dvkRetainPeriodDeadLine.add(Calendar.DAY_OF_MONTH, dvkRetainPeriod.intValue());
//        properties.put(propertyQName, dvkRetainPeriodDeadLine.getTime());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
//        documentService.addPropertiesModifierCallback(QName.createQName("http://alfresco.webmedia.ee/model/document/1.0", "test"), this);
    }

    // START: getters/setters
    @Override
    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }
    // END: getters/setters

}
