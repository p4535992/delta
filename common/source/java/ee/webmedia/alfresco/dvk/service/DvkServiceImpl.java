package ee.webmedia.alfresco.dvk.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAddressbookService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getParametersService;
import static ee.webmedia.alfresco.utils.DvkUtil.getFileMimeType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.jsoup.Jsoup;
import org.jsoup.examples.HtmlToPlainText;
import org.springframework.util.Assert;

import com.nortal.jroad.client.dhl.DhlDocumentVersion;
import com.nortal.jroad.client.dhl.DhlXTeeService;
import com.nortal.jroad.client.dhl.DhlXTeeService.ContentToSend;
import com.nortal.jroad.client.dhl.DhlXTeeService.MetainfoHelper;
import com.nortal.jroad.client.dhl.DhlXTeeService.ReceivedDocumentsWrapper;
import com.nortal.jroad.client.dhl.DhlXTeeService.ReceivedDocumentsWrapper.ReceivedDocument;
import com.nortal.jroad.client.dhl.DhlXTeeService.SendDocumentsDecContainerCallback;
import com.nortal.jroad.client.dhl.DhlXTeeService.SendDocumentsDokumentCallback;
import com.nortal.jroad.client.dhl.DhlXTeeService.SendDocumentsRequestCallback;
import com.nortal.jroad.client.dhl.DhlXTeeService.SendStatus;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.deccontainer.vers21.AccessConditionType;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.deccontainer.vers21.ContactDataType;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.Access;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.Recipient;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.Transport;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.Transport.DecRecipient;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.Transport.DecSender;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.deccontainer.vers21.OrganisationType;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.deccontainer.vers21.PersonType;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.dhl.AadressType;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.dhl.DhlDokumentType;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.dhl.DokumentDocument;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.dhl.MetaxmlDocument.Metaxml;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.dhl.TagasisideType;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.dhl.rkelLetter.Letter;
import com.nortal.jroad.client.dhl.types.ee.riik.schemas.dhl_meta_automatic.impl.DhlKaustDocumentImpl;
import com.nortal.jroad.client.dhl.types.ee.riik.xrd.dhl.producers.producer.dhl.SendDocumentsV4RequestType;
import com.nortal.jroad.client.dhl.types.ee.sk.digiDoc.v13.DataFileType;
import com.nortal.jroad.client.dhl.types.ee.sk.digiDoc.v13.SignedDocType;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.addressbook.service.AddressbookService;
import ee.webmedia.alfresco.adit.service.AditService;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.common.service.ApplicationService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.dvk.model.DvkModel;
import ee.webmedia.alfresco.dvk.model.DvkReceivedLetterDocument;
import ee.webmedia.alfresco.dvk.model.DvkSendDocuments;
import ee.webmedia.alfresco.dvk.model.DvkSendReviewTask;
import ee.webmedia.alfresco.dvk.model.DvkSendWorkflowDocuments;
import ee.webmedia.alfresco.email.model.EmailAttachment;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.alfresco.notification.model.NotificationCache;
import ee.webmedia.alfresco.notification.model.NotificationResult;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.utils.DvkUtil;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.XmlUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

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
    private DocumentAdminService documentAdminService;
    private String institutionCode;
    private String subSystemCode;

    
    private List<Document> dvkSendFailedDocuments = new ArrayList<Document>();
    private String noTitleSpacePrefix;

    @Override
    public int updateOrganizationsDvkCapability() {
        log.info("UPDATE ORGANIZATIONS DVK CAPABILITY....");
        final Map<String /* regNum */, String /* orgName */> sendingOptions = getSendingOptions();
        final List<Node> organizations = addressbookService.listOrganization();
        if(organizations != null){
            log.debug("Found organizations... " + organizations.size());
        } else {
            log.debug("Found organizations... NULL!");
        }
        int dvkCapableOrgs = 0;
        for (Node orgNode : organizations) {
            final Map<String, Object> oProps = orgNode.getProperties();
            String orgCode = (String) oProps.get(AddressbookModel.Props.ORGANIZATION_CODE.toString());
            log.debug("OrgCode: " + orgCode);

            final boolean dvkCapable = sendingOptions.containsKey(orgCode);
            log.debug("DVK Capable... " + dvkCapable);
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
        log.info("UPDATE ORGANIZATION LIST...");
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
        log.info("RECEIVE DOCUMENTS...");
        final long maxReceiveDocumentsNr = parametersService.getLongParameter(Parameters.DVK_MAX_RECEIVE_DOCUMENTS_NR);
        log.debug("Max receive documents nr: " + maxReceiveDocumentsNr);

        final String dvkReceiveDocumentsInvoiceFolder = parametersService.getStringParameter(Parameters.DVK_RECEIVE_DOCUMENTS_INVOICE_FOLDER);
        log.debug("DVK receive documents invoice folder: " +dvkReceiveDocumentsInvoiceFolder);
        final NodeRef dvkIncomingFolder = BeanHelper.getConstantNodeRefsBean().getReceivedDvkDocumentsRoot();

        log.debug("DVK incoming folder: StoreRef: " + dvkIncomingFolder.toString());
        log.info("Starting to receive documents (max " + maxReceiveDocumentsNr + " documents at the time)");
        final Set<String> receiveDocuments = new HashSet<String>();
        Collection<String> lastReceiveDocuments;
        Collection<String> lastFailedDocuments;
        final Collection<String> previouslyFailedDvkIds = getPreviouslyFailedDvkIds();
        log.debug("Previously failed DVK ID's list size: " + previouslyFailedDvkIds.size());
        int countServiceCalls = 0;
        do {
        	Pair<Collection<String>, Collection<String>> results = BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Pair<Collection<String>, Collection<String>>>() {
	            @Override
	            public Pair<Collection<String>, Collection<String>> execute() throws Throwable {
	            	final Pair<Collection<String>, Collection<String>> results //
	                = receiveDocumentsServiceCall((int) maxReceiveDocumentsNr, dvkIncomingFolder, previouslyFailedDvkIds, dvkReceiveDocumentsInvoiceFolder);
	            	Collection<String> lastReceiveDocuments = results.getFirst();
	            	Collection<String> lastFailedDocuments = results.getSecond();
	                if (lastReceiveDocuments.size() != 0 || lastFailedDocuments.size() != 0) {
	                    final ArrayList<String> markReceived = new ArrayList<String>(lastReceiveDocuments);
	                    markReceived.addAll(lastFailedDocuments);
	                    Collection<TagasisideType> tagasisideTypeMarkreceived = new ArrayList<>();
	                    for (String dhlId: markReceived) {
	                    	try {
	                    	TagasisideType tagasisideType = TagasisideType.Factory.newInstance();
	                    	tagasisideType.setDhlId(new BigInteger(dhlId));
	                    	tagasisideTypeMarkreceived.add(tagasisideType);
	                    	} catch (NumberFormatException e) {
	                    		log.warn("Failed to parse to number dhlId = " + dhlId, e);
	                    	}
	                    }
	                    try {
	                        dhlXTeeService.markDocumentsReceivedV2(tagasisideTypeMarkreceived);
	                        MonitoringUtil.logSuccess(MonitoredService.OUT_XTEE_DVK);
	                    } catch (RuntimeException e) {
	                        MonitoringUtil.logError(MonitoredService.OUT_XTEE_DVK, e);
	                        throw e;
	                    }
	                    receiveDocuments.addAll(lastReceiveDocuments);
	                }
	                return results;
	            }
	        }, false, true);
            
        	lastReceiveDocuments = results.getFirst();
        	lastFailedDocuments = results.getSecond();
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
                BeanHelper.getFileService().reorderFiles(storedDocuments);
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
        List<NodeRef> result;
        if (DhlDocumentVersion.VER_1.equals(receivedDocument.getDocumentVersion())) {
            result = storeDocumentV1(receivedDocument, dhlId, dvkIncomingFolder, previouslyFailedDvkIds);
        } else if (DhlDocumentVersion.VER_2.equals(receivedDocument.getDocumentVersion())) {
            result = storeDocumentV2(receivedDocument, dhlId, dvkIncomingFolder, previouslyFailedDvkIds);
        } else {
            throw new IllegalArgumentException("Only DhlDocumentVersion.VER_1 and DhlDocumentVersion.VER_2 are currently supported!");
        }

        return result;
    }

    protected List<NodeRef> storeDocumentV1(ReceivedDocument receivedDocument, String dhlId, NodeRef dvkIncomingFolder, Collection<String> previouslyFailedDvkIds) {
        final MetainfoHelper metaInfoHelper = receivedDocument.getMetaInfoHelper();
        final DhlDokumentType dhlDokument = receivedDocument.getDhlDocument();
        final SignedDocType signedDoc = receivedDocument.getSignedDoc();
        String dhlSaatjaAsutuseNr = metaInfoHelper.getDhlSaatjaAsutuseNr();
        if (log.isTraceEnabled()) {
            log.trace("dokument element=\n" + dhlDokument + "'");
            log.trace("helper.getObject(DhlIdDocumentImpl)=" + dhlId + " " + metaInfoHelper.getDhlSaatjaAsutuseNimi() + " "
                    + dhlSaatjaAsutuseNr + " saadeti: " + metaInfoHelper.getDhlSaatmisAeg() + " saabus: "
                    + metaInfoHelper.getDhlSaabumisAeg() + "\nmetaManual:\nKoostajaFailinimi: " + metaInfoHelper.getKoostajaFailinimi());
        }

        try {
            Assert.isTrue(StringUtils.isNotBlank(dhlId), "dhlId can't be blank");
            com.nortal.jroad.client.dhl.types.ee.riik.schemas.dhl.TransportDocument.Transport transport = dhlDokument.getTransport();
            AadressType saatja = transport.getSaatja();

            Assert.isTrue(StringUtils.isNotBlank(saatja.getRegnr()), "sender regNr can't be blank");
            if (log.isDebugEnabled()) {
                log.debug("sender: " + saatja.getRegnr() + " : " + saatja.getAsutuseNimi());
            }

            List<DataFileType> dataFileList;
            if (signedDoc == null) {
                dataFileList = Collections.emptyList();
                log.warn("document contains 0 datafiles. signedDoc is null\n    dvk id: " + dhlId + ", sender: " + metaInfoHelper.getDhlSaatjaAsutuseNimi()
                        + " " + dhlSaatjaAsutuseNr);
            } else {
                dataFileList = signedDoc.getDataFileList();
                if (dataFileList.size() == 0) {
                    log.warn("document contains " + dataFileList.size() + " datafiles. signedDoc:\n" + getSignedDocOutput(signedDoc) + "\ndvk id: " + dhlId + ", sender: "
                            + metaInfoHelper.getDhlSaatjaAsutuseNimi() + " " + dhlSaatjaAsutuseNr);
                } else if (log.isDebugEnabled()) {
                    log.debug("document contains " + dataFileList.size() + " datafiles");
                }
            }

            // gather properties that will be attached to space created for this document
            final DvkReceivedLetterDocument rd = new DvkReceivedLetterDocument();
            rd.setDvkId(dhlId);
            rd.setSenderRegNr(dhlSaatjaAsutuseNr);
            rd.setSenderOrgName(metaInfoHelper.getDhlSaatjaAsutuseNimi());
            rd.setSenderEmail(metaInfoHelper.getDhlSaatjaEpost());

            String dhlKaust = (metaInfoHelper.getObject(DhlKaustDocumentImpl.class)).getDhlKaust();
            List<NodeRef> importedNodeRefs = handleImports(receivedDocument, dhlDokument, dataFileList, dhlId, dhlKaust, dhlSaatjaAsutuseNr, dvkIncomingFolder,
                    previouslyFailedDvkIds);
            if (importedNodeRefs != null) {
                return importedNodeRefs;
            }

            fillLetterData(rd, dhlDokument);
            String documentFolderName = getDvkDocumentFolderName(dataFileList, dhlSaatjaAsutuseNr, rd.getLetterSenderTitle());
            NodeRef documentFolder = createDocumentNode(DvkUtil.fillV1PropsFromDvkReceivedDocument(rd), dvkIncomingFolder, documentFolderName,
                    DvkUtil.isAditDocument(dhlSaatjaAsutuseNr), "");

            for (DataFileType dataFile : dataFileList) {
                storeFile(dhlId, dhlSaatjaAsutuseNr, documentFolder, dataFile);
            }
            return Arrays.asList(documentFolder);
        } catch (AlfrescoRuntimeException e) {
            final String msg = "Failed to store document with dhlId='" + dhlId + "'";
            log.fatal(msg, e);
            throw new RuntimeException(msg, e);
        } catch (Exception e) {
            handleStorageFailure(receivedDocument, dhlId, dvkIncomingFolder, previouslyFailedDvkIds, e);
            return null;
        }
    }

    protected List<NodeRef> storeDocumentV2(ReceivedDocument receivedDocument, String dhlId, NodeRef dvkIncomingFolder, Collection<String> previouslyFailedDvkIds) {
    	
    	DecContainer decContainer = receivedDocument.getDhlDocumentV2();
        if (log.isTraceEnabled()) {
            log.trace("dokument element=\n" + decContainer + "'");
            DecSender decSender = decContainer.getTransport().getDecSender();
            log.trace("helper.getObject(DhlIdDocumentImpl)=" + dhlId + " " + decSender.getOrganisationCode() + " "
                    + (decSender.isSetPersonalIdCode() ? decSender.getPersonalIdCode() : "") + " saabus: " + decContainer.getDecMetadata().getDecReceiptDate());
        }

        try {
            Assert.isTrue(StringUtils.isNotBlank(dhlId), "dhlId can't be blank");
            Transport decTransport = decContainer.getTransport();
            DecSender decSender = decTransport.getDecSender();
            String senderOrganisationCode = decSender.getOrganisationCode();
            Assert.isTrue(StringUtils.isNotBlank(senderOrganisationCode), "sender regNr can't be blank");
            if (log.isDebugEnabled()) {
                log.debug("sender: " + senderOrganisationCode);
            }

            List<DecContainer.File> fileList = decContainer.getFileList();

            if (fileList == null || fileList.size() == 0) {
                log.warn("document contains " + (fileList != null ? fileList.size() : 0) + " datafiles.\ndvk id: " + dhlId + ", sender: " + senderOrganisationCode);
            } else if (log.isDebugEnabled()) {
                log.debug("document contains " + fileList.size() + " datafiles");
            }

            // Handle various imports
            List<NodeRef> importedNodeRefs = handleImports(receivedDocument, decContainer, fileList, dhlId, decContainer.getDecMetadata().getDecFolder(), senderOrganisationCode,
                    dvkIncomingFolder, previouslyFailedDvkIds);
            if (importedNodeRefs != null) {
                return importedNodeRefs;
            }

            String documentFolderName = getDvkDocumentFolderName(fileList, senderOrganisationCode, decContainer.getRecordMetadata().getRecordTitle());
            Map<QName, Serializable> documentProperties = mapRelatedIncomingElements(SystematicDocumentType.INCOMING_LETTER.getId(), decContainer);
            boolean isAditDocument = DvkUtil.isAditDocument(senderOrganisationCode);
            String messageForRecipient = DvkUtil.getMessageForRecipient(decContainer, getSenderAddress().getOrganisationCode());
            NodeRef documentFolder = createDocumentNode(documentProperties, dvkIncomingFolder, documentFolderName, isAditDocument, messageForRecipient);

            for (DecContainer.File dataFile : fileList) {
                storeFile(dhlId, senderOrganisationCode, documentFolder, dataFile);
            }
            writeDecContainerWithoutFiles(dhlId, decContainer, documentFolder);

            return Arrays.asList(documentFolder);
        } catch (AlfrescoRuntimeException e) {
            final String msg = "Failed to store document with dhlId='" + dhlId + "'";
            log.fatal(msg, e);
            throw new RuntimeException(msg, e);
        } catch (Exception e) {
            handleStorageFailure(receivedDocument, dhlId, dvkIncomingFolder, previouslyFailedDvkIds, e);
            return null;
        }
    }

    private void writeDecContainerWithoutFiles(String dhlId, DecContainer decContainer, NodeRef documentFolder) {
        DecContainer decCopy = (DecContainer) decContainer.copy();
        decCopy.setFileArray(new DecContainer.File[0]); // Remove files from DecContainer
        String fileName = GUID.generate();
        NodeRef file = createFileNode(documentFolder, fileName);
        nodeService.setProperty(documentFolder, DvkModel.Props.DEC_CONTAINER, file);

        // Add aspect with DHL ID
        Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>(1);
        aspectProperties.put(DvkModel.Props.DVK_ID, dhlId);
        nodeService.addAspect(file, DvkModel.Aspects.DEC_CONTAINER, aspectProperties);

        log.info("Writing file '" + fileName + "' (fileRef: " + file + ") with DVK document dvkId '" + dhlId + "' metadata to repository space: '"
                + receivedDvkDocumentsPath + "' (parentRef: " + documentFolder + ")");

        final ContentWriter writer = fileFolderService.getWriter(file);
        OutputStream contentOutputStream = null;
        try {
            contentOutputStream = writer.getContentOutputStream();
            decCopy.save(contentOutputStream, DvkUtil.getDecContainerXmlOptions());
        } catch (IOException e) {
            String msg = "Failed to save original DecContainer to document files!";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            IOUtils.closeQuietly(contentOutputStream);
        }

    }

    @Override
    public Map<QName, Serializable> mapRelatedIncomingElements(String documentTypeId, NodeRef storedDecContainer) {
        DecContainerDocument decContainerDocumentocument = parseDecContainer(storedDecContainer);
        return mapRelatedIncomingElements(documentTypeId, decContainerDocumentocument.getDecContainer());
    }

    private DecContainerDocument parseDecContainer(NodeRef storedDecContainer) {
        InputStream contentInputStream = null;
        try {
            contentInputStream = fileFolderService.getReader(storedDecContainer).getContentInputStream();
            DecContainerDocument decContainer = getTypeFromDecodedSoapArray(contentInputStream, DecContainerDocument.class);
            return decContainer;
        } catch (IOException e) {
            String msg = "Unable to parse DecContainer from file '" + storedDecContainer + "'.";
            log.error(msg, e);
        } finally {
            IOUtils.closeQuietly(contentInputStream);
        }

        return null;
    }

    private static <T> T getTypeFromDecodedSoapArray(InputStream decContainerStream, Class<T> responseClass) throws IOException {
        SchemaType unencodedType = null;
        try {
            unencodedType = (SchemaType) responseClass.getField("type").get(null);
            log.debug("unencodedType=" + unencodedType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get value of '" + responseClass.getCanonicalName() + ".type' to get corresponding SchemaType object: ", e);
        }
        if (decContainerStream == null) {
            return null;
        }
        T resultItem = null;
        try {
            if (log.isTraceEnabled()) {
                log.trace("Starting to parse input stream to class: " + responseClass.getCanonicalName() + "\n\n");
            }
            resultItem = (T) XmlObject.Factory.parse(decContainerStream, new XmlOptions());

        } catch (XmlException e) {
            throw new RuntimeException("Failed to parse '" + decContainerStream + "' to class: " + responseClass.getCanonicalName(), e);
        }
        return resultItem;
    }

    private Map<QName, Serializable> mapRelatedIncomingElements(String documentTypeId, DecContainer decContainer) {
        Assert.hasText(documentTypeId, "Document type ID must be specified!");
        final Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        if (decContainer == null) {
            log.debug("No DecContainer provided for mapping " + documentTypeId);
            return props;
        }

        DocumentType documentType = documentAdminService.getDocumentType(documentTypeId, DocumentAdminService.DOC_TYPE_WITHOUT_OLDER_DT_VERSION_CHILDREN);
        if (documentType == null) {
            String errorMessage = "Couldn't find document type for id '" + documentTypeId + "'";
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        List<Field> fields = documentType.getLatestDocumentTypeVersion().getFieldsDeeply();
        Recipient recipient = getCurrentOrganisationRecipient(decContainer);
        for (Field field : fields) {
            List<String> relatedIncomingDecElement = field.getRelatedIncomingDecElement();
            if (relatedIncomingDecElement == null) {
                continue;
            }
            for (String decElement : relatedIncomingDecElement) {
                StringTokenizer tokenizer = new StringTokenizer(decElement, ",");
                if (!tokenizer.hasMoreTokens()) {
                    continue; // Shortcut
                }

                List<Object> tokenDecValues = new ArrayList<Object>();
                while (tokenizer.hasMoreTokens()) {
                    Object value;
                    String key = StringUtils.trim(tokenizer.nextToken());
                    try {
                        // If sender hasn't specified the mapped element or we are unable to retrieve the value, continue with the next mapping if available.
                        if (recipient != null && key.startsWith("<Recipient>")) {
                            value = DecContainerHandler.getValue(key, recipient, 1);
                        } else {
                            value = DecContainerHandler.getValue(key, decContainer);
                        }
                        if (value == null || value instanceof String && StringUtils.isBlank((String) value)) {
                            continue;
                        }
                    } catch (RuntimeException e) {
                        log.debug("Error occurred while extracting " + key + " from DecContainer.", e);
                        continue;
                    }
                    tokenDecValues.add(value);
                }

                if (tokenDecValues.isEmpty()) {
                    continue;
                }

                Object propertyValue;
                if (tokenDecValues.size() > 1) { // If we have multiple elements mapped to one field then the type must be string. See FieldDetailsDialog#validateDecMappings()
                    propertyValue = StringUtils.join(CollectionUtils.collect(tokenDecValues, new org.apache.commons.collections4.Transformer<Object, String>() {
                        @Override
                        public String transform(Object input) {
                            return DefaultTypeConverter.INSTANCE.convert(String.class, input);
                        }
                    }), " ");
                } else {
                    propertyValue = tokenDecValues.get(0);
                    if (propertyValue instanceof Calendar) {
                        propertyValue = ((Calendar) propertyValue).getTime();
                    }
                }

                // Check data type and handle multivalued properties
                DynamicPropertyDefinition propertyDefinition = getDocumentConfigService().createPropertyDefinition(field);
                Object convertedValue = DefaultTypeConverter.INSTANCE.convert(propertyDefinition.getDataType(), propertyValue);
                propertyValue = propertyDefinition.isMultiValued() ? Arrays.asList(convertedValue) : convertedValue;

                props.put(QName.createQName(DocumentDynamicModel.URI, field.getFieldId()), (Serializable) propertyValue);
                break; // If value is found, continue with other fields
            }
        }

        // Overwrites access restriction if it is already somehow set
        return DvkUtil.setAccessRestrictionProperties(decContainer, props);
    }

    private Recipient getCurrentOrganisationRecipient(DecContainer decContainer) {
        Recipient recipient = null;
        for (Recipient r : decContainer.getRecipientList()) {
            OrganisationType org = r.getOrganisation();
            if (org == null) {
                continue;
            }

            if (StringUtils.equals(org.getOrganisationCode(), getInstitutionCode())
                    || StringUtils.equals(parametersService.getStringParameter(Parameters.DVK_ORGANIZATION_NAME), org.getName())
                    || StringUtils.equals(getOrganisationName(getInstitutionCode()), org.getName())) {
                recipient = r;
                break;
            }
        }

        return recipient;
    }

    private <F extends XmlObject> String getDvkDocumentFolderName(List<F> fileList, String senderOrgCode, String letterSenderTitle) {
        String documentFolderName;
        if (StringUtils.isNotBlank(letterSenderTitle)) {
            documentFolderName = letterSenderTitle;
        } else {
            if (fileList.size() > 0) {
                documentFolderName = noTitleSpacePrefix + DvkUtil.getFileName(fileList.get(0));
            } else {
                documentFolderName = noTitleSpacePrefix + senderOrgCode + " " + getOrganisationName(senderOrgCode);
            }
        }
        return documentFolderName;
    }

    private <D extends XmlObject, F extends XmlObject> List<NodeRef> handleImports(ReceivedDocument receivedDocument, D decContainer, List<F> fileList, String dhlId,
            String dvkFolder
            , String senderOrganisationCode, NodeRef dvkIncomingFolder, Collection<String> previouslyFailedDvkIds) throws IOException {

        if (isFromFolder(parametersService.getStringParameter(Parameters.SAP_FINANCIAL_DIMENSIONS_FOLDER_IN_DVK), dvkFolder)) {
            List<NodeRef> dimensionNodes = importDimensionData(dhlId, fileList);
            if (dimensionNodes.size() == 0) {
                String msg = "Failed to parse dimension lists from DVK document with dhlId='" + dhlId + "'";
                log.error(msg);
                throw new RuntimeException(msg);
            }
            return dimensionNodes;
        }

        NodeRef docNode = importWorkflowData(getOrganisationName(senderOrganisationCode), decContainer, dvkIncomingFolder, fileList, dhlId);
        if (docNode != null) {
            log.info("Stored workflow data from " + dhlId + " to " + docNode);
            return Arrays.asList(docNode);
        }
        NodeRef taskNode = importTaskData(decContainer, dhlId);
        if (taskNode != null) {
            log.info("Stored task data from " + dhlId + " to " + taskNode);
            return Arrays.asList(taskNode);
        }
        List<NodeRef> invoiceNodes = importInvoiceData(dhlId, senderOrganisationCode, fileList);
        if (invoiceNodes.size() > 0) {
            log.info("Stored invoice data from " + dhlId + " to " + invoiceNodes.size() + " invoices.");
            return invoiceNodes;
        }
        NodeRef reviewTaskNotificationNode = importReviewTaskData(decContainer, dhlId);
        if (reviewTaskNotificationNode != null) {
            if (RepoUtil.isUnsaved(reviewTaskNotificationNode)) {
                handleStorageFailure(receivedDocument, dhlId, dvkIncomingFolder, previouslyFailedDvkIds, new RuntimeException(
                        "Couldn't find corresponding linkedReviewTask in Delta!"));
                return null;
            }
            return Arrays.asList(reviewTaskNotificationNode);
        }

        return null;
    }

    private boolean isFromFolder(String folderName, String dvkFolderName) {
        return TextUtil.isBlankEqual(folderName, dvkFolderName);
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
    protected void handleStorageFailure(ReceivedDocument receivedDocument, String dhlId, NodeRef dvkIncomingFolder, Collection<String> previouslyFailedDvkIds, Exception e) {
        XmlObject doc;
        if (DhlDocumentVersion.VER_1.equals(receivedDocument.getDocumentVersion())) {
            doc = receivedDocument.getDhlDocument();
        } else if (DhlDocumentVersion.VER_2.equals(receivedDocument.getDocumentVersion())) {
            doc = receivedDocument.getDhlDocumentV2();
        } else {
            throw new IllegalArgumentException("Only DhlDocumentVersion.VER_1 and DhlDocumentVersion.VER_2 are currently supported!");
        }

        log.error("Failed to store document with dhlId='" + dhlId + "' to " + dvkIncomingFolder, e);
    }

    abstract protected NodeRef createDocumentNode(Map<QName, Serializable> documentProperties, NodeRef dvkIncomingFolder, String documentFolderName, boolean isAditDocument,
            String messageForRecipient);

    protected Collection<String> getPreviouslyFailedDvkIds() {
        log.debug("GET PREVIOUSLY FAILED DVK ID'S....");
        NodeRef corruptFolderRef = BeanHelper.getConstantNodeRefsBean().getDvkCorruptRoot();
        final List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(corruptFolderRef);
        final HashSet<String> failedDvkIds = new HashSet<String>(childAssocs.size());
        for (ChildAssociationRef failedAssocRef : childAssocs) {
            final NodeRef failedRef = failedAssocRef.getChildRef();
            String dvkId = (String) nodeService.getProperty(failedRef, DvkModel.Props.DVK_ID);
            log.debug("DVK ID: " + dvkId);
            failedDvkIds.add(dvkId);
        }
        log.debug("GET PREVIOUSLY FAILED DVK ID'S.... RETURN ID's");
        return failedDvkIds;
    }

    protected <F extends XmlObject> NodeRef storeFile(String dhlId, String senderOrgNr, NodeRef documentFolder, F dataFile) throws IOException {
        String displayName = DvkUtil.getFileName(dataFile);
        String filename = FilenameUtil.checkAndGetUniqueFilename(documentFolder, displayName, BeanHelper.getGeneralService());
        NodeRef file = createFileNode(documentFolder, filename);
        nodeService.setProperty(file, FileModel.Props.DISPLAY_NAME, displayName);
        log.info("Writing file '" + filename + "' (fileRef: " + file + ", original name: " + displayName + ") from DVK document with dvkId '" + dhlId + "' to repository space: '"
                + receivedDvkDocumentsPath + "' (parentRef: " + documentFolder + ")");

        final ContentWriter writer = fileFolderService.getWriter(file);
        String originalMimeType = StringUtils.lowerCase(getFileMimeType(dataFile));
        String mimeType = mimetypeService.guessMimetype(filename);
        if (log.isInfoEnabled() && !StringUtils.equals(mimeType, originalMimeType)) {
            log.info("Original mimetype '" + originalMimeType + "', but we are guessing mimetype based on filename '" + filename + "' => '" + mimeType
                    + "'\n    dvk id: " + dhlId + ", sender: " + senderOrgNr);
        }
        writer.setMimetype(mimeType);
        final OutputStream os = writer.getContentOutputStream();
        try {
            IOUtils.copy(DvkUtil.getFileContents(dataFile), os);
        } catch (Base64DecodingException e) {
            RuntimeException ex = new RuntimeException("Failed to decode", e);
            log.error("Failed to decode DVK documents (" + dhlId + ") file " + filename + " contents", ex);
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

    protected abstract <D extends XmlObject, F extends XmlObject> NodeRef importWorkflowData(String senderOrgName, D dhlDokument, NodeRef dvkDefaultIncomingFolder,
            List<F> dataFileList, String dvkId);

    abstract protected <D extends XmlObject> NodeRef importTaskData(D dhlDokument, String dvkId);

    abstract protected Pair<Location, Boolean> getDvkWorkflowDocLocation(String senderName, NodeRef dvkDefaultIncomingFolder);

    abstract protected <F extends XmlObject> List<NodeRef> importInvoiceData(String dhlId, String senderOrgNr, List<F> dataFileList) throws IOException;

    abstract protected <F extends XmlObject> List<NodeRef> importDimensionData(String dhlId, List<F> dataFileList);

    abstract protected <D extends XmlObject> NodeRef importReviewTaskData(D dhlDokument, String dvkId);

    private String sendDocument(Collection<ContentToSend> contentsToSend, final DvkSendDocuments sd, boolean requireFiles, final SendDocumentsDecContainerCallback callback) {
        final Collection<String> recipientsRegNrs = sd.getRecipientsRegNrs();
        List<String> personIdCodes = sd.getPersonIdCodes();
        verifyEnoughData(contentsToSend, recipientsRegNrs, personIdCodes, requireFiles);
        try {
            final Set<String> sendDocuments = dhlXTeeService.sendDocuments(contentsToSend, getRecipients(recipientsRegNrs, personIdCodes), getSenderAddress(),
                    callback, new SendDocumentsRequestCallback() {

                        @Override
                        public void doWithRequest(SendDocumentsV4RequestType dokumentDocument) {
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
        } catch (RuntimeException e) {;
            MonitoringUtil.logError(MonitoredService.OUT_XTEE_DVK, e);
            throw e;
        }
    }

    private String sendDocuments(Collection<ContentToSend> contentsToSend, DvkSendDocuments sendDocument, boolean requireFiles) {
        return sendDocument(contentsToSend, sendDocument, requireFiles, new SimDhsSendDocumentsCallback(sendDocument));
    }

    @Override
    public String sendDocuments(Collection<ContentToSend> contentsToSend, DvkSendDocuments sendDocument) {
        return sendDocuments(contentsToSend, sendDocument, true);
    }

    @Override
    public String forwardDecDocument(Collection<ContentToSend> contentsToSend, DvkSendDocuments sendDocument) {
        return sendDocument(contentsToSend, sendDocument, false, new ForwardDecContainerCallback(sendDocument));
    }

    @Override
    public NotificationResult sendTaskNotificationDocument(Task task, NotificationCache notificationCache) throws Exception {
        NotificationResult result = new NotificationResult();
        if (task.isStatus(Status.IN_PROGRESS) && StringUtils.isBlank(task.getOwnerId()) && StringUtils.isBlank(task.getInstitutionName())
                && !task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK) && task.getParent().getParent().isDocumentWorkflow()) {

            String taskOwnerName = task.getOwnerName();
            String taskOwnerEmail = task.getOwnerEmail();
            NodeRef orgNodeRef = getAddressbookService().getOrganizationNodeRef(taskOwnerEmail, taskOwnerName);
            boolean isDvkCapable = orgNodeRef != null && Boolean.TRUE.equals(nodeService.getProperty(orgNodeRef, AddressbookModel.Props.DVK_CAPABLE));
            boolean isDecTaskCapable = isDvkCapable && Boolean.TRUE.equals(nodeService.getProperty(orgNodeRef, AddressbookModel.Props.DEC_TASK_CAPABLE));
            boolean canSendDvk = isDecTaskCapable && (!task.isType(WorkflowSpecificModel.Types.REVIEW_TASK)
                    || !BeanHelper.getWorkflowConstantsBean().isReviewToOtherOrgEnabled() || StringUtils.isBlank(task.getOwnerOrgStructUnit()));

            if (canSendDvk) {
                Workflow workflow = task.getParent();
                final NodeRef docNodeRef = workflow.getParent().getParent();
                try {
                    String recipientRegNr = (String) nodeService.getProperty(orgNodeRef, AddressbookModel.Props.ORGANIZATION_CODE);
                    DvkSendDocuments sd = new DvkSendDocuments();
                    sd.setSenderOrgName(getParametersService().getStringParameter(Parameters.DVK_ORGANIZATION_NAME));
                    sd.setSenderEmail(getParametersService().getStringParameter(Parameters.DOC_SENDER_EMAIL));
                    sd.setRecipientsRegNrs(Collections.singletonList(recipientRegNr));
                    sd.setOrgNames(Collections.singletonList(taskOwnerName));
                    sd.setDocumentNodeRef(docNodeRef);
                    sd.setTextContent(WorkflowUtil.getTaskMessageForRecipient(task));
                    String dvkId;
                    
                    
                    List<EmailAttachment> attachments = notificationCache.getAttachments().get(docNodeRef);
                    if (attachments == null) {
                        List<NodeRef> docFileRefs = BeanHelper.getFileService().getAllFileRefs(docNodeRef, true);
                        attachments = BeanHelper.getEmailService().getAttachments(docFileRefs, false, null, null);
                        notificationCache.getAttachments().put(docNodeRef, attachments);
                        
                    	
                    }
                    
                    String sentFiles = getSentFiles(docNodeRef);
                    
                    List<ContentToSend> contentsToSend = CollectionUtils.isNotEmpty(attachments) ? BeanHelper.getSendOutService().prepareContents(attachments)
                            : Collections.<ContentToSend> emptyList();
                    dvkId = sendDocuments(contentsToSend, sd, false);

                    Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                    props.put(DocumentCommonModel.Props.SEND_INFO_RECIPIENT, taskOwnerName);
                    props.put(DocumentCommonModel.Props.SEND_INFO_RECIPIENT_REG_NR, recipientRegNr);
                    props.put(DocumentCommonModel.Props.SEND_INFO_SEND_DATE_TIME, new Date());
                    props.put(DocumentCommonModel.Props.SEND_INFO_SEND_MODE, SendMode.DVK.getValueName());
                    props.put(DocumentCommonModel.Props.SEND_INFO_SEND_STATUS, SendStatus.SENT.toString());
                    props.put(DocumentCommonModel.Props.SEND_INFO_DVK_ID, dvkId);
                    props.put(DocumentCommonModel.Props.SEND_INFO_RESOLUTION, WorkflowUtil.getTaskSendInfoResolution(task));
                    props.put(DocumentCommonModel.Props.SEND_INFO_SENDER, "süsteem");
                    if (StringUtils.isNotBlank(sentFiles)) {
                    	props.put(DocumentCommonModel.Props.SEND_INFO_SENT_FILES, sentFiles);
                    }
                    result.setDocRef(docNodeRef);
                    result.addSendInfoProps(props);
                    result.markSent();
                } catch (RuntimeException e) {
                    log.debug("Sending document over dvk failed: ", e);
                    RetryingTransactionHelper txHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
                    try {
                        txHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
                            @Override
                            public Void execute() throws Throwable {
                                BeanHelper.getLogService().addLogEntry(LogEntry.createWithSystemUser(LogObject.DOCUMENT, docNodeRef, "document_log_status_sending_failed"));
                                return null;
                            }
                        }, false, true);
                    } catch (Exception e1) {
                        log.error(e1);
                    }
                }
            }
        }
        return result;
    }
    
    private String getSentFiles(NodeRef docNodeRef) {
    	StringBuilder sentFiles = new StringBuilder();
    	List<NodeRef> docFileRefs = BeanHelper.getFileService().getAllFileRefs(docNodeRef, true);
        for (NodeRef fileRef : docFileRefs) {
            String fileName = (String) nodeService.getProperty(fileRef, ContentModel.PROP_NAME);
            if (sentFiles.length() > 0) {
            	sentFiles.append("; ");
            }
            sentFiles.append(fileName);
        }
        
        return sentFiles.toString();
    }

    @Override
    public abstract void sendDvkTasksWithDocument(NodeRef docNodeRef, NodeRef compoundWorkflowNodeRef, Map<NodeRef, List<String>> additionalRecipients, String messageForRecipient) throws Exception;

    @Override
    public abstract void sendDvkTask(Task task);

    public String sendExternalReviewWorkflowData(Collection<ContentToSend> contentsToSend, final DvkSendWorkflowDocuments sd) {
        final Collection<String> recipientsRegNrs = new ArrayList<String>();
        recipientsRegNrs.add(sd.getRecipientsRegNr());
        verifyEnoughData(contentsToSend, recipientsRegNrs, null, false);
        try {
            final Set<String> sendDocuments = dhlXTeeService.sendDocuments(contentsToSend, getRecipients(recipientsRegNrs, null), getSenderAddress(),
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
            public void doWithRequest(SendDocumentsV4RequestType request) {
                setSailitustahtaeg(request);
            }
        };
    }

    public SendDocumentsRequestCallback getSendDocumentToFolderRequestCallback(final String folder) {
        return new SendDocumentsRequestCallback() {
            @Override
            public void doWithRequest(SendDocumentsV4RequestType request) {
                setSailitustahtaeg(request);
                if (StringUtils.isNotBlank(folder)) {
                    request.setKaust(folder);
                }
            }
        };
    }

    private void setSailitustahtaeg(SendDocumentsV4RequestType request) {
        final Long dvkRetainDaysPeriod = parametersService.getLongParameter(Parameters.DVK_RETAIN_PERIOD);
        final Calendar retainCal = Calendar.getInstance();
        retainCal.add(Calendar.DAY_OF_MONTH, dvkRetainDaysPeriod.intValue());
        request.setSailitustahtaeg(retainCal);
    }

    public void verifyEnoughData(Collection<ContentToSend> contentsToSend, final Collection<String> recipientsRegNrs, final Collection<String> personIdCodes, boolean requireFiles) {
        if ((requireFiles && contentsToSend.size() == 0) || (recipientsRegNrs.size() == 0 && (personIdCodes == null || personIdCodes.isEmpty()))) {
            if (log.isDebugEnabled()) {
                log.debug("To send files using DVK you must have at least one file and recipient(contentsToSend='" //
                        + contentsToSend + "', recipientsRegNrs='" + recipientsRegNrs + "', personIdCodes='" + personIdCodes + "')");
            }
            throw new UnableToPerformException(MessageSeverity.ERROR, "dvk_send_error_notEnoughData");
        }
    }

    private String getOrganisationName(String orgRegNum) {
        // TODO: implementation will probably change
        try {
            String organizationName = dhlXTeeService.getDvkOrganizationsHelper().getOrganizationName(orgRegNum);
            MonitoringUtil.logSuccess(MonitoredService.OUT_XTEE_DVK);
            return organizationName;
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_XTEE_DVK, e);
            throw e;
        }
    }

    protected DecRecipient[] getRecipients(Collection<String> recipientsRegNrs, List<String> personIdCodes) {
        DecRecipient[] recipients = new DecRecipient[recipientsRegNrs.size() + (personIdCodes != null ? personIdCodes.size() : 0)];
        int i = 0;
        for (String regNr : recipientsRegNrs) {
            DecRecipient recipient = DecRecipient.Factory.newInstance();
            recipient.setOrganisationCode(regNr);
            recipients[i] = recipient;
            i++;
        }
        if (personIdCodes != null) {
            for (String personIdCode : personIdCodes) {
                DecRecipient recipient = DecRecipient.Factory.newInstance();
                recipient.setOrganisationCode(AditService.NAME);
                recipient.setPersonalIdCode(personIdCode);
                recipients[i] = recipient;
                i++;
            }
        }
        return recipients;
    }

    protected DecSender getSenderAddress() {
        DecSender sender = DecSender.Factory.newInstance();
        if (StringUtils.isNotBlank(subSystemCode)) {
        	sender.setOrganisationCode(subSystemCode);
        } else {
        	sender.setOrganisationCode(institutionCode);
        }
        return sender;
    }

    private class ForwardDecContainerCallback implements SendDocumentsDecContainerCallback {
        private final DvkSendDocuments dvkSendDocuments;

        private ForwardDecContainerCallback(DvkSendDocuments dvkSendDocuments) {
            dvkSendDocuments.validateOutGoing();
            this.dvkSendDocuments = dvkSendDocuments;
        }

        @Override
        public void doWithDocument(DecContainerDocument decContainerDocument) {
            Node document = new Node(dvkSendDocuments.getDocumentNodeRef());
            DecContainer newContainer = decContainerDocument.getDecContainer();
            NodeRef oldDecContainer = BeanHelper.getFileService().getDecContainer(document.getNodeRef());
            String xmlAsString = fileFolderService.getReader(oldDecContainer).getContentString();

            try {
                DecContainerDocument oldDoc = (DecContainerDocument) XmlObject.Factory.parse(xmlAsString);
                DecContainer oldContainer = oldDoc.getDecContainer();

                newContainer.setAccess(oldContainer.getAccess());
                newContainer.setDecMetadata(oldContainer.getDecMetadata());
                newContainer.setInitiator(oldContainer.getInitiator());
                newContainer.setRecipientArray(oldContainer.getRecipientArray());
                newContainer.setRecordCreator(oldContainer.getRecordCreator());
                newContainer.setRecordMetadata(oldContainer.getRecordMetadata());
                newContainer.setRecordSenderToDec(oldContainer.getRecordSenderToDec());
                newContainer.setRecordTypeSpecificMetadata(oldContainer.getRecordTypeSpecificMetadata());
                newContainer.setSignatureMetadataArray(oldContainer.getSignatureMetadataArray());
            } catch (XmlException e) {
                log.warn("Failed to parse xml string", e);
                throw new UnableToPerformException("existing_dec_container_parsing_failed");
            }
        }
    }

    private class SimDhsSendDocumentsCallback implements SendDocumentsDecContainerCallback {
        private final DvkSendDocuments dvkSendDocuments;

        public SimDhsSendDocumentsCallback(DvkSendDocuments dvkSendDocuments) {
            dvkSendDocuments.validateOutGoing();
            this.dvkSendDocuments = dvkSendDocuments;
        }

        @Override
        public void doWithDocument(DecContainerDocument decContainerDocument) {
            Node document = new Node(dvkSendDocuments.getDocumentNodeRef());
            Map<String, Object> properties = document.getProperties();
            DocumentTypeVersion documentTypeVersion = BeanHelper.getDocumentConfigService().getDocumentTypeAndVersion(document, false).getSecond();
            final DecContainer decContainer = decContainerDocument.getDecContainer();
            Pair<Boolean, Map<String, String>> addDocumentMetadata = addDocumentMetadata(properties, documentTypeVersion, decContainer);
            if (!addDocumentMetadata.getFirst()) {
                throw new UnableToPerformException(MessageSeverity.ERROR, "dvk_send_error_document_type_configuration_missing");
            }

            addAccessRestriction(properties, decContainer);
            addOrganisationRecipients(decContainer);
            addPersonRecipients(decContainer);
            addRecordMetadata(document, decContainer);
            addSignatureInformation(decContainer);
            addRecordCreatorAndRecordSenderToDecMetadata(decContainer);

            if (!DecContainerHandler.validateMandatoryKeysPresent(decContainer, addDocumentMetadata.getSecond())) {
                throw new RuntimeException("Found missing elements!");
            }
            decContainerDocument.validate();
        }

        private void addRecordCreatorAndRecordSenderToDecMetadata(DecContainer decContainer) {
            String organizationName = getOrganizationName();
            String email = parametersService.getStringParameter(Parameters.DOC_SENDER_EMAIL);
            DecContainer.RecordSenderToDec recordSenderToDec = decContainer.getRecordSenderToDec();
            if (recordSenderToDec == null) {
                recordSenderToDec = decContainer.addNewRecordSenderToDec();
            }
            recordSenderToDec.addNewOrganisation().setName(organizationName);
            DecContainer.RecordCreator recordCreator = decContainer.getRecordCreator();
            if (recordCreator == null) {
                recordCreator = decContainer.addNewRecordCreator();
            }
            recordCreator.addNewOrganisation().setName(organizationName);
            ContactDataType contactData = recordCreator.getContactData();
            if (contactData == null) {
                contactData = recordCreator.addNewContactData();
            }
            contactData.setEmail(email);
        }

        private void addRecordMetadata(Node document, DecContainer decContainer) {
            DecContainer.RecordMetadata recordMetadata = decContainer.getRecordMetadata();
            if (recordMetadata == null) {
                recordMetadata = decContainer.addNewRecordMetadata();
            }
            recordMetadata.setRecordGuid(document.getNodeRef().getId());
            String documentTypeName = BeanHelper.getDocumentAdminService().getDocumentTypeName(document);
            recordMetadata.setRecordType(documentTypeName);
        }

        private Pair<Boolean, Map<String, String>> addDocumentMetadata(Map<String, Object> properties, DocumentTypeVersion documentTypeVersion, DecContainer decContainer) {
            Boolean foundRelatedOutgoingField = Boolean.FALSE;
            Map<String, String> usedFieldNameByKey = new HashMap<String, String>();
            for (Field field : documentTypeVersion.getFieldsDeeply()) {
                List<String> decFields = field.getRelatedOutgoingDecElement();
                if (decFields == null || decFields.isEmpty()) {
                    continue;
                }
                String fieldName = field.getName();
                Object value = properties.get(field.getQName());
                for (String decField : decFields) {
                    StringTokenizer tokenizer = new StringTokenizer(decField, ",");
                    while (tokenizer.hasMoreTokens()) {
                        String outgoingField = StringUtils.trim(tokenizer.nextToken());
                        if (StringUtils.isNotBlank(outgoingField)) {
                            if (!DecContainerHandler.hasUserKey(outgoingField)) {
                                throw new RuntimeException("Unable to find outgoing field for key " + outgoingField);
                            }
                            usedFieldNameByKey.put(outgoingField, fieldName);
                            foundRelatedOutgoingField = Boolean.TRUE;
                            if (value != null && !fieldName.equals("Isikukood") && !fieldName.equals("Registrikood / Isikukood")) {
                                DecContainerHandler.setValue(fieldName, outgoingField, decContainer, value);
                            }
                        }
                    }
                }
            }

            return new Pair<Boolean, Map<String, String>>(foundRelatedOutgoingField, usedFieldNameByKey);
        }

        private void addAccessRestriction(Map<String, Object> properties, DecContainer decContainer) {
            Access access = decContainer.addNewAccess();
            String accessRestriction = (String) properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
            boolean openAccessRestriction = AccessRestriction.OPEN.getValueName().equals(accessRestriction);
            if (openAccessRestriction) {
                access.setAccessConditionsCode(AccessConditionType.AVALIK);
            } else if (AccessRestriction.AK.getValueName().equals(accessRestriction)) {
                access.setAccessConditionsCode(AccessConditionType.AK);
            } else {
                throw new UnableToPerformException(MessageSeverity.ERROR, "dvk_send_error_unsupported_access_restriction");
            }

            if (!openAccessRestriction) {
                Access.AccessRestriction dvkAccessRestriction = access
                        .addNewAccessRestriction();
                Date accessRestrictionBeginDate = (Date) properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE);
                if (accessRestrictionBeginDate != null) {
                    Calendar beginDate = Calendar.getInstance();
                    beginDate.setTime(accessRestrictionBeginDate);
                    dvkAccessRestriction.setRestrictionBeginDate(beginDate);
                }
                Date accessRestrictionEndDate = (Date) properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE);
                if (accessRestrictionEndDate != null) {
                    Calendar endDate = Calendar.getInstance();
                    endDate.setTime(accessRestrictionEndDate);
                    dvkAccessRestriction.setRestrictionEndDate(endDate);
                }
                String accessRestrictionEndEvent = (String) properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC);
                if (StringUtils.isNotBlank(accessRestrictionEndEvent)) {
                    dvkAccessRestriction.setRestrictionEndEvent(accessRestrictionEndEvent);
                }
                String accessRestrictionReason = (String) properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON);
                if (StringUtils.isNotBlank(accessRestrictionReason)) {
                    dvkAccessRestriction.setRestrictionBasis(accessRestrictionReason);
                }
                String organisationName = getOrganizationName();
                dvkAccessRestriction.setInformationOwner(organisationName);
            }
        }

        private String getOrganizationName() {
            return StringUtils.defaultIfEmpty(parametersService.getStringParameter(Parameters.DVK_ORGANIZATION_NAME),
                    getOrganisationName(getInstitutionCode()));
        }

        private void addSignatureInformation(DecContainer decContainer) {
            // Check if we have only one file
            List<DecContainer.File> fileList = decContainer.getFileList();
            if (fileList.size() != 1) {
                return;
            }
            // And if it is a DigiDoc
            DecContainer.File file = fileList.get(0);
            if (!SignatureService.DIGIDOC_MIMETYPE.equals(file.getMimeType()) || !FilenameUtil.isDigiDocFile(file.getFileName())) {
                return;
            }

            //
            SignatureItemsAndDataItems signatureItems = null;
            InputStream signatureInput = null;
            try {
                signatureInput = DvkUtil.getFileContents(file);
                signatureItems = BeanHelper.getDigiDoc4JSignatureService().getDataItemsAndSignatureItems(signatureInput, false);
            } catch (SignatureException e) {
                log.error("Failed to retrieve signatures from " + file.getFileName() + " (" + file.getFileGuid() + ")!", e);
                throw new RuntimeException(e);
            } catch (Base64DecodingException e) {
                log.error("Failed to decode DigiDoc from " + file.getFileName() + "!", e);
                throw new RuntimeException(e);
            } catch (IOException e) {
                log.error("Failed to read DigiDoc from " + file.getFileName() + "!", e);
                throw new RuntimeException(e);
            } finally {
                IOUtils.closeQuietly(signatureInput);
            }

            for (SignatureItem signature : signatureItems.getSignatureItems()) {
                DecContainer.SignatureMetadata signatureMetadata = decContainer.addNewSignatureMetadata();
                signatureMetadata.setSignatureType("Digiallkiri");
                signatureMetadata.setSigner(signature.getName());
                signatureMetadata.setVerified(signature.isValid() ? "Allkiri on kehtiv" : "Allkiri on kehtetu");
                Calendar verificationDate = Calendar.getInstance();
                verificationDate.setTime(signature.getSigningTime());
                signatureMetadata.setSignatureVerificationDate(verificationDate);
            }
        }

        private void addPersonRecipients(DecContainer decContainer) {
            List<String> personNames = dvkSendDocuments.getPersonNames();
            if (personNames != null && !personNames.isEmpty()) {
                List<String> idCodes = dvkSendDocuments.getPersonIdCodes();
                Assert.isTrue(idCodes != null && personNames.size() == idCodes.size(), "Expected same number of person names and ID codes!");

                // Check if we already have a transport field
                Transport transport = decContainer.getTransport();
                if (transport == null) {
                    transport = decContainer.addNewTransport();
                }

                String messageForRecipient = new HtmlToPlainText().getPlainText(Jsoup.parse(dvkSendDocuments.getTextContent()));
                for (int i = 0; i < personNames.size(); i++) {
                    String personName = personNames.get(i);
                    String idCode = idCodes.get(i);

                    Recipient recipient = decContainer.addNewRecipient();
                    PersonType person = recipient.addNewPerson();
                    person.setName(personName);
                    person.setPersonalIdCode(idCode);

                    recipient.setMessageForRecipient(messageForRecipient);
                }
            }
        }

        private void addOrganisationRecipients(DecContainer decContainer) {
            // Organisation DecRecipient elements are added during container creation
            List<String> orgRegNrs = dvkSendDocuments.getRecipientsRegNrs();
            if (orgRegNrs != null) {
                List<String> orgNames = dvkSendDocuments.getOrgNames();
                Assert.isTrue(orgNames != null && orgRegNrs.size() == orgNames.size(), "Expected same number of organisation names and registration codes!");
                for (int i = 0; i < orgRegNrs.size(); i++) {
                    String organisationCode = orgRegNrs.get(i);
                    String organisationName = orgNames.get(i);

                    Recipient recipient = decContainer.addNewRecipient();
                    OrganisationType organisation = recipient.addNewOrganisation();
                    organisation.setName(organisationName);
                    organisation.setOrganisationCode(organisationCode);

                    recipient.setMessageForRecipient(new HtmlToPlainText().getPlainText(Jsoup.parse(dvkSendDocuments.getTextContent())));
                }
            }
        }
    }

    private class DhsSendWorkflowCallback implements SendDocumentsDecContainerCallback {
        private final DvkSendWorkflowDocuments dvkSendDocuments;

        public DhsSendWorkflowCallback(DvkSendWorkflowDocuments dvkSendDocuments) {
            // TODO: validate?
            // dvkSendDocuments.validateOutGoing();
            this.dvkSendDocuments = dvkSendDocuments;
        }

        @Override
        public void doWithDocument(DecContainerDocument decContainerDocument) {
            DecContainer decContainer = decContainerDocument.getDecContainer();
            Recipient recipient = decContainer.addNewRecipient();
            recipient.setMessageForRecipient(dvkSendDocuments.getTextContent());

            DecContainer.RecordTypeSpecificMetadata specificMetadata = null;
            try {
                if (dvkSendDocuments.isDocumentNode()) {
                    specificMetadata = composeWorkflowMetaxml(dvkSendDocuments.getRecipientDocNode(), EXTERNAL_REVIEW_DOCUMENT_QNAME);
                } else {
                    specificMetadata = composeWorkflowMetaxml(dvkSendDocuments.getRecipientDocNode(), EXTERNAL_REVIEW_TASK_COMPLETED_QNAME);
                }
                decContainer.setRecordTypeSpecificMetadata(specificMetadata);
            } catch (XmlException e) {
                log.debug("Unable to parse alfresco document xml, error: " + e.getMessage());
                throw new ReviewTaskException(ReviewTaskException.ExceptionType.PARSING_EXCEPTION);
            }

        }

        // FIXME remove when doWithDocument(DecContainerDocument arg0) implements this functionality
        public void doWithDocument(DokumentDocument dokumentDocument) {
            log.debug("altering dokument");
            //
            // final DhlDokumentType dhlDokument = dokumentDocument.getDokument();
            // final Transport transport = dhlDokument.getTransport();
            // fillDefaultSenderData(transport, dvkSendDocuments);
            //
            // Metaxml metaxml = null;
            // try {
            // if (dvkSendDocuments.isDocumentNode()) {
            // metaxml = composeWorkflowMetaxml(dvkSendDocuments.getRecipientDocNode(), EXTERNAL_REVIEW_DOCUMENT_QNAME);
            // } else {
            // metaxml = composeWorkflowMetaxml(dvkSendDocuments.getRecipientDocNode(), EXTERNAL_REVIEW_TASK_COMPLETED_QNAME);
            // }
            // } catch (XmlException e) {
            // log.debug("Unable to parse alfresco document xml, error: " + e.getMessage());
            // throw new ReviewTaskException(ExceptionType.PARSING_EXCEPTION);
            // }
            // dhlDokument.setMetaxml(metaxml);
            //
            // dhlDokument.setTransport(transport);
        }

        private DecContainer.RecordTypeSpecificMetadata composeWorkflowMetaxml(org.w3c.dom.Node domNode, javax.xml.namespace.QName... wrappers) throws XmlException {
            final XmlObject documentXml = XmlObject.Factory.parse(domNode);
            final DecContainer.RecordTypeSpecificMetadata specificMetadata = DecContainer.RecordTypeSpecificMetadata.Factory.newInstance();
            final XmlCursor cursorM = specificMetadata.newCursor();
            cursorM.toNextToken();
            cursorM.beginElement(DELTA_QNAME);
            cursorM.insertElementWithText(DELTA_VERSION, applicationService.getProjectVersion());
            DecContainer.RecordTypeSpecificMetadata docXml = composeMetaxml(documentXml, Arrays.asList(wrappers));
            String docXmlStr = docXml.toString();
            // "resolve" relative namespaces (deprecated feature which cannot be interpreted correctly in importer)
            docXmlStr = StringUtils.replace(docXmlStr, "xmlns:view=\"view\"", "");
            docXmlStr = StringUtils.replace(docXmlStr, "xmlns:view1=\"view\"", "");
            docXmlStr = StringUtils.replace(docXmlStr, "view1:view", "view:view");
            XmlObject docXmlResolved = XmlObject.Factory.parse(docXmlStr);
            docXmlResolved.newCursor().copyXmlContents(cursorM);
            cursorM.dispose();
            return specificMetadata;
        }

    }

    class DhsSendInvoiceToSapCallback implements SendDocumentsDokumentCallback {

        @Override
        public void doWithDocument(DokumentDocument dokumentDocument) {
            throw new RuntimeException("This method is not implemented");
        }
    }

    protected class DhsSendReviewNotificationCallback implements SendDocumentsDecContainerCallback {
        private final DvkSendReviewTask dvkSendReviewTask;

        public DhsSendReviewNotificationCallback(DvkSendReviewTask dvkSendReviewTask) {
            this.dvkSendReviewTask = dvkSendReviewTask;
        }

        @Override
        public void doWithDocument(DecContainerDocument decContainerDocument) {
            DecContainer decContainer = decContainerDocument.getDecContainer();

            Recipient recipient = decContainer.addNewRecipient();
            OrganisationType organisation = recipient.addNewOrganisation();
            organisation.setName(dvkSendReviewTask.getInstitutionName());

            DecContainer.RecordMetadata recordMetadata = decContainer.getRecordMetadata();
            if (recordMetadata == null) {
                recordMetadata = decContainer.addNewRecordMetadata();
            }
            recordMetadata.setRecordGuid(dvkSendReviewTask.getTaskId());
            recordMetadata.setRecordType("Iseseisev terviktöövoog");
            recordMetadata.setRecordOriginalIdentifier("000");
            recordMetadata.setRecordDateRegistered(Calendar.getInstance());
            recordMetadata.setRecordTitle(dvkSendReviewTask.getWorkflowTitle());

            Access access = decContainer.getAccess();
            if (access == null) {
                access = decContainer.addNewAccess();
            }
            access.setAccessConditionsCode(AccessConditionType.AVALIK);

            DecContainer.RecordTypeSpecificMetadata specificMetadata = null;
            try {
                specificMetadata = composeReviewNotificationMetadata(dvkSendReviewTask.getRecipientDocNode());
            } catch (XmlException e) {
                log.debug("Unable to parse deltaKK document xml, error: " + e.getMessage());
                throw new ReviewTaskException(ReviewTaskException.ExceptionType.PARSING_EXCEPTION);
            }
            decContainer.setRecordTypeSpecificMetadata(specificMetadata);
        }

        private DecContainer.RecordTypeSpecificMetadata composeReviewNotificationMetadata(org.w3c.dom.Node domNode) throws XmlException {
            final XmlObject documentXml = XmlObject.Factory.parse(domNode);
            return composeMetaxml(documentXml, null);
        }

    }

    private DecContainer.RecordTypeSpecificMetadata composeMetaxml(final XmlObject documentXml, List<javax.xml.namespace.QName> wrappers) {
        final DecContainer.RecordTypeSpecificMetadata specificMetadata = DecContainer.RecordTypeSpecificMetadata.Factory.newInstance();
        final XmlCursor cursorL = documentXml.newCursor();
        final XmlCursor cursorM = specificMetadata.newCursor();

        cursorM.toNextToken();
        if (wrappers != null) {
            for (javax.xml.namespace.QName wrapper : wrappers) {
                cursorM.beginElement(wrapper);
            }
        }
        cursorL.copyXmlContents(cursorM);
        if (log.isDebugEnabled()) {
            log.debug("specificMetadata composed based on document node:\n" + specificMetadata + "\n\n");
        }
        cursorL.dispose();
        cursorM.dispose();
        return specificMetadata;
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

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    // TODO: move to Application or somewhere else?
    @Override
    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }
    @Override
    public String getSubSystemCode() {
        return subSystemCode;
    }

    public void setSubSystemCode(String subSystemCode) {
        this.subSystemCode = subSystemCode;
    }
    
	/**
	 * @return the dvkSendFailedDocuments
	 */
	public List<Document> getDvkSendFailedDocuments() {
		return dvkSendFailedDocuments;
	}

	/**
	 * @param dvkSendFailedDocuments the dvkSendFailedDocuments to set
	 */
	public void setDvkSendFailedDocuments(List<Document> dvkSendFailedDocuments) {
		this.dvkSendFailedDocuments = dvkSendFailedDocuments;
	}


    // END: getters / setters

}
