package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

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
 * Sets changeableIf=CHANGEABLE_IF_WORKING_DOC for field definitions where id="regNumber". See Cl task 210404.
 */
public class DocumentRegNrFieldUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(
                generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION, DocumentAdminModel.Types.FIELD),
                generateStringExactQuery(DocumentCommonModel.Props.REG_NUMBER.getLocalName(), DocumentAdminModel.Props.FIELD_ID),
                generateStringExactQuery(FieldChangeableIf.ALWAYS_NOT_CHANGEABLE.name(), DocumentAdminModel.Props.CHANGEABLE_IF)
                );
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.setProperty(nodeRef, DocumentAdminModel.Props.CHANGEABLE_IF, FieldChangeableIf.CHANGEABLE_IF_WORKING_DOC.name());
        return null;
    }

}
