package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * @author Alar Kvell
 */
public class LeaveTypeFieldTypeUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(DocumentAdminModel.Types.FIELD)
                , generateStringExactQuery(DocumentSpecificModel.Props.LEAVE_TYPE.getLocalName(), DocumentAdminModel.Props.ORIGINAL_FIELD_ID)
                ));
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef fieldRef) throws Exception {
        String fieldType = (String) nodeService.getProperty(fieldRef, DocumentAdminModel.Props.FIELD_TYPE);
        String oldValue = fieldType == null ? "null" : fieldType;
        String newValue = oldValue;
        if (!FieldType.COMBOBOX.name().equals(fieldType)) {
            newValue = FieldType.COMBOBOX.name();
            nodeService.setProperty(fieldRef, DocumentAdminModel.Props.FIELD_TYPE, newValue);
        }
        return new String[] { oldValue, newValue };
    }

}
