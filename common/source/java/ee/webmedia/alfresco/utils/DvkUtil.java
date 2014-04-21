package ee.webmedia.alfresco.utils;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.dvk.model.DvkModel;
import ee.webmedia.alfresco.dvk.model.DvkReceivedLetterDocument;
import ee.webmedia.xtee.client.dhl.DhlDocumentVersion;
import ee.webmedia.xtee.client.dhl.DhlXTeeService;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.OrganisationType;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.DhlDokumentType;
import ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Class containing various utilities for DVK/DEC functionality
 */
public class DvkUtil {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DvkUtil.class);

    public static boolean isAditDocument(String senderOrganisationCode) {
        Assert.hasLength("Organisation code is mandatory parameter!");
        return ee.webmedia.alfresco.adit.service.AditService.NAME.equalsIgnoreCase(senderOrganisationCode);
    }

    /**
     * Return the file contents as
     * @param dataFile
     * @param <F>
     * @return
     */
    public static <F extends XmlObject> InputStream getFileContents(F dataFile) throws Base64DecodingException, IOException {
        if (dataFile instanceof DataFileType) {
            return new ByteArrayInputStream(org.apache.xml.security.utils.Base64.decode(((DataFileType) dataFile).getStringValue()));
        } else if (dataFile instanceof DecContainerDocument.DecContainer.File) {
            return
            //new GZIPInputStream( // FIXME Hetkel commitin testimise huvides puuduliku funktsionaalsusega seisu.
            // Caused by: java.io.IOException: Not in GZIP format
            // at java.util.zip.GZIPInputStream.readHeader(GZIPInputStream.java:143)
                    new ByteArrayInputStream(Base64.decode(((DecContainerDocument.DecContainer.File) dataFile).getZipBase64Content())
            //)
            );
        }
        throw new IllegalArgumentException("Expected ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType or " +
                "ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.File!");
    }

    public static <F extends XmlObject> F setFileContents(F dataFile, String stringContents) throws IOException {
        if (dataFile instanceof DataFileType) {
            ((DataFileType) dataFile).setStringValue(stringContents);
        } else if (dataFile instanceof DecContainerDocument.DecContainer.File) {
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(new StringReader(stringContents),
                    //new GZIPOutputStream( // FIXME Vt üles.
                    arrayOutputStream //)
                    );
            ((DecContainerDocument.DecContainer.File) dataFile).setZipBase64Content(Base64.encode(arrayOutputStream.toByteArray()));
        } else {
            throw new IllegalArgumentException("Expected ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType or " +
                    "ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.File!");
        }

        return dataFile;
    }

    public static String getCorruptedDocumentName(DhlXTeeService.ReceivedDocumentsWrapper.ReceivedDocument receivedDocument, String dhlId) {
        String[] objects;
        DhlDocumentVersion documentVersion = receivedDocument.getDocumentVersion();
        if (DhlDocumentVersion.VER_1.equals(documentVersion)) {
            DhlXTeeService.MetainfoHelper metaInfoHelper = receivedDocument.getMetaInfoHelper();
            objects = new String[] { dhlId, metaInfoHelper.getDhlSaatjaAsutuseNr(), metaInfoHelper.getDhlSaatjaAsutuseNimi() };
        } else if (DhlDocumentVersion.VER_2.equals(documentVersion)) {
            DecContainerDocument.DecContainer.Transport.DecSender organisation = receivedDocument.getDhlDocumentV2().getTransport().getDecSender();
            objects = new String[] { dhlId, organisation.getOrganisationCode(), organisation.getStructuralUnit() };
        } else {
            throw new IllegalArgumentException("Only DhlDocumentVersion.VER_1 and DhlDocumentVersion.VER_2 are currently supported!");
        }
        return String.format("%s %s %s", objects);
    }

    public static <F extends XmlObject> String getDvkIdAndFilename(F dataFile) {
        return getFileId(dataFile) + " " + getFileName(dataFile);
    }

    public static <F extends XmlObject> String getFileId(F dataFile) {
        if (dataFile instanceof DataFileType) {
            return ((DataFileType) dataFile).getId();
        } else if (dataFile instanceof DecContainerDocument.DecContainer.File) {
            return ((DecContainerDocument.DecContainer.File) dataFile).getFileGuid();
        }
        throw new IllegalArgumentException("Expected ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType or " +
                "ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.File!");
    }

    public static <F extends XmlObject> String getFileName(F dataFile) {
        if (dataFile instanceof DataFileType) {
            return ((DataFileType) dataFile).getFilename();
        } else if (dataFile instanceof DecContainerDocument.DecContainer.File) {
            return ((DecContainerDocument.DecContainer.File) dataFile).getFileName();
        }
        throw new IllegalArgumentException("Expected ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType or " +
                "ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.File!");
    }

    public static <F extends XmlObject, X extends XmlTokenSource> String getFileMimeType(F dataFile) {
        if (dataFile instanceof DataFileType) {
            return ((DataFileType) dataFile).getMimeType();
        } else if (dataFile instanceof DecContainerDocument.DecContainer.File) {
            return ((DecContainerDocument.DecContainer.File) dataFile).getMimeType();
        }
        throw new IllegalArgumentException("Expected ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType or " +
                "ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.File!");
    }

    public static <X extends XmlTokenSource, D extends XmlObject> X getMetaXmlElement(D dhlDokument) {
        if (dhlDokument instanceof DhlDokumentType) {
            return (X) ((DhlDokumentType) dhlDokument).getMetaxml();
        } else if (dhlDokument instanceof DecContainerDocument.DecContainer) {
            return (X) ((DecContainerDocument.DecContainer) dhlDokument).getRecordTypeSpecificMetadata();
        }
        throw new IllegalArgumentException("Expected ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.DhlDokumentType or " +
                "ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer!");
    }

    public static Map<QName, Serializable> fillV1PropsFromDvkReceivedDocument(DvkReceivedLetterDocument rd) {
        HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();

        // common
        props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION, rd.getLetterAccessRestriction());
        props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, rd.getLetterAccessRestrictionBeginDate());
        props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE, rd.getLetterAccessRestrictionEndDate());
        props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON, rd.getLetterAccessRestrictionReason());
        // specific
        props.put(DocumentSpecificModel.Props.SENDER_DETAILS_NAME, rd.getSenderOrgName());
        props.put(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL, rd.getSenderEmail());
        props.put(DocumentSpecificModel.Props.SENDER_REG_DATE, rd.getLetterSenderDocSignDate());
        props.put(DocumentSpecificModel.Props.SENDER_REG_NUMBER, rd.getLetterSenderDocNr());
        props.put(DocumentSpecificModel.Props.DUE_DATE, rd.getLetterDeadLine());
        //
        props.put(DvkModel.Props.DVK_ID, rd.getDvkId());

        return props;
    }

    public static Map<QName, Serializable> setAccessRestrictionProperties(DecContainerDocument.DecContainer decContainer, Map<QName, Serializable> props) {
        props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION, decContainer.getAccess().getAccessConditionsCode().toString());

        List<DecContainerDocument.DecContainer.Access.AccessRestriction> accessRestrictionList = decContainer.getAccess().getAccessRestrictionList();
        Date today = new Date();
        for (DecContainerDocument.DecContainer.Access.AccessRestriction accessRestriction : accessRestrictionList) {
            Calendar restrictionInvalidSince = accessRestriction.getRestrictionInvalidSince();
            Calendar restrictionEndDate = accessRestriction.getRestrictionEndDate();
            if ((restrictionEndDate == null || restrictionEndDate.getTime().after(today)) && (restrictionInvalidSince == null || restrictionInvalidSince.after(today))) {
                props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON, accessRestriction.getRestrictionBasis());
                props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, CalendarUtil.getDateOrNull(accessRestriction.getRestrictionBeginDate()));
                props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE, CalendarUtil.getDateOrNull(accessRestriction.getRestrictionEndDate()));
                props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC, accessRestriction.getRestrictionEndEvent());
                break;
            }
        }
        return props;
    }

    public static XmlOptions getDecContainerXmlOptions() {
        return new XmlOptions()
                .setSaveSuggestedPrefixes(Collections.singletonMap("http://www.riik.ee/schemas/deccontainer/vers_2_1/", ""))
                .setSaveSyntheticDocumentElement(new javax.xml.namespace.QName("http://www.riik.ee/schemas/deccontainer/vers_2_1/", "DecContainer"))
                .setSaveNamespacesFirst();
    }

    public static String getMessageForRecipient(DecContainerDocument.DecContainer decContainer, String recipientOrganisationCode) {
        Assert.hasText(recipientOrganisationCode, "Must specify an organisation code!");

        String message = null;
        List<DecContainerDocument.DecContainer.Recipient> recipientList = decContainer.getRecipientList();
        if (recipientList != null) {
            for (DecContainerDocument.DecContainer.Recipient recipient : recipientList) {
                OrganisationType organisation = recipient.getOrganisation();
                if (organisation != null && recipientOrganisationCode.equals(organisation.getOrganisationCode())) {
                    message = recipient.getMessageForRecipient();
                    break;
                }
            }
        }
        return StringUtils.defaultString(message, "");
    }
}
