package ee.webmedia.alfresco.postipoiss;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.lang.ObjectUtils;

import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

public class StorageTypeUpdater extends AbstractNodeUpdater {

    private FileService fileService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(
                SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                SearchUtil.generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE)
                );
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        resultSets.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        resultSets.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef docRef) throws Exception {
        String regNumber = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.REG_NUMBER);
        String docName = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.DOC_NAME);
        String oldStorageType = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.STORAGE_TYPE);
        String newStorageType = oldStorageType;
        int filesCount = -1;

        if (StorageType.DIGITAL.getValueName().equals(oldStorageType) || StorageType.PAPER.getValueName().equals(oldStorageType)
                || StorageType.XML.getValueName().equals(oldStorageType)) {
            // do nothing
        } else if ("Digitaaldokument".equalsIgnoreCase(oldStorageType) || "DIAT register".equalsIgnoreCase(oldStorageType)) {
            newStorageType = StorageType.DIGITAL.getValueName();
        } else if ("Paberdokument".equalsIgnoreCase(oldStorageType)) {
            newStorageType = StorageType.PAPER.getValueName();
        } else {
            List<ee.webmedia.alfresco.document.file.model.File> files = fileService.getAllFilesExcludingDigidocSubitems(docRef);
            filesCount = files.size();
            if (filesCount > 0) {
                newStorageType = StorageType.DIGITAL.getValueName();
            } else {
                newStorageType = StorageType.PAPER.getValueName();
            }
        }
        String action = "notChanged";
        if (!ObjectUtils.equals(oldStorageType, newStorageType)) {
            action = "changed";
            nodeService.setProperty(docRef, DocumentCommonModel.Props.STORAGE_TYPE, newStorageType);
        }

        return new String[] { action, oldStorageType, newStorageType, Integer.toString(filesCount), regNumber, docName };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] {
                "documentNodeRef",
                "action",
                "oldStorageType",
                "newStorageType",
                "documentFilesCount",
                "documentRegNumber",
                "documentDocName" };
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

}
