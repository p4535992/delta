package ee.webmedia.alfresco.template.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Changes document template DOCTYPE_ID "E-maili mall" -> "E-kirja mall"
 */
public class DocumentTemplateEmailTypeIdUpdater extends AbstractNodeUpdater {

    private final String docTypeId = MessageUtil.getMessage("template_email_template");

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(SeriesModel.Types.SERIES),
                generateStringExactQuery("E-maili mall", DocumentTemplateModel.Prop.DOCTYPE_ID)
                ));
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) {
        nodeService.setProperty(nodeRef, DocumentTemplateModel.Prop.DOCTYPE_ID, docTypeId);
        return new String[] { "updateDocumentTemplateEmailTypeId", docTypeId };
    }
}
