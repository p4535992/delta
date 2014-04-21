package ee.webmedia.alfresco.document.sendout.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.model.ParametersModel;

/**
 * Update docSendOutInfo parameter value.
 */
public class DocSendOutInfoParameterUpdater extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        BeanHelper.getNodeService().setProperty(
                BeanHelper.getGeneralService().getNodeRef(Parameters.DOC_SENDOUT_INFO.toString()),
                ParametersModel.Props.Parameter.VALUE,
                "Kui sisestada meiliteate sisu, peab olema saatmisviisiks e-post või e-post/DVK");
    }
}
