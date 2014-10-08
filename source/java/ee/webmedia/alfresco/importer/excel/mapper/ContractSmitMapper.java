<<<<<<< HEAD
package ee.webmedia.alfresco.importer.excel.mapper;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.importer.excel.vo.ContractSmitDocument;

public class ContractSmitMapper extends AbstractSmitExcelMapper<ContractSmitDocument> {
    /** Lepingu number */
    @ExcelColumn('C')
    Integer RegNumber;

    /** E: Leping objekt (docName) */
    @ExcelColumn('E')
    Integer DocName;

    /** F: Leping sõlmiti (comment) */
    @ExcelColumn('F')
    Integer RegDateTime;

    /** K : Leping */
    @ExcelColumn('K')
    Integer Link;

    /** L : Hankedokumendid */
    @ExcelColumn('L')
    Integer ContractDoc;

    /** J: Märkused (comment) */
    @ExcelColumn('J')
    Integer Comment;

    /** D PartyNames */
    @ExcelColumn('D')
    Integer PartyNameFirst;

    /** M PartyNames */
    @ExcelColumn('M')
    Integer PartyNameSecond;

    /** N PartyNames */
    @ExcelColumn('N')
    Integer PartyNameThird;

    /** G: Lepingu lõpp */
    @ExcelColumn('G')
    Integer ContractEnd;

    /** H: Garantii */
    @ExcelColumn('H')
    Integer Warranty;

    /** I: Lepingu muudatused */
    @ExcelColumn('I')
    Integer ContractChange;

    private static final String ACCESS_RESTRICTION_REASON = "AvTS § 35 lg 1";

    @Override
    protected ContractSmitDocument createDocument(Row row) {
        final ContractSmitDocument doc = new ContractSmitDocument();
        doc.setDocumentTypeId(DocumentSubtypeModel.Types.CONTRACT_SMIT);
        setPartyNames(doc, row);
        setContractEnd(row, doc);
        doc.setWarranty(get(row, Warranty));
        doc.setContractChange(get(row, ContractChange));
        addFileIfExists(doc, get(row, ContractDoc), ContractDoc);
        return doc;
    }

    @Override
    protected void setVolume(ContractSmitDocument doc, String seriesMark, String volumeTitle) {
        try {
            final int volumeYear = Integer.parseInt(volumeTitle);
            if (volumeYear < 2007 || volumeYear > 2010) {
                throw new NumberFormatException();
            }
            doc.setVolumeTitle(volumeTitle);
            final String vol = volumeTitle.substring(2); // short year number from inserted volume title
            super.setVolume(doc, seriesMark, vol);
        } catch (NumberFormatException e) {
            final FieldMismatchException fieldMismatchException = new FieldMismatchException(
                    "VolumeMark for contract documents must contain year number(2007...2010), but value is : '" + volumeTitle + "'");
            fieldMismatchException.setColumnName("VolumeMark");
            throw fieldMismatchException;
        }
    }

    private void setContractEnd(Row row, ContractSmitDocument doc) {
        final String contractEnd = get(row, ContractEnd);
        final Date contractEndDate = extractDate(contractEnd);
        if (contractEndDate != null) {
            doc.setContractSmitEndDate(contractEndDate);
        } else {
            doc.setContractEndAnnotation(contractEnd);
        }
    }

    private void setPartyNames(ContractSmitDocument doc, Row row) {
        doc.setFirstPartyName(get(row, PartyNameFirst));
        doc.setSecondPartyName(get(row, PartyNameSecond));
        doc.setThirdPartyName(get(row, PartyNameThird));
    }

    @Override
    protected void fillAccessRestrictions(Row row, ContractSmitDocument doc) {
        fillAccessRestriction(doc, null);
    }

    @Override
    protected void fillAccessRestriction(ContractSmitDocument doc, String empty) {
        doc.setAccessRestriction(ee.webmedia.alfresco.classificator.enums.AccessRestriction.AK.getValueName());
        doc.setAccessRestrictionReason(ACCESS_RESTRICTION_REASON);
        Date accessRestrictionBeginDate = doc.getRegDateTime();
        assertNotNull("ContractDate", accessRestrictionBeginDate);
        doc.setAccessRestrictionBeginDate(accessRestrictionBeginDate);

        final int accessRestrictionYears;
        if (StringUtils.equals("4-4", doc.getSeries())) {
            accessRestrictionYears = 75;
        } else {
            accessRestrictionYears = 5;
        }
        final Calendar cal = Calendar.getInstance();
        cal.setTime(accessRestrictionBeginDate);
        cal.add(Calendar.YEAR, accessRestrictionYears);
        doc.setAccessRestrictionEndDate(cal.getTime());
    }

    @Override
    protected void postProcess(ContractSmitDocument doc) {
        final int volumeYear = Integer.parseInt(doc.getVolumeTitle());
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(volumeYear, 0, 1, 0, 0);// first day of the year
        doc.setAccessRestrictionBeginDate(cal.getTime());
    }

=======
package ee.webmedia.alfresco.importer.excel.mapper;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.importer.excel.vo.ContractSmitDocument;

public class ContractSmitMapper extends AbstractSmitExcelMapper<ContractSmitDocument> {
    /** Lepingu number */
    @ExcelColumn('C')
    Integer RegNumber;

    /** E: Leping objekt (docName) */
    @ExcelColumn('E')
    Integer DocName;

    /** F: Leping sõlmiti (comment) */
    @ExcelColumn('F')
    Integer RegDateTime;

    /** K : Leping */
    @ExcelColumn('K')
    Integer Link;

    /** L : Hankedokumendid */
    @ExcelColumn('L')
    Integer ContractDoc;

    /** J: Märkused (comment) */
    @ExcelColumn('J')
    Integer Comment;

    /** D PartyNames */
    @ExcelColumn('D')
    Integer PartyNameFirst;

    /** M PartyNames */
    @ExcelColumn('M')
    Integer PartyNameSecond;

    /** N PartyNames */
    @ExcelColumn('N')
    Integer PartyNameThird;

    /** G: Lepingu lõpp */
    @ExcelColumn('G')
    Integer ContractEnd;

    /** H: Garantii */
    @ExcelColumn('H')
    Integer Warranty;

    /** I: Lepingu muudatused */
    @ExcelColumn('I')
    Integer ContractChange;

    private static final String ACCESS_RESTRICTION_REASON = "AvTS § 35 lg 1";

    @Override
    protected ContractSmitDocument createDocument(Row row) {
        final ContractSmitDocument doc = new ContractSmitDocument();
        doc.setDocumentTypeId(DocumentSubtypeModel.Types.CONTRACT_SMIT);
        setPartyNames(doc, row);
        setContractEnd(row, doc);
        doc.setWarranty(get(row, Warranty));
        doc.setContractChange(get(row, ContractChange));
        addFileIfExists(doc, get(row, ContractDoc), ContractDoc);
        return doc;
    }

    @Override
    protected void setVolume(ContractSmitDocument doc, String seriesMark, String volumeTitle) {
        try {
            final int volumeYear = Integer.parseInt(volumeTitle);
            if (volumeYear < 2007 || volumeYear > 2010) {
                throw new NumberFormatException();
            }
            doc.setVolumeTitle(volumeTitle);
            final String vol = volumeTitle.substring(2); // short year number from inserted volume title
            super.setVolume(doc, seriesMark, vol);
        } catch (NumberFormatException e) {
            final FieldMismatchException fieldMismatchException = new FieldMismatchException(
                    "VolumeMark for contract documents must contain year number(2007...2010), but value is : '" + volumeTitle + "'");
            fieldMismatchException.setColumnName("VolumeMark");
            throw fieldMismatchException;
        }
    }

    private void setContractEnd(Row row, ContractSmitDocument doc) {
        final String contractEnd = get(row, ContractEnd);
        final Date contractEndDate = extractDate(contractEnd);
        if (contractEndDate != null) {
            doc.setContractSmitEndDate(contractEndDate);
        } else {
            doc.setContractEndAnnotation(contractEnd);
        }
    }

    private void setPartyNames(ContractSmitDocument doc, Row row) {
        doc.setFirstPartyName(get(row, PartyNameFirst));
        doc.setSecondPartyName(get(row, PartyNameSecond));
        doc.setThirdPartyName(get(row, PartyNameThird));
    }

    @Override
    protected void fillAccessRestrictions(Row row, ContractSmitDocument doc) {
        fillAccessRestriction(doc, null);
    }

    @Override
    protected void fillAccessRestriction(ContractSmitDocument doc, String empty) {
        doc.setAccessRestriction(ee.webmedia.alfresco.classificator.enums.AccessRestriction.AK.getValueName());
        doc.setAccessRestrictionReason(ACCESS_RESTRICTION_REASON);
        Date accessRestrictionBeginDate = doc.getRegDateTime();
        assertNotNull("ContractDate", accessRestrictionBeginDate);
        doc.setAccessRestrictionBeginDate(accessRestrictionBeginDate);

        final int accessRestrictionYears;
        if (StringUtils.equals("4-4", doc.getSeries())) {
            accessRestrictionYears = 75;
        } else {
            accessRestrictionYears = 5;
        }
        final Calendar cal = Calendar.getInstance();
        cal.setTime(accessRestrictionBeginDate);
        cal.add(Calendar.YEAR, accessRestrictionYears);
        doc.setAccessRestrictionEndDate(cal.getTime());
    }

    @Override
    protected void postProcess(ContractSmitDocument doc) {
        final int volumeYear = Integer.parseInt(doc.getVolumeTitle());
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(volumeYear, 0, 1, 0, 0);// first day of the year
        doc.setAccessRestrictionBeginDate(cal.getTime());
    }

>>>>>>> develop-5.1
}