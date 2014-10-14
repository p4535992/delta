package ee.webmedia.alfresco.dvk.service;

import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;
import static ee.webmedia.alfresco.utils.DvkUtil.getFileContents;
import static ee.webmedia.alfresco.utils.DvkUtil.getFileName;
import static ee.webmedia.alfresco.utils.XmlUtil.findChildByQName;
import static ee.webmedia.alfresco.utils.XmlUtil.getXmlGregorianCalendar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.view.ExcludingExporterCrawlerParameters;
import org.alfresco.service.cmr.view.ExporterException;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.cmr.view.ReferenceType;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlTokenSource;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.adit.service.AditService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.classificator.enums.TransmittalMode;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.service.ApplicationConstantsBean;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.dvk.model.DvkModel;
import ee.webmedia.alfresco.dvk.model.DvkSendReviewTask;
import ee.webmedia.alfresco.dvk.model.DvkSendWorkflowDocuments;
import ee.webmedia.alfresco.dvk.service.ReviewTaskException.ExceptionType;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.alfresco.notification.model.NotificationModel;
import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.utils.DvkUtil;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.volume.model.UnmodifiableVolume;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.workflow.generated.DeleteLinkedReviewTaskType;
import ee.webmedia.alfresco.workflow.generated.DeltaKKRootType;
import ee.webmedia.alfresco.workflow.generated.LinkedReviewTaskType;
import ee.webmedia.alfresco.workflow.generated.ObjectFactory;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.ExternalReviewWorkflowImporterService;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.xtee.client.dhl.DhlDocumentVersion;
import ee.webmedia.xtee.client.dhl.DhlXTeeService;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ReceivedDocumentsWrapper.ReceivedDocument;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.deccontainer.vers21.DecContainerDocument.DecContainer.Transport.DecRecipient;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.EdastusDocument.Edastus;
import ee.webmedia.xtee.client.dhl.types.ee.riik.xtee.dhl.producers.producer.dhl.GetSendStatusResponseTypeUnencoded.Item;

public class DvkServiceSimImpl extends DvkServiceImpl {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DvkServiceSimImpl.class);
    private DocumentService documentService;
    private DocumentLogService documentLogService;
    private DocumentSearchService documentSearchService;
    private DocumentDynamicService documentDynamicService;
    private VolumeService volumeService;
    private ExternalReviewWorkflowImporterService importerService;
    private ExporterService exporterService;
    private SendOutService sendOutService;
    private FileService fileService;
    private WorkflowService workflowService;
    private NotificationService notificationService;
    private EInvoiceService einvoiceService;
    private ApplicationConstantsBean applicationConstantsBean;

    @Override
    public int updateDocAndTaskSendStatuses() {
        // there are as many sendInfoRefs to the same dvkId as there were recipients whom doc were delivered using DVK
        final Map<NodeRef /* sendInfo */, Pair<String /* dvkId */, String /* recipientRegNr */>> docRefsAndIds = documentSearchService.searchOutboxDvkIds();
        final Map<NodeRef /* taskRef */, Pair<String /* dvkId */, String /* recipientRegNr */>> taskRefsAndIds //
        = documentSearchService.searchTaskBySendStatusQuery(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK);
        taskRefsAndIds.putAll(documentSearchService.searchTaskBySendStatusQuery(WorkflowSpecificModel.Types.REVIEW_TASK));
        if (docRefsAndIds.size() == 0 && taskRefsAndIds.size() == 0) {
            return 0; // no need to ask statuses
        }
        // get unique dvkIds
        final Set<String> dvkIds = new HashSet<String>(docRefsAndIds.size() + taskRefsAndIds.size());
        for (Pair<String, String> value : docRefsAndIds.values()) {
            dvkIds.add(value.getFirst());
        }
        Map<String, String> taskDvkIdsAndRegNrs = new HashMap<String, String>();
        for (Pair<String, String> value : taskRefsAndIds.values()) {
            String dvkId = value.getFirst();
            dvkIds.add(dvkId);
            taskDvkIdsAndRegNrs.put(dvkId, value.getSecond());
        }

        List<Item> sendStatuses = null;
        // get sendStatus for each dvkId
        try {
            sendStatuses = dhlXTeeService.getSendStatuses(dvkIds);
            MonitoringUtil.logSuccess(MonitoredService.OUT_XTEE_DVK);
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_XTEE_DVK, e);
            throw e;
        }
        // fill map containing statuses by ids
        final HashMap<String /* dhlId */, Map<String, Pair<SendStatus, Date>>> statusesByIds = new HashMap<String, Map<String, Pair<SendStatus, Date>>>();
        for (Item item : sendStatuses) {
            Map<String, Pair<SendStatus, Date>> statusesForDvkId = new HashMap<String, Pair<SendStatus, Date>>();
            String dhlId = item.getDhlId();
            statusesByIds.put(dhlId, statusesForDvkId);
            List<Edastus> forwardings = item.getEdastusList();
            for (Edastus forwarding : forwardings) {
                Calendar read = forwarding.getLoetud();
                Date receiveTime = read != null ? read.getTime() : null;
                Pair<SendStatus, Date> sendStatusAndReceivedTime = new Pair<SendStatus, Date>(SendStatus.get(forwarding.getStaatus()), receiveTime);
                String regNr = taskDvkIdsAndRegNrs.containsKey(dhlId) ? taskDvkIdsAndRegNrs.get(dhlId) : forwarding.getSaaja().getRegnr();
                if (AditService.NAME.equalsIgnoreCase(regNr)) {
                    // For documents that were sent to "adit" the recipient regNr value will be "adit". Use id codes instead to distinguish between recipients.
                    regNr = forwarding.getSaaja().getIsikukood();
                }
                statusesForDvkId.put(regNr, sendStatusAndReceivedTime);
            }
        }

        return updateNodeSendStatus(docRefsAndIds, statusesByIds, DocumentCommonModel.Props.SEND_INFO_SEND_STATUS)
                + updateNodeSendStatus(taskRefsAndIds, statusesByIds, WorkflowSpecificModel.Props.SEND_STATUS);
    }

    @Override
    public Pair<Integer, Integer> resendFailedSends() {
        List<Task> tasksToResend = documentSearchService.searchReviewTaskToResendQuery();
        int tasksFound = tasksToResend.size();
        int tasksSent = 0;
        RetryingTransactionHelper retryingTransactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
        for (final Task task : tasksToResend) {
            try {
                tasksSent += retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Integer>() {
                    @Override
                    public Integer execute() throws Throwable {
                        return sendReviewTaskNotification(task) ? 1 : 0;
                    }
                }, false, true);
            } catch (RuntimeException e) {
                log.error("Failed to resend review task to dvk, task=" + task, e);
            }
        }
        return new Pair<Integer, Integer>(tasksFound, tasksSent);
    }

    private int updateNodeSendStatus(final Map<NodeRef, Pair<String, String>> refsAndIds, final HashMap<String, Map<String, Pair<SendStatus, Date>>> statusesByIds, QName propToSet) {
        final HashSet<String> dhlIdsStatusChanged = new HashSet<String>();
        boolean isDocSendInfo = DocumentCommonModel.Props.SEND_INFO_SEND_STATUS.equals(propToSet);
        WorkflowDbService workflowDbService = null;
        if (!isDocSendInfo) {
            workflowDbService = BeanHelper.getWorkflowDbService();
        }
        // update each sendInfoRef if status has changed(from SENT to RECEIVED or CANCELLED)
        for (Entry<NodeRef, Pair<String, String>> refAndDvkId : refsAndIds.entrySet()) {
            final NodeRef sendInfoRef = refAndDvkId.getKey();
            final String dvkId = refAndDvkId.getValue().getFirst();
            final String recipientRegNr = refAndDvkId.getValue().getSecond();
            Map<String, Pair<SendStatus, Date>> recipientStatuses = statusesByIds.get(dvkId);
            SendStatus status = null;
            Date receivedDateTime = null;
            if (recipientStatuses != null) {
                Pair<SendStatus, Date> sendStatusAndReceivedTime = recipientStatuses.get(recipientRegNr);
                if (sendStatusAndReceivedTime != null) {
                    status = sendStatusAndReceivedTime.getFirst();
                    receivedDateTime = sendStatusAndReceivedTime.getSecond();
                }
            }
            if (status != null && !status.equals(SendStatus.SENT)) {
                dhlIdsStatusChanged.add(dvkId);
                Map<QName, Serializable> propsToUpdate = new HashMap<QName, Serializable>();
                propsToUpdate.put(propToSet, status.toString());
                if (status.equals(SendStatus.RECEIVED) && receivedDateTime != null) {
                    propsToUpdate.put(isDocSendInfo ? DocumentCommonModel.Props.SEND_INFO_RECEIVED_DATE_TIME : WorkflowSpecificModel.Props.RECEIVED_DATE_TIME, receivedDateTime);
                }
                if (isDocSendInfo) {
                    nodeService.addProperties(sendInfoRef, propsToUpdate);
                } else if (workflowDbService != null) {
                    workflowDbService.updateTaskProperties(sendInfoRef, propsToUpdate);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("dvk status changed for dvkIds: " + dhlIdsStatusChanged);
        }
        return dhlIdsStatusChanged.size();
    }

    @Override
    protected NodeRef createDocumentNode(Map<QName, Serializable> documentProperties, NodeRef dvkIncomingFolder, String nvlDocumentTitle, boolean isAditDocument,
            String messageForRecipient) {
        final NodeRef docRef = documentDynamicService.createNewDocument(
                SystematicDocumentType.INCOMING_LETTER.getId(),
                dvkIncomingFolder).getFirst().getNodeRef();

        final Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.putAll(documentProperties);
        props.put(DocumentCommonModel.Props.DOC_NAME, nvlDocumentTitle);
        props.put(DocumentCommonModel.Props.STORAGE_TYPE, StorageType.DIGITAL.getValueName());
        props.put(DocumentSpecificModel.Props.TRANSMITTAL_MODE, isAditDocument ? TransmittalMode.STATE_PORTAL_EESTI_EE.getValueName() : TransmittalMode.DVK.getValueName());
        nodeService.addProperties(docRef, props);

        documentLogService.addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_imported"
                , I18NUtil.getMessage("document_log_creator_dvk"), messageForRecipient), I18NUtil.getMessage("document_log_creator_dvk"));
        return docRef;
    }

    private <D extends XmlObject> org.w3c.dom.Node getDhsNodeForParsing(D dhlDokument, javax.xml.namespace.QName dhsNodeName) {
        XmlTokenSource metaXmlElement = DvkUtil.getMetaXmlElement(dhlDokument);
        if (metaXmlElement == null) {
            return null;
        }

        org.w3c.dom.Node deltaNode = findChildByQName(DELTA_QNAME, metaXmlElement.getDomNode().getFirstChild());
        if (deltaNode != null) {
            // TODO: log version element?
            org.w3c.dom.Node externalReviewNode = findChildByQName(dhsNodeName, deltaNode);
            if (externalReviewNode != null) {
                NodeList childNodes = externalReviewNode.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    if ("view".equals(childNodes.item(i).getLocalName())) {
                        return childNodes.item(i);
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected <D extends XmlObject, F extends XmlObject> NodeRef importWorkflowData(String senderOrgName, D dhlDokument, NodeRef dvkDefaultIncomingFolder,
            List<F> dataFileList, String dvkId) {
        org.w3c.dom.Node docNode = getDhsNodeForParsing(dhlDokument, EXTERNAL_REVIEW_DOCUMENT_QNAME);
        if (docNode != null) {
            // Location will be used for creating new document if document doesn't exist
            Pair<Location, Boolean> locationWithCheck = getDvkWorkflowDocLocation(senderOrgName, dvkDefaultIncomingFolder);
            Location location = locationWithCheck.getFirst();
            // get original dvk ids here to avoid altering current importing process
            // (create parent -> create children)
            // NB! if more sophisticated travesring of new document is needed, it may be easier
            // to import it twice: first to drafts space, then retrieve and/or check necessary properties and
            // import it second time over existing document
            Map<String, String> originalDvkIdsAndStatuses = new HashMap<String, String>();
            getOriginalDvkIds(docNode, originalDvkIdsAndStatuses);

            List<String> originalDvkIds = new ArrayList<String>(originalDvkIdsAndStatuses.keySet());
            originalDvkIds.add(dvkId);
            List<Task> dvkTasks = documentSearchService.searchTasksByOriginalDvkIdsQuery(originalDvkIds);
            NodeRef existingDocumentNodeRef = getExistingDocumentNodeRef(dvkTasks);
            // status error notification content needs to be generated before actual import because
            // import is going to overwrite the task and workflows we need to report
            List<String> statusErrorNotificationContents = checkDvkDocumentVersionAndStatus(dvkTasks, dvkId, existingDocumentNodeRef, originalDvkIdsAndStatuses);
            try {
                Map<QName, Task> notifications = new HashMap<QName, Task>();
                String oldRegNumber = null;
                if (existingDocumentNodeRef != null) {
                    oldRegNumber = (String) nodeService.getProperty(existingDocumentNodeRef, REG_NUMBER);
                }
                NodeRef importedDoc = importerService.importWorkflowDocument(XmlObject.Factory.parse(docNode).newReader(), location, existingDocumentNodeRef,
                        dataFileList, dvkId, notifications);
                if (existingDocumentNodeRef == null && importedDoc != null) {
                    documentService.updateParentNodesContainingDocsCount(importedDoc, true);
                }
                if (importedDoc != null) {
                    String regNumber = (String) nodeService.getProperty(importedDoc, REG_NUMBER);
                    documentService.updateParentDocumentRegNumbers(importedDoc, oldRegNumber, regNumber);
                }
                if (existingDocumentNodeRef == null && !locationWithCheck.getSecond()) {
                    // new document was imported to wrong (default) location
                    notifications.put(NotificationModel.NotificationType.EXTERNAL_REVIEW_WORKFLOW_SERIES_ERROR, documentSearchService.searchTaskByOriginalDvkIdQuery(dvkId));
                }
                // send user notifications and notifications about non-fatal errors during import to admin
                sendNotifications(notifications, statusErrorNotificationContents);
                return importedDoc;
            } catch (XmlException e) {
                throw new ReviewTaskException(ExceptionType.PARSING_EXCEPTION, "Failed to parse DVK input document, dhlDocument=" + dhlDokument + ",\n docNode=" + docNode);
            }
        }
        return null;
    }

    @Override
    protected <F extends XmlObject> List<NodeRef> importInvoiceData(String dhlId, String senderOrgNr, List<F> dataFileList) {
        List<NodeRef> newInvoices = new ArrayList<NodeRef>();
        if (!applicationConstantsBean.isEinvoiceEnabled()) {
            return newInvoices;
        }
        try {
            NodeRef receivedInvoiceFolder = BeanHelper.getConstantNodeRefsBean().getReceivedincoiceRoot();
            // read invoice(s) from attachments
            Map<NodeRef, Integer> invoiceRefToDatafile = new HashMap<NodeRef, Integer>();
            Map<NodeRef, Integer> transactionRefToDataFile = new HashMap<NodeRef, Integer>();
            Integer dataFileIndex = 0;
            for (F dataFile : dataFileList) {
                if (isXmlMimetype(getFileName(dataFile))) {
                    List<NodeRef> newDocRefs = einvoiceService.importInvoiceFromXml(receivedInvoiceFolder, getFileContents(dataFile), TransmittalMode.DVK);
                    newInvoices.addAll(newDocRefs);
                    for (NodeRef newDocRef : newDocRefs) {
                        invoiceRefToDatafile.put(newDocRef, dataFileIndex);
                    }
                }
                dataFileIndex++;
            }
            transactionRefToDataFile = einvoiceService.importTransactionsForInvoices(newInvoices, dataFileList);
            for (NodeRef invoiceRef : newInvoices) {
                documentLogService.addDocumentLog(invoiceRef, I18NUtil.getMessage("document_log_status_imported"
                        , I18NUtil.getMessage("document_log_creator_dvk")), I18NUtil.getMessage("document_log_creator_dvk"));
                dataFileIndex = 0;
                for (F dataFile : dataFileList) {
                    if ((!transactionRefToDataFile.containsValue(dataFileIndex) || transactionRefToDataFile.get(invoiceRef).equals(dataFileIndex))
                            && (!invoiceRefToDatafile.containsValue(dataFileIndex) || invoiceRefToDatafile.get(invoiceRef).equals(dataFileIndex))) {
                        storeFile(dhlId, senderOrgNr, invoiceRef, dataFile);
                    }
                    dataFileIndex++;
                }
            }

            return newInvoices;
        } catch (Base64DecodingException e) {
            throw new RuntimeException("Failed to decode", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected <F extends XmlObject> List<NodeRef> importDimensionData(String dhlId, List<F> dataFileList) {
        List<NodeRef> dimensionNodes = new ArrayList<NodeRef>();
        if (!applicationConstantsBean.isEinvoiceEnabled() || dataFileList == null) {
            return dimensionNodes;
        }
        for (XmlObject dataFile : dataFileList) {
            if (isXmlMimetype(getFileName(dataFile))) {
                List<NodeRef> fileImportNodes = new ArrayList<NodeRef>();
                try {
                    InputStream inputStream = getFileContents(dataFile);
                    fileImportNodes.addAll(einvoiceService.importVatCodeList(inputStream));
                    if (fileImportNodes.size() == 0) {
                        dimensionNodes.addAll(einvoiceService.importAccountList(inputStream));
                    }
                    if (fileImportNodes.size() == 0) {
                        fileImportNodes.addAll(einvoiceService.importSellerList(inputStream));
                    }
                    if (fileImportNodes.size() == 0) {
                        fileImportNodes.addAll(einvoiceService.importDimensionsList(inputStream));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to decode", e);
                }
                dimensionNodes.addAll(fileImportNodes);
            }
        }
        return dimensionNodes;
    }

    @Override
    public boolean isXmlMimetype(String dataFileName) {
        Assert.hasLength("Must provide file name to check!");
        String mimeType = mimetypeService.guessMimetype(dataFileName);
        boolean isXmlMimetype = MimetypeMap.MIMETYPE_XML.equalsIgnoreCase(mimeType);
        return isXmlMimetype;
    }

    private void sendNotifications(Map<QName, Task> notifications, List<String> statusErrorNotificationContent) {
        for (Entry<QName, Task> entry : notifications.entrySet()) {
            QName notificationType = entry.getKey();
            Task task = entry.getValue();
            if (NotificationModel.NotificationType.TASK_NEW_TASK_NOTIFICATION.equals(notificationType)
                    || NotificationModel.NotificationType.TASK_CANCELLED_TASK_NOTIFICATION.equals(notificationType)) {
                notificationService.notifyTaskEvent(task);
            } else if (NotificationModel.NotificationType.EXTERNAL_REVIEW_WORKFLOW_RECIEVING_ERROR.equals(notificationType)
                    || NotificationModel.NotificationType.EXTERNAL_REVIEW_WORKFLOW_SERIES_ERROR.equals(notificationType)) {
                notificationService.notifyExternalReviewError(task);
            }
        }
        if (statusErrorNotificationContent != null) {
            for (String content : statusErrorNotificationContent) {
                notificationService.notifyExternalReviewError(content);
            }
        }
    }

    private List<String> checkDvkDocumentVersionAndStatus(List<Task> existingDvkTasks, String dvkIdStr,
            NodeRef existingDocumentNodeRef, Map<String, String> originalDvkIdsAndStatuses) {
        // nothing to check for new document
        if (existingDocumentNodeRef == null) {
            return null;
        }
        int dvkId = 0;
        try {
            dvkId = Integer.parseInt(dvkIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
        List<String> statusErrors = new ArrayList<String>();
        for (Task task : existingDvkTasks) {
            String taskRecievedDvkIdStr = task.getProp(WorkflowSpecificModel.Props.RECIEVED_DVK_ID);
            int taskRecievedDvkId = 0;
            try {
                taskRecievedDvkId = Integer.parseInt(taskRecievedDvkIdStr);
            } catch (NumberFormatException e) {
                continue;
            }
            if (dvkId <= taskRecievedDvkId) {
                throw new ReviewTaskException(ExceptionType.VERSION_CONFLICT, task, taskRecievedDvkIdStr);
            }
            if (taskRecievedDvkIdStr != null) {
                String originalStatus = originalDvkIdsAndStatuses.get(task.getProp(WorkflowSpecificModel.Props.ORIGINAL_DVK_ID));
                if (!task.getStatus().equalsIgnoreCase(originalStatus) && task.getProp((WorkflowSpecificModel.Props.SEND_STATUS)) != null) {
                    log.error("Task with dvkId=" + originalDvkIdsAndStatuses + " has already been sent, going to overwrite. Old task=" + task);
                    statusErrors.add(notificationService.generateTemplateContent(NotificationModel.NotificationType.EXTERNAL_REVIEW_WORKFLOW_RECIEVING_ERROR, task));
                }
            }
        }
        return statusErrors;
    }

    @Override
    protected <D extends XmlObject> NodeRef importTaskData(D dhlDokument, String dvkId) throws ReviewTaskException {
        org.w3c.dom.Node taskNode = getDhsNodeForParsing(dhlDokument, EXTERNAL_REVIEW_TASK_COMPLETED_QNAME);
        if (taskNode != null) {
            // get original dvk ids here to avoid altering current importing process
            // (create parent -> create children)
            Map<String, String> originalDvkIdsAndStatuses = new HashMap<String, String>();
            getOriginalDvkIds(taskNode, originalDvkIdsAndStatuses);
            Task originalTask = null;
            if (originalDvkIdsAndStatuses.size() > 0) {
                List<Task> tasks = documentSearchService.searchTasksByOriginalDvkIdsQuery(originalDvkIdsAndStatuses.keySet());
                originalTask = getExistingTask(tasks);
                if (originalTask != null) {
                    String attemptedStatus = getPropByName(taskNode, WorkflowCommonModel.Props.STATUS);
                    if (Status.FINISHED.equals(attemptedStatus)
                            && originalTask.isStatus(Status.IN_PROGRESS)) {
                        workflowService.finishInProgressExternalReviewTask(originalTask,
                                getPropByName(taskNode, WorkflowSpecificModel.Props.COMMENT),
                                getPropByName(taskNode, WorkflowCommonModel.Props.OUTCOME),
                                DefaultTypeConverter.INSTANCE.convert(Date.class, getPropByName(taskNode, WorkflowCommonModel.Props.COMPLETED_DATE_TIME)),
                                dvkId);
                        return originalTask.getNodeRef();
                    } else {
                        throw new ReviewTaskException(ExceptionType.TASK_OVERWRITE_WRONG_STATUS, originalTask, dvkId, attemptedStatus);
                    }
                }
            }
            if (originalTask == null) {
                throw new ReviewTaskException(ExceptionType.ORIGINAL_TASK_NOT_FOUND, null
                        , originalDvkIdsAndStatuses.size() > 0 ? originalDvkIdsAndStatuses.keySet().iterator().next() : null);
            }
        }
        return null;
    }

    @Override
    protected <D extends XmlObject> NodeRef importReviewTaskData(D dhlDokument, String dvkId) throws ReviewTaskException {
        final XmlTokenSource metaxml = DvkUtil.getMetaXmlElement(dhlDokument);
        if (metaxml != null) {
            InputStream taskInputStream = metaxml.newInputStream();
            JAXBElement<DeltaKKRootType> deltaKKRoot = null;
            deltaKKRoot = WorkflowUtil.unmarshalDeltaKK(taskInputStream);
            if (deltaKKRoot != null) {
                LinkedReviewTaskType linkedTask = deltaKKRoot.getValue().getLinkedReviewTask();
                if (linkedTask != null) {
                    return workflowService.importLinkedReviewTask(linkedTask, dvkId);
                }
                DeleteLinkedReviewTaskType deletedTask = deltaKKRoot.getValue().getDeleteLinkedReviewTask();
                if (deletedTask != null) {
                    NodeRef existingTaskRef = workflowService.markLinkedReviewTaskDeleted(deletedTask);
                    // don't return null because this initiates futher dvk import, but at this point it is sure that document was
                    // meant to be imported as linkedReviewTask, even if no corresponding task is found
                    return existingTaskRef != null ? existingTaskRef : RepoUtil.createNewUnsavedNodeRef();
                }
            }
        }
        return null;
    }

    // return first matching document containing some of given tasks and having aspect notEditable property notEditable=true
    private NodeRef getExistingDocumentNodeRef(List<Task> dvkTasks) {
        // TODO: maybe check if all returned tasks belong to one document?
        for (Task task : dvkTasks) {
            NodeRef docRef = getTaskDocument(task);
            if (Boolean.TRUE.equals(nodeService.getProperty(docRef, DocumentCommonModel.Props.NOT_EDITABLE))) {
                return docRef;
            }
        }
        return null;
    }

    // return document containing some of given tasks;
    // in case of several documents prefer one with no aspect notEditable
    private Task getExistingTask(List<Task> dvkTasks) {
        Task task = null;
        for (Task tsk : dvkTasks) {
            NodeRef docRef = getTaskDocument(tsk);
            if (task == null || !nodeService.hasAspect(docRef, DocumentCommonModel.Aspects.NOT_EDITABLE)) {
                task = tsk;
            }
        }
        return task;
    }

    public NodeRef getTaskDocument(Task task) {
        Workflow workflow = task.getParent();
        CompoundWorkflow compoundWorkflow = workflowService.getCompoundWorkflow(nodeService.getPrimaryParent(workflow.getNode().getNodeRef()).getParentRef());
        NodeRef docRef = compoundWorkflow.getParent();
        return docRef;
    }

    private void getOriginalDvkIds(org.w3c.dom.Node docNode, Map<String, String> originalDvkIdsAndStatuses) {
        // TODO: maybe implement more sophisticated traversing (checking parent node types etc)
        NodeList childNodes = docNode.getChildNodes();
        String originalDvkId = null;
        String status = null;
        for (int i = 0; i < childNodes.getLength(); i++) {
            org.w3c.dom.Node childNode = childNodes.item(i);
            if (WorkflowSpecificModel.Props.ORIGINAL_DVK_ID.getLocalName().equalsIgnoreCase(childNode.getLocalName())
                    && WorkflowSpecificModel.Props.ORIGINAL_DVK_ID.getNamespaceURI().equalsIgnoreCase(childNode.getNamespaceURI())) {
                org.w3c.dom.Node text = childNode.getFirstChild();
                if (text != null && text.getNodeType() == org.w3c.dom.Node.TEXT_NODE && StringUtils.isNotBlank(text.getNodeValue())) {
                    originalDvkId = text.getNodeValue();
                }
            } else if (WorkflowCommonModel.Props.STATUS.getLocalName().equalsIgnoreCase(childNode.getLocalName())
                    && WorkflowCommonModel.Props.STATUS.getNamespaceURI().equalsIgnoreCase(childNode.getNamespaceURI())) {
                org.w3c.dom.Node text = childNode.getFirstChild();
                if (text != null && text.getNodeType() == org.w3c.dom.Node.TEXT_NODE && StringUtils.isNotBlank(text.getNodeValue())) {
                    status = text.getNodeValue();
                }
            }
            getOriginalDvkIds(childNode, originalDvkIdsAndStatuses);
        }
        if (originalDvkId != null) {
            originalDvkIdsAndStatuses.put(originalDvkId, status);
        }
    }

    // return first matching not null element text value
    private String getPropByName(org.w3c.dom.Node docNode, QName propName) {
        NodeList childNodes = docNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            org.w3c.dom.Node childNode = childNodes.item(i);
            if (propName.getLocalName().equalsIgnoreCase(childNode.getLocalName())
                    && propName.getNamespaceURI().equalsIgnoreCase(childNode.getNamespaceURI())) {
                org.w3c.dom.Node text = childNode.getFirstChild();
                if (text != null && text.getNodeType() == org.w3c.dom.Node.TEXT_NODE && StringUtils.isNotBlank(text.getNodeValue())) {
                    return text.getNodeValue();
                }
                return null;
            }
            String propFromChild = getPropByName(childNode, propName);
            if (propFromChild != null) {
                return propFromChild;
            }
        }
        return null;
    }

    @Override
    protected void handleStorageFailure(ReceivedDocument receivedDocument, String dhlId, NodeRef dvkIncomingFolder, Collection<String> previouslyFailedDvkIds, Exception e) {
        super.handleStorageFailure(receivedDocument, dhlId, dvkIncomingFolder, previouslyFailedDvkIds, e);
        if (e instanceof ReviewTaskException) {
            handleExternalReviewException((ReviewTaskException) e);
        }
        if (previouslyFailedDvkIds.contains(dhlId)) {
            log.debug("tried to receive document with dvkId='" + dhlId + "' that we had already failed to receive before");
            return;
        }
        previouslyFailedDvkIds.add(dhlId);
        final XmlObject dhlDocument = DhlDocumentVersion.VER_1.equals(receivedDocument.getDocumentVersion()) ? receivedDocument.getDhlDocument() : receivedDocument
                .getDhlDocumentV2();
        try {
            String corruptDocName = DvkUtil.getCorruptedDocumentName(receivedDocument, dhlId);
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            String time = format.format(new Date());
            corruptDocName = FilenameUtil.buildFileName(corruptDocName + " " + time, "xml");
            log.debug("Trying to store DVK document to '" + corruptDvkDocumentsPath + "+/" + corruptDocName + "'");
            NodeRef corruptFolder = BeanHelper.getConstantNodeRefsBean().getDvkCorruptRoot();
            final NodeRef corruptDocNodeRef = fileFolderService.create(corruptFolder, corruptDocName, DvkModel.Types.FAILED_DOC).getNodeRef();
            nodeService.setProperty(corruptDocNodeRef, DvkModel.Props.DVK_ID, dhlId);
            final ContentWriter writer = fileFolderService.getWriter(corruptDocNodeRef);
            writer.setMimetype(MimetypeMap.MIMETYPE_XML);
            final OutputStream os = writer.getContentOutputStream();
            try {
                dhlDocument.save(os, DvkUtil.getDecContainerXmlOptions());
            } catch (IOException e3) {
                throw new RuntimeException("Failed write output to repository: '" + corruptDvkDocumentsPath + "' nodeRef=" + corruptDocNodeRef + " contentUrl="
                        + writer.getContentUrl(), e);
            } finally {
                os.close();
            }
        } catch (Exception e3) {
            final String msg = "Failed to store DVK document and failed to handle storage failure of document with dhlId=" + dhlId + ".";
            log.fatal(msg, e3);
            throw new RuntimeException(msg, e3);
        }
    }

    private void handleExternalReviewException(ReviewTaskException e) {
        if (e.isType(ExceptionType.PARSING_EXCEPTION)) {
            log.error(e.getMessage(), e);
        } else if (e.isType(ExceptionType.VERSION_CONFLICT)) {
            log.error(e.getMessage(), e);
            notifyExternalReviewReceivingFailure(e.getTask());
        } else if (e.isType(ExceptionType.TASK_OVERWRITE_WRONG_STATUS)) {
            log.error(e.getMessage(), e);
            notifyExternalReviewReceivingFailure(e.getTask());
        } else if (e.isType(ExceptionType.ORIGINAL_TASK_NOT_FOUND)) {
            log.error(e.getMessage(), e);
        }

    }

    private void notifyExternalReviewReceivingFailure(Task task) {
        notificationService.notifyExternalReviewError(task);
    }

    @Override
    protected Pair<Location, Boolean> getDvkWorkflowDocLocation(String senderName, NodeRef dvkDefaultIncomingFolder) {
        String dvkWorkflowSeriesMark = parametersService.getStringParameter(Parameters.EXTERNAL_REVIEW_AUTOMATIC_FOLDER);
        if (dvkWorkflowSeriesMark == null) {
            return new Pair<Location, Boolean>(new Location(dvkDefaultIncomingFolder), Boolean.FALSE);
        }
        NodeRef seriesRef = documentSearchService.searchSeriesByIdentifier(dvkWorkflowSeriesMark);
        NodeRef dvkDocVolumeRef = null;
        if (seriesRef == null) {
            return new Pair<Location, Boolean>(new Location(dvkDefaultIncomingFolder), Boolean.FALSE);
        }
        List<UnmodifiableVolume> volumes = volumeService.getAllValidVolumesBySeries(seriesRef);
        for (UnmodifiableVolume volume : volumes) {
            if (StringUtils.equalsIgnoreCase(volume.getTitle(), senderName)) {
                dvkDocVolumeRef = volume.getNodeRef();
                break;
            }
        }
        if (dvkDocVolumeRef == null) {
            Volume dvkDocVolume = volumeService.createVolume(seriesRef);
            // TODO: standard configuration for creating new dvkWorkflowDocumentFolder
            dvkDocVolume.setValidFrom(new Date());
            dvkDocVolume.setContainsCases(false);
            dvkDocVolume.setTitle(senderName);
            dvkDocVolume.setStatus(DocListUnitStatus.OPEN.getValueName());
            dvkDocVolume.setVolumeMark(dvkWorkflowSeriesMark + "-" + senderName);
            dvkDocVolume.setVolumeTypeEnum(VolumeType.ANNUAL_FILE);
            dvkDocVolumeRef = volumeService.saveOrUpdate(dvkDocVolume, false);
        }
        return new Pair<Location, Boolean>(new Location(dvkDocVolumeRef), Boolean.TRUE);
    }

    @Override
    public void sendDvkTask(Task obj) {
        if (!(obj instanceof Task)) {
            throw new RuntimeException("Only tasks allowed! Object class is " + obj.getClass());
        }
        Task task = obj;
        List<Node> dvkCapableOrgs = addressbookService.getDvkCapableOrgs();
        if (isDvkCapable(dvkCapableOrgs, task.getInstitutionCode())) {
            ExcludingExporterCrawlerParameters exportParameters = getTaskExportParameters(task.getNodeRef());
            org.w3c.dom.Node taskDomNode = exportDom(exportParameters);
            DvkSendWorkflowDocuments sd = new DvkSendWorkflowDocuments();
            sd.setIsDocumentNode(false);
            sd.setRecipientsRegNr(task.getCreatorInstitutionCode());
            sd.setRecipientStructuralUnit(task.getOwnerName());
            sd.setRecipientDocNode(taskDomNode);
            sd.setTextContent(WorkflowUtil.getTaskMessageForRecipient(task));
            String dvkId = sendExternalReviewWorkflowData(new ArrayList<ContentToSend>(), sd);
            nodeService.setProperty(task.getNodeRef(), WorkflowSpecificModel.Props.SENT_DVK_ID, dvkId);
            nodeService.setProperty(task.getNodeRef(), WorkflowSpecificModel.Props.SEND_STATUS, SendStatus.SENT);
        }
    }

    @Override
    public String sendInvoiceFileToSap(Node document, File file) {
        throw new RuntimeException("This method is not implemented!");
    }

    @Override
    public String generateAndSendInvoiceFileToSap(Node node, List<Transaction> transactions) throws IOException {
        return sendInvoiceFileToSap(node, einvoiceService.generateTransactionXmlFile(node, transactions));
    }

    @Override
    public boolean sendReviewTaskNotification(Task task) {
        return sendReviewTaskNotification(task, createDeltaKKRootTypeNode(task), false);
    }

    @Override
    public void sendReviewTaskDeletingNotification(Task task) {
        sendReviewTaskNotification(task, createDeltaKKRootTypeDeletingNode(task), true);
    }

    private boolean sendReviewTaskNotification(Task task, Document nodeToSend, boolean isDeletingNotification) {
        NodeRef taskRef = task.getNodeRef();
        // these properties are used to determine if last sending was successful or not
        // and to retry sending to DVK if needed.
        WorkflowDbService workflowDbService = BeanHelper.getWorkflowDbService();
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(WorkflowSpecificModel.Props.SENT_DVK_ID, null);
        props.put(WorkflowSpecificModel.Props.SEND_STATUS, null);
        workflowDbService.updateTaskProperties(task.getNodeRef(), props);
        DvkSendReviewTask sendReviewTask = new DvkSendReviewTask();
        sendReviewTask.setSenderRegNr(task.getCreatorInstitutionCode());
        sendReviewTask.setSenderOrgName(task.getCreatorInstitutionName());
        sendReviewTask.setSenderName(task.getCreatorName());
        sendReviewTask.setSenderEmail(task.getCreatorEmail());
        sendReviewTask.setRecipientsRegNr(task.getInstitutionCode());
        sendReviewTask.setRecipientStructuralUnit(task.getOwnerName());
        sendReviewTask.setInstitutionName(task.getInstitutionName());
        sendReviewTask.setWorkflowTitle(task.getCompoundWorkflowTitle());
        sendReviewTask.setTaskId(task.getNodeRef().getId());

        sendReviewTask.setRecipientDocNode(nodeToSend);

        try {
            DecRecipient recipient = DecRecipient.Factory.newInstance();
            if (!applicationConstantsBean.isInternalTesting()) {
                recipient.setOrganisationCode(sendReviewTask.getRecipientsRegNr());
            } else {
                // For internal testing, always send task to current organization itself
                DecContainerDocument.DecContainer.Transport.DecSender senderAddress = getSenderAddress();
                recipient.setOrganisationCode(senderAddress.getOrganisationCode());
            }

            if (isDvkCapable(addressbookService.getDvkCapableOrgs(), recipient.getOrganisationCode())) {
                final Set<String> sendDocuments = dhlXTeeService.sendDocuments(new ArrayList<DhlXTeeService.ContentToSend>(),
                        new DecRecipient[] { recipient }, getSenderAddress(), new DhsSendReviewNotificationCallback(sendReviewTask),
                        getSendDocumentRequestCallback());
                Assert.isTrue(1 == sendDocuments.size(), "Surprise! Size of sendDocuments is " + sendDocuments.size());
                if (!isDeletingNotification) {
                    String dvkId = sendDocuments.iterator().next();
                    props = new HashMap<QName, Serializable>();
                    props.put(WorkflowSpecificModel.Props.SENT_DVK_ID, dvkId);
                    props.put(WorkflowSpecificModel.Props.SEND_STATUS, SendStatus.SENT.toString());
                    workflowDbService.updateTaskProperties(taskRef, props);
                }
                if (isDeletingNotification) {
                    BeanHelper.getLogService().addLogEntry(
                            LogEntry.create(LogObject.TASK, BeanHelper.getUserService(), task.getNodeRef(), "applog_task_review_delete_sent_to_dvk", task.getOwnerName(),
                                    task.getInstitutionName(), MessageUtil.getTypeName(task.getType()))
                            );
                } else {
                    BeanHelper.getLogService().addLogEntry(
                            LogEntry.create(LogObject.TASK, BeanHelper.getUserService(), task.getNodeRef(), "applog_task_review_sent_to_dvk", task.getOwnerName(),
                                    task.getInstitutionName(), MessageUtil.getTypeName(task.getType()))
                            );
                }
                MonitoringUtil.logSuccess(MonitoredService.OUT_XTEE_DVK);
                return true;
            }
            if (isDeletingNotification) {
                throw new ReviewTaskException(ExceptionType.REVIEW_DVK_CAPABILITY_ERROR);
            }
        } catch (RuntimeException e) {
            if (!(e instanceof ReviewTaskException)) {
                MonitoringUtil.logError(MonitoredService.OUT_XTEE_DVK, e);
            }
            throw e;
        }
        return false;
    }

    private Document createDeltaKKRootTypeDeletingNode(Task task) {
        ObjectFactory objectFactory = new ObjectFactory();
        DeltaKKRootType deltaKKRoot = objectFactory.createDeltaKKRootType();

        DeleteLinkedReviewTaskType deletedLinkedReviewTask = objectFactory.createDeleteLinkedReviewTaskType();
        deletedLinkedReviewTask.setOriginalNoderefId(task.getNodeRef().getId());

        deltaKKRoot.setDeleteLinkedReviewTask(deletedLinkedReviewTask);

        return deltaKKRootToDocNode(objectFactory.createDeltaKK(deltaKKRoot));
    }

    private Document createDeltaKKRootTypeNode(Task task) {
        ObjectFactory objectFactory = new ObjectFactory();
        DeltaKKRootType deltaKKRoot = objectFactory.createDeltaKKRootType();
        LinkedReviewTaskType linkedReviewTaskType = objectFactory.createLinkedReviewTaskType();
        linkedReviewTaskType.setCreatorName(task.getCreatorName());
        linkedReviewTaskType.setCreatorId(task.getCreatorId());
        linkedReviewTaskType.setStartedDateTime(getXmlGregorianCalendar(task.getStartedDateTime()));
        linkedReviewTaskType.setOwnerId(task.getOwnerId());
        linkedReviewTaskType.setOwnerName(task.getOwnerName());
        linkedReviewTaskType.setDueDate(getXmlGregorianCalendar(task.getDueDate()));
        linkedReviewTaskType.setStatus(task.getStatus());
        Workflow workflow = task.getParent();
        CompoundWorkflow compoundWorkflow = workflow != null && workflow.getParent() != null ? workflow.getParent() : workflowService.getCompoundWorkflow(generalService
                .getPrimaryParent(task.getWorkflowNodeRef()).getNodeRef());
        linkedReviewTaskType.setCompoundWorkflowTitle(compoundWorkflow.getTitle());
        linkedReviewTaskType.setTaskResolution(task.getResolution());
        linkedReviewTaskType.setCreatorInstitutionCode(task.getCreatorInstitutionCode());
        linkedReviewTaskType.setCreatorInstitutionName(task.getCreatorInstitutionName());
        linkedReviewTaskType.setOriginalNoderefId(task.getNodeRef().getId());
        linkedReviewTaskType.setOriginalTaskObjectUrl(BeanHelper.getDocumentTemplateService().getCompoundWorkflowUrl(compoundWorkflow.getNodeRef()));
        String taskOutcome = task.getOutcome();
        if (StringUtils.isNotBlank(taskOutcome)) {
            linkedReviewTaskType.setOutcome(taskOutcome);
        }
        String taskComment = task.getComment();
        if (StringUtils.isNotBlank(taskComment)) {
            linkedReviewTaskType.setComment(taskComment);
        }
        Date completedDate = task.getCompletedDateTime();
        if (completedDate != null) {
            linkedReviewTaskType.setCompletedDateTime(getXmlGregorianCalendar(completedDate));
        }
        Date stoppedDate = task.getStoppedDateTime();
        if (stoppedDate != null) {
            linkedReviewTaskType.setStoppedDateTime(getXmlGregorianCalendar(stoppedDate));
        }

        deltaKKRoot.setLinkedReviewTask(linkedReviewTaskType);

        return deltaKKRootToDocNode(objectFactory.createDeltaKK(deltaKKRoot));
    }

    private Document deltaKKRootToDocNode(JAXBElement<DeltaKKRootType> deltaKKRoot) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = null;
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.newDocument();
            WorkflowUtil.marshalDeltaKK(deltaKKRoot, doc);

        } catch (ParserConfigurationException e) {
            log.debug("Failed to create new DOM node document", e);
            throw new RuntimeException(e);
        }
        return doc;
    }

    @Override
    /**
     * compoundWorkflowRef - if not null only this compound workflow recipients get updates;
     * if null all compound workflows are checked for recipients
     */
    public void sendDvkTasksWithDocument(NodeRef documentNodeRef, NodeRef compoundWorkflowRef, Map<NodeRef, List<String>> additionalRecipients, String recipientMessage) {

        List<CompoundWorkflow> compoundWorkflows = workflowService.getCompoundWorkflows(documentNodeRef);
        List<String> recipients = getAllRecipients(documentNodeRef, compoundWorkflowRef, compoundWorkflows, additionalRecipients);

        @SuppressWarnings("unchecked")
        List<NodeRef> fileNodeRefs = (List<NodeRef>) CollectionUtils.collect(fileService.getAllFilesExcludingDigidocSubitems(documentNodeRef), new Transformer() {
            @Override
            public Object transform(Object file) {
                return ((File) file).getNode().getNodeRef();
            }
        });

        boolean zipIt = false; // fileNodeRefs.size() > 0;
        Collection<ContentToSend> content = sendOutService.prepareContents(documentNodeRef, fileNodeRefs, zipIt);

        DvkSendWorkflowDocuments sd = new DvkSendWorkflowDocuments();
        // collect compoundWorkflows and workflows to send for each taskCapable and dvkCapable recipient
        Map<String, List<NodeRef>> recipientInclusionMap = new HashMap<String, List<NodeRef>>();
        List<NodeRef> allWorkflowsNodeRefs = getAllWorkflows(compoundWorkflows);
        List<Task> externalReviewTasks = new ArrayList<Task>();
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<String, Set<NodeRef>> sendableWorkflowNodeRefs = new HashMap();

        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            NodeRef compoundWorkflowNodeRef = compoundWorkflow.getNodeRef();
            // all workflows before this workflow plus this workflow
            List<NodeRef> traversedWorkflows = new ArrayList<NodeRef>();
            for (Workflow wf : compoundWorkflow.getWorkflows()) {
                NodeRef wfNodeRef = wf.getNodeRef();
                traversedWorkflows.add(wfNodeRef);
                if (wf.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
                    for (Task task : wf.getTasks()) {
                        String institutionCode = task.getInstitutionCode();
                        if (recipients.contains(institutionCode)) {
                            // tasks with current institution code are sent to this institution
                            // and are not meant to be sent out
                            if (applicationConstantsBean.isInternalTesting() || !getInstitutionCode().equalsIgnoreCase(institutionCode)) {
                                if (!recipientInclusionMap.containsKey(institutionCode)) {
                                    recipientInclusionMap.put(institutionCode, new ArrayList<NodeRef>());
                                }
                                recipientInclusionMap.get(institutionCode).add(compoundWorkflowNodeRef);
                                if (sendableWorkflowNodeRefs.get(institutionCode) == null) {
                                    sendableWorkflowNodeRefs.put(institutionCode, new HashSet<NodeRef>());
                                }
                                sendableWorkflowNodeRefs.get(institutionCode).addAll(traversedWorkflows);
                                externalReviewTasks.add(task);
                                break;
                            }
                        }
                    }
                }
            }
        }
        @SuppressWarnings("unchecked")
        List<NodeRef> compoundWorkflowNodeRefs = (List<NodeRef>) CollectionUtils.collect(compoundWorkflows, new Transformer() {

            @Override
            public Object transform(Object compWorkflow) {
                return ((CompoundWorkflow) compWorkflow).getNodeRef();
            }

        });

        // every recipient needs different workflow xml
        for (String recipientsRegNr : recipientInclusionMap.keySet()) {
            // get excluded compound workflows and workflows for each reciepent
            List<NodeRef> excludedNodeRefs = new ArrayList<NodeRef>();
            excludedNodeRefs.addAll(compoundWorkflowNodeRefs);
            excludedNodeRefs.removeAll(recipientInclusionMap.get(recipientsRegNr));
            excludedNodeRefs.addAll(allWorkflowsNodeRefs);
            excludedNodeRefs.removeAll(sendableWorkflowNodeRefs.get(recipientsRegNr));
            ExcludingExporterCrawlerParameters exportParameters = getDocumentExportParameters(documentNodeRef, excludedNodeRefs);
            org.w3c.dom.Node docDomNode = exportDom(exportParameters);
            sd.setRecipientsRegNr(recipientsRegNr);
            sd.setRecipientDocNode(docDomNode);
            sd.setTextContent(recipientMessage);
            String dvkId = sendExternalReviewWorkflowData(content, sd);
            for (Task externalReviewTask : externalReviewTasks) {
                if (!excludedNodeRefs.contains(externalReviewTask.getParent().getNode().getNodeRef())
                        && recipientsRegNr.equals(externalReviewTask.getInstitutionCode())) {
                    NodeRef taskNodeRef = externalReviewTask.getNode().getNodeRef();
                    nodeService.setProperty(taskNodeRef, WorkflowSpecificModel.Props.SENT_DVK_ID, dvkId);
                    // NB! it is assumed that with each dvk call
                    // only one new (i.e. with empty originalDvkId) in progress external review task is sent!!
                    if (externalReviewTask.isStatus(Status.IN_PROGRESS)
                            && StringUtils.isBlank((String) externalReviewTask.getProp(WorkflowSpecificModel.Props.ORIGINAL_DVK_ID))) {
                        nodeService.setProperty(taskNodeRef, WorkflowSpecificModel.Props.ORIGINAL_DVK_ID, dvkId);
                    }
                    nodeService.setProperty(taskNodeRef, WorkflowSpecificModel.Props.SEND_STATUS, SendStatus.SENT);
                    nodeService.setProperty(taskNodeRef, WorkflowSpecificModel.Props.SEND_DATE_TIME, new Date());
                }
            }
        }

    }

    private List<NodeRef> getAllWorkflows(List<CompoundWorkflow> compoundWorkflows) {
        List<NodeRef> workflows = new ArrayList<NodeRef>();
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                workflows.add(workflow.getNodeRef());
            }
        }
        return workflows;
    }

    private List<String> getAllRecipients(NodeRef documentNodeRef, NodeRef compoundWorkflowRef
            , List<CompoundWorkflow> compoundWorkflows, Map<NodeRef, List<String>> additionalRecipients) {
        // TODO: check here also if taskCapable property is still active?
        List<Node> dvkCapableOrgs = addressbookService.getDvkCapableOrgs();
        List<String> recipients = new ArrayList<String>();
        String dvkCapabilityErrorMessage = null;
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            if (compoundWorkflowRef == null || compoundWorkflowRef.equals(compoundWorkflow.getNodeRef())) {
                for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                    if (workflow.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
                        for (Task task : workflow.getTasks()) {
                            if (task.isStatus(Status.IN_PROGRESS, Status.STOPPED) && task.getInstitutionCode() != null) {
                                if (!recipients.contains(task.getInstitutionCode()) && !isDvkCapable(dvkCapableOrgs, task.getInstitutionCode())) {
                                    throw new ReviewTaskException(ExceptionType.EXTERNAL_REVIEW_DVK_CAPABILITY_ERROR, dvkCapabilityErrorMessage);
                                }
                                recipients.add(task.getInstitutionCode());
                            }
                        }
                    }
                }
            }
        }
        if (additionalRecipients != null && additionalRecipients.get(documentNodeRef) != null) {
            for (String orgCode : additionalRecipients.get(documentNodeRef)) {
                if (!recipients.contains(orgCode) && !isDvkCapable(dvkCapableOrgs, orgCode)) {
                    throw new ReviewTaskException(ExceptionType.EXTERNAL_REVIEW_DVK_CAPABILITY_ERROR, dvkCapabilityErrorMessage);
                }
            }
            recipients.addAll(additionalRecipients.get(documentNodeRef));
        }
        return recipients;
    }

    public org.w3c.dom.Node exportDom(ExcludingExporterCrawlerParameters exportParameters) {
        org.w3c.dom.Node docDomNode = null;
        try {
            docDomNode = exporterService.exportViewNode(exportParameters);
        } catch (ExporterException e) {
            // TODO: error processing
            log.debug("Error exporting document to dvk xml");
            throw new RuntimeException(e);
        }
        return docDomNode;
    }

    @Override
    public boolean isDvkCapable(List<Node> dvkCapableOrgs, String recipientOrgCode) {
        for (Node organization : dvkCapableOrgs) {
            String orgCode = (String) organization.getProperties().get(AddressbookModel.Props.ORGANIZATION_CODE.toString());
            if (recipientOrgCode.equalsIgnoreCase(orgCode)) {
                return true;
            }
        }
        return false;
    }

    // TODO: maybe move to ExporterComponent?
    private ExcludingExporterCrawlerParameters getDocumentExportParameters(NodeRef documentNodeRef, List<NodeRef> excludedNodeRef) {
        ExcludingExporterCrawlerParameters parameters = new ExcludingExporterCrawlerParameters();
        Location location = new Location(documentNodeRef);
        parameters.setExportFrom(location);
        parameters.setCrawlSelf(true);
        parameters.setCrawlContent(false);
        parameters.getExcludedProperties().addAll(
                Arrays.asList(new QName[] { DocumentCommonModel.Props.FUNCTION,
                        DocumentCommonModel.Props.SERIES,
                        DocumentCommonModel.Props.VOLUME,
                        DocumentCommonModel.Props.CASE,
                        DocumentCommonModel.Types.SEND_INFO,
                        WorkflowSpecificModel.Props.SENT_DVK_ID,
                        WorkflowSpecificModel.Props.RECIEVED_DVK_ID,
                        WorkflowSpecificModel.Props.SEND_DATE_TIME,
                        WorkflowSpecificModel.Props.SEND_STATUS }));
        parameters.getExcludedAssocTypes().addAll(
                Arrays.asList(new QName[] { /* DocumentCommonModel.Assocs.DOCUMENT_LOG */
                        // TODO send log messages separately, if needed
                        DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP,
                        DocumentCommonModel.Assocs.DOCUMENT_REPLY,
                        DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT }));
        parameters.setExcludeNodeRefs(excludedNodeRef);
        parameters.setReferenceType(ReferenceType.NODEREF);
        parameters.setCrawlNullProperties(true);
        return parameters;
    }

    private ExcludingExporterCrawlerParameters getTaskExportParameters(NodeRef taskNodeRef) {
        ExcludingExporterCrawlerParameters parameters = new ExcludingExporterCrawlerParameters();
        Location location = new Location(taskNodeRef);
        parameters.setExportFrom(location);
        parameters.setCrawlSelf(true);
        parameters.setCrawlContent(false);
        parameters.getExcludedProperties().addAll(
                Arrays.asList(new QName[] { WorkflowSpecificModel.Props.SEND_DATE_TIME,
                        WorkflowSpecificModel.Props.SEND_STATUS }));
        parameters.setReferenceType(ReferenceType.NODEREF);
        parameters.setCrawlNullProperties(true);
        return parameters;
    }

    // START: getters / setters
    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setImporterService(ExternalReviewWorkflowImporterService importerService) {
        this.importerService = importerService;
    }

    public void setExporterService(ExporterService exporterService) {
        this.exporterService = exporterService;
    }

    public void setSendOutService(SendOutService sendOutService) {
        this.sendOutService = sendOutService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void setEinvoiceService(EInvoiceService einvoiceService) {
        this.einvoiceService = einvoiceService;
    }

    public void setApplicationConstantsBean(ApplicationConstantsBean applicationConstantsBean) {
        this.applicationConstantsBean = applicationConstantsBean;
    }

    // END: getters / setters

}
