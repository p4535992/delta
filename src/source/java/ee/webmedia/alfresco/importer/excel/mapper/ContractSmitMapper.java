package ee.webmedia.alfresco.importer.excel.mapper;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.importer.excel.vo.ContractSmitDocument;

public class ContractSmitMapper extends AbstractSmitExcelMapper<ContractSmitDocument> {
    /** C: Pealkiri (docName) */
    @ExcelColumn('C')
    Integer DocName;

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
    Integer PartyNames;

    /** G: Lepingu lõpp */
    @ExcelColumn('G')
    Integer ContractEnd;

    /** H: Garantii */
    @ExcelColumn('H')
    Integer Warranty;

    /** I: Lepingu muudatused */
    @ExcelColumn('I')
    Integer ContractChange;

    /** E: Leping objekt (comment) */
    @ExcelColumn('E')
    Integer ContractObject;

    /** F: Leping sõlmiti (comment) */
    @ExcelColumn('F')
    Integer ContractSigned;

    @Override
    protected ContractSmitDocument createDocument(Row row) {
        final ContractSmitDocument doc = new ContractSmitDocument();
        doc.setDocumentTypeId(DocumentSubtypeModel.Types.CONTRACT_SMIT);
        setPartyNames(doc, get(row, PartyNames));
        setContractEnd(row, doc);
        doc.setWarranty(get(row, Warranty));
        doc.setContractChange(get(row, ContractChange));
        doc.addFileLocation(get(row, ContractDoc));
        addToComment(doc, "Leping objekt", get(row, ContractObject));
        addToComment(doc, "Leping sõlmiti", get(row, ContractSigned));
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

    private void setPartyNames(ContractSmitDocument doc, String partyNames) {
        if (partyNames != null) {
            partyNames = partyNames.trim();
            int index = partyNames.indexOf('/');
            if (index >= 0) {
                doc.setFirstPartyName(StringUtils.trimToNull(partyNames.substring(0, index)));
                partyNames = StringUtils.trimToEmpty(partyNames.substring(index + 1).trim());
                if (partyNames.length() > 0) {
                    index = partyNames.indexOf('/');
                    if (index >= 0) {
                        doc.setSecondPartyName(StringUtils.trimToNull(partyNames.substring(0, index)));
                        partyNames = StringUtils.trimToEmpty(partyNames.trim().substring(index + 1));
                        doc.setThirdPartyName(StringUtils.trimToNull(partyNames));
                    } else {
                        doc.setSecondPartyName(StringUtils.trimToNull(partyNames));
                    }
                }
            } else {
                doc.setFirstPartyName(StringUtils.trimToNull(partyNames));
            }
        }
    }

    @Override
    protected void fillRegistrationInfoAndAccessRestrictions(Row row, ContractSmitDocument doc) {
        // ContractSmit doesn't have some of the fields that all the other documents have, so just skipping it
    }

    @Override
    protected void postProcess(ContractSmitDocument doc) {
        final int volumeYear = Integer.parseInt(doc.getVolumeTitle());
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(volumeYear, 0, 1, 0, 0);// first day of the year
        doc.setAccessRestrictionBeginDate(cal.getTime());
    }

}