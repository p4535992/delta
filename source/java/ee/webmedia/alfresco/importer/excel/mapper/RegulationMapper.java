package ee.webmedia.alfresco.importer.excel.mapper;

import org.apache.poi.ss.usermodel.Row;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.importer.excel.vo.ImportDocument;

public class RegulationMapper extends AbstractSmitExcelMapper<ImportDocument> {
    /** E: Pealkiri (docName) */
    @ExcelColumn('E')
    Integer DocName;

    /** F: Koostaja (ownerName) */
    @ExcelColumn('F')
    Integer OwnerName = 5;

    /** G: Allkirjastaja (signerName) */
    @ExcelColumn('G')
    Integer SignerName;

    /** I: Juurdepääsupiirang (accessRestrictionEndDate&accessRestrictionEndDesc) */
    @ExcelColumn('I')
    Integer AccessRestriction;

    /** H: Dokumendi link ) */
    @ExcelColumn('H')
    Integer Link;

    /** J: Märkused (comment) */
    @ExcelColumn('J')
    Integer Comment;

    @Override
    protected ImportDocument createDocument(Row row) {
        final ImportDocument doc = new ImportDocument();
        doc.setDocumentTypeId(DocumentSubtypeModel.Types.REGULATION);
        doc.setOwnerName(get(row, OwnerName));
        return doc;
    }

}