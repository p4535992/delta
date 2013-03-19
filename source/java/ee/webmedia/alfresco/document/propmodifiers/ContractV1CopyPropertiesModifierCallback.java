package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocChildAssocInfoHolder;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * Callback that creates parties from V1 data to V2 document
 * 
 * @author Kaarel Jõgeva
 */

public class ContractV1CopyPropertiesModifierCallback extends AbstractDocChildCreator {

    @Override
    public QName getAspectName() {
        return DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1;
    }

    @Override
    protected List<DocChildAssocInfoHolder> getDocChildAssocInfo(Node docNode) {
        final ArrayList<DocChildAssocInfoHolder> result = new ArrayList<DocChildAssocInfoHolder>(2);
        final DocChildAssocInfoHolder doc2Party = new DocChildAssocInfoHolder(
                DocumentSpecificModel.Assocs.CONTRACT_PARTIES,
                DocumentSpecificModel.Types.CONTRACT_PARTY_TYPE);

        Map<String, Object> properties = docNode.getProperties();
        if (properties.containsKey(DocumentSpecificModel.Props.SECOND_PARTY_NAME.toString())) {
            Map<QName, Serializable> partyProps = new HashMap<QName, Serializable>(4);
            partyProps.put(DocumentSpecificModel.Props.PARTY_NAME, (Serializable) properties.get(DocumentSpecificModel.Props.SECOND_PARTY_NAME));
            partyProps.put(DocumentSpecificModel.Props.PARTY_EMAIL, (Serializable) properties.get(DocumentSpecificModel.Props.SECOND_PARTY_EMAIL));
            partyProps.put(DocumentSpecificModel.Props.PARTY_SIGNER, (Serializable) properties.get(DocumentSpecificModel.Props.SECOND_PARTY_SIGNER));
            partyProps.put(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON,
                    (Serializable) properties.get(DocumentSpecificModel.Props.SECOND_PARTY_CONTACT_PERSON));
            doc2Party.setProperties(partyProps);
        }
        result.add(doc2Party);

        if (properties.containsKey(DocumentSpecificModel.Props.THIRD_PARTY_NAME.toString())) {
            final DocChildAssocInfoHolder doc2Party2 = new DocChildAssocInfoHolder(
                    DocumentSpecificModel.Assocs.CONTRACT_PARTIES,
                    DocumentSpecificModel.Types.CONTRACT_PARTY_TYPE);

            Map<QName, Serializable> partyProps = new HashMap<QName, Serializable>(4);
            partyProps.put(DocumentSpecificModel.Props.PARTY_NAME, (Serializable) properties.get(DocumentSpecificModel.Props.THIRD_PARTY_NAME));
            partyProps.put(DocumentSpecificModel.Props.PARTY_EMAIL, (Serializable) properties.get(DocumentSpecificModel.Props.THIRD_PARTY_EMAIL));
            partyProps.put(DocumentSpecificModel.Props.PARTY_SIGNER, (Serializable) properties.get(DocumentSpecificModel.Props.THIRD_PARTY_SIGNER));
            partyProps.put(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON,
                    (Serializable) properties.get(DocumentSpecificModel.Props.THIRD_PARTY_CONTACT_PERSON));

            doc2Party2.setProperties(partyProps);
            result.add(doc2Party2);
        }

        // Remove properties when we have transferred them to child nodes
        nodeService.removeAspect(docNode.getNodeRef(), DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1);

        return result;
    }

    @Override
    protected void createChildNodes(Node docNode) {
        // This is handled by doWithNode
    }

}
