package ee.webmedia.alfresco.document.propmodifiers;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * Callback that creates default party (as childNode) for contractSim and contractSmit document
 * 
 * @author Kaarel Jõgeva
 */

public class ContractDocChildCreator extends AbstractDocChildCreator {

    @Override
    public QName getAspectName() {
        return DocumentSpecificModel.Aspects.CONTRACT_DETAILS;
    }

    @Override
    protected List<Pair<QName, QName>> getAssocTypesAndAssocTargetTypes() {
        final Pair<QName, QName> doc2Party = new Pair<QName, QName>(
                DocumentSpecificModel.Assocs.CONTRACT_PARTIES,
                DocumentSpecificModel.Types.CONTRACT_PARTY_TYPE);
        final ArrayList<Pair<QName, QName>> result = new ArrayList<Pair<QName, QName>>(2);
        result.add(doc2Party);
        return result;
    }
    
    @Override
    protected void createChildNodes(Node docNode) {
        inMemoryChildNodeHelper.addParty(docNode);
    }

}
