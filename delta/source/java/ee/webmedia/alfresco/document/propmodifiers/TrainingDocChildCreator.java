package ee.webmedia.alfresco.document.propmodifiers;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * Callback that creates default applicant (as childNode) for trainingApplication document
 * 
 * @author Ats Uiboupin
 */
public class TrainingDocChildCreator extends AbstractDocChildCreator {

    @Override
    public QName getAspectName() {
        return DocumentSpecificModel.Aspects.TRAINING_APPLICATION;
    }

    @Override
    protected List<Pair<QName, QName>> getAssocTypesAndAssocTargetTypes() {
        final Pair<QName, QName> doc2Applicant = new Pair<QName, QName>(
                DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS,
                DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE);
        final ArrayList<Pair<QName, QName>> result = new ArrayList<Pair<QName, QName>>(2);
        result.add(doc2Applicant);
        return result;
    }

}
