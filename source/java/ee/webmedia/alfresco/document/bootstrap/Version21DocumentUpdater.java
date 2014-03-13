package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Perform document model changes that were introduced in 2.1, on existing data.
 */
public class Version21DocumentUpdater extends AbstractNodeUpdater {

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        /*
         * These four aspects find a lot of document types:
         * docsub:outgoingLetter
         * docsub:supervisionReport
         * docsub:chancellorsOrder
         * docsub:minutes
         * docsub:regulation
         * docsub:personelleOrderSmit
         * docsub:personelleOrderSim
         * docsub:decree
         * docsub:ministersOrder
         * docsub:managementsOrder
         * docsub:internalApplication
         * docsub:contractSim (both signer and contractSimDetails aspects)
         * docsub:contractSmit
         * docsub:vacationOrder
         * docsub:vacationOrderSmit
         * docsub:errandOrderAbroad
         * docsub:errandApplicationDomestic
         */
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(SearchUtil.generateAspectQuery(DocumentCommonModel.Aspects.SIGNER));
        queryParts.add(SearchUtil.generateAspectQuery(DocumentSpecificModel.Aspects.VACATION_ORDER));
        queryParts.add(SearchUtil.generateAspectQuery(DocumentSpecificModel.Aspects.CONTRACT_SIM_DETAILS));
        queryParts.add(SearchUtil.generateAspectQuery(DocumentSpecificModel.Aspects.ERRAND_DOC));
        String query = SearchUtil.joinQueryPartsOr(queryParts);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        Set<QName> aspects = nodeService.getAspects(nodeRef);

        Pair<Boolean, String> result = updateDocument(nodeRef, aspects);

        if (result.getFirst()) {
            Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
            setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
            setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
            nodeService.addProperties(nodeRef, setProps);
        }

        return new String[] { result.getSecond() };
    }

    public Pair<Boolean, String> updateDocument(NodeRef nodeRef, Set<QName> aspects) {
        boolean modified = false;
        List<String> actions = new ArrayList<String>();

        if (aspects.contains(DocumentCommonModel.Aspects.SIGNER)) {
            if (aspects.contains(DocumentCommonModel.Aspects.SIGNER_NAME)) {
                actions.add("signerNameAspectExists");
            } else {
                nodeService.addAspect(nodeRef, DocumentCommonModel.Aspects.SIGNER_NAME, null);
                actions.add("signerNameAspectAdded");
                modified = true;
            }
        }

        if (aspects.contains(DocumentSpecificModel.Aspects.VACATION_ORDER)) {
            if (aspects.contains(DocumentSpecificModel.Aspects.VACATION_ORDER_COMMON)) {
                actions.add("vacationOrderCommonAspectExists");
            } else {
                nodeService.addAspect(nodeRef, DocumentSpecificModel.Aspects.VACATION_ORDER_COMMON, null);
                actions.add("vacationOrderCommonAspectAdded");
                modified = true;
            }
        }

        if (aspects.contains(DocumentSpecificModel.Aspects.CONTRACT_SIM_DETAILS)) {
            if (aspects.contains(DocumentSpecificModel.Aspects.CONTRACT_COMMON_DETAILS)) {
                actions.add("contractCommonDetailsAspectExists");
            } else {
                nodeService.addAspect(nodeRef, DocumentSpecificModel.Aspects.CONTRACT_COMMON_DETAILS, null);
                actions.add("contractCommonDetailsAspectAdded");
                modified = true;
            }
        }

        if (aspects.contains(DocumentSpecificModel.Aspects.ERRAND_DOC)) {
            if (aspects.contains(DocumentSpecificModel.Aspects.ERRAND_DOC_COMMON)) {
                actions.add("errandDocCommonAspectExists");
            } else {
                nodeService.addAspect(nodeRef, DocumentSpecificModel.Aspects.ERRAND_DOC_COMMON, null);
                actions.add("errandDocCommonAspectAdded");
                modified = true;
            }
        }

        return new Pair<Boolean, String>(modified, StringUtils.join(actions, ','));
    }

}
