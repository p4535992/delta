package ee.webmedia.alfresco.document.type.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;

public class DocumentTypeHelper {

    public static final QName[] INCOMING_LETTER_TYPES;
    public static final QName[] OUTGOING_LETTER_TYPES;
    public static final QName[] INSTRUMENT_OF_DELIVERY_AND_RECEIPT_TYPES;
    public static final Collection<QName> incomingLetterTypes;
    public static final Collection<QName> outgoingLetterTypes;
    public static final Collection<QName> incomingOutgoingLetterTypes;

    static {
        INCOMING_LETTER_TYPES = new QName[] { DocumentSubtypeModel.Types.INCOMING_LETTER, DocumentSubtypeModel.Types.INCOMING_LETTER_MV };
        OUTGOING_LETTER_TYPES = new QName[] { DocumentSubtypeModel.Types.OUTGOING_LETTER, DocumentSubtypeModel.Types.OUTGOING_LETTER_MV };
        INSTRUMENT_OF_DELIVERY_AND_RECEIPT_TYPES = new QName[] { DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT
                , DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT_MV };
        incomingLetterTypes = Collections.unmodifiableCollection(Arrays.asList(INCOMING_LETTER_TYPES));
        outgoingLetterTypes = Collections.unmodifiableCollection(Arrays.asList(OUTGOING_LETTER_TYPES));
        
        List<QName> incomingOutgoingList = new ArrayList<QName>();
        incomingOutgoingList.addAll(incomingLetterTypes);
        incomingOutgoingList.addAll(outgoingLetterTypes);
        incomingOutgoingLetterTypes = Collections.unmodifiableCollection(incomingOutgoingList);
    }

    public static boolean isIncomingLetter(QName documentType) {
        return is(documentType, INCOMING_LETTER_TYPES);
    }

    public static boolean isOutgoingLetter(QName documentType) {
        return is(documentType, OUTGOING_LETTER_TYPES);
    }
    
    public static boolean isInstrumentOfDeliveryAndReciept(QName documentType) {
        return is(documentType, INSTRUMENT_OF_DELIVERY_AND_RECEIPT_TYPES);
    }    

    public static boolean isIncomingOrOutgoingLetter(QName documentType) {
        return isIncomingLetter(documentType) || isOutgoingLetter(documentType);
    }

    public static boolean is(QName documentType, QName... allowedTypes) {
        for (QName allowedType : allowedTypes) {
            if (allowedType.equals(documentType)) {
                return true;
            }
        }
        return false;
    }

}
