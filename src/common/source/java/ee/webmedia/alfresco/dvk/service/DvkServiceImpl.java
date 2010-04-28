package ee.webmedia.alfresco.dvk.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.dvk.model.DvkReceivedDocument;
import ee.webmedia.alfresco.dvk.model.DvkReceivedDocumentImpl;
import ee.webmedia.alfresco.dvk.model.DvkSendDocuments;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.XmlUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.xtee.client.service.DhlXTeeService;
import ee.webmedia.xtee.client.service.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.service.DhlXTeeService.MetainfoHelper;
import ee.webmedia.xtee.client.service.DhlXTeeService.ReceivedDocumentsWrapper;
import ee.webmedia.xtee.client.service.DhlXTeeService.SendDocumentsDokumentCallback;
import ee.webmedia.xtee.client.service.DhlXTeeService.SendDocumentsRequestCallback;
import ee.webmedia.xtee.client.service.DhlXTeeService.ReceivedDocumentsWrapper.ReceivedDocument;
import ee.webmedia.xtee.client.service.configuration.provider.XTeeProviderPropertiesResolver;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.AadressType;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.DhlDokumentType;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.DokumentDocument;
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
public abstract class DvkServiceImpl implements DvkService {
    private static Log log = LogFactory.getLog(DvkServiceImpl.class);

    private String receivedDvkDocumentsPath;
    protected String corruptDvkDocumentsPath;
    protected DhlXTeeService dhlXTeeService;
    protected NodeService nodeService;
    protected FileFolderService fileFolderService;
    private MimetypeService mimetypeService;
    protected GeneralService generalService;
    private ParametersService parametersService;
    private AddressbookService addressbookService;

    private XTeeProviderPropertiesResolver propertiesResolver;

    private String noTitleSpacePrefix;

    // private SendOutService sendOutService;

    @Override
    public int updateOrganizationsDvkCapability() {
        final Map<String /* regNum */, String /* orgName */> sendingOptions = getSendingOptions();
        final List<Node> organizations = addressbookService.listOrganization();
        int dvkCapableOrgs = 0;
        for (Node orgNode : organizations) {
            final Map<String, Object> oProps = orgNode.getProperties();
            String orgCode = (String) oProps.get(AddressbookModel.Props.ORGANIZATION_CODE.toString());
            final boolean dvkCapable = sendingOptions.containsKey(orgCode);
            oProps.put(AddressbookModel.Props.DVK_CAPABLE.toString(), dvkCapable);
            addressbookService.updateNode(orgNode);
            if (dvkCapable) {
                dvkCapableOrgs++;
            }
        }
        return dvkCapableOrgs;
    }

    @Override
    public Map<String, String> getSendingOptions() {
        return dhlXTeeService.getSendingOptions();
    }

    @Override
    public void updateOrganizationList() {
        dhlXTeeService.getDvkOrganizationsHelper().updateDvkCapableOrganisationsCache();
    }

    @Override
    public Collection<String> receiveDocuments() {
        final long maxReceiveDocumentsNr = parametersService.getLongParameter(Parameters.DVK_MAX_RECEIVE_DOCUMENTS_NR);
        NodeRef dvkIncomingFolder = generalService.getNodeRef(receivedDvkDocumentsPath);
        log.info("Starting to receive documents(max " + maxReceiveDocumentsNr + " documents at the time)");
        final Set<String> receiveDocuments = new HashSet<String>();
        Collection<String> lastReceiveDocuments;
        Collection<String> lastFailedDocuments;
        Collection<String> previouslyFailedDvkIds = getPreviouslyFailedDvkIds();
        int countServiceCalls = 0;
        do {
            final Pair<Collection<String>, Collection<String>> results //
            = receiveDocumentsServiceCall((int) maxReceiveDocumentsNr, dvkIncomingFolder, previouslyFailedDvkIds);
            lastReceiveDocuments = results.getFirst();
            lastFailedDocuments = results.getSecond();
            if (lastReceiveDocuments.size() != 0) {
                dhlXTeeService.markDocumentsReceived(lastReceiveDocuments);
                receiveDocuments.addAll(lastReceiveDocuments);
            }
            countServiceCalls++;
        } while (lastReceiveDocuments.size() >= maxReceiveDocumentsNr);
        log.info("received " + receiveDocuments.size() + " documents from dvk with " + countServiceCalls + " DVK service calls");
        if (lastFailedDocuments.size() != 0) {
            log.error("failed to receive " + lastFailedDocuments.size() + " documents from dvk with "
                    + countServiceCalls + " DVK service calls: " + lastFailedDocuments);
        }
        return receiveDocuments;
    }

    /**
     * @param maxReceiveDocumentsNr
     * @return nr of documents (not files in documents) received
     */
    private Pair<Collection<String>, Collection<String>> receiveDocumentsServiceCall(final int maxReceiveDocumentsNr
            , NodeRef dvkIncomingFolder, Collection<String> previouslyFailedDvkIds) {
        final ReceivedDocumentsWrapper receiveDocuments = dhlXTeeService.receiveDocuments(maxReceiveDocumentsNr);
        final Set<String> receivedDocumentIds = new HashSet<String>();
        final List<String> receiveFaileddDocumentIds = new ArrayList<String>();
        log.debug("received " + receiveDocuments.size() + " documents from DVK");
        for (String dhlId : receiveDocuments) {
            final ReceivedDocument receivedDocument = receiveDocuments.get(dhlId);
            NodeRef storedDocument = null;
            try {
                storedDocument = storeDocument(receivedDocument, dhlId, dvkIncomingFolder, previouslyFailedDvkIds);
            } catch (RuntimeException e) {
                throw e;// didn't even manage to handle exception
            }
            if (storedDocument != null) {
                receivedDocumentIds.add(dhlId);
            } else {
                receiveFaileddDocumentIds.add(dhlId);
            }
        }
        log.debug("received " + receivedDocumentIds.size() + " documents: " + receivedDocumentIds);
        if (receiveFaileddDocumentIds.size() > 0) {
            log.error("FAILED to receive " + receiveFaileddDocumentIds.size() + " documents: " + receiveFaileddDocumentIds);
        }

        return new Pair<Collection<String>, Collection<String>>(receivedDocumentIds, receiveFaileddDocumentIds);
    }

    protected NodeRef storeDocument(ReceivedDocument receivedDocument, String dhlId, NodeRef dvkIncomingFolder, Collection<String> previouslyFailedDvkIds) {
        final MetainfoHelper metaInfoHelper = receivedDocument.getMetaInfoHelper();
        final DhlDokumentType dhlDokument = receivedDocument.getDhlDocument();
        final SignedDocType signedDoc = receivedDocument.getSignedDoc();
        if (log.isTraceEnabled()) {
            log.trace("dokument element=\n" + dhlDokument + "'");
            log.trace("helper.getObject(DhlIdDocumentImpl)=" + dhlId + " " + metaInfoHelper.getDhlSaatjaAsutuseNimi() + " "
                    + metaInfoHelper.getDhlSaatjaAsutuseNr() + " saadeti: " + metaInfoHelper.getDhlSaatmisAeg() + " saabus: "
                    + metaInfoHelper.getDhlSaabumisAeg() + "\nmetaManual:\nKoostajaFailinimi: " + metaInfoHelper.getKoostajaFailinimi());
        }

        try {
            Assert.isTrue(StringUtils.isNotBlank(dhlId), "dhlId can't be blank");
            Transport transport = dhlDokument.getTransport();
            AadressType saatja = transport.getSaatja();
            Assert.isTrue(StringUtils.isNotBlank(saatja.getRegnr()), "sender regNr can't be blank");
            if (log.isDebugEnabled()) {
                log.debug("sender: " + saatja.getRegnr() + " : " + saatja.getAsutuseNimi());
            }

            List<DataFileType> dataFileList = signedDoc.getDataFileList();
            log.debug("document contains " + dataFileList.size() + " datafiles");

            // gather properties that will be attached to space created for this document
            final DvkReceivedDocument rd = new DvkReceivedDocumentImpl();
            rd.setDvkId(dhlId);
            rd.setSenderRegNr(metaInfoHelper.getDhlSaatjaAsutuseNr());
            rd.setSenderOrgName(metaInfoHelper.getDhlSaatjaAsutuseNimi());
            rd.setSenderEmail(metaInfoHelper.getDhlSaatjaEpost());
            fillLetterData(rd, dhlDokument);

            String documentFolderName;
            if (StringUtils.isNotBlank(rd.getLetterSenderTitle())) {
                documentFolderName = rd.getLetterSenderTitle();
            } else {
                documentFolderName = noTitleSpacePrefix + dataFileList.get(0).getFilename();
            }

            NodeRef documentFolder = createDocumentNode(rd, dvkIncomingFolder, documentFolderName);

            for (DataFileType dataFile : dataFileList) {
                storeFile(rd, documentFolder, dataFile);
            }
            return documentFolder;
        } catch (AlfrescoRuntimeException e) {
            final String msg = "Failed to store document with dhlId='" + dhlId + "'";
            log.fatal(msg, e);
            throw new RuntimeException(msg, e);
        } catch (Exception e) {
            handleStorageFailure(receivedDocument, dhlId, dvkIncomingFolder, metaInfoHelper, previouslyFailedDvkIds, e);
            return null;
        }
    }

    /**
     * @param receivedDocument
     * @param dhlId
     * @param dvkIncomingFolder
     * @param metaInfoHelper
     * @param previouslyFailedDvkIds
     * @param e
     */
    protected void handleStorageFailure(ReceivedDocument receivedDocument, String dhlId, NodeRef dvkIncomingFolder
            , MetainfoHelper metaInfoHelper, Collection<String> previouslyFailedDvkIds, Exception e) {
        log.error("Failed to store document with dhlId='" + dhlId + "'", e);
    }

    abstract protected NodeRef createDocumentNode(DvkReceivedDocument rd, NodeRef dvkIncomingFolder, String documentFolderName);

    protected Collection<String> getPreviouslyFailedDvkIds() {
        // FIXME: kui tk projektis ka vastu võtmine implemenditakse, siis võiks järgneva rea asendada sim'i ilmplementatsiooniga
        return new HashSet<String>(0);
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
            throw new RuntimeException("Failed write output to repository: '" + receivedDvkDocumentsPath + "' nodeRef=" + file + " contentUrl="
                    + writer.getContentUrl(), e);
        }
    }

    /**
     * @param rd
     * @param documentFolder
     * @param filename
     * @return
     */
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
        } catch (RuntimeException e) {//
        }
        try {
            rd.setLetterSenderDocNr(letter.getLetterMetaData().getSenderIdentifier());
        } catch (RuntimeException e) {//
        }
        try {
            rd.setLetterSenderTitle(letter.getLetterMetaData().getTitle());
        } catch (RuntimeException e) {//
        }
        try {
            rd.setLetterDeadLine(letter.getLetterMetaData().getDeadline().getTime());
        } catch (RuntimeException e) {//
        }
        try {
            rd.setLetterAccessRestriction(letter.getLetterMetaData().getAccessRights().getRestriction());
        } catch (RuntimeException e) {//
        }
        try {
            rd.setLetterAccessRestrictionBeginDate(letter.getLetterMetaData().getAccessRights().getBeginDate().getTime());
        } catch (RuntimeException e) {//
        }
        try {
            rd.setLetterAccessRestrictionEndDate(letter.getLetterMetaData().getAccessRights().getEndDate().getTime());
        } catch (RuntimeException e) {//
        }
        try {
            rd.setLetterAccessRestrictionReason(letter.getLetterMetaData().getAccessRights().getReason());
        } catch (RuntimeException e) {//
        }
    }

    @Override
    public String sendDocuments(NodeRef document, Collection<ContentToSend> contentsToSend, final DvkSendDocuments sd) {
        final Collection<String> recipientsRegNrs = sd.getRecipientsRegNrs();
        if (contentsToSend.size() == 0 || recipientsRegNrs.size() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("To send files using DVK you must have at least one file and recipient(contentsToSend='" //
                        + contentsToSend + "', recipientsRegNrs='" + recipientsRegNrs + "')");
            }
            throw new UnableToPerformException(MessageSeverity.ERROR, "dvk_send_error_notEnoughData");
        }
        final Set<String> sendDocuments = dhlXTeeService.sendDocuments(contentsToSend, getRecipients(recipientsRegNrs), getSenderAddress(),
                new SimDhsSendDocumentsCallback(sd), new SendDocumentsRequestCallback() {

            @Override
            public void doWithRequest(SendDocumentsV2RequestType dokumentDocument) {
                final Long dvkRetainDaysPeriod = parametersService.getLongParameter(Parameters.DVK_RETAIN_PERIOD);
                final Calendar retainCal = Calendar.getInstance();
                retainCal.add(Calendar.DAY_OF_MONTH, dvkRetainDaysPeriod.intValue());
                dokumentDocument.setSailitustahtaeg(retainCal);
            }
        });
        Assert.isTrue(1 == sendDocuments.size(), "Supprise! Size of sendDocuments is " + sendDocuments.size());
        return sendDocuments.iterator().next();
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
        sender.setRegnr(propertiesResolver.getProperty("x-tee.institution"));
        // sender.setAsutuseNimi(senderName); // set in DhlXTeeServiceImpl.constructDokumentDocument() based on regNr
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
            final Transport transport = dokumentDocument.getDokument().getTransport();
            // add senders information
            final AadressType transportSaatja = transport.getSaatja();
            final String senderRegNr = dvkSendDocuments.getSenderRegNr();
            if (StringUtils.isNotBlank(senderRegNr)) { // use senderRegNr from X-Tee conf if senderRegNr not given
                transportSaatja.setRegnr(senderRegNr);
            }
            String senderOrgName = dvkSendDocuments.getSenderOrgName();
            // use default senderOrgName (from DVK capable orgs list) if senderOrgName not given
            senderOrgName = StringUtils.isNotBlank(senderOrgName) ? senderOrgName : transportSaatja.getAsutuseNimi();
            letter.addNewAuthor().addNewOrganisation().setOrganisationName(senderOrgName);
            transportSaatja.setAsutuseNimi(senderOrgName);
            // Maiga: paneme senderOrgName nimi elementi (nagu postipoisis) ja dubleerime asutuseNimes
            transportSaatja.setNimi(senderOrgName);
            transportSaatja.setEpost(dvkSendDocuments.getSenderEmail());
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
            // kirja saajaid DhlXteeServiceImpl#sendDocuments() saajate järgi ei määra erinevalt dokument/transport/saaja elementidest
            // (kuna kirja kasutamine pole kohustuslik)
            for (String addresseeRegNum : dvkSendDocuments.getRecipientsRegNrs()) {
                final PartyType letterAddressee = letterAddressees.addNewAddressee();
                letterAddressee.addNewOrganisation().setOrganisationName(getOrganisationName(addresseeRegNum));
            }

            final DhlDokumentType dhlDokument = dokumentDocument.getDokument();

            final Metaxml metaxml = composeMetaxml(letter);
            dhlDokument.setMetaxml(metaxml);

            dhlDokument.setTransport(transport);
            if (log.isTraceEnabled()) {
                log.trace("\n\naltered dhlDokument:\n" + dhlDokument + "\n\n");
            }
        }

        private Metaxml composeMetaxml(Letter letter) {
            final Metaxml metaXml = Metaxml.Factory.newInstance();
            final XmlCursor cursorL = letter.newCursor();
            final XmlCursor cursorM = metaXml.newCursor();
            cursorM.toNextToken();
            cursorL.copyXmlContents(cursorM);
            if (log.isDebugEnabled()) {
                log.debug("metaXml composed based on letter:\n" + metaXml + "\n\n");
            }
            cursorL.dispose();
            cursorM.dispose();
            return metaXml;
        }
    }

    // START: getters / setters
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setReceivedDvkDocumentsPath(String receivedDvkDocumentsPath) {
        this.receivedDvkDocumentsPath = receivedDvkDocumentsPath;
    }

    public void setCorruptDvkDocumentsPath(String receivedDvkDocumentsPath) {
        this.corruptDvkDocumentsPath = receivedDvkDocumentsPath;
    }

    @Override
    public String getCorruptDvkDocumentsPath() {
        return corruptDvkDocumentsPath;
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

    public void setAddressbookService(AddressbookService addressbookService) {
        this.addressbookService = addressbookService;
    }
    // END: getters / setters

}
