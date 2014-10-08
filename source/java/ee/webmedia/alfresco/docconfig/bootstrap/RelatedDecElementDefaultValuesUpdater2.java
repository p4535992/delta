package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.utils.TextUtil;

/**
 * This class removes the {@code DocumentAdminModel.Props.RELATED_OUTGOING_DEC_ELEMENT} property value {@code <RecordCreator><ContactData><Email>} from
 * all {@link Field} and {@link FieldDefinition} nodes
 */
public class RelatedDecElementDefaultValuesUpdater2 extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsOr(generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION), generateTypeQuery(DocumentAdminModel.Types.FIELD));
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        List<String> relatedOutgoingDecElements = (List<String>) nodeService.getProperty(nodeRef, DocumentAdminModel.Props.RELATED_OUTGOING_DEC_ELEMENT);
        if (CollectionUtils.isNotEmpty(relatedOutgoingDecElements)) {
            List<String> updatedOutgoingDecElements = new ArrayList<String>();
            for (String element : relatedOutgoingDecElements) {
                if (StringUtils.isBlank(element)
                        || "<RecordCreator><ContactData><Email>".equalsIgnoreCase(element)
                        || "<RecordCreator><Organisation><Name>".equalsIgnoreCase(element)
                        || "<RecordSenderToDec><Organisation><Name>".equalsIgnoreCase(element)) {
                    continue;
                }
                updatedOutgoingDecElements.add(element);
            }
            if (updatedOutgoingDecElements.size() > 0) {
                nodeService.setProperty(nodeRef, DocumentAdminModel.Props.RELATED_OUTGOING_DEC_ELEMENT, (Serializable) updatedOutgoingDecElements);
            } else {
                nodeService.removeProperty(nodeRef, DocumentAdminModel.Props.RELATED_OUTGOING_DEC_ELEMENT);
            }
            boolean updated = CollectionUtils.isEqualCollection(relatedOutgoingDecElements, updatedOutgoingDecElements);
            return new String[] { TextUtil.joinNonBlankStrings(relatedOutgoingDecElements, ", "), updated ? TextUtil.joinNonBlankStrings(updatedOutgoingDecElements, ", ") : "null" };
        }
        return null;
    }
}
