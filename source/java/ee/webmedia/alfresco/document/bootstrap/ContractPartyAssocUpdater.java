package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

public class ContractPartyAssocUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentCommonModel.Types.METADATA_CONTAINER);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
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
        Map<QName, Serializable> docProps = nodeService.getProperties(docRef);
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(docRef, DocumentCommonModel.Types.METADATA_CONTAINER, RegexQNamePattern.MATCH_ALL);
        List<String> childRefs = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (ChildAssociationRef childAssociationRef : childAssocs) {
            NodeRef childRef = childAssociationRef.getChildRef();
            QName childType = nodeService.getType(childRef);
            if (!DocumentCommonModel.Types.METADATA_CONTAINER.equals(childType)) {
                info.add("childTypeIsNotMetadataContainer," + childType.toPrefixString(serviceRegistry.getNamespaceService()));
                continue;
            }
            Map<QName, Serializable> childProps = nodeService.getProperties(childRef);
            nodeService.deleteNode(childRef);
            detectProblems(childProps, childRef, docProps, sb);
            NodeRef childNodeRef = nodeService.createNode(docRef, DocumentChildModel.Assocs.CONTRACT_PARTY, DocumentChildModel.Assocs.CONTRACT_PARTY,
                    DocumentChildModel.Assocs.CONTRACT_PARTY, childProps).getChildRef();
            childRefs.add(childNodeRef.toString());
        }
        info.add("createdAndDeletedChildNodes, " + childRefs.size() + " ( " + StringUtils.join(childRefs, " ") + " )");
        String problems = sb.toString();
        if (!problems.isEmpty()) {
            info.add(problems);
        }
        return info.toArray(new String[info.size()]);
    }

    private void detectProblems(Map<QName, Serializable> childProps, NodeRef childRef, Map<QName, Serializable> docProps, StringBuilder sb) {
        if (childProps.get(DocumentAdminModel.Props.OBJECT_TYPE_ID) == null) {
            Serializable value = docProps.get(DocumentAdminModel.Props.OBJECT_TYPE_ID);
            childProps.put(DocumentAdminModel.Props.OBJECT_TYPE_ID, value);
            sb.append("objectTypeId valueWasMissing, set " + value + " ");
        }
        if (childProps.get(DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR) == null) {
            Serializable value = docProps.get(DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR);
            childProps.put(DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR, value);
            sb.append("objectTypeVersionNr valueWasMissing, set " + value + " ");
        }
        String problems = sb.toString();
        if (!problems.isEmpty()) {
            sb.append("on docChildRef=").append(childRef).append(" ");
        }
    }

}
