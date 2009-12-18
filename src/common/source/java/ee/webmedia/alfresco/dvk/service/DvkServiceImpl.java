package ee.webmedia.alfresco.dvk.service;

import static junit.framework.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.apache.xmlbeans.XmlCursor;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.dvk.model.DvkModel;
import ee.webmedia.alfresco.dvk.model.DvkReceivedDocument;
import ee.webmedia.alfresco.dvk.model.DvkReceivedDocumentImpl;
import ee.webmedia.alfresco.dvk.model.DvkSendDocuments;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.XmlUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.xtee.client.service.DhlXTeeService;
import ee.webmedia.xtee.client.service.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.service.DhlXTeeService.MetainfoHelper;
import ee.webmedia.xtee.client.service.DhlXTeeService.ReceivedDocumentsWrapper;
import ee.webmedia.xtee.client.service.DhlXTeeService.SendDocumentsDokumentCallback;
import ee.webmedia.xtee.client.service.DhlXTeeService.SendDocumentsRequestCallback;
import ee.webmedia.xtee.client.service.DhlXTeeService.ReceivedDocumentsWrapper.ReceivedDocument;
import ee.webmedia.xtee.client.service.provider.XTeeProviderPropertiesResolver;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.AadressType;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.DhlDokumentType;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.DokumentDocument;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.MetainfoDocument.Metainfo;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.MetaxmlDocument.Metaxml;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.TransportDocument.Transport;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.rkelLetter.AccessRightsType;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.rkelLetter.Addressee;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.rkelLetter.Letter;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.rkelLetter.LetterType;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.rkelLetter.PartyType;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.rkelLetter.PersonType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.SendDocumentsV2RequestType;
import ee.webmedia.xtee.types.ee.sk.digiDoc.v13.DataFileType;
import ee.webmedia.xtee.types.ee.sk.digiDoc.v13.SignedDocType;

/**
 * @author Ats Uiboupin
 */
public class DvkServiceImpl implements DvkService {
    private static Log log = LogFactory.getLog(DvkServiceImpl.class);

    private String receivedDvkDocumentsPath;
    private DhlXTeeService dhlXTeeService;
    protected NodeService nodeService;
    protected FileFolderService fileFolderService;
    private MimetypeService mimetypeService;
    protected GeneralService generalService;
    private ParametersService parametersService;

    private XTeeProviderPropertiesResolver propertiesResolver;

    private String noTitleSpacePrefix;

    private BeanPropertyMapper<DvkReceivedDocument> beanPropertyMapper;

    // private SendOutService sendOutService;

    @Override
    public void updateOrganizationList() {
        dhlXTeeService.getDvkOrganizationsHelper().updateDvkCapableOrganisationsCache();
    }

    @Override
    public Collection<String> receiveDocuments() {
        final long maxReceiveDocumentsNr = parametersService.getLongParameter(Parameters.DVK_MAX_RECEIVE_DOCUMENTS_NR);
        NodeRef dvkIncomingFolder = generalService.getNodeRef(receivedDvkDocumentsPath);
        log.debug("Starting to receive documents(max " + maxReceiveDocumentsNr + " documents at the time)");
        final Set<String> receiveDocuments = new HashSet<String>();
        Collection<String> lastReceiveDocuments;
        int countServiceCalls = 0;
        do {
            lastReceiveDocuments = receiveDocumentsServiceCall((int)maxReceiveDocumentsNr, dvkIncomingFolder);
            dhlXTeeService.markDocumentsReceived(lastReceiveDocuments);
            receiveDocuments.addAll(lastReceiveDocuments);
            countServiceCalls++;
        } while (lastReceiveDocuments.size() >= maxReceiveDocumentsNr);
        log.debug("received " + receiveDocuments.size() + " documents from dvk with " + countServiceCalls + " DVK service calls");
        return receiveDocuments;
    }

    /**
     * @param maxReceiveDocumentsNr
     * @return nr of documents (not files in documents) received
     */
    private Collection<String> receiveDocumentsServiceCall(final int maxReceiveDocumentsNr, NodeRef dvkIncomingFolder) {
        final ReceivedDocumentsWrapper receiveDocuments = dhlXTeeService.receiveDocuments(maxReceiveDocumentsNr);
        final Set<String> receivedDocumentIds = new HashSet<String>();
        final List<String> receiveFaileddDocumentIds = new ArrayList<String>();
        log.debug("received " + receiveDocuments.size() + " documents from DVK");
        for (String dhlId : receiveDocuments) {
            final ReceivedDocument receivedDocument = receiveDocuments.get(dhlId);
            try {
                storeDocument(receivedDocument, dhlId, dvkIncomingFolder);
                receivedDocumentIds.add(dhlId);
            } catch (Exception e) { //FIXME do not catch Exception! transaction would be rolled back and we would be unable to continue anyway
                log.error("Failed to save files from dhlDokument:\n" + receivedDocument.getDhlDocument() + "\n\n", e);
                receiveFaileddDocumentIds.add(dhlId);
            }
        }
        log.info("received " + receivedDocumentIds.size() + " documents: " + receivedDocumentIds);
        if (receiveFaileddDocumentIds.size() > 0) {
            log.error("FAILED to receive " + receiveFaileddDocumentIds.size() + " documents: " + receiveFaileddDocumentIds);
        }
        return receivedDocumentIds;
    }

    protected NodeRef storeDocument(ReceivedDocument receivedDocument, String dhlId, NodeRef dvkIncomingFolder) {
        final MetainfoHelper metaInfoHelper = receivedDocument.getMetaInfoHelper();
        final DhlDokumentType dhlDokument = receivedDocument.getDhlDocument();
        final SignedDocType signedDoc = receivedDocument.getSignedDoc();
        log.debug("dokument element=\n" + dhlDokument + "'");
        log.debug("helper.getObject(DhlIdDocumentImpl)=" + dhlId + " " + metaInfoHelper.getDhlSaatjaAsutuseNimi() + " "
                + metaInfoHelper.getDhlSaatjaAsutuseNr() + " saadeti: " + metaInfoHelper.getDhlSaatmisAeg() + " saabus: "
                + metaInfoHelper.getDhlSaabumisAeg() + "\nmetaManual:\nKoostajaFailinimi: " + metaInfoHelper.getKoostajaFailinimi());
        assertTrue(StringUtils.isNotBlank(dhlId));
        Transport transport = dhlDokument.getTransport();
        AadressType saatja = transport.getSaatja();
        assertTrue(saatja != null && StringUtils.isNotBlank(saatja.getRegnr()));
        log.debug("sender: " + saatja.getRegnr() + " : " + saatja.getAsutuseNimi());

        List<DataFileType> dataFileList = signedDoc.getDataFileList();
        log.debug("document contains " + dataFileList.size() + " datafiles");

        // gather properties that will be attached to space created for this document
        final DvkReceivedDocument rd = new DvkReceivedDocumentImpl();
        rd.setDvkId(dhlId);
        rd.setSenderRegNr(metaInfoHelper.getDhlSaatjaAsutuseNr());
        rd.setSenderOrgName(metaInfoHelper.getDhlSaatjaAsutuseNimi());
        rd.setSenderEmail(metaInfoHelper.getDhlSaatjaEpost());
        fillLetterData(rd, dhlDokument);

        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy_hhmm");
        final String timeStamp = formatter.format(metaInfoHelper.getDhlSaabumisAeg().getTime());
        String documentFolderName;
        if (StringUtils.isNotBlank(rd.getLetterSenderTitle())) {
            documentFolderName = rd.getLetterSenderTitle();
        } else {
            documentFolderName = noTitleSpacePrefix + dataFileList.get(0).getFilename();
        }
        documentFolderName += "_" + timeStamp;

        NodeRef documentFolder = createDocumentNode(rd, dvkIncomingFolder, documentFolderName);

        for (DataFileType dataFile : dataFileList) {
            storeFile(rd, documentFolder, dataFile);
        }
        return documentFolder;
    }

    protected NodeRef createDocumentNode(DvkReceivedDocument rd, NodeRef dvkIncomingFolder, String documentFolderName) {
        final Map<org.alfresco.service.namespace.QName, Serializable> properties//
        = getBeanPropertyMapper().toProperties(rd);
        if (log.isDebugEnabled()) {
            log.debug("Prepared space metadata\n from object: '" + ToStringBuilder.reflectionToString(rd) + "' to properties: '" + properties + "'");
        }
        FileInfo documentFolder = fileFolderService.create(dvkIncomingFolder, documentFolderName, ContentModel.TYPE_FOLDER);
        nodeService.addAspect(documentFolder.getNodeRef(), DvkModel.Aspects.RECEIVED_DVK_DOCUMENT, null);
        nodeService.addAspect(documentFolder.getNodeRef(), DvkModel.Aspects.ACCESS_RIGHTS, null);
        nodeService.addProperties(documentFolder.getNodeRef(), properties);
        return documentFolder.getNodeRef();
    }

    /**
     * @return cached BeanPropertyMapper for DvkReceivedDocumentImpl.class or create it if not yet created
     */
    private BeanPropertyMapper<DvkReceivedDocument> getBeanPropertyMapper() {
        if (beanPropertyMapper == null) {
            beanPropertyMapper = BeanPropertyMapper.newInstance(DvkReceivedDocument.class);
        }
        return beanPropertyMapper;
    }

    protected NodeRef storeFile(DvkReceivedDocument rd, NodeRef documentFolder, DataFileType dataFile) {
        String filename = dataFile.getId() + " " + dataFile.getFilename();
        log.info("writing file '" + filename + "' from dvk document with dvkId '" + dataFile.getId() + "' to repository space: '"
                + receivedDvkDocumentsPath + "'");
        NodeRef file = createFileNode(rd, documentFolder, filename);

        final ContentWriter writer = fileFolderService.getWriter(file);
        String mimeType = dataFile.getMimeType();
        if (StringUtils.isEmpty(mimeType)) { // DataFile MimeType attribute may be empty
            mimeType = mimetypeService.guessMimetype(filename);
        }
        writer.setMimetype(mimeType);
        try {
            final OutputStream os = writer.getContentOutputStream();
            os.write(Base64.decode(dataFile.getStringValue()));
            os.close();
            return file;
        } catch (Base64DecodingException e) {
            throw new RuntimeException("Failed to decode", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed write output to repository: '" + receivedDvkDocumentsPath + "' nodeRef=" + file + " contentUrl=" + writer.getContentUrl(), e);
        }
    }

    protected NodeRef createFileNode(DvkReceivedDocument rd, NodeRef documentFolder, String filename) {
        return fileFolderService.create(documentFolder, filename, ContentModel.TYPE_CONTENT).getNodeRef();
    }

    private void fillLetterData(DvkReceivedDocument rd, DhlDokumentType dhlDokument) {
        final Metaxml metaxml = dhlDokument.getMetaxml();
        final String xml = metaxml.toString();
        Letter letter;
        try {
            letter = XmlUtil.getTypeFromXml(xml, Letter.class);
        } catch (Exception e) {
            return;// letter content was not found in metaxml or didn't match schema
        }

        /*
         * Usually we can get 2 types of RuntimeExceptions:
         * NullpointerException - if parent element is present, but child is not
         * some kind of xmlbeans exception - when there is empty element(for example <signDate />);
         * there doesn't appear to be a way to check for this situation in XmlBeans
         */
        try {
            rd.setLetterSenderDocSignDate(letter.getLetterMetaData().getSignDate().getTime());
        } catch (RuntimeException e) {
        }
        try {
            rd.setLetterSenderDocNr(letter.getLetterMetaData().getSenderIdentifier());
        } catch (RuntimeException e) {
        }
        try {
            rd.setLetterSenderTitle(letter.getLetterMetaData().getTitle());
        } catch (RuntimeException e) {
        }
        try {
            rd.setLetterDeadLine(letter.getLetterMetaData().getDeadline().getTime());
        } catch (RuntimeException e) {
        }
        try {
            rd.setLetterAccessRestriction(letter.getLetterMetaData().getAccessRights().getRestriction());
        } catch (RuntimeException e) {
        }
        try {
            rd.setLetterAccessRestrictionBeginDate(letter.getLetterMetaData().getAccessRights().getBeginDate().getTime());
        } catch (RuntimeException e) {
        }
        try {
            rd.setLetterAccessRestrictionEndDate(letter.getLetterMetaData().getAccessRights().getEndDate().getTime());
        } catch (RuntimeException e) {
        }
        try {
            rd.setLetterAccessRestrictionReason(letter.getLetterMetaData().getAccessRights().getReason());
        } catch (RuntimeException e) {
        }
    }

    @Override
    public List<String> sendDocuments(NodeRef document, Collection<ContentToSend> contentsToSend, final DvkSendDocuments sd) {
        final Collection<String> recipientsRegNrs = sd.getRecipientsRegNrs();
        if (contentsToSend.size() == 0 || recipientsRegNrs.size() == 0) {
            throw new IllegalArgumentException("To send files using DVK you must have at least one file and recipient(contentsToSend='"+contentsToSend+"', recipientsRegNrs='"+recipientsRegNrs+"')");
        }
        final List<String> sendDocuments = dhlXTeeService.sendDocuments(contentsToSend, getRecipients(recipientsRegNrs), getSenderAddress(),
                new SimDhsSendDocumentsCallback(sd), new SendDocumentsRequestCallback() {

            @Override
            public void doWithRequest(SendDocumentsV2RequestType dokumentDocument) {
                final Long dvkRetainDaysPeriod = parametersService.getLongParameter(Parameters.DVK_RETAIN_PERIOD);
                final Calendar retainCal = Calendar.getInstance();
                retainCal.add(Calendar.DAY_OF_MONTH, dvkRetainDaysPeriod.intValue());
                dokumentDocument.setSailitustahtaeg(retainCal);
            }
        });
        Assert.assertEquals(1, sendDocuments.size());
        // TODO: sendOutService
        // final String dhlId = sendDocuments.get(0);
        // for (String regNr : recipientsRegNrs) {
        // sendOutService.addDvkSendOut(document, getOrganisationName(regNr), dhlId, regNr);
        // }
        return sendDocuments;
    }

    private String getOrganisationName(String addresseeRegNum) {
        // TODO: implementation will probably change
        return dhlXTeeService.getDvkOrganizationsHelper().getOrganizationName(addresseeRegNum);
    }

    private AadressType[] getRecipients(Collection<String> recipientsRegNrs) {
        AadressType[] recipients = new AadressType[recipientsRegNrs.size()];
        int i = 0;
        for (String regNr : recipientsRegNrs) {
            AadressType recipient = AadressType.Factory.newInstance();
            recipient.setRegnr(regNr);
            recipients[i] = recipient;
            i++;
        }
        return recipients;
    }

    private AadressType getSenderAddress() {
        AadressType sender = AadressType.Factory.newInstance();
        sender.setRegnr(propertiesResolver.getProperty("institution"));
        sender.setAsutuseNimi(propertiesResolver.getProperty("institution_name"));
        return sender;
    }

    private class SimDhsSendDocumentsCallback implements SendDocumentsDokumentCallback {
        private DvkSendDocuments dvkSendDocuments;

        public SimDhsSendDocumentsCallback(DvkSendDocuments dvkSendDocuments) {
            dvkSendDocuments.validateOutGoing();
            this.dvkSendDocuments = dvkSendDocuments;
        }

        @Override
        public void doWithDocument(DokumentDocument dokumentDocument) {
            log.debug("altering dokument");
            final Letter letter = Letter.Factory.newInstance();
            final Addressee letterAddressees = letter.addNewAddressees();
            final Transport transport = Transport.Factory.newInstance();
            // add senders information
            final AadressType letterTransportSaatja = transport.addNewSaatja();
            letterTransportSaatja.setRegnr(dvkSendDocuments.getSenderRegNr());
            letterTransportSaatja.setEpost(dvkSendDocuments.getSenderEmail());
            letterTransportSaatja.setNimi(dvkSendDocuments.getSenderOrgName());
            letter.addNewAuthor().addNewOrganisation().setOrganisationName(dvkSendDocuments.getSenderOrgName());
            // 
            final LetterType letterMeta = letter.addNewLetterMetaData();

            final Calendar calSignDate = Calendar.getInstance();
            final Date letterSenderDocSignDate = dvkSendDocuments.getLetterSenderDocSignDate();
            calSignDate.setTime(letterSenderDocSignDate);
            letterMeta.setSignDate(calSignDate);

            letterMeta.setSenderIdentifier(dvkSendDocuments.getLetterSenderDocNr());
            letterMeta.setTitle(dvkSendDocuments.getLetterSenderTitle());
            letterMeta.setType(dvkSendDocuments.getDocType());
            final PersonType letterCompilator = letter.addNewCompilators().addNewCompilator();
            letterCompilator.setFirstname(dvkSendDocuments.getLetterCompilatorFirstname());
            letterCompilator.setSurname(dvkSendDocuments.getLetterCompilatorSurname());
            letterCompilator.setJobtitle(dvkSendDocuments.getLetterCompilatorJobTitle());
            //
            final AccessRightsType accessRights = AccessRightsType.Factory.newInstance();
            accessRights.setRestriction(dvkSendDocuments.getLetterAccessRestriction());

            final Calendar accessRestrBeginCal = Calendar.getInstance();
            final Date arBeginDate = dvkSendDocuments.getLetterAccessRestrictionBeginDate();
            if (arBeginDate != null) {
                accessRestrBeginCal.setTime(arBeginDate);
            }
            accessRights.setBeginDate(accessRestrBeginCal);

            final Calendar accessRestrEndCal = Calendar.getInstance();
            final Date arEndDate = dvkSendDocuments.getLetterAccessRestrictionEndDate();
            if (arEndDate != null) {
                accessRestrEndCal.setTime(arEndDate);
            }
            accessRights.setEndDate(accessRestrEndCal);

            accessRights.setReason(dvkSendDocuments.getLetterAccessRestrictionReason());

            letterMeta.setAccessRights(accessRights);
            //
            for (String addresseeRegNum : dvkSendDocuments.getRecipientsRegNrs()) {
                transport.addNewSaaja().setRegnr(addresseeRegNum);
                final PartyType letterAddressee = letterAddressees.addNewAddressee();
                letterAddressee.addNewOrganisation().setOrganisationName(getOrganisationName(addresseeRegNum));
            }

            final DhlDokumentType dhlDokument = dokumentDocument.getDokument();

            final Metaxml metaxml = composeMetaxml(letter);
            dhlDokument.setMetaxml(metaxml);

            dhlDokument.setTransport(transport);
            log.debug("\n\naltered dhlDokument:\n" + dhlDokument + "\n\n");
        }
        
        private Metaxml composeMetaxml(Letter letter) {
          final Metaxml metaInfo = Metaxml.Factory.newInstance();
          log.debug("letter:\n" + letter + "\n\n");
          final XmlCursor cursorL = letter.newCursor();
          final XmlCursor cursorM = metaInfo.newCursor();
          log.debug("metaInfo1:\n" + metaInfo + "\n\n");
          cursorM.toNextToken();
          cursorL.copyXmlContents(cursorM);
          log.debug("metaInfo2:\n" + metaInfo + "\n\n");
          cursorL.dispose();
          cursorM.dispose();
          return metaInfo;
      }
    };

    // START: getters / setters
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setReceivedDvkDocumentsPath(String receivedDvkDocumentsPath) {
        this.receivedDvkDocumentsPath = receivedDvkDocumentsPath;
    }

    public void setDhlXTeeService(DhlXTeeService dhlXTeeService) {
        this.dhlXTeeService = dhlXTeeService;
    }

    public void setPropertiesResolver(XTeeProviderPropertiesResolver propertiesResolver) {
        this.propertiesResolver = propertiesResolver;
    }

    public void setNoTitleSpacePrefix(String noTitleSpacePrefix) {
        this.noTitleSpacePrefix = noTitleSpacePrefix;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }
    // END: getters / setters

}
