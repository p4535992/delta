package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyBooleanQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ObjectUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * This updater sets removableFromSystematicFieldGroup to true for leaveChangedDays and leaveCancelledDays so they could be removed from systematic group.
 * @see SIMDHS-4011
 */
public class LeaveChangedCancelledDaysUpdater extends AbstractNodeUpdater {

    private static final String LEAVE_CHANGED_DAYS = "leaveChangedDays";
    private static final String LEAVE_CANCELLED_DAYS = "leaveCancelledDays";

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(
                generateTypeQuery(DocumentAdminModel.Types.FIELD),
                generatePropertyBooleanQuery(DocumentAdminModel.Props.SYSTEMATIC, true),
                joinQueryPartsOr(
                        generateStringExactQuery(LEAVE_CHANGED_DAYS, DocumentAdminModel.Props.ORIGINAL_FIELD_ID),
                        generateStringExactQuery(LEAVE_CANCELLED_DAYS, DocumentAdminModel.Props.ORIGINAL_FIELD_ID)
                        )
                );
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef fieldRef) throws Exception {
        Map<QName, Serializable> oldProps = nodeService.getProperties(fieldRef);
        String originalFieldId = (String) oldProps.get(DocumentAdminModel.Props.ORIGINAL_FIELD_ID);
        Boolean systematic = (Boolean) oldProps.get(DocumentAdminModel.Props.SYSTEMATIC);
        String fieldId = (String) oldProps.get(DocumentAdminModel.Props.FIELD_ID);
        String name = (String) oldProps.get(DocumentAdminModel.Props.NAME);

        List<String> columns = new ArrayList<String>();
        columns.add(originalFieldId);
        columns.add(ObjectUtils.toString(systematic, "null"));
        columns.add(fieldId);
        columns.add(name);
        if (Boolean.TRUE.equals(systematic) && (LEAVE_CHANGED_DAYS.equals(originalFieldId) || LEAVE_CANCELLED_DAYS.equals(originalFieldId))) {
            nodeService.setProperty(fieldRef, DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_FIELD_GROUP, Boolean.TRUE);
            columns.add("removableFromSystematicFieldGroup");
        } else {
            columns.add("notModified");
        }
        return columns.toArray(new String[columns.size()]);
    }
}