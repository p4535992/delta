package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * Callback that creates default applicant (as childNode) for errandOrderAbroadMv document and default errand (as childNode for applicant)
 * 
 * @author Ats Uiboupin
 */
public class ErrandAbroadMvApplicantPropertiesModifierCallback extends AbstractDocChildCreator {
    
    @Override
    public QName getAspectName() {
        return DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_MV;
    }
    
    @Override
    public void doWithProperties(Map<QName, Serializable> properties) {
    }

    @Override
    protected List<Pair<QName, QName>> getAssocTypesAndAssocTargetTypes() {
        final Pair<QName, QName> doc2Applicant = new Pair<QName, QName>(
                DocumentSpecificModel.Assocs.ERRAND_ORDER_ABROAD_MV_APPLICANTS,
                DocumentSpecificModel.Types.ERRAND_ORDER_ABROAD_MV_APPLICANT_MV);
        final Pair<QName, QName> applicant2Errand = new Pair<QName, QName>(
                DocumentSpecificModel.Assocs.ERRAND_ABROAD_MV,
                DocumentSpecificModel.Types.ERRAND_ABROAD_MV_TYPE);
        final ArrayList<Pair<QName, QName>> result = new ArrayList<Pair<QName, QName>>(2);
        result.add(doc2Applicant);
        result.add(applicant2Errand);
        return result;
    }

}