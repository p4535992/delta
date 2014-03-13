package ee.webmedia.alfresco.dvk.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.dvk.model.DvkModel;
import ee.webmedia.alfresco.dvk.model.DvkReceivedDocumentImpl;
import ee.webmedia.alfresco.dvk.model.DvkReceivedLetterDocument;
import ee.webmedia.alfresco.dvk.model.DvkSendLetterDocuments;
import ee.webmedia.alfresco.dvk.model.DvkSendReviewTask;
import ee.webmedia.alfresco.dvk.model.DvkSendWorkflowDocuments;
import ee.webmedia.alfresco.dvk.model.IDocument;
import ee.webmedia.alfresco.dvk.service.ReviewTaskException.ExceptionType;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.XmlUtil;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.xtee.client.dhl.DhlXTeeService;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.MetainfoHelper;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ReceivedDocumentsWrapper;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ReceivedDocumentsWrapper.ReceivedDocument;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendDocumentsDokumentCallback;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendDocumentsRequestCallback;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.AadressType;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.DhlDokumentType;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.DokumentDocument;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.MetaxmlDocument.Metaxml;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.TransportDocument.Transport;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.rkelLetter.AccessRightsType;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.rkelLetter.Addressee;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.rkelLetter.Letter;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.rkelLetter.LetterType;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.rkelLetter.PartyType;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.rkelLetter.PersonType;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl_meta_automatic.impl.DhlKaustDocumentImpl;
import ee.webmedia.xtee.client.dhl.types.ee.riik.xtee.dhl.producers.producer.dhl.SendDocumentsV2RequestType;
import ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType;
import ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.SignedDocType;
import ee.webmedia.xtee.client.service.configuration.provider.XTeeProviderPropertiesResolver;

public abstract class DvkServiceImpl implements DvkService {

    private static Log log = LogFactory.getLog(DvkServiceImpl.class);

    protected static final String DELTA_URI = "http://alfresco.webmedia.ee/schemas/dhl/delta/1.0";
    protected static final javax.xml.namespace.QName DELTA_QNAME = new javax.xml.namespace.QName(DELTA_URI, "delta", "delta");
    protected static final javax.xml.namespace.QName DELTA_VERSION = new javax.xml.namespace.QName(DELTA_URI, "deltaVersion");
    protected static final javax.xml.namespace.QName EXTERNAL_REVIEW_DOCUMENT_QNAME = new javax.xml.namespace.QName(DELTA_URI, "externalReviewDocument");
    protected static final javax.xml.namespace.QName EXTERNAL_REVIEW_TASK_COMPLETED_QNAME = new javax.xml.namespace.QName(DELTA_URI, "externalReviewTaskCompleted");

    private String receivedDvkDocumentsPath;
    protected String corruptDvkDocumentsPath;
    protected DhlXTeeService dhlXTeeService;
    protected NodeService nodeService;
    protected FileFolderService fileFolderService;
    protected MimetypeService mimetypeService;
    protected GeneralService generalService;
    protected ParametersService parametersService;
    protected AddressbookService addressbookService;
    protected ApplicationService applicationService;
    private String institutionCode;

    private XTeeProviderPropertiesResolver propertiesResolver;

    private String noTitleSpacePrefix;

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
        try {
            Map<String, String> sendingOptions = dhlXTeeService.getSendingOptions();
            MonitoringUtil.logSuccess(MonitoredService.OUT_XTEE_DVK);
            return sendingOptions;
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_XTEE_DVK, e);
            throw e;
        }
    }

    @Override
    public void updateOrganizationList() {
        try {
            dhlXTeeService.getDvkOrganizationsHelper().updateDvkCapableOrganisationsCache();
            MonitoringUtil.logSuccess(MonitoredService.OUT_XTEE_DVK);
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_XTEE_DVK, e);
            throw e;
        }
    }

    @Override
    public Collection<String> receiveDocuments() {
        final long maxReceiveDocumentsNr = parametersService.getLongParameter(Parameters.DVK_MAX_RECEIVE_DOCUMENTS_NR);
        final String dvkReceiveDocumentsInvoiceFolder = parametersService.getStringParameter(Parameters.DVK_RECEIVE_DOCUMENTS_INVOICE_FOLDER);
        NodeRef dvkIncomingFolder = generalService.getNodeRef(receivedDvkDocumentsPath);
        log.info("Starting to receive documents (max " + maxReceiveDocumentsNr + " documents at the time)");
        final Set<String> receiveDocuments = new HashSet<String>();
        Collection<String> lastReceiveDocuments;
        Collection<String> lastFailedDocuments;
        Collection<String> previouslyFailedDvkIds = getPreviouslyFailedDvkIds();
        int countServiceCalls = 0;
        do {
            final Pair<Collection<String>, Collection<String>> results //
            = receiveDocumentsServiceCall((int) maxReceiveDocumentsNr, dvkIncomingFolder, previouslyFailedDvkIds, dvkReceiveDocumentsInvoiceFolder);
            lastReceiveDocuments = results.getFirst();
            lastFailedDocuments = results.getSecond();
            if (lastReceiveDocuments.size() != 0 || lastFailedDocuments.size() != 0) {
                final ArrayList<String> markReceived = new ArrayList<String>(lastReceiveDocuments);
                markReceived.addAll(lastFailedDocuments);
                try {
                    dhlXTeeService.markDocumentsReceived(markReceived);
                    MonitoringUtil.logSuccess(MonitoredService.OUT_XTEE_DVK);
                } catch (RuntimeException e) {
                    MonitoringUtil.logError(MonitoredService.OUT_XTEE_DVK, e);
                    throw e;
                }
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
     * @param dvkReceiveDocumentsInvoiceFolder
     * @return nr of documents (not files in documents) received
     */
    private Pair<Collection<String>, Collection<String>> receiveDocumentsServiceCall(final int maxReceiveDocumentsNr
            , NodeRef dvkIncomingFolder, Collection<String> previouslyFailedDvkIds, String dvkReceiveDocumentsInvoiceFolder) {
        ReceivedDocumentsWrapper receiveDocuments = null;
        try {
            receiveDocuments = dhlXTeeService.receiveDocuments(maxReceiveDocumentsNr);
            MonitoringUtil.logSuccess(MonitoredService.OUT_XTEE_DVK);
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_XTEE_DVK, e);
            throw e;
        }

        final Set<String> receivedDocumentIds = new HashSet<String>();
        final List<String> receiveFaileddDocumentIds = new ArrayList<String>();
        log.debug("Received " + receiveDocuments.size() + " documents from DVK");
        List<String> sortedKeys = new ArrayList<String>(receiveDocuments.keySet());
        // sort descending to avoid unnecessary version overwrite
        // when receiving for example external review workflow documents
        Collections.sort(sortedKeys, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int k1 = 0;
                int k2 = 0;
                try {
                    k1 = Integer.parseInt(o1);
                    k2 = Integer.parseInt(o2);
                } catch (NumberFormatException e) {
                    return 0;
                }
                return (k1 < k2) ? 1 : ((k1 == k2) ? 0 : -1);
            }
        });
        int createdDocumentCounter = 0;
        for (String dhlId : sortedKeys) {
            final ReceivedDocument receivedDocument = receiveDocuments.get(dhlId);
            List<NodeRef> storedDocuments = null;
            try {
                storedDocuments = storeDocument(receivedDocument, dhlId, dvkIncomingFolder, previouslyFailedDvkIds);
            } catch (RuntimeException e) {
                log.error("Failed to store DVK document " + dhlId + " to " + dvkIncomingFolder, e);
                throw e;// didn't even manage to handle exception
            }
            if (storedDocuments != null && storedDocuments.size() > 0) {
                receivedDocumentIds.add(dhlId);
                createdDocumentCounter += storedDocuments.size();
            } else {
                receiveFaileddDocumentIds.add(dhlId);
            }
        }
        log.info("Received " + receivedDocumentIds.size() + " documents: " + receivedDocumentIds + ", created " + createdDocumentCounter + " documents");
        if (receiveFaileddDocumentIds.size() > 0) {
            log.error("FAILED to receive " + receiveFaileddDocumentIds.size() + " documents: " + receiveFaileddDocumentIds);
        }

        return new Pair<Collection<String>, Collection<String>>(receivedDocumentIds, receiveFaileddDocumentIds);
    }

    protected List<NodeRef> storeDocument(ReceivedDocument receivedDocument, String dhlId, NodeRef dvkIncomingFolder, Collection<String> previouslyFailedDvkIds) {
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

            List<DataFileType> dataFileList;
            if (signedDoc == null) {
                dataFileList = Collections.emptyList();
                log.warn("document contains 0 datafiles. signedDoc is null\n    dvk id: " + dhlId + ", sender: " + metaInfoHelper.getDhlSaatjaAsutuseNimi()
                        + " " + metaInfoHelper.getDhlSaatjaAsutuseNr());
            } else {
                dataFileList = signedDoc.getDataFileList();
                if (dataFileList.size() == 0) {
                    log.warn("document contains " + dataFileList.size() + " datafiles. signedDoc:\n" + getSignedDocOutput(signedDoc) + "\ndvk id: " + dhlId + ", sender: "
                            + metaInfoHelper.getDhlSaatjaAsutuseNimi() + " " + metaInfoHelper.getDhlSaatjaAsutuseNr());
                } else if (log.isDebugEnabled()) {
                    log.debug("document contains " + dataFileList.size() + " datafiles");
                }
            }

            // gather properties that will be attached to space created for this document
            final DvkReceivedLetterDocument rd = new DvkReceivedDocumentImpl();
            rd.setDvkId(dhlId);
            rd.setSenderRegNr(metaInfoHelper.getDhlSaatjaAsutuseNr());
            rd.setSenderOrgName(metaInfoHelper.getDhlSaatjaAsutuseNimi());
            rd.setSenderEmail(metaInfoHelper.getDhlSaatjaEpost());

            // if document is from invoice dimensions folder,
            // no futher parsing is executed, no matter if import was successful or not
            if (isFromFolder(parametersService.getStringParameter(Parameters.SAP_FINANCIAL_DIMENSIONS_FOLDER_IN_DVK), metaInfoHelper)) {
                List<NodeRef> dimensionNodes = importDimensionData(rd, dhlDokument, dhlId, dataFileList);
                if (dimensionNodes.size() == 0) {
                    String msg = "Failed to parse dimension lists from DVK document with dhlId='" + dhlId + "'";
                    log.error(msg);
                    throw new RuntimeException(msg);
                }
                return dimensionNodes;
            }

            NodeRef docNode = importWorkflowData(rd, dhlDokument, dvkIncomingFolder, dataFileList, dhlId);
            if (docNode != null) {
                log.info("Stored workflow data from " + dhlId + " to " + docNode);
                return Arrays.asList(docNode);
            }
            NodeRef taskNode = importTaskData(rd, dhlDokument, dhlId);
            if (taskNode != null) {
                log.info("Stored task data from " + dhlId + " to " + taskNode);
                return Arrays.asList(taskNode);
            }
            List<NodeRef> invoiceNodes = importInvoiceData(rd, dhlDokument, dhlId, dataFileList);
            if (invoiceNodes.size() > 0) {
                log.info("Stored invoice data from " + dhlId + " to " + invoiceNodes.size() + " invoices.");
                return invoiceNodes;
            }
            NodeRef sapRegisteredDoc = importSapInvoiceRegistration(rd, dhlDokument, dhlId, dataFileList);
            if (sapRegisteredDoc != null) {
                log.info("Stored SAP document data from " + dhlId + " to " + sapRegisteredDoc);
                return Arrays.asList(sapRegisteredDoc);
            }
            NodeRef reviewTaskNotificationNode = importReviewTaskData(rd, dhlDokument, dhlId);
            if (reviewTaskNotificationNode != null) {
                if (RepoUtil.isUnsaved(reviewTaskNotificationNode)) {
                    handleStorageFailure(receivedDocument, dhlId, dvkIncomingFolder, metaInfoHelper, previouslyFailedDvkIds, new RuntimeException(
                            "Couldn't find corresponding linkedReviewTask in Delta!"));
                    return null;
                }
                return Arrays.asList(reviewTaskNotificationNode);
            }
            fillLetterData(rd, dhlDokument);
            String documentFolderName;
            if (StringUtils.isNotBlank(rd.getLetterSenderTitle())) {
                documentFolderName = rd.getLetterSenderTitle();
            } else {
                if (dataFileList.size() > 0) {
                    documentFolderName = noTitleSpacePrefix + dataFileList.get(0).getFilename();
                } else {
                    documentFolderName = noTitleSpacePrefix + metaInfoHelper.getDhlSaatjaAsutuseNr() + " " + metaInfoHelper.getDhlSaatjaAsutuseNimi();
                }
            }

            NodeRef documentFolder = null;
            documentFolder = createDocumentNode(rd, dvkIncomingFolder, documentFolderName);

            for (DataFileType dataFile : dataFileList) {
                storeFile(rd, documentFolder, dataFile);
            }
            return Arrays.asList(documentFolder);
        } catch (AlfrescoRuntimeException e) {
            final String msg = "Failed to store document with dhlId='" + dhlId + "'";
            log.fatal(msg, e);
            throw new RuntimeException(msg, e);
        } catch (Exception e) {
            handleStorageFailure(receivedDocument, dhlId, dvkIncomingFolder, metaInfoHelper, previouslyFailedDvkIds, e);
            return null;
        }
    }

    private boolean isFromFolder(String folderName, MetainfoHelper metainfoHelper) {
        String dhlKaust = (metainfoHelper.getObject(DhlKaustDocumentImpl.class)).getDhlKaust();
        return TextUtil.isBlankEqual(folderName, dhlKaust);
    }

    /**
     * Overridden in TK
     */
    protected String getSignedDocOutput(SignedDocType signedDoc) {
        return signedDoc.toString();
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
        log.error("Failed to store document " + (metaInfoHelper != null ? metaInfoHelper.getKoostajaFailinimi() : "") + " with dhlId='" + dhlId + "' to " + dvkIncomingFolder, e);
    }

    abstract protected NodeRef createDocumentNode(DvkReceivedLetterDocument rd, NodeRef dvkIncomingFolder, String documentFolderName);

    protected Collection<String> getPreviouslyFailedDvkIds() {
        NodeRef corruptFolderRef = generalService.getNodeRef(corruptDvkDocumentsPath);
        final List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(corruptFolderRef);
        final HashSet<String> failedDvkIds = new HashSet<String>(childAssocs.size());
        for (ChildAssociationRef failedAssocRef : childAssocs) {
            final NodeRef failedRef = failedAssocRef.getChildRef();
            String dvkId = (String) nodeService.getProperty(failedRef, DvkModel.Props.DVK_ID);
            failedDvkIds.add(dvkId);
        }
        return failedDvkIds;
    }

    protected NodeRef storeFile(DvkReceivedLetterDocument rd, NodeRef documentFolder, DataFileType dataFile) throws IOException {
        String filename = FilenameUtil.getDvkFilename(dataFile);
        NodeRef file = createFileNode(documentFolder, filename);
        log.info("Writing file '" + filename + "' (fileRef: " + file + ") from DVK document with dvkId '" + rd.getDvkId() + "' to repository space: '"
                + receivedDvkDocumentsPath + "' (parentRef: " + documentFolder + ")");

        final ContentWriter writer = fileFolderService.getWriter(file);
        String originalMimeType = StringUtils.lowerCase(dataFile.getMimeType());
        String mimeType = mimetypeService.guessMimetype(filename);
        if (log.isInfoEnabled() && !StringUtils.equals(mimeType, originalMimeType)) {
            log.info("Original mimetype '" + originalMimeType + "', but we are guessing mimetype based on filename '" + filename + "' => '" + mimeType
                    + "'\n    dvk id: " + rd.getDvkId() + ", sender: " + rd.getSenderRegNr() + " " + rd.getSenderOrgName());
        }
        writer.setMimetype(mimeType);
        final OutputStream os = writer.getContentOutputStream();
        try {
            os.write(Base64.decode(dataFile.getStringValue()));
        } catch (Base64DecodingException e) {
            RuntimeException ex = new RuntimeException("Failed to decode", e);
            log.error("Failed to decode DVK documents (" + rd.getDvkId() + ") file " + filename + " contents", ex);
            throw ex;
        } catch (IOException e) {
            String message = "Failed to write output to repository: '" + receivedDvkDocumentsPath + "' nodeRef=" + file + " contentUrl="
                    + writer.getContentUrl();
            RuntimeException ex = new RuntimeException(message, e);
            log.error(message, ex);
            throw ex;
        } finally {
            os.close();
        }
        return file;
    }

    /**
     * @param rd
     * @param documentFolder
     * @param filename
     * @return
     */
    protected NodeRef createFileNode(NodeRef documentFolder, String filename) {
        return fileFolderService.create(documentFolder, filename, ContentModel.TYPE_CONTENT).getNodeRef();
    }

    private boolean fillLetterData(DvkReceivedLetterDocument rd, DhlDokumentType dhlDokument) {
        final Metaxml metaxml = dhlDokument.getMetaxml();
        final String xml = metaxml.toString();
        Letter letter;
        try {
            letter = XmlUtil.getTypeFromXml(xml, Letter.class);
        } catch (Exception e) {
            return false;// letter content was not found in metaxml or didn't match schema
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
        return true;
    }

    protected abstract NodeRef importWorkflowData(DvkReceivedLetterDocument rd, DhlDokumentType dhlDokument
            , NodeRef dvkDefaultIncomingFolder, List<DataFileType> dataFileList, String dvkId);

    abstract protected NodeRef importTaskData(DvkReceivedLetterDocument rd, DhlDokumentType dhlDokument, String dvkId);

    abstract protected Pair<Location, Boolean> getDvkWorkflowDocLocation(String senderName, NodeRef dvkDefaultIncomingFolder);

    abstract protected List<NodeRef> importInvoiceData(DvkReceivedLetterDocument rd, DhlDokumentType dhlDokument, String dhlId, List<DataFileType> dataFileList) throws IOException;

    abstract protected List<NodeRef> importDimensionData(DvkReceivedLetterDocument rd, DhlDokumentType dhlDokument, String dhlId, List<DataFileType> dataFileList);

    abstract protected NodeRef importSapInvoiceRegistration(DvkReceivedLetterDocument rd, DhlDokumentType dhlDokument, String dhlId, List<DataFileType> dataFileList);

    abstract protected NodeRef importReviewTaskData(DvkReceivedLetterDocument rd, DhlDokumentType dhlDokument, String dvkId);

    @Override
    public String sendLetterDocuments(NodeRef document, Collection<ContentToSend> contentsToSend, final DvkSendLetterDocuments sd) {
        final Collection<String> recipientsRegNrs = sd.getRecipientsRegNrs();
        verifyEnoughData(contentsToSend, recipientsRegNrs, true);
        try {
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
            String next = sendDocuments.iterator().next();
            MonitoringUtil.logSuccess(MonitoredService.OUT_XTEE_DVK);
            return next;
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_XTEE_DVK, e);
            throw e;
        }
    }

    @Override
    public abstract void sendDvkTasksWithDocument(NodeRef docNodeRef, NodeRef compoundWorkflowNodeRef, Map<NodeRef, List<String>> additionalRecipients);

    @Override
    public abstract void sendDvkTask(Task task);

    public String sendExternalReviewWorkflowData(Collection<ContentToSend> contentsToSend, final DvkSendWorkflowDocuments sd) {
        final Collection<String> recipientsRegNrs = new ArrayList<String>();
        recipientsRegNrs.add(sd.getRecipientsRegNr());
        verifyEnoughData(contentsToSend, recipientsRegNrs, false);
        try {
            final Set<String> sendDocuments = dhlXTeeService.sendDocuments(contentsToSend, getRecipients(recipientsRegNrs), getSenderAddress(),
                    new DhsSendWorkflowCallback(sd), getSendDocumentRequestCallback());
            Assert.isTrue(1 == sendDocuments.size(), "Supprise! Size of sendDocuments is " + sendDocuments.size());
            String next = sendDocuments.iterator().next();
            MonitoringUtil.logSuccess(MonitoredService.OUT_XTEE_DVK);
            return next;
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_XTEE_DVK, e);
            throw e;
        }
    }

    @Override
    public abstract String sendInvoiceFileToSap(Node document, File file);

    public SendDocumentsRequestCallback getSendDocumentRequestCallback() {
        return new SendDocumentsRequestCallback() {

            @Override
            public void doWithRequest(SendDocumentsV2RequestType request) {
                setSailitustahtaeg(request);
            }
        };
    }

    public SendDocumentsRequestCallback getSendDocumentToFolderRequestCallback(final String folder) {
        return new SendDocumentsRequestCallback() {
            @Override
            public void doWithRequest(SendDocumentsV2RequestType request) {
                setSailitustahtaeg(request);
                if (StringUtils.isNotBlank(folder)) {
                    request.setKaust(folder);
                }
            }
        };
    }

    private void setSailitustahtaeg(SendDocumentsV2RequestType request) {
        final Long dvkRetainDaysPeriod = parametersService.getLongParameter(Parameters.DVK_RETAIN_PERIOD);
        final Calendar retainCal = Calendar.getInstance();
        retainCal.add(Calendar.DAY_OF_MONTH, dvkRetainDaysPeriod.intValue());
        request.setSailitustahtaeg(retainCal);
    }

    public void verifyEnoughData(Collection<ContentToSend> contentsToSend, final Collection<String> recipientsRegNrs, boolean requireFiles) {
        if ((requireFiles && contentsToSend.size() == 0) || recipientsRegNrs.size() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("To send files using DVK you must have at least one file and recipient(contentsToSend='" //
                        + contentsToSend + "', recipientsRegNrs='" + recipientsRegNrs + "')");
            }
            throw new UnableToPerformException(MessageSeverity.ERROR, "dvk_send_error_notEnoughData");
        }
    }

    private String getOrganisationName(String addresseeRegNum) {
        // TODO: implementation will probably change
        try {
            String organizationName = dhlXTeeService.getDvkOrganizationsHelper().getOrganizationName(addresseeRegNum);
            MonitoringUtil.logSuccess(MonitoredService.OUT_XTEE_DVK);
            return organizationName;
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_XTEE_DVK, e);
            throw e;
        }
    }

    protected AadressType[] getRecipients(Collection<String> recipientsRegNrs) {
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

    protected AadressType getSenderAddress() {
        AadressType sender = AadressType.Factory.newInstance();
        sender.setRegnr(propertiesResolver.getProperty("x-tee.institution"));
        // sender.setAsutuseNimi(senderName); // set in DhlXTeeServiceImpl.constructDokumentDocument() based on regNr
        return sender;
    }

    private class SimDhsSendDocumentsCallback implements SendDocumentsDokumentCallback {
        private final DvkSendLetterDocuments dvkSendDocuments;

        public SimDhsSendDocumentsCallback(DvkSendLetterDocuments dvkSendDocuments) {
            dvkSendDocuments.validateOutGoing();
            this.dvkSendDocuments = dvkSendDocuments;
        }

        @Override
        public void doWithDocument(DokumentDocument dokumentDocument) {
            log.debug("altering dokument");
            final Letter letter = Letter.Factory.newInstance();

            final Addressee letterAddressees = letter.addNewAddressees();
            final DhlDokumentType dhlDokument = dokumentDocument.getDokument();
            final Transport transport = dhlDokument.getTransport();
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

            final Date arBeginDate = dvkSendDocuments.getLetterAccessRestrictionBeginDate();
            if (arBeginDate != null) {
                final Calendar accessRestrBeginCal = Calendar.getInstance();
                accessRestrBeginCal.setTime(arBeginDate);
                accessRights.setBeginDate(accessRestrBeginCal);
            }

            final Date arEndDate = dvkSendDocuments.getLetterAccessRestrictionEndDate();
            if (arEndDate != null) {
                final Calendar accessRestrEndCal = Calendar.getInstance();
                accessRestrEndCal.setTime(arEndDate);
                accessRights.setEndDate(accessRestrEndCal);
            }

            accessRights.setReason(dvkSendDocuments.getLetterAccessRestrictionReason());

            letterMeta.setAccessRights(accessRights);
            // kirja saajaid DhlXteeServiceImpl#sendDocuments() saajate järgi ei määra erinevalt dokument/transport/saaja elementidest
            // (kuna kirja kasutamine pole kohustuslik)
            for (String addresseeRegNum : dvkSendDocuments.getRecipientsRegNrs()) {
                final PartyType letterAddressee = letterAddressees.addNewAddressee();
                letterAddressee.addNewOrganisation().setOrganisationName(getOrganisationName(addresseeRegNum));
            }

            final Metaxml metaxml = composeLetterMetaxml(letter);
            dhlDokument.setMetaxml(metaxml);

            dhlDokument.setTransport(transport);
        }

        private Metaxml composeLetterMetaxml(Letter letter) {
            return composeMetaxml(letter, null);
        }
    }

    private class DhsSendWorkflowCallback implements SendDocumentsDokumentCallback {
        private final DvkSendWorkflowDocuments dvkSendDocuments;

        public DhsSendWorkflowCallback(DvkSendWorkflowDocuments dvkSendDocuments) {
            // TODO: validate?
            // dvkSendDocuments.validateOutGoing();
            this.dvkSendDocuments = dvkSendDocuments;
        }

        @Override
        public void doWithDocument(DokumentDocument dokumentDocument) {
            log.debug("altering dokument");

            final DhlDokumentType dhlDokument = dokumentDocument.getDokument();
            final Transport transport = dhlDokument.getTransport();
            fillDefaultSenderData(transport, dvkSendDocuments);

            Metaxml metaxml = null;
            try {
                if (dvkSendDocuments.isDocumentNode()) {
                    metaxml = composeWorkflowMetaxml(dvkSendDocuments.getRecipientDocNode(), EXTERNAL_REVIEW_DOCUMENT_QNAME);
                } else {
                    metaxml = composeWorkflowMetaxml(dvkSendDocuments.getRecipientDocNode(), EXTERNAL_REVIEW_TASK_COMPLETED_QNAME);
                }
            } catch (XmlException e) {
                log.debug("Unable to parse alfresco document xml, error: " + e.getMessage());
                throw new ReviewTaskException(ExceptionType.PARSING_EXCEPTION);
            }
            dhlDokument.setMetaxml(metaxml);

            dhlDokument.setTransport(transport);
        }

        private Metaxml composeWorkflowMetaxml(org.w3c.dom.Node domNode, javax.xml.namespace.QName... wrappers) throws XmlException {
            final XmlObject documentXml = XmlObject.Factory.parse(domNode);
            final Metaxml metaXml = Metaxml.Factory.newInstance();
            final XmlCursor cursorM = metaXml.newCursor();
            cursorM.toNextToken();
            cursorM.beginElement(DELTA_QNAME);
            cursorM.insertElementWithText(DELTA_VERSION, applicationService.getProjectVersion());
            Metaxml docXml = composeMetaxml(documentXml, Arrays.asList(wrappers));
            String docXmlStr = docXml.toString();
            // "resolve" relative namespaces (deprecated feature which cannot be interpreted correctly in importer)
            docXmlStr = StringUtils.replace(docXmlStr, "xmlns:view=\"view\"", "");
            docXmlStr = StringUtils.replace(docXmlStr, "xmlns:view1=\"view\"", "");
            docXmlStr = StringUtils.replace(docXmlStr, "view1:view", "view:view");
            XmlObject docXmlResolved = XmlObject.Factory.parse(docXmlStr);
            docXmlResolved.newCursor().copyXmlContents(cursorM);
            cursorM.dispose();
            return metaXml;
        }
    }

    class DhsSendInvoiceToSapCallback implements SendDocumentsDokumentCallback {

        @Override
        public void doWithDocument(DokumentDocument dokumentDocument) {
            log.debug("altering dokument");

            final DhlDokumentType dhlDokument = dokumentDocument.getDokument();
            final Transport transport = dhlDokument.getTransport();
            fillDefaultSenderData(transport, null);

            dhlDokument.setTransport(transport);
        }
    }

    protected class DhsSendReviewNotificationCallback implements SendDocumentsDokumentCallback {
        private final DvkSendReviewTask dvkSendReviewTask;

        public DhsSendReviewNotificationCallback(DvkSendReviewTask dvkSendReviewTask) {
            this.dvkSendReviewTask = dvkSendReviewTask;
        }

        @Override
        public void doWithDocument(DokumentDocument dokumentDocument) {
            log.debug("altering dokument");

            final DhlDokumentType dhlDokument = dokumentDocument.getDokument();
            final Transport transport = dhlDokument.getTransport();
            fillDefaultSenderData(transport, dvkSendReviewTask);
            AadressType transportSaatja = transport.getSaatja();
            transportSaatja.setNimi(dvkSendReviewTask.getSenderName());
            transportSaatja.setEpost(dvkSendReviewTask.getSenderEmail());

            Metaxml metaxml = null;
            try {
                metaxml = composeReviewNotificationMetaxml(dvkSendReviewTask.getRecipientDocNode());
            } catch (XmlException e) {
                log.debug("Unable to parse deltaKK document xml, error: " + e.getMessage());
                throw new ReviewTaskException(ExceptionType.PARSING_EXCEPTION);
            }
            dhlDokument.setMetaxml(metaxml);

            dhlDokument.setTransport(transport);
        }

        private Metaxml composeReviewNotificationMetaxml(org.w3c.dom.Node domNode) throws XmlException {
            final XmlObject documentXml = XmlObject.Factory.parse(domNode);
            final Metaxml metaXml = Metaxml.Factory.newInstance();
            final XmlCursor cursorM = metaXml.newCursor();
            cursorM.toNextToken();
            return composeMetaxml(documentXml, null);
        }
    }

    private Metaxml composeMetaxml(final XmlObject documentXml, List<javax.xml.namespace.QName> wrappers) {
        final Metaxml metaXml = Metaxml.Factory.newInstance();
        final XmlCursor cursorL = documentXml.newCursor();
        final XmlCursor cursorM = metaXml.newCursor();

        cursorM.toNextToken();
        if (wrappers != null) {
            for (javax.xml.namespace.QName wrapper : wrappers) {
                cursorM.beginElement(wrapper);
            }
        }
        cursorL.copyXmlContents(cursorM);
        if (log.isDebugEnabled()) {
            log.debug("metaXml composed based on document node:\n" + metaXml + "\n\n");
        }
        cursorL.dispose();
        cursorM.dispose();
        return metaXml;
    }

    private void fillDefaultSenderData(final Transport transport, IDocument dvkSendDocuments) {
        // add senders information
        final AadressType transportSaatja = transport.getSaatja();

        String senderRegNr = null;
        String senderOrgName = null;
        if (dvkSendDocuments != null) {
            senderRegNr = dvkSendDocuments.getSenderRegNr();
            senderOrgName = dvkSendDocuments.getSenderOrgName();
        }
        if (StringUtils.isNotBlank(senderRegNr)) { // use senderRegNr from X-Tee conf if senderRegNr not given
            transportSaatja.setRegnr(senderRegNr);
        }
        // use default senderOrgName (from DVK capable orgs list) if senderOrgName not given
        senderOrgName = StringUtils.isNotBlank(senderOrgName) ? senderOrgName : transportSaatja.getAsutuseNimi();

        transportSaatja.setAsutuseNimi(senderOrgName);
        // Maiga: paneme senderOrgName nimi elementi (nagu postipoisis) ja dubleerime asutuseNimes
        transportSaatja.setNimi(senderOrgName);
        transportSaatja.setEpost(parametersService.getStringParameter(Parameters.DOC_SENDER_EMAIL));
    }

    // START: getters / setters
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setReceivedDvkDocumentsPath(String receivedDvkDocumentsPath) {
        this.receivedDvkDocumentsPath = receivedDvkDocumentsPath;
    }

    @Override
    public String getReceivedDvkDocumentsPath() {
        return receivedDvkDocumentsPath;
    }

    public void setCorruptDvkDocumentsPath(String receivedDvkDocumentsPath) {
        corruptDvkDocumentsPath = receivedDvkDocumentsPath;
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

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    // TODO: move to Application or somewhere else?
    @Override
    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    // END: getters / setters

}
