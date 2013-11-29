package ee.webmedia.alfresco.sharepoint;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * @author Alar Kvell
 */
public class DocumentContentPropertyHtmlUpdater extends AbstractNodeUpdater {

    private static QName PROP_CONTENT = QName.createQName(DocumentDynamicModel.URI, "content");

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(
                SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                SearchUtil.generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE),
                SearchUtil.generateStringNotEmptyQuery(PROP_CONTENT));
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        resultSets.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        resultSets.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef docRef) throws Exception {
        String regNumber = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.REG_NUMBER);
        String docName = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.DOC_NAME);
        String oldContent = (String) nodeService.getProperty(docRef, PROP_CONTENT);
        String newContent = oldContent;
        String action = "notChanged";

        if (StringUtils.isNotBlank(oldContent)) {
            newContent = StringEscapeUtils.unescapeHtml(oldContent.replaceAll("\\<.*?\\>", ""));
        }
        if (!ObjectUtils.equals(oldContent, newContent)) {
            action = "changed";
            nodeService.setProperty(docRef, PROP_CONTENT, newContent);
        }

        return new String[] { action, oldContent, newContent, regNumber, docName };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] {
                "documentNodeRef",
                "action",
                "oldContent",
                "newContent",
                "documentRegNumber",
                "documentDocName" };
    }

}
