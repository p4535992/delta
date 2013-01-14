package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyBooleanQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * Sets mandatoryForVol=false for field group definitions where name="Saatja reg nr ja kpv". See Cl task 210404.
 * 
 * @author Riina Tens
 */
public class DocumentSenderRegNrFieldGroupUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(
                generateTypeQuery(DocumentAdminModel.Types.FIELD_GROUP),
                generateStringExactQuery(SystematicFieldGroupNames.SENDER_REG_NUMBER_AND_DATE, DocumentAdminModel.Props.NAME),
                generatePropertyBooleanQuery(DocumentAdminModel.Props.MANDATORY_FOR_VOL, Boolean.TRUE)
                );
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.setProperty(nodeRef, DocumentAdminModel.Props.MANDATORY_FOR_VOL, Boolean.FALSE);
        return null;
    }

}
