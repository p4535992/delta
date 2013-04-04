package ee.webmedia.alfresco.document.type.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.type.model.DocumentTypeModel;

public class ChangeMinistersOrderNameBootstrap extends AbstractModuleComponent {

    private GeneralService generalService;

    @Override
    protected void executeInternal() throws Throwable {
        String xPath = DocumentTypeModel.Repo.DOCUMENT_TYPES_SPACE + "/docsub:ministersOrder";
        NodeRef nodeRef = generalService.getNodeRef(xPath);
        serviceRegistry.getNodeService().setProperty(nodeRef, DocumentTypeModel.Props.NAME, "Üldkäskkiri");
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

}
