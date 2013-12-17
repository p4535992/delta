package ee.webmedia.alfresco.docdynamic.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Alar Kvell
 */
public class DocumentRegNumbersUpdater extends AbstractNodeUpdater {

    private DocumentUpdater documentUpdater;

    @Override
    protected boolean usePreviousState() {
        return false;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        Assert.notNull(documentUpdater.getDocumentRegNumbers());

        String query = joinQueryPartsOr(
                generateTypeQuery(VolumeModel.Types.VOLUME),
                generateTypeQuery(CaseModel.Types.CASE));
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        List<String> documentRegNumbers = documentUpdater.getDocumentRegNumbers().get(nodeRef);
        if (nodeService.hasAspect(nodeRef, DocumentCommonModel.Aspects.DOCUMENT_REG_NUMBERS_CONTAINER)) {
            nodeService.setProperty(nodeRef, DocumentCommonModel.Props.DOCUMENT_REG_NUMBERS, (Serializable) documentRegNumbers);
        } else {
            HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(DocumentCommonModel.Props.DOCUMENT_REG_NUMBERS, (Serializable) documentRegNumbers);
            nodeService.addAspect(nodeRef, DocumentCommonModel.Aspects.DOCUMENT_REG_NUMBERS_CONTAINER, props);
        }
        return new String[] { documentRegNumbers == null ? "null" : Integer.toString(documentRegNumbers.size()) };
    }

    public void setDocumentUpdater(DocumentUpdater documentUpdater) {
        this.documentUpdater = documentUpdater;
    }

}
