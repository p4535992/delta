package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * Updater to move task file from wfs:file property to child association
 * 
 * @author Riina Tens
 */
public class MoveTaskFileToChildAssoc extends AbstractNodeUpdater {

    public static final QName TASK_FILE_PROP_QNAME = QName.createQName(WorkflowSpecificModel.URI, "file");
    private FileService fileService;
    private MimetypeService mimetypeService;
    private ContentService contentService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(
                Arrays.asList(SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.TASK),
                        SearchUtil.generatePropertyNotNullQuery(TASK_FILE_PROP_QNAME)));
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query),
                searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        QName propName = TASK_FILE_PROP_QNAME;
        ContentReader reader = contentService.getReader(nodeRef, propName);
        if (reader == null) {
            return new String[] { "contentDataIsNull" };
        }
        String extension = mimetypeService.getExtension(reader.getMimetype());
        String fileName = "Arvamuse fail." + (StringUtils.isNotBlank(extension) ? extension : "bin");
        fileService.addFile(fileName, fileName, nodeRef, reader);
        nodeService.removeAspect(nodeRef, propName);
        return new String[] { "addedFileToTask", fileName, reader.getContentUrl(), reader.getEncoding(), "" + reader.getLocale(), reader.getMimetype(), "" + reader.getSize() };
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

}
