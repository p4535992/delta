package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.DocChildAssocInfoHolder;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;

/**
 * Callback that creates default applicant (as childNode) for errandOrderAbroad document and default errand (as childNode for applicant)
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> develop-5.1
 */
public class ErrandApplicantAbroadPropertiesModifierCallback extends AbstractDocChildCreator {

    private ParametersService parametersService;

    @Override
    public QName getAspectName() {
        return DocumentSpecificModel.Aspects.ERRAND_ORDER_ABROAD_V2;
    }

    @Override
    public void doWithProperties(Map<QName, Serializable> properties) {
        properties.put(DocumentCommonModel.Props.DOC_NAME, parametersService.getStringParameter(Parameters.DOC_PROP_ERRAND_ORDER_ABROAD_DOC_NAME));
    }

    @Override
    protected List<DocChildAssocInfoHolder> getDocChildAssocInfo(Node docNode) {
        final DocChildAssocInfoHolder doc2Applicant = new DocChildAssocInfoHolder(
                DocumentSpecificModel.Assocs.ERRAND_ORDER_APPLICANTS_ABROAD_V2,
                DocumentSpecificModel.Types.ERRAND_ORDER_APPLICANT_ABROAD_V2,
                getMetadataBlock().setApplicantName(AuthenticationUtil.getRunAsUser(), new HashMap<String, Object>(),
                        new HashSet<QName>(Arrays.asList(DocumentSpecificModel.Aspects.ERRAND_ORDER_APPLICANT_ABROAD_V2))));
        final DocChildAssocInfoHolder applicant2Errand = new DocChildAssocInfoHolder(
                DocumentSpecificModel.Assocs.ERRAND_ABROAD_V2,
                DocumentSpecificModel.Types.ERRAND_ABROAD_TYPE_V2);

        final ArrayList<DocChildAssocInfoHolder> result = new ArrayList<DocChildAssocInfoHolder>(2);
        result.add(doc2Applicant);
        result.add(applicant2Errand);
        return result;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public MetadataBlockBean getMetadataBlock() {
        return (MetadataBlockBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MetadataBlockBean.BEAN_NAME);
    }
}
