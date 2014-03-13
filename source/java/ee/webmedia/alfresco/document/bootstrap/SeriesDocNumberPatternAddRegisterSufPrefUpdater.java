package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.register.model.RegisterModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.numberpattern.NumberPatternParser.RegisterNumberPatternParams;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Adds to docNumberPattern register prefix and suffix if they exist.
 */
public class SeriesDocNumberPatternAddRegisterSufPrefUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(SeriesModel.Types.SERIES),
                SearchUtil.generatePropertyWildcardQuery(SeriesModel.Props.DOC_NUMBER_PATTERN, RegisterNumberPatternParams.DN.name(), true, true)
                ));
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) {
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        if (origProps.get(SeriesModel.Props.REGISTER) == null) {
            return new String[] { "series has no register property" };
        }
        if (origProps.get(SeriesModel.Props.DOC_NUMBER_PATTERN) == null) {
            return new String[] { "series document number pattern is null" };
        }
        Map<QName, Serializable> newProps = new HashMap<QName, Serializable>(3);
        StringBuilder sb = new StringBuilder();

        String origDocNumberPattern = (String) origProps.get(SeriesModel.Props.DOC_NUMBER_PATTERN);
        String docNumberPatternPartToChange = null;

        int registerId = (Integer) origProps.get(SeriesModel.Props.REGISTER);
        Node registerNode = BeanHelper.getRegisterService().getRegisterNode(registerId);
        QName prefixPropKey = QName.createQName(RegisterModel.URI, "prefix");
        QName suffixPropKey = QName.createQName(RegisterModel.URI, "suffix");
        String[] parts = StringUtils.split(origDocNumberPattern, "{");
        for (String part : parts) {
            if (StringUtils.contains(part, RegisterNumberPatternParams.DN.name())) {
                docNumberPatternPartToChange = "{" + part;
            }
        }
        if (registerNode.hasProperty(prefixPropKey.toString())) {
            Object prefix = registerNode.getProperties().get(prefixPropKey);
            if (prefix != null) {
                sb.append((String) prefix);
            }
            nodeService.removeProperty(registerNode.getNodeRef(), prefixPropKey);
        }
        sb.append(docNumberPatternPartToChange);
        if (registerNode.hasProperty(suffixPropKey.toString())) {
            Object suffix = registerNode.getProperties().get(suffixPropKey);
            if (suffix != null) {
                sb.append((String) suffix);
            }
            nodeService.removeProperty(registerNode.getNodeRef(), suffixPropKey);
        }
        String docNumberPattern = StringUtils.replace(origDocNumberPattern, docNumberPatternPartToChange, sb.toString());

        newProps.put(SeriesModel.Props.DOC_NUMBER_PATTERN, docNumberPattern);
        newProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        newProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, newProps);
        return new String[] { "new DocRegNumberPattern: " + docNumberPattern };
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

}
