package ee.webmedia.alfresco.archivals.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ObjectUtils;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * Corrects docdyn:function/series/volume/case property values for archived documents.
 * Only needed for existing 3.11 and 3.13 installations; not needed for migrating from older version installations, but does not hurt correctness either.
 * 
 * @author Alar Kvell
 */
public class DocumentArchivalLocationPropsUpdater extends AbstractNodeUpdater {

    private DocumentService documentService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getArchivalsStoreRefs()) { // Exclude workspace://SpacesStore
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef docRef) throws Exception {
        QName type = nodeService.getType(docRef);
        if (!DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            return new String[] { "skipped", "isNotDocument",
                    type.toPrefixString(serviceRegistry.getNamespaceService()) };
        }
        NodeRef parentRef = nodeService.getPrimaryParent(docRef).getParentRef();
        QName parentType = nodeService.getType(parentRef);
        Map<QName, Serializable> props = nodeService.getProperties(docRef);
        if (VolumeModel.Types.VOLUME.equals(parentType) || CaseFileModel.Types.CASE_FILE.equals(parentType)) {
            if (parentRef.equals(props.get(DocumentCommonModel.Props.VOLUME)) && null == props.get(DocumentCommonModel.Props.CASE)) {
                return new String[] { "skippedAndIsUnderVolumeOrCaseFile",
                        parentType.toPrefixString(serviceRegistry.getNamespaceService()) };
            }
        } else if (CaseModel.Types.CASE.equals(parentType)) {
            if (parentRef.equals(props.get(DocumentCommonModel.Props.CASE))) {
                return new String[] { "skippedAndIsUnderCase",
                        parentType.toPrefixString(serviceRegistry.getNamespaceService()) };
            }
        } else {
            return new String[] { "skippedAndIsUnderOther",
                    parentType.toPrefixString(serviceRegistry.getNamespaceService()) };
        }

        DocumentParentNodesVO ancestorNodes = documentService.getAncestorNodesByDocument(docRef);
        Map<QName, Serializable> newProps = new HashMap<QName, Serializable>();
        newProps.put(DocumentCommonModel.Props.FUNCTION, ancestorNodes.getFunctionNode().getNodeRef());
        newProps.put(DocumentCommonModel.Props.SERIES, ancestorNodes.getSeriesNode().getNodeRef());
        newProps.put(DocumentCommonModel.Props.VOLUME, ancestorNodes.getVolumeNode().getNodeRef());
        newProps.put(DocumentCommonModel.Props.CASE, ancestorNodes.getCaseNode() == null ? null : ancestorNodes.getCaseNode().getNodeRef());
        nodeService.addProperties(docRef, newProps);
        return new String[] { "updated",
                parentType.toPrefixString(serviceRegistry.getNamespaceService()),
                ObjectUtils.toString(props.get(DocumentCommonModel.Props.FUNCTION)),
                ObjectUtils.toString(props.get(DocumentCommonModel.Props.SERIES)),
                ObjectUtils.toString(props.get(DocumentCommonModel.Props.VOLUME)),
                ObjectUtils.toString(props.get(DocumentCommonModel.Props.CASE)),
                ObjectUtils.toString(newProps.get(DocumentCommonModel.Props.FUNCTION)),
                ObjectUtils.toString(newProps.get(DocumentCommonModel.Props.SERIES)),
                ObjectUtils.toString(newProps.get(DocumentCommonModel.Props.VOLUME)),
                ObjectUtils.toString(newProps.get(DocumentCommonModel.Props.CASE)), };
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

}
