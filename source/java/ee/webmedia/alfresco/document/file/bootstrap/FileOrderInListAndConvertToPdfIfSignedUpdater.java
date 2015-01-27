package ee.webmedia.alfresco.document.file.bootstrap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.repo.domain.QNameDAO;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.collections4.CollectionUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractParallelNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * This uploader generates {@code FileModel.Props.FILE_ORDER_IN_LIST} property to all files
 * and sets {@code FileModel.Props.CONVERT_TO_PDF_IF_SIGNED} value to true if file is transformable
 */
public class FileOrderInListAndConvertToPdfIfSignedUpdater extends AbstractParallelNodeUpdater {

    private FileService fileService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<ResultSet> result = new ArrayList<>();
        String query = SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef docRef) throws Exception {
        List<File> files = fileService.getAllFilesExcludingDigidocSubitems(docRef);
        if (CollectionUtils.isEmpty(files)) {
            return null;
        }
        fileService.reorderFiles(docRef);
        Set<NodeRef> filesToTransform = new HashSet<>();
        for (File f : files) {
            String mimeType = f.getMimeType();
            boolean isTransformable = Boolean.FALSE.equals(f.isTransformableToPdf()) && fileService.isTransformableToPdf(mimeType);
            if (isTransformable) {
                filesToTransform.add(getFileRef(f));
            }
        }
        for (NodeRef fileRef : filesToTransform) {
            nodeService.setProperty(fileRef, FileModel.Props.CONVERT_TO_PDF_IF_SIGNED, Boolean.TRUE);
        }

        return new String[] { "Reordered and set 'convertToPdfIfSigned' = true for " + filesToTransform.size() + " files" };
    }

    @Override
    protected void prepareForUpdating() {
        RetryingTransactionHelper helper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
        helper.doInTransaction(new RetryingTransactionCallback<Void>() {
            @Override
            public Void execute() throws Throwable {
                QNameDAO qnameDao = BeanHelper.getSpringBean(QNameDAO.class, "qnameDAO");
                qnameDao.getOrCreateQName(FileModel.Props.FILE_ORDER_IN_LIST);
                return null;
            }
        }, false, true);
    }

    private NodeRef getFileRef(File f) {
        NodeRef fileRef = f.getNodeRef();
        if (fileRef == null) {
            fileRef = f.getNode() != null ? f.getNode().getNodeRef() : null;
        }
        return fileRef;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

}
