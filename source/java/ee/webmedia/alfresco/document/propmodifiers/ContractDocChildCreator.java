package ee.webmedia.alfresco.document.propmodifiers;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocChildAssocInfoHolder;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * Callback that creates default party (as childNode) for contractSim and contractSmit document
 */

public class ContractDocChildCreator extends AbstractDocChildCreator {

    @Override
    public QName getAspectName() {
        return DocumentSpecificModel.Aspects.CONTRACT_DETAILS;
    }

    @Override
    protected List<DocChildAssocInfoHolder> getDocChildAssocInfo(Node docNode) {
        final DocChildAssocInfoHolder doc2Party = new DocChildAssocInfoHolder(
                DocumentSpecificModel.Assocs.CONTRACT_PARTIES,
                DocumentSpecificModel.Types.CONTRACT_PARTY_TYPE);
        final ArrayList<DocChildAssocInfoHolder> result = new ArrayList<DocChildAssocInfoHolder>(1);
        result.add(doc2Party);
        return result;
    }

    @Override
    protected void createChildNodes(Node docNode) {
        inMemoryChildNodeHelper.addParty(docNode);
    }

}
