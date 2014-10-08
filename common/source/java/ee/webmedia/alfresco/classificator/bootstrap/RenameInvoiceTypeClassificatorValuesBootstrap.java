package ee.webmedia.alfresco.classificator.bootstrap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.classificator.model.ClassificatorModel;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.einvoice.model.InvoiceType;

/**
 * Switch classificator invoiceTypes values' valueName and classificatorDescription properties values
 */
public class RenameInvoiceTypeClassificatorValuesBootstrap extends AbstractModuleComponent {
    private static final String INVOICE_TYPE_CLASSIFICATOR_PATH = "cl:classificators/cl:invoiceTypes";
    private GeneralService generalService;
    private NodeService nodeService;

    @Override
    protected void executeInternal() throws Throwable {
        final NodeRef nodeRef = generalService.getNodeRef(INVOICE_TYPE_CLASSIFICATOR_PATH);
        if (nodeRef != null) {
            List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef);
            for (ChildAssociationRef childAssocRef : childAssocs) {
                NodeRef classificatorValueRef = childAssocRef.getChildRef();
                Map<QName, Serializable> clValueProps = nodeService.getProperties(classificatorValueRef);
                InvoiceType invoiceType = InvoiceType.getInvoiceTypeByComment((String) clValueProps.get(ClassificatorModel.Props.CL_VALUE_NAME));
                if (invoiceType != null) {
                    clValueProps.put(ClassificatorModel.Props.CL_VALUE_NAME, invoiceType.getValue());
                    clValueProps.put(ClassificatorModel.Props.CL_VALUE_DESCRIPTION, invoiceType.getComment());
                    nodeService.addProperties(classificatorValueRef, clValueProps);
                }
            }
        }
    }

    // START: getters / setters

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    // END: getters / setters
}
