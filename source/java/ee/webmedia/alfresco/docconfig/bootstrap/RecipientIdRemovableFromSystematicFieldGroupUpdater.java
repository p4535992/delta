package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;

/**
 * This class changes <code>removableFromSystematicFieldGroup</code> values to false in fields <code>recipientId<code> and <code>additionalRecipientId<code>
 */
public class RecipientIdRemovableFromSystematicFieldGroupUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(
                joinQueryPartsOr(generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION), generateTypeQuery(DocumentAdminModel.Types.FIELD)),
                generatePropertyExactQuery(DocumentAdminModel.Props.FIELD_ID,
                        Arrays.asList(DocumentDynamicModel.Props.RECIPIENT_ID.getLocalName(), DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_ID.getLocalName()))
                );
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Boolean removable = (Boolean) nodeService.getProperty(nodeRef, DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_FIELD_GROUP);
        String oldValue = removable == null ? "null" : removable.toString();
        String newValue = "";
        if (!Boolean.FALSE.equals(removable)) {
            removable = Boolean.FALSE;
            nodeService.setProperty(nodeRef, DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_FIELD_GROUP, removable);
            newValue = removable.toString();
        }
        return new String[] { oldValue, newValue };
    }

}
