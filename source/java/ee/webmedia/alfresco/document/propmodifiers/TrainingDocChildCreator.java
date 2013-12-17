package ee.webmedia.alfresco.document.propmodifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.DocChildAssocInfoHolder;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * Callback that creates default applicant (as childNode) for trainingApplication document
 * 
 * @author Ats Uiboupin
 */
public class TrainingDocChildCreator extends AbstractDocChildCreator {

    @Override
    public QName getAspectName() {
        return DocumentSpecificModel.Aspects.TRAINING_APPLICATION_V2;
    }

    @Override
    protected List<DocChildAssocInfoHolder> getDocChildAssocInfo(Node docNode) {
        final DocChildAssocInfoHolder doc2Applicant = new DocChildAssocInfoHolder(
                DocumentSpecificModel.Assocs.TRAINING_APPLICATION_APPLICANTS_V2,
                DocumentSpecificModel.Types.TRAINING_APPLICATION_APPLICANT_TYPE_V2,
                getMetadataBlock().setApplicantName(AuthenticationUtil.getRunAsUser(), new HashMap<String, Object>(), null));
        final ArrayList<DocChildAssocInfoHolder> result = new ArrayList<DocChildAssocInfoHolder>(1);
        result.add(doc2Applicant);
        return result;
    }

    public MetadataBlockBean getMetadataBlock() {
        return (MetadataBlockBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MetadataBlockBean.BEAN_NAME);
    }
}
