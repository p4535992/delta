package ee.webmedia.alfresco.docadmin.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Update properties of type STRUCT_UNIT to multivalued properties (String -> List<String>)
 * 
 * @author Riina Tens
 */
public class StructUnitPropertiesToMultivaluedUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        // All documents have structUnit field in owner group,
        // so there's no point in making more specific query by orgStruct properties
        String query = SearchUtil.joinQueryPartsAnd(
                SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                SearchUtil.generateAspectQuery(DocumentAdminModel.Aspects.OBJECT)
                );
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query),
                searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        List<String> resultLog = new ArrayList<String>();
        Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
        Map<QName, Serializable> updatedProps = new HashMap<QName, Serializable>();
        for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
            QName propQName = entry.getKey();
            if (propQName.getLocalName().contains(StructUnitFieldTypeUpdater.ORG_STRUCT_UNIT)) {
                Serializable currentValue = entry.getValue();
                if (!(currentValue instanceof List)) {
                    List<String> newValue = new ArrayList<String>();
                    if (StringUtils.isNotBlank((String) currentValue)) {
                        newValue.add((String) currentValue);
                    }
                    updatedProps.put(propQName, (Serializable) newValue);
                    resultLog.add(propQName.toString());
                }
            }
        }
        nodeService.addProperties(nodeRef, updatedProps);
        return new String[] { StringUtils.join(resultLog, ", ") };
    }
}
