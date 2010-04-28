package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;

/**
 * Callback that creates default applicant (as childNode) for errandOrderAbroad document and default errand (as childNode for applicant)
 * 
 * @author Ats Uiboupin
 */
public class ErrandApplicantAbroadPropertiesModifierCallback extends AbstractDocChildCreator {

    private ParametersService parametersService;
    
    @Override
    public QName getAspectName() {
        return DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD;
    }
    
    @Override
    public void doWithProperties(Map<QName, Serializable> properties) {
        properties.put(DocumentCommonModel.Props.DOC_NAME, parametersService.getStringParameter(Parameters.DOC_PROP_ERRAND_ORDER_ABROAD_DOC_NAME));
    }

    @Override
    protected List<Pair<QName, QName>> getAssocTypesAndAssocTargetTypes() {
        final Pair<QName, QName> doc2Applicant = new Pair<QName, QName>(
                DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD,
                DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD);
        final Pair<QName, QName> applicant2Errand = new Pair<QName, QName>(
                DocumentSpecificModel.Assocs.ERRAND_ABROAD,
                DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE);
        final ArrayList<Pair<QName, QName>> result = new ArrayList<Pair<QName, QName>>(2);
        result.add(doc2Applicant);
        result.add(applicant2Errand);
        return result;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }
}
