package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.service.DocumentService.PropertiesModifierCallback;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;

public class OutgoingLetterMaaisPropertiesModifierCallback extends PropertiesModifierCallback {

    private ParametersService parametersService;

    @Override
    public QName getAspectName() {
        return DocumentSubtypeModel.Types.OUTGOING_LETTER_MAAIS;
    }

    @Override
    public void doWithProperties(Map<QName, Serializable> properties) {
        properties.put(DocumentCommonModel.Props.SIGNER_NAME, parametersService.getStringParameter(Parameters.MAAIS_OUTGOING_LETTER_SIGNER_NAME));
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

}
