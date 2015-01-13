package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.service.DocumentService.PropertiesModifierCallback;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;

public class ContractMvPropertiesModifierCallback extends PropertiesModifierCallback {

    private ParametersService parametersService;

    @Override
    public QName getAspectName() {
        return DocumentSubtypeModel.Types.CONTRACT_MV;
    }

    @Override
    public void doWithProperties(Map<QName, Serializable> properties) {
        properties.put(DocumentCommonModel.Props.SIGNER_NAME, parametersService.getStringParameter(Parameters.SIGNER_NAME));
        properties.put(DocumentSpecificModel.Props.FIRST_PARTY_NAME, parametersService.getStringParameter(Parameters.CONTRACT_FIRST_PARTY_NAME));
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

}
