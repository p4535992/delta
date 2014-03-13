package ee.webmedia.alfresco.series.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * CL 204311
 */
public class SeriesRetentionPropsRemoveUpdater extends AbstractNodeUpdater {

    private static final QName RETENTION_DESC = QName.createQName(SeriesModel.URI, "retentionDesc");
    private static final QName RETENTION_PERIOD = QName.createQName(SeriesModel.URI, "retentionPeriod");

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(SeriesModel.Types.SERIES);

        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) {
        Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
        String description = StringUtils.defaultString((String) props.get(SeriesModel.Props.DESCRIPTION));

        nodeService.removeProperty(nodeRef, RETENTION_PERIOD);
        nodeService.removeProperty(nodeRef, RETENTION_DESC);

        Long retentionPeriod = (Long) props.get(RETENTION_PERIOD);
        if (retentionPeriod != null && retentionPeriod > 0) {
            String line = "Säilitustähtaeg (vana): " + retentionPeriod;
            if (StringUtils.isNotBlank(description)) {
                line = "\n" + line;
            }
            description += line;
        }

        String retentionDesc = (String) props.get(RETENTION_DESC);
        if (StringUtils.isNotBlank(retentionDesc)) {
            String line = "Elukäigu ajakava (vana): " + retentionDesc;
            if (StringUtils.isNotBlank(description)) {
                line = "\n" + line;
            }
            description += line;
        }

        if (StringUtils.isNotBlank(description)) {
            nodeService.setProperty(nodeRef, SeriesModel.Props.DESCRIPTION, description);
        }

        return new String[] {
                (String) props.get(SeriesModel.Props.SERIES_IDENTIFIER),
                (String) props.get(SeriesModel.Props.TITLE),
                ObjectUtils.toString(props.get(RETENTION_PERIOD)),
                ObjectUtils.toString(props.get(RETENTION_DESC)) };
    }

}
