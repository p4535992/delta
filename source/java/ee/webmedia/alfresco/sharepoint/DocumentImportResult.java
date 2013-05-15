package ee.webmedia.alfresco.sharepoint;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;

import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.sharepoint.mapping.DocumentMetadata;
import ee.webmedia.alfresco.sharepoint.mapping.MappedDocument;

/**
 * A class representing a document import result. For each processed document, exactly one result is created. This object can be logged to the log file once import is completed.
 * 
 * @author Martti Tamm
 */
public class DocumentImportResult {

    private final DocumentMetadata meta;

    private final File docXmlFile;

    private final NodeRef documentRef;

    private final MappedDocument mappedDoc;

    private final String error;

    private final boolean success;

    public DocumentImportResult(DocumentMetadata meta, File docXmlFile, NodeRef documentRef, MappedDocument mappedDoc) {
        this.meta = meta;
        this.docXmlFile = docXmlFile;
        this.documentRef = documentRef;
        this.mappedDoc = mappedDoc;
        success = true;
        error = null;
    }

    public DocumentImportResult(DocumentMetadata meta, File docXmlFile, String error) {
        this.meta = meta;
        this.docXmlFile = docXmlFile;
        this.error = error;
        success = false;
        documentRef = null;
        mappedDoc = null;
    }

    public DocumentImportResult(DocumentMetadata meta, File docXmlFile, String error, boolean success) {
        this.meta = meta;
        this.docXmlFile = docXmlFile;
        this.error = error;
        this.success = success;
        documentRef = null;
        mappedDoc = null;
    }

    public boolean isSuccessful() {
        return success;
    }

    public NodeRef getDocumentRef() {
        return documentRef;
    }

    public MappedDocument getMappedDoc() {
        return mappedDoc;
    }

    public DocumentMetadata getMeta() {
        return meta;
    }

    /**
     * Writes the document import result state to the log file. One of the provided log files will be used depending on the state of import result.
     * 
     * @param csvSuccess The CSV writer to use when the document was imported successfully.
     * @param csvFail The CSV writer to use when the document was not imported for some reason.
     * @throws IOException For any error during writing.
     */
    public void logResult(CsvWriter csvSuccess, CsvWriter csvFail) throws IOException {
        logResult(error != null ? csvFail : csvSuccess);
    }

    private void logResult(CsvWriter csv) throws IOException {
        Map<QName, Serializable> props = mappedDoc == null ? null : mappedDoc.getPropertyValues();
        // Columns 1, 2, 3 are read elsewhere, so these must stay in this order
        csv.write(FilenameUtils.getBaseName(docXmlFile.getName()));
        csv.write(documentRef == null ? "" : documentRef.toString());
        csv.write(meta == null ? "" : meta.getOriginalLocation());
        csv.write(meta == null ? "" : meta.getOriginalLocationName());
        csv.write(meta == null || meta.getFiles().isEmpty() ? "" : meta.getFiles().get(0).getTitle());
        csv.write(meta == null ? "" : ImportUtil.formatDateTime(meta.getCreated()));
        csv.write(props == null ? "" : (String) props.get(DocumentCommonModel.Props.REG_NUMBER));
        csv.write(props == null ? "" : ImportUtil.formatDate((Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME)));
        csv.write(props == null ? "" : (String) props.get(DocumentCommonModel.Props.DOC_NAME));
        csv.write(props == null ? "" : (String) props.get(DocumentCommonModel.Props.OWNER_ID));
        csv.write(props == null ? "" : (String) props.get(DocumentCommonModel.Props.OWNER_NAME));
        csv.write(props == null ? "" : (String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION));
        csv.write(meta == null ? "" : meta.getDocumentType());
        csv.write(meta == null ? "" : meta.getDirection());
        csv.write(meta == null ? "" : meta.getSubtype());
        csv.write(props == null ? "" : (String) props.get(DocumentAdminModel.Props.OBJECT_TYPE_ID));
        csv.write(error == null ? "" : error);
        csv.endRecord();
    }
}
