<<<<<<< HEAD
package ee.webmedia.alfresco.importer.excel.mapper;

import org.apache.poi.ss.usermodel.Row;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.importer.excel.vo.ImportDocument;

public class MinutesDocumentMapper extends AbstractSmitExcelMapper<ImportDocument> {

    /** E: Pealkiri (docName) */
    @ExcelColumn('E')
    Integer DocName;

    /** F: Juurdepääsupiirang (accessRestrictionEndDate&accessRestrictionEndDesc) */
    @ExcelColumn('F')
    Integer AccessRestriction;

    /** G: Dokumendi link */
    @ExcelColumn('G')
    Integer Link;
    //
    /** H: Märkused (comment) */
    @ExcelColumn('H')
    Integer Comment;

    @Override
    protected ImportDocument createDocument(Row row) {
        final ImportDocument doc = new ImportDocument();
        doc.setDocumentTypeId(DocumentSubtypeModel.Types.MINUTES);
        return doc;
    }

=======
package ee.webmedia.alfresco.importer.excel.mapper;

import org.apache.poi.ss.usermodel.Row;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.importer.excel.vo.ImportDocument;

public class MinutesDocumentMapper extends AbstractSmitExcelMapper<ImportDocument> {

    /** E: Pealkiri (docName) */
    @ExcelColumn('E')
    Integer DocName;

    /** F: Juurdepääsupiirang (accessRestrictionEndDate&accessRestrictionEndDesc) */
    @ExcelColumn('F')
    Integer AccessRestriction;

    /** G: Dokumendi link */
    @ExcelColumn('G')
    Integer Link;
    //
    /** H: Märkused (comment) */
    @ExcelColumn('H')
    Integer Comment;

    @Override
    protected ImportDocument createDocument(Row row) {
        final ImportDocument doc = new ImportDocument();
        doc.setDocumentTypeId(DocumentSubtypeModel.Types.MINUTES);
        return doc;
    }

>>>>>>> develop-5.1
}