package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Fixes mimeType on files (CL task 122959)
 */
public class FileMimeTypeUpdater extends AbstractNodeUpdater {

    private MimetypeService mimetypeService;
    private Date beginDate;

    @Override
    protected void executeInternal() throws Throwable {
        if (beginDate == null) {
            log.debug("Skipping fileMimeType update, begin date is blank");
            return;
        }
        super.executeInternal();
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(SearchUtil.generateTypeQuery(ContentModel.TYPE_CONTENT));
        queryParts.add(SearchUtil.generateDatePropertyRangeQuery(beginDate, null, ContentModel.PROP_CREATED));
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {

        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        String name = (String) origProps.get(ContentModel.PROP_NAME);
        if (StringUtils.isEmpty(name)) {
            return new String[] { "1" };
        }
        String correctMimeType = mimetypeService.guessMimetype(name);

        ContentData oldContent = (ContentData) origProps.get(ContentModel.PROP_CONTENT);
        if (oldContent == null) {
            return new String[] { "2", name };
        }
        if (correctMimeType.equals(oldContent.getMimetype())) {
            return new String[] { "3", name, oldContent.getMimetype(), oldContent.getMimetype(), oldContent.getEncoding(), Long.toString(oldContent.getSize()),
                    oldContent.getContentUrl() };
        }
        ContentData newContent = ContentData.setMimetype(oldContent, correctMimeType);

        Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
        setProps.put(ContentModel.PROP_CONTENT, newContent);
        setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, setProps);

        return new String[] { "4", name, oldContent.getMimetype(), newContent.getMimetype(), newContent.getEncoding(),
                Long.toString(newContent.getSize()), newContent.getContentUrl() };
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setBeginDate(String beginDate) {
        if (StringUtils.isNotBlank(beginDate)) {
            try {
                this.beginDate = dateFormat.parse(beginDate);
            } catch (ParseException e) {
                throw new RuntimeException("Parsing configuration property fileMimeTypeUpdater.begin.date value failed: " + e.getMessage(), e);
            }
        }
    }

}
