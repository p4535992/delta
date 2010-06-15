package ee.webmedia.alfresco.importer.excel.service;

import java.util.List;

import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.importer.excel.vo.ImportDocument;

public interface DocumentImportService extends DocumentService {
    /**
     * @param documents - NB! expected that the iterator of the documents list returns documents in the order they appear in the documents list
     *            (to be able to name case for document based on the registration date or order of appearance if the registration dates are equal)
     */
    <IDoc extends ImportDocument> void importDocuments(List<IDoc> documents);

    /**
     * Call this method after the last transaction of importing documents has been completed
     */
    void createAssocs();

}
