package ee.webmedia.alfresco.workflow.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.model.ParametersModel;

/**
 * Update taskOwnerStructUnit parameter description.
 * 
 * @author Riina Tens
 */
public class TaskOwnerStructUnitParameterUpdater extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        BeanHelper.getNodeService().setProperty(
                BeanHelper.getGeneralService().getNodeRef(Parameters.TASK_OWNER_STRUCT_UNIT.toString()),
                ParametersModel.Props.Parameter.DESCRIPTION,
                "Käesolevale asutusele vastava kasutajagrupi nimetus. Kui parameeter on väärtustatud, siis: "
                        + "1) tööülesande saajaks on võimalik määrata ainult selliseid kasutajaid, kes kuuluvad parameetri väärtusele vastavasse kasutajagruppi; "
                        + "2) Delta kasutajaliides on piiratud nendele kasutajatele, kes ei kuulu parameetri väärtusele vastavasse kasutajagruppi.");
    }

}
