package ee.webmedia.alfresco.importer.excel.mapper;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.importer.excel.vo.ContractSmitDocument;
import ee.webmedia.alfresco.importer.excel.vo.ImportDocument;

public class ContractSmitMapper extends AbstractSmitExcelMapper<ImportDocument> {
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
    protected ImportDocument createDocument(Row row) {
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
    protected void fillRegistrationInfoAndAccessRestrictions(Row row, ImportDocument doc) {
        // ContractSmit doesn't have some of the fields that all the other documents have, so just skipping it
    }

}