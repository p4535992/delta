package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

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

    private FileService fileService;
    private MimetypeService mimetypeService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(
                Arrays.asList(SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.TASK),
                        SearchUtil.generatePropertyNotNullQuery(QName.createQName(WorkflowSpecificModel.URI, "file"))));
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query),
                searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        ContentData contentData = (ContentData) nodeService.getProperty(nodeRef, QName.createQName(WorkflowSpecificModel.URI, "file"));
        File file = new File(contentData.getContentUrl());
        String fileName = "Arvamuse fail" + mimetypeService.getExtension(contentData.getMimetype());
        fileService.addFileToTask(fileName, fileName, nodeRef, file, contentData.getMimetype());
        nodeService.removeAspect(nodeRef, QName.createQName(WorkflowSpecificModel.URI, "file"));
        return new String[] { fileName };
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

}
