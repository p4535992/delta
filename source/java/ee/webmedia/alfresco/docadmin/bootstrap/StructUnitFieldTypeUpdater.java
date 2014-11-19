<<<<<<< HEAD
package ee.webmedia.alfresco.docadmin.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Change field type to STRUCT_UNIT for fields where field name contains "OrgStructUnit"
 * 
 * @author Riina Tens
 */
public class StructUnitFieldTypeUpdater extends AbstractNodeUpdater {

    public static final String ORG_STRUCT_UNIT = "OrgStructUnit";

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(
                joinQueryPartsOr(
                        SearchUtil.generateTypeQuery(DocumentAdminModel.Types.FIELD),
                        SearchUtil.generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION)
                ),
                SearchUtil.generatePropertyWildcardQuery(DocumentAdminModel.Props.ORIGINAL_FIELD_ID, ORG_STRUCT_UNIT, true, true)
                );
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> originalProps = nodeService.getProperties(nodeRef);
        String result = originalProps.get(DocumentAdminModel.Props.FIELD_ID) + ", " + originalProps.get(DocumentAdminModel.Props.ORIGINAL_FIELD_ID) + ", "
                + originalProps.get(DocumentAdminModel.Props.FIELD_TYPE);
        nodeService.setProperty(nodeRef, DocumentAdminModel.Props.FIELD_TYPE, FieldType.STRUCT_UNIT.name());

        return new String[] { result };
    }

}
=======
package ee.webmedia.alfresco.docadmin.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Change field type to STRUCT_UNIT for fields where field name contains "OrgStructUnit"
 */
public class StructUnitFieldTypeUpdater extends AbstractNodeUpdater {

    public static final String ORG_STRUCT_UNIT = "OrgStructUnit";

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(
                joinQueryPartsOr(
                        SearchUtil.generateTypeQuery(DocumentAdminModel.Types.FIELD),
                        SearchUtil.generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION)
                ),
                SearchUtil.generatePropertyWildcardQuery(DocumentAdminModel.Props.ORIGINAL_FIELD_ID, ORG_STRUCT_UNIT, true, true)
                );
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> originalProps = nodeService.getProperties(nodeRef);
        String result = originalProps.get(DocumentAdminModel.Props.FIELD_ID) + ", " + originalProps.get(DocumentAdminModel.Props.ORIGINAL_FIELD_ID) + ", "
                + originalProps.get(DocumentAdminModel.Props.FIELD_TYPE);
        nodeService.setProperty(nodeRef, DocumentAdminModel.Props.FIELD_TYPE, FieldType.STRUCT_UNIT.name());

        return new String[] { result };
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
