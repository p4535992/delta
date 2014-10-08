package ee.webmedia.alfresco.workflow.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;

/**
 * Fix invalid task outcomes.
 */
public class FixTaskOutcomesBootstrap extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        WorkflowDbService workflowDbService = BeanHelper.getWorkflowDbService();
        workflowDbService.replaceTaskOutcomes("Märgi tööülesanne teostatuks", "Tööülesanne täidetud", null);
        workflowDbService.replaceTaskOutcomes("Kinnitan", "Kinnitatud", "confirmationTask");
        workflowDbService.replaceTaskOutcomes("Ei kinnita", "Kinnitamata", "confirmationTask");
        workflowDbService.replaceTaskOutcomes("Kinnitan", "Tähtaeg pikendatud", "dueDateExtensionTask");
        workflowDbService.replaceTaskOutcomes("Ei kinnita", "Tähtaega ei pikendatud", "dueDateExtensionTask");
        workflowDbService.replaceTaskOutcomes("Kooskõlastan", "Kooskõlastatud", "externalReviewTask");
        workflowDbService.replaceTaskOutcomes("Ei kooskõlasta", "Tagasi saadetud", "externalReviewTask");
        workflowDbService.replaceTaskOutcomes("Võtan teadmiseks", "Teadmiseks võetud", "informationTask");
        workflowDbService.replaceTaskOutcomes("Kinnitan", "Arvamus antud", "opinionTask");
        workflowDbService.replaceTaskOutcomes("Ei allkirjasta", "Allkirjastamata", "signatureTask");
        workflowDbService.replaceTaskOutcomes("Allkirjastan ID-kaardiga", "Allkirjastatud", "signatureTask");
        workflowDbService.replaceTaskOutcomes("Allkirjastan Mobiil-IDga", "Allkirjastatud", "signatureTask");
    }
}
