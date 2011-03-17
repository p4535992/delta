package ee.webmedia.alfresco.document.propmodifiers;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * Callback that creates default party (as childNode) for contractMv document
 * 
 * @author Riina Tens
 */

public class ContractMvDocChildCreator extends ContractDocChildCreator {

    @Override
    public QName getAspectName() {
        return DocumentSpecificModel.Aspects.CONTRACT_MV_DETAILS;
    }

    @Override
    protected List<Pair<QName, QName>> getAssocTypesAndAssocTargetTypes() {
        final Pair<QName, QName> doc2Party = new Pair<QName, QName>(
                DocumentSpecificModel.Assocs.CONTRACT_MV_PARTIES,
                DocumentSpecificModel.Types.CONTRACT_MV_PARTY_TYPE);
        final ArrayList<Pair<QName, QName>> result = new ArrayList<Pair<QName, QName>>(2);
        result.add(doc2Party);
        return result;
    }
}
