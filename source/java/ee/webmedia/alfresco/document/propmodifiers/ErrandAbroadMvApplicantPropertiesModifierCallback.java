<<<<<<< HEAD
package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocChildAssocInfoHolder;
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
    protected List<DocChildAssocInfoHolder> getDocChildAssocInfo(Node docNode) {
        final DocChildAssocInfoHolder doc2Applicant = new DocChildAssocInfoHolder(
                DocumentSpecificModel.Assocs.ERRAND_ORDER_ABROAD_MV_APPLICANTS,
                DocumentSpecificModel.Types.ERRAND_ORDER_ABROAD_MV_APPLICANT_MV);
        final DocChildAssocInfoHolder applicant2Errand = new DocChildAssocInfoHolder(
                DocumentSpecificModel.Assocs.ERRAND_ABROAD_MV,
                DocumentSpecificModel.Types.ERRAND_ABROAD_MV_TYPE);
        final ArrayList<DocChildAssocInfoHolder> result = new ArrayList<DocChildAssocInfoHolder>(2);
        result.add(doc2Applicant);
        result.add(applicant2Errand);
        return result;
    }

=======
package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocChildAssocInfoHolder;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * Callback that creates default applicant (as childNode) for errandOrderAbroadMv document and default errand (as childNode for applicant)
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
    protected List<DocChildAssocInfoHolder> getDocChildAssocInfo(Node docNode) {
        final DocChildAssocInfoHolder doc2Applicant = new DocChildAssocInfoHolder(
                DocumentSpecificModel.Assocs.ERRAND_ORDER_ABROAD_MV_APPLICANTS,
                DocumentSpecificModel.Types.ERRAND_ORDER_ABROAD_MV_APPLICANT_MV);
        final DocChildAssocInfoHolder applicant2Errand = new DocChildAssocInfoHolder(
                DocumentSpecificModel.Assocs.ERRAND_ABROAD_MV,
                DocumentSpecificModel.Types.ERRAND_ABROAD_MV_TYPE);
        final ArrayList<DocChildAssocInfoHolder> result = new ArrayList<DocChildAssocInfoHolder>(2);
        result.add(doc2Applicant);
        result.add(applicant2Errand);
        return result;
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}