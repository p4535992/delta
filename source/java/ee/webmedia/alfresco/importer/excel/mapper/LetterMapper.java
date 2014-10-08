<<<<<<< HEAD
package ee.webmedia.alfresco.importer.excel.mapper;

import static ee.webmedia.alfresco.importer.excel.mapper.LetterMapper.LetterConstants.SenderForOutgoin;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.model.LetterDocument;
import ee.webmedia.alfresco.importer.excel.vo.ImportDocument;
import ee.webmedia.alfresco.importer.excel.vo.IncomingLetter;
import ee.webmedia.alfresco.importer.excel.vo.OutgoingLetter;
import ee.webmedia.alfresco.importer.excel.vo.SendInfo;

public class LetterMapper extends AbstractSmitExcelMapper<LetterDocument> {

    interface LetterConstants {
        String SenderForOutgoin = "Siseministeeriumi infotehnoloogia- ja arenduskeskus";
    }

    /** F: Pealkiri (docName) */
    @SuppressWarnings("unused")
    // annotation overrides the value of superclass field
    @ExcelColumn('F')
    private Integer DocName;
    /** K: Juurdepääsupiirang (accessRestrictionEndDate&accessRestrictionEndDesc) */
    @SuppressWarnings("unused")
    // annotation overrides the value of superclass field
    @ExcelColumn('K')
    private Integer AccessRestriction;
    /** S: Märkused (comment) */
    @SuppressWarnings("unused")
    // annotation overrides the value of superclass field
    @ExcelColumn('S')
    private Integer Comment;
    /** P: Dokumendi link */
    @SuppressWarnings("unused")
    // annotation overrides the value of superclass field
    @ExcelColumn('P')
    private Integer Link;

    /** E: Saatmisviis (transmittalMode TODO: klassifikaatoriga kokku viimine | FIXME: recipientSendMode -> sendMode???) */
    @ExcelColumn('E')
    private Integer SendMode;
    /** G: Kirja kpv (senderRegDate FIXME: vist väljuval ka sama, mitte mitte järgnev? regDateTime?) */
    @ExcelColumn('G')
    private Integer SenderRegDate;
    /** H: Kirja nr */
    @ExcelColumn('H')
    private Integer SenderRegNumber;
    /** I: Saatja (senderName|null) */
    @ExcelColumn('I')
    private Integer Sender;
    /** J: Allkirjastaja (null|signerName) */
    @ExcelColumn('J')
    private Integer SignerName;
    /** L: Vastus vajalik (comment) */
    @ExcelColumn('L')
    private Integer ReplyNeeded;
    /** M: Kellele suunatud (Täitja / Asutus) (comment|recipientName) */
    @ExcelColumn('M')
    private Integer RecipientName;
    /** N: Resolutsioon (comment) */
    @ExcelColumn('N')
    private Integer Resolution;
    /** O: Vastamise tähtaeg (dueDate|null) */
    @ExcelColumn('O')
    private Integer DueDate;
    /** Dokumendi link avalikku veebi (comment) */
    @ExcelColumn('Q')
    private Integer PublicLink;
    /** Täitmismärge (comment) */
    @ExcelColumn('R')
    private Integer CompletedNote;

    @Override
    protected LetterDocument createDocument(Row row) {
        final LetterDocument doc;
        final String sender = get(row, Sender);
        final String sendMode = get(row, SendMode);
        final String signerName = get(row, SignerName);
        final String recipientName = get(row, RecipientName);
        if (isOutGoing(sender)) {
            final OutgoingLetter out = new OutgoingLetter();
            out.setDocumentTypeId(DocumentSubtypeModel.Types.OUTGOING_LETTER);
            if (StringUtils.isNotBlank(sendMode)) {
                getNvlSendInfo(out).setSendMode(sendMode);
            }
            out.setSignerName(signerName);
            if (recipientName != null) {
                final int recipientSplitIndex = recipientName.lastIndexOf('/');
                if (recipientSplitIndex >= 0) {
                    out.setRecipientName(recipientName.substring(0, recipientSplitIndex));
                }
            }
            doc = out;
        } else {
            IncomingLetter in = new IncomingLetter();
            in.setDocumentTypeId(DocumentSubtypeModel.Types.INCOMING_LETTER);
            in.setSenderName(sender);
            in.setTransmittalMode(sendMode);
            final Date dueDate = getDate(row, DueDate);
            doc = in;
            if (dueDate == null) {
                doc.setDocStatus(DocumentStatus.WORKING.getValueName());
            } else {
                doc.setDocStatus(DocumentStatus.FINISHED.getValueName());
                in.setDueDate(dueDate);
            }
            addToComment(in, "Allkirjastaja", signerName);
        }
        { // common fields of letter
            doc.setSenderRegDate(getDate(row, SenderRegDate));
            doc.setSenderRegNumber(get(row, SenderRegNumber));
            addToComment(doc, "Vastus vajalik", get(row, ReplyNeeded));
            addToComment(doc, "Resolutsioon", get(row, Resolution));
            addToComment(doc, "Kellele suunatud (Täitja / Asutus)", recipientName);
            addToComment(doc, "Dokumendi link avalikku veebi", get(row, PublicLink));
            addToComment(doc, "Täitmismärge", get(row, CompletedNote));
        }
        return doc;
    }

    private boolean isOutGoing(final String sender) {
        if (StringUtils.equals(sender, SenderForOutgoin)) {
            return true;
        }
        return false;
    }

    private SendInfo getNvlSendInfo(final ImportDocument doc) {
        SendInfo sendInfo = doc.getSendInfo();
        if (sendInfo == null) {
            sendInfo = new SendInfo();
            doc.setSendInfo(sendInfo);
        }
        return sendInfo;
    }

=======
package ee.webmedia.alfresco.importer.excel.mapper;

import static ee.webmedia.alfresco.importer.excel.mapper.LetterMapper.LetterConstants.SenderForOutgoin;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.model.LetterDocument;
import ee.webmedia.alfresco.importer.excel.vo.ImportDocument;
import ee.webmedia.alfresco.importer.excel.vo.IncomingLetter;
import ee.webmedia.alfresco.importer.excel.vo.OutgoingLetter;
import ee.webmedia.alfresco.importer.excel.vo.SendInfo;

public class LetterMapper extends AbstractSmitExcelMapper<LetterDocument> {

    interface LetterConstants {
        String SenderForOutgoin = "Siseministeeriumi infotehnoloogia- ja arenduskeskus";
    }

    /** F: Pealkiri (docName) */
    @SuppressWarnings("unused")
    // annotation overrides the value of superclass field
    @ExcelColumn('F')
    private Integer DocName;
    /** K: Juurdepääsupiirang (accessRestrictionEndDate&accessRestrictionEndDesc) */
    @SuppressWarnings("unused")
    // annotation overrides the value of superclass field
    @ExcelColumn('K')
    private Integer AccessRestriction;
    /** S: Märkused (comment) */
    @SuppressWarnings("unused")
    // annotation overrides the value of superclass field
    @ExcelColumn('S')
    private Integer Comment;
    /** P: Dokumendi link */
    @SuppressWarnings("unused")
    // annotation overrides the value of superclass field
    @ExcelColumn('P')
    private Integer Link;

    /** E: Saatmisviis (transmittalMode TODO: klassifikaatoriga kokku viimine | FIXME: recipientSendMode -> sendMode???) */
    @ExcelColumn('E')
    private Integer SendMode;
    /** G: Kirja kpv (senderRegDate FIXME: vist väljuval ka sama, mitte mitte järgnev? regDateTime?) */
    @ExcelColumn('G')
    private Integer SenderRegDate;
    /** H: Kirja nr */
    @ExcelColumn('H')
    private Integer SenderRegNumber;
    /** I: Saatja (senderName|null) */
    @ExcelColumn('I')
    private Integer Sender;
    /** J: Allkirjastaja (null|signerName) */
    @ExcelColumn('J')
    private Integer SignerName;
    /** L: Vastus vajalik (comment) */
    @ExcelColumn('L')
    private Integer ReplyNeeded;
    /** M: Kellele suunatud (Täitja / Asutus) (comment|recipientName) */
    @ExcelColumn('M')
    private Integer RecipientName;
    /** N: Resolutsioon (comment) */
    @ExcelColumn('N')
    private Integer Resolution;
    /** O: Vastamise tähtaeg (dueDate|null) */
    @ExcelColumn('O')
    private Integer DueDate;
    /** Dokumendi link avalikku veebi (comment) */
    @ExcelColumn('Q')
    private Integer PublicLink;
    /** Täitmismärge (comment) */
    @ExcelColumn('R')
    private Integer CompletedNote;

    @Override
    protected LetterDocument createDocument(Row row) {
        final LetterDocument doc;
        final String sender = get(row, Sender);
        final String sendMode = get(row, SendMode);
        final String signerName = get(row, SignerName);
        final String recipientName = get(row, RecipientName);
        if (isOutGoing(sender)) {
            final OutgoingLetter out = new OutgoingLetter();
            out.setDocumentTypeId(DocumentSubtypeModel.Types.OUTGOING_LETTER);
            if (StringUtils.isNotBlank(sendMode)) {
                getNvlSendInfo(out).setSendMode(sendMode);
            }
            out.setSignerName(signerName);
            if (recipientName != null) {
                final int recipientSplitIndex = recipientName.lastIndexOf('/');
                if (recipientSplitIndex >= 0) {
                    out.setRecipientName(recipientName.substring(0, recipientSplitIndex));
                }
            }
            doc = out;
        } else {
            IncomingLetter in = new IncomingLetter();
            in.setDocumentTypeId(DocumentSubtypeModel.Types.INCOMING_LETTER);
            in.setSenderName(sender);
            in.setTransmittalMode(sendMode);
            final Date dueDate = getDate(row, DueDate);
            doc = in;
            if (dueDate == null) {
                doc.setDocStatus(DocumentStatus.WORKING.getValueName());
            } else {
                doc.setDocStatus(DocumentStatus.FINISHED.getValueName());
                in.setDueDate(dueDate);
            }
            addToComment(in, "Allkirjastaja", signerName);
        }
        { // common fields of letter
            doc.setSenderRegDate(getDate(row, SenderRegDate));
            doc.setSenderRegNumber(get(row, SenderRegNumber));
            addToComment(doc, "Vastus vajalik", get(row, ReplyNeeded));
            addToComment(doc, "Resolutsioon", get(row, Resolution));
            addToComment(doc, "Kellele suunatud (Täitja / Asutus)", recipientName);
            addToComment(doc, "Dokumendi link avalikku veebi", get(row, PublicLink));
            addToComment(doc, "Täitmismärge", get(row, CompletedNote));
        }
        return doc;
    }

    private boolean isOutGoing(final String sender) {
        if (StringUtils.equals(sender, SenderForOutgoin)) {
            return true;
        }
        return false;
    }

    private SendInfo getNvlSendInfo(final ImportDocument doc) {
        SendInfo sendInfo = doc.getSendInfo();
        if (sendInfo == null) {
            sendInfo = new SendInfo();
            doc.setSendInfo(sendInfo);
        }
        return sendInfo;
    }

>>>>>>> develop-5.1
}