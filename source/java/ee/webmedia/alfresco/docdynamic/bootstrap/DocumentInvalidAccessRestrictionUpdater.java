package ee.webmedia.alfresco.docdynamic.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * @author Alar Kvell
 */
public class DocumentInvalidAccessRestrictionUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateAndNotQuery(
                SearchUtil.joinQueryPartsAnd(
                        SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                        SearchUtil.generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE)
                        ),
                SearchUtil.joinQueryPartsOr(
                        SearchUtil.generateStringExactQuery(AccessRestriction.OPEN.getValueName(), DocumentCommonModel.Props.ACCESS_RESTRICTION),
                        SearchUtil.generateStringExactQuery(AccessRestriction.AK.getValueName(), DocumentCommonModel.Props.ACCESS_RESTRICTION),
                        SearchUtil.generateStringExactQuery(AccessRestriction.INTERNAL.getValueName(), DocumentCommonModel.Props.ACCESS_RESTRICTION),
                        SearchUtil.generateStringExactQuery(AccessRestriction.LIMITED.getValueName(), DocumentCommonModel.Props.ACCESS_RESTRICTION)
                        )
                );
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        QName type = nodeService.getType(nodeRef);
        if (!DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            return new String[] { "isNotDocumentType", type.toPrefixString(serviceRegistry.getNamespaceService()) };
        }
        Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
        String action;
        String accessRestriction = (String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
        String newAccessRestriction = accessRestriction;
        Date regDateTime = (Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME);
        String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
        String docName = (String) props.get(DocumentCommonModel.Props.DOC_NAME);
        if (!nodeService.hasAspect(nodeRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
            action = "doesNotHaveSearchableAspect";
        } else if (AccessRestriction.OPEN.getValueName().equals(accessRestriction)
                || AccessRestriction.AK.getValueName().equals(accessRestriction)
                || AccessRestriction.INTERNAL.getValueName().equals(accessRestriction)
                || AccessRestriction.LIMITED.getValueName().equals(accessRestriction)) {
            action = "validAndUnchanged";
        } else if ("Asutusesiseseks kasutamiseks".equalsIgnoreCase(StringUtils.strip(accessRestriction))) {
            newAccessRestriction = AccessRestriction.AK.getValueName();
            nodeService.setProperty(nodeRef, DocumentCommonModel.Props.ACCESS_RESTRICTION, newAccessRestriction);
            action = "notValidAndChangedToAK";
        } else {
            action = "notValidAndUnchanged";
        }
        return new String[] { action,
                type.toPrefixString(serviceRegistry.getNamespaceService()),
                accessRestriction,
                newAccessRestriction,
                regDateTime == null ? null : dateFormat.format(regDateTime),
                regNumber,
                docName };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "nodeRef", "action", "type", "oldAccessRestriction", "newAccessRestriction", "regDate", "regNumber", "docName" };
    }

}
