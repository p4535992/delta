package ee.webmedia.alfresco.archivals.bootstrap;

import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.FUNCTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.SERIES;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.VOLUME;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyWildcardQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class FixArchivedVolumesAndCasesStructurePropertiesBootstrap extends AbstractNodeUpdater {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(FixArchivedVolumesAndCasesStructurePropertiesBootstrap.class);

    private Map<NodeRef, NodeRef> parentsCache = new HashMap<>();

    @Override
    protected void executeUpdater() throws Exception {
        super.executeUpdater();
        parentsCache = null;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(
                generateTypeQuery(CaseFileModel.Types.CASE_FILE, VolumeModel.Types.VOLUME, CaseModel.Types.CASE),
                joinQueryPartsOr(
                        generatePropertyWildcardQuery(FUNCTION, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.toString(), false, true),
                        generatePropertyWildcardQuery(SERIES, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.toString(), false, true))
                );
        List<ResultSet> result = new ArrayList<>();
        for (StoreRef store : generalService.getArchivalsStoreRefs()) {
            result.add(searchService.query(store, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        QName type = nodeService.getType(nodeRef);
        Map<QName, Serializable> structureProps = new HashMap<>();

        if (CaseModel.Types.CASE.equals(type)) {
            ChildAssociationRef caRef = nodeService.getPrimaryParent(nodeRef);
            NodeRef volumeRef = caRef.getParentRef();
            structureProps.put(VOLUME, volumeRef);
            resolveSeriesAndFunction(volumeRef, structureProps);
        } else if (CaseFileModel.Types.CASE_FILE.equals(type) || VolumeModel.Types.VOLUME.equals(type)) {
            resolveSeriesAndFunction(nodeRef, structureProps);
        } else {
            LOG.warn("Unexpected type: " + type + " for nodeRef=" + nodeRef);
            return null;
        }
        nodeService.addProperties(nodeRef, structureProps);
        return getUpdatedProps(structureProps);
    }

    private void resolveSeriesAndFunction(NodeRef volumeRef, Map<QName, Serializable> structureProps) {
        NodeRef seriesRef = getParentRef(volumeRef);
        structureProps.put(SERIES, seriesRef);
        structureProps.put(FUNCTION, getParentRef(seriesRef));
    }

    private NodeRef getParentRef(NodeRef childRef) {
        NodeRef parentRef = parentsCache.get(childRef);
        if (parentRef == null) {
            ChildAssociationRef parent = nodeService.getPrimaryParent(childRef);
            parentRef = parent.getParentRef();
            parentsCache.put(childRef, parentRef);
        }
        return parentRef;
    }

    private String[] getUpdatedProps(Map<QName, Serializable> structureProps) {
        Set<QName> updated = structureProps.keySet();
        String[] result = new String[updated.size()];
        Iterator<QName> it = updated.iterator();
        int i = 0;
        while (it.hasNext()) {
            result[i] = it.next().toString();
            i++;
        }
        return result;
    }

}
