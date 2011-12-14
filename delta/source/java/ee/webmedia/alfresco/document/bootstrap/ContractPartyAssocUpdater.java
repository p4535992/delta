package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * @author Alar Kvell
 */
public class ContractPartyAssocUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentCommonModel.Types.METADATA_CONTAINER);
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query),
                searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        QName type = nodeService.getType(nodeRef);
        if (!DocumentCommonModel.Types.METADATA_CONTAINER.equals(type)) {
            return new String[] { "typeIsNotMetadataContainer," + type.toPrefixString(serviceRegistry.getNamespaceService()) };
        }
        NodeRef docRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
        QName docType = nodeService.getType(docRef);
        if (!DocumentCommonModel.Types.DOCUMENT.equals(docType)) {
            return new String[] { "parentTypeIsNotDocument," + docType.toPrefixString(serviceRegistry.getNamespaceService()) };
        }
        List<String> info = new ArrayList<String>();
        if (nodeService.hasAspect(docRef, DocumentChildModel.Aspects.CONTRACT_PARTY_CONTAINER)) {
            nodeService.addAspect(docRef, DocumentChildModel.Aspects.CONTRACT_PARTY_CONTAINER, null);
            info.add("addedContractPartyContainerAspect");
        }
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(docRef, DocumentCommonModel.Types.METADATA_CONTAINER, RegexQNamePattern.MATCH_ALL);
        int i = 0;
        for (ChildAssociationRef childAssociationRef : childAssocs) {
            NodeRef childRef = childAssociationRef.getChildRef();
            QName childType = nodeService.getType(childRef);
            if (!DocumentCommonModel.Types.METADATA_CONTAINER.equals(childType)) {
                info.add("childTypeIsNotMetadataContainer," + childType.toPrefixString(serviceRegistry.getNamespaceService()));
                continue;
            }
            Map<QName, Serializable> props = nodeService.getProperties(childRef);
            nodeService.createNode(docRef, DocumentChildModel.Assocs.CONTRACT_PARTY, DocumentChildModel.Assocs.CONTRACT_PARTY, DocumentChildModel.Assocs.CONTRACT_PARTY, props);
            nodeService.deleteNode(childRef);
            i++;
        }
        info.add("createdAndDeletedChildNodes," + i);
        return info.toArray(new String[info.size()]);
    }

}
