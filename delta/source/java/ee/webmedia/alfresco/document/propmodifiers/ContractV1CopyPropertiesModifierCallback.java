package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * Callback that creates  parties from V1 data to V2 document
 * 
 * @author Kaarel Jõgeva
 */

public class ContractV1CopyPropertiesModifierCallback extends AbstractDocChildCreator {

    @Override
    public QName getAspectName() {
        return DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1;
    }
    
    @Override
    public void doWithNode(Node docNode, String phase) {
        NodeRef nodeRef = docNode.getNodeRef();
        Map<String, Object> properties = docNode.getProperties();
        Pair<QName, QName> assocTypeAndAssocTargetType = getAssocTypesAndAssocTargetTypes().get(0);

        if(properties.containsKey(DocumentSpecificModel.Props.SECOND_PARTY_NAME.toString())) {
            Map<QName, Serializable> partyProps = new HashMap<QName, Serializable>(4);
            partyProps.put(DocumentSpecificModel.Props.PARTY_NAME, (Serializable) properties.get(DocumentSpecificModel.Props.SECOND_PARTY_NAME));
            partyProps.put(DocumentSpecificModel.Props.PARTY_EMAIL, (Serializable) properties.get(DocumentSpecificModel.Props.SECOND_PARTY_EMAIL));
            partyProps.put(DocumentSpecificModel.Props.PARTY_SIGNER, (Serializable) properties.get(DocumentSpecificModel.Props.SECOND_PARTY_SIGNER));
            partyProps.put(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON, (Serializable) properties.get(DocumentSpecificModel.Props.SECOND_PARTY_CONTACT_PERSON));
            
            createNode(nodeRef, assocTypeAndAssocTargetType, partyProps);
        }
        
        if(properties.containsKey(DocumentSpecificModel.Props.THIRD_PARTY_NAME.toString())) {
            Map<QName, Serializable> partyProps = new HashMap<QName, Serializable>(4);
            partyProps.put(DocumentSpecificModel.Props.PARTY_NAME, (Serializable) properties.get(DocumentSpecificModel.Props.THIRD_PARTY_NAME));
            partyProps.put(DocumentSpecificModel.Props.PARTY_EMAIL, (Serializable) properties.get(DocumentSpecificModel.Props.THIRD_PARTY_EMAIL));
            partyProps.put(DocumentSpecificModel.Props.PARTY_SIGNER, (Serializable) properties.get(DocumentSpecificModel.Props.THIRD_PARTY_SIGNER));
            partyProps.put(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON, (Serializable) properties.get(DocumentSpecificModel.Props.THIRD_PARTY_CONTACT_PERSON));
            
            createNode(nodeRef, assocTypeAndAssocTargetType, partyProps);
        }

        // Also removes properties
        nodeService.removeAspect(nodeRef, DocumentSpecificModel.Aspects.CONTRACT_DETAILS_V1);
    }

    @Override
    protected List<Pair<QName, QName>> getAssocTypesAndAssocTargetTypes() {
        final Pair<QName, QName> doc2Party = new Pair<QName, QName>(
                DocumentSpecificModel.Assocs.CONTRACT_PARTIES,
                DocumentSpecificModel.Types.CONTRACT_PARTY_TYPE);
        final ArrayList<Pair<QName, QName>> result = new ArrayList<Pair<QName, QName>>(1);
        result.add(doc2Party);
        return result;
    }
    
    @Override
    protected void createChildNodes(Node docNode) {
        // This is handled by doWithNode
    }

}
