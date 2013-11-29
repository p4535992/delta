package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.classificator.constant.FieldChangeableIf;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Set field with id="regDateTime" value to ALWAYS_NOT_CHANGEABLE. See CL 214605
 * 
 * @author Marti Laast
 */
public class DocumentRegDateFieldUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(
                generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION, DocumentAdminModel.Types.FIELD),
                generateStringExactQuery(DocumentCommonModel.Props.REG_DATE_TIME.getLocalName(), DocumentAdminModel.Props.FIELD_ID),
                joinQueryPartsOr(generateStringExactQuery(FieldChangeableIf.ALWAYS_CHANGEABLE.name(), DocumentAdminModel.Props.CHANGEABLE_IF),
                        generateStringExactQuery(FieldChangeableIf.CHANGEABLE_IF_WORKING_DOC.name(), DocumentAdminModel.Props.CHANGEABLE_IF))
                );
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.setProperty(nodeRef, DocumentAdminModel.Props.CHANGEABLE_IF, FieldChangeableIf.ALWAYS_NOT_CHANGEABLE.name());
        return null;
    }
}
