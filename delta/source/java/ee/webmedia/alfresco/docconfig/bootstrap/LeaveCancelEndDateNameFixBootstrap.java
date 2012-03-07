package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyBooleanQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

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
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * Fix for CL 185808
 * 
 * @author Alar Kvell
 */
public class LeaveCancelEndDateNameFixBootstrap extends AbstractNodeUpdater {

    private static final String NAME_OLD = "Puhkuse tühistamine alates";
    private static final String NAME_NEW = "Puhkuse tühistamine kuni";

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(
                generateTypeQuery(DocumentAdminModel.Types.FIELD),
                generatePropertyBooleanQuery(DocumentAdminModel.Props.SYSTEMATIC, true),
                generateStringExactQuery(DocumentSpecificModel.Props.LEAVE_CANCEL_END_DATE.getLocalName(), DocumentAdminModel.Props.ORIGINAL_FIELD_ID),
                generateStringExactQuery(NAME_OLD, DocumentAdminModel.Props.NAME)
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
        columns.add(ObjectUtils.toString(systematic, "[null]"));
        columns.add(fieldId);
        columns.add(name);
        if (DocumentSpecificModel.Props.LEAVE_CANCEL_END_DATE.getLocalName().equals(originalFieldId) && Boolean.TRUE.equals(systematic) && NAME_OLD.equals(name)) {
            name = NAME_NEW;
            nodeService.setProperty(fieldRef, DocumentAdminModel.Props.NAME, name);
            columns.add(name);
        } else {
            columns.add("[no match]");
        }
        return columns.toArray(new String[columns.size()]);
    }
}
