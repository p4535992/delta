package ee.webmedia.alfresco.dvk.service;

import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.REG_NUMBER;
import static ee.webmedia.alfresco.utils.XmlUtil.findChildByQName;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.content.MimetypeMap;
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
import org.apache.xml.security.utils.Base64;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.springframework.util.Assert;
import org.w3c.dom.NodeList;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.classificator.enums.TransmittalMode;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
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
import ee.webmedia.alfresco.dvk.model.DvkReceivedLetterDocument;
import ee.webmedia.alfresco.dvk.model.DvkSendWorkflowDocuments;
import ee.webmedia.alfresco.dvk.model.DvkSendWorkflowDocumentsImpl;
import ee.webmedia.alfresco.dvk.service.ExternalReviewException.ExceptionType;
import ee.webmedia.alfresco.notification.model.NotificationModel;
import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.ExternalReviewWorkflowImporterService;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.MetainfoHelper;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ReceivedDocumentsWrapper.ReceivedDocument;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.DhlDokumentType;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.EdastusDocument.Edastus;
import ee.webmedia.xtee.client.dhl.types.ee.riik.schemas.dhl.MetaxmlDocument.Metaxml;
import ee.webmedia.xtee.client.dhl.types.ee.riik.xtee.dhl.producers.producer.dhl.GetSendStatusResponseTypeUnencoded.Item;
import ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType;

/**
 * @author Ats Uiboupin
 */
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

    @Override
    public int updateDocAndTaskSendStatuses() {
        // there are as many sendInfoRefs to the same dvkId as there were recipients whom doc were delivered using DVK
        final Map<NodeRef /* sendInfo */, Pair<String /* dvkId */, String /* recipientRegNr */>> docRefsAndIds = documentSearchService.searchOutboxDvkIds();
        final Map<NodeRef /* sendInfo */, Pair<String /* dvkId */, String /* recipientRegNr */>> taskRefsAndIds //
        = documentSearchService.searchTaskBySendStatusQuery(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK);
        if (docRefsAndIds.size() == 0 && taskRefsAndIds.size() == 0) {
            return 0; // no need to ask statuses
        }
        // get unique dvkIds
        final Set<String> dvkIds = new HashSet<String>(docRefsAndIds.size() + taskRefsAndIds.size());
        for (Entry<NodeRef, Pair<String, String>> entry : docRefsAndIds.entrySet()) {
            dvkIds.add(entry.getValue().getFirst());
        }
        for (Entry<NodeRef, Pair<String, String>> entry : taskRefsAndIds.entrySet()) {
            dvkIds.add(entry.getValue().getFirst());
        }

        List<Item> sendStatuses = null;
        // get sendStatus for each dvkId
        sendStatuses = dhlXTeeService.getSendStatuses(dvkIds);
        // fill map containing statuses by ids
        final HashMap<String /* dhlId */, Map<String, SendStatus>> statusesByIds = new HashMap<String, Map<String, SendStatus>>();
        for (Item item : sendStatuses) {
            Map<String, SendStatus> statusesForDvkId = new HashMap<String, SendStatus>();
            statusesByIds.put(item.getDhlId(), statusesForDvkId);
            List<Edastus> forwardings = item.getEdastusList();
            for (Edastus forwarding : forwardings) {
                statusesForDvkId.put(forwarding.getSaaja().getRegnr(), SendStatus.get(forwarding.getStaatus()));
            }
        }

        return updateNodeSendStatus(docRefsAndIds, statusesByIds, DocumentCommonModel.Props.SEND_INFO_SEND_STATUS)
                + updateNodeSendStatus(taskRefsAndIds, statusesByIds, WorkflowSpecificModel.Props.SEND_STATUS);
    }

    public int updateNodeSendStatus(final Map<NodeRef, Pair<String, String>> refsAndIds, final HashMap<String, Map<String, SendStatus>> statusesByIds, QName propToSet) {
        final HashSet<String> dhlIdsStatusChanged = new HashSet<String>();
        // update each sendInfoRef if status has changed(from SENT to RECEIVED or CANCELLED)
        for (Entry<NodeRef, Pair<String, String>> refAndDvkId : refsAndIds.entrySet()) {
            final NodeRef sendInfoRef = refAndDvkId.getKey();
            final String dvkId = refAndDvkId.getValue().getFirst();
            final String recipientRegNr = refAndDvkId.getValue().getSecond();
            Map<String, SendStatus> recipientStatuses = statusesByIds.get(dvkId);
            SendStatus status = null;
            if (recipientStatuses != null) {
                status = recipientStatuses.get(recipientRegNr);
            }
            if (status != null && !status.equals(SendStatus.SENT)) {
                dhlIdsStatusChanged.add(dvkId);
                nodeService.setProperty(sendInfoRef, propToSet, status.toString());
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("dvk status changed for dvkIds: " + dhlIdsStatusChanged);
        }
        return dhlIdsStatusChanged.size();
    }

    @Override
    protected NodeRef createDocumentNode(DvkReceivedLetterDocument rd, NodeRef dvkIncomingFolder, String nvlDocumentTitle) {
        final NodeRef docRef = documentDynamicService.createNewDocument(
                SystematicDocumentType.INCOMING_LETTER.getId(),
                dvkIncomingFolder).getFirst().getNodeRef();

        final Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        fillPropsFromDvkReceivedDocument(rd, props);
        props.put(DocumentCommonModel.Props.DOC_NAME, nvlDocumentTitle);
        props.put(DocumentCommonModel.Props.STORAGE_TYPE, StorageType.DIGITAL.getValueName());
        props.put(DocumentSpecificModel.Props.TRANSMITTAL_MODE, TransmittalMode.DVK.getValueName());
        nodeService.addProperties(docRef, props);

        documentLogService.addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_imported"
                , I18NUtil.getMessage("document_log_creator_dvk")), I18NUtil.getMessage("document_log_creator_dvk"));
        return docRef;
    }

    private void fillPropsFromDvkReceivedDocument(DvkReceivedLetterDocument rd, Map<QName, Serializable> props) {
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
    }

    private org.w3c.dom.Node getDhsNodeForParsing(DhlDokumentType dhlDokument, javax.xml.namespace.QName dhsNodeName) {
        final Metaxml metaxml = dhlDokument.getMetaxml();
        org.w3c.dom.Node node = metaxml.newDomNode();

        org.w3c.dom.Node deltaNode = findChildByQName(DELTA_QNAME, node.getFirstChild());
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
    protected NodeRef importWorkflowData(DvkReceivedLetterDocument rd, DhlDokumentType dhlDokument, NodeRef dvkDefaultIncomingFolder,
            List<DataFileType> dataFileList, String dvkId) {
        org.w3c.dom.Node docNode = getDhsNodeForParsing(dhlDokument, EXTERNAL_REVIEW_DOCUMENT_QNAME);
        if (docNode != null) {
            // Location will be used for creating new document if document doesn't exist
            Pair<Location, Boolean> locationWithCheck = getDvkWorkflowDocLocation(rd.getSenderOrgName(), dvkDefaultIncomingFolder);
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
                throw new ExternalReviewException(ExceptionType.PARSING_EXCEPTION, "Failed to parse DVK input document, dhlDocument=" + dhlDokument + ",\n docNode=" + docNode);
            }
        }
        return null;
    }

    @Override
    protected List<NodeRef> importInvoiceData(DvkReceivedLetterDocument rd, DhlDokumentType dhlDokument, String dhlId, List<DataFileType> dataFileList) {
        List<NodeRef> newInvoices = new ArrayList<NodeRef>();
        if (!einvoiceService.isEinvoiceEnabled()) {
            return newInvoices;
        }
        try {
            NodeRef receivedInvoiceFolder = generalService.getNodeRef(documentService.getReceivedInvoicePath());
            // read invoice(s) from attachments
            Map<NodeRef, Integer> invoiceRefToDatafile = new HashMap<NodeRef, Integer>();
            Map<NodeRef, Integer> transactionRefToDataFile = new HashMap<NodeRef, Integer>();
            Integer dataFileIndex = 0;
            for (DataFileType dataFile : dataFileList) {
                if (isXmlMimetype(dataFile)) {
                    InputStream input = new ByteArrayInputStream(Base64.decode(dataFile.getStringValue()));
                    List<NodeRef> newDocRefs = einvoiceService.importInvoiceFromXml(receivedInvoiceFolder, input, TransmittalMode.DVK);
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
                for (DataFileType dataFile : dataFileList) {
                    if ((!transactionRefToDataFile.containsValue(dataFileIndex) || transactionRefToDataFile.get(invoiceRef).equals(dataFileIndex))
                            && (!invoiceRefToDatafile.containsValue(dataFileIndex) || invoiceRefToDatafile.get(invoiceRef).equals(dataFileIndex))) {
                        storeFile(rd, invoiceRef, dataFile);
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
    protected NodeRef importSapInvoiceRegistration(DvkReceivedLetterDocument rd, DhlDokumentType dhlDokument, String dhlId, List<DataFileType> dataFileList) {
        if (!einvoiceService.isEinvoiceEnabled()) {
            return null;
        }
        try {
            for (DataFileType dataFile : dataFileList) {
                if (isXmlMimetype(dataFile)) {
                    InputStream input = new ByteArrayInputStream(Base64.decode(dataFile.getStringValue()));
                    Pair<String, String> docUrlAndErpDocNumber = einvoiceService.getDocUrlAndErpDocNumber(input);
                    if (docUrlAndErpDocNumber != null) {
                        NodeRef updatedDoc = einvoiceService.updateDocumentEntrySapNumber(docUrlAndErpDocNumber.getFirst(), docUrlAndErpDocNumber.getSecond());
                        if (updatedDoc == null) {
                            // throw exception if document was parsed sucessfully, but related document could not be found
                            String message = "Failed to find document with url " + docUrlAndErpDocNumber.getFirst() + ", saving dvk document to failed dvk folder.";
                            log.error(message);
                            throw new RuntimeException(message);
                        }
                        documentLogService.addDocumentLog(updatedDoc, I18NUtil.getMessage("document_log_status_add_sap_entry_number"));
                        return updatedDoc;
                    }
                }
            }
        } catch (Base64DecodingException e) {
            throw new RuntimeException("Failed to decode", e);
        }
        return null;
    }

    @Override
    protected List<NodeRef> importDimensionData(DvkReceivedLetterDocument rd, DhlDokumentType dhlDokument, String dhlId, List<DataFileType> dataFileList) {
        List<NodeRef> dimensionNodes = new ArrayList<NodeRef>();
        if (!einvoiceService.isEinvoiceEnabled()) {
            return dimensionNodes;
        }
        for (DataFileType dataFile : dataFileList) {
            if (isXmlMimetype(dataFile)) {
                List<NodeRef> fileImportNodes = new ArrayList<NodeRef>();
                try {
                    byte[] decode = Base64.decode(dataFile.getStringValue());
                    fileImportNodes.addAll(einvoiceService.importVatCodeList(new ByteArrayInputStream(decode)));
                    if (fileImportNodes.size() == 0) {
                        dimensionNodes.addAll(einvoiceService.importAccountList(new ByteArrayInputStream(decode)));
                    }
                    if (fileImportNodes.size() == 0) {
                        fileImportNodes.addAll(einvoiceService.importSellerList(new ByteArrayInputStream(decode)));
                    }
                    if (fileImportNodes.size() == 0) {
                        fileImportNodes.addAll(einvoiceService.importDimensionsList(new ByteArrayInputStream(decode)));
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
    public boolean isXmlMimetype(DataFileType dataFile) {
        String mimeType = mimetypeService.guessMimetype(dataFile.getFilename());
        boolean isXmlMimetype = MimetypeMap.MIMETYPE_XML.equalsIgnoreCase(mimeType);
        return isXmlMimetype;
    }

    private void sendNotifications(Map<QName, Task> notifications, List<String> statusErrorNotificationContent) {
        for (Entry<QName, Task> entry : notifications.entrySet()) {
            QName notificationType = entry.getKey();
            Task task = entry.getValue();
            if (NotificationModel.NotificationType.TASK_NEW_TASK_NOTIFICATION.equals(notificationType)) {
                notificationService.notifyTaskEvent(task);
            } else if (NotificationModel.NotificationType.TASK_CANCELLED_TASK_NOTIFICATION.equals(notificationType)) {
                notificationService.notifyTaskEvent(task);
            } else if (NotificationModel.NotificationType.EXTERNAL_REVIEW_WORKFLOW_RECIEVING_ERROR.equals(notificationType)) {
                notifyExternalReviewReceivingFailure(task);
            } else if (NotificationModel.NotificationType.EXTERNAL_REVIEW_WORKFLOW_SERIES_ERROR.equals(notificationType)) {
                notifyExternalReviewSeriesFailure(task);
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
                throw new ExternalReviewException(ExceptionType.VERSION_CONFLICT, task, taskRecievedDvkIdStr);
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
    protected NodeRef importTaskData(DvkReceivedLetterDocument rd, DhlDokumentType dhlDokument, String dvkId) throws ExternalReviewException {
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
                        throw new ExternalReviewException(ExceptionType.TASK_OVERWRITE_WRONG_STATUS, originalTask, dvkId, attemptedStatus);
                    }
                }
            }
            if (originalTask == null) {
                throw new ExternalReviewException(ExceptionType.ORIGINAL_TASK_NOT_FOUND, null
                        , originalDvkIdsAndStatuses.size() > 0 ? originalDvkIdsAndStatuses.keySet().iterator().next() : null);
            }
        }
        return null;
    }

    // return first matching document containing some of given tasks and having aspect notEditable property notEditable=true
    private NodeRef getExistingDocumentNodeRef(List<Task> dvkTasks) {
        // TODO: maybe check if all returned tasks belong to one document?
        for (Task task : dvkTasks) {
            NodeRef docRef = getTaskDocument(task);
            if (Boolean.TRUE.equals(nodeService.getProperty(docRef, DocumentSpecificModel.Props.NOT_EDITABLE))) {
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
            if (task == null || !nodeService.hasAspect(docRef, DocumentSpecificModel.Aspects.NOT_EDITABLE)) {
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
    protected void handleStorageFailure(ReceivedDocument receivedDocument, String dhlId, NodeRef dvkIncomingFolder
            , MetainfoHelper metaInfoHelper, Collection<String> previouslyFailedDvkIds, Exception e) {
        super.handleStorageFailure(receivedDocument, dhlId, dvkIncomingFolder, metaInfoHelper, previouslyFailedDvkIds, e);
        if (e instanceof ExternalReviewException) {
            handleExternalReviewException((ExternalReviewException) e);
        }
        if (previouslyFailedDvkIds.contains(dhlId)) {
            log.debug("tried to receive document with dvkId='" + dhlId + "' that we had already failed to receive before");
            return;
        }
        previouslyFailedDvkIds.add(dhlId);
        final DhlDokumentType dhlDocument = receivedDocument.getDhlDocument();
        try {
            String corruptDocName = dhlId + " " + metaInfoHelper.getDhlSaatjaAsutuseNr() + " " + metaInfoHelper.getDhlSaatjaAsutuseNimi();
            corruptDocName = FilenameUtil.buildFileName(corruptDocName, "xml");
            log.debug("trygin to store DVK document to '" + corruptDvkDocumentsPath + "+/" + corruptDocName + "'");
            NodeRef corruptFolder = generalService.getNodeRef(corruptDvkDocumentsPath);
            final NodeRef corruptDocNodeRef = fileFolderService.create(corruptFolder, corruptDocName, DvkModel.Types.FAILED_DOC).getNodeRef();
            nodeService.setProperty(corruptDocNodeRef, DvkModel.Props.DVK_ID, dhlId);
            final ContentWriter writer = fileFolderService.getWriter(corruptDocNodeRef);
            writer.setMimetype(MimetypeMap.MIMETYPE_XML);
            final OutputStream os = writer.getContentOutputStream();
            try {
                os.write(dhlDocument.toString().getBytes());
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

    private void handleExternalReviewException(ExternalReviewException e) {
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

    private void notifyExternalReviewSeriesFailure(Task task) {
        notificationService.notifyExternalReviewError(task);
    }

    @Override
    protected Pair<Location, Boolean> getDvkWorkflowDocLocation(String senderName, NodeRef dvkDefaultIncomingFolder) {
        String dvkWorkflowSeriesMark = parametersService.getStringParameter(Parameters.EXTERNAL_REVIEW_AUTOMATIC_FOLDER);
        if (dvkWorkflowSeriesMark == null) {
            return new Pair<Location, Boolean>(new Location(dvkDefaultIncomingFolder), Boolean.FALSE);
        }
        Series series = documentSearchService.searchSeriesByIdentifier(dvkWorkflowSeriesMark);
        Volume dvkDocVolume = null;
        if (series == null) {
            return new Pair<Location, Boolean>(new Location(dvkDefaultIncomingFolder), Boolean.FALSE);
        } else {
            List<Volume> volumes = volumeService.getAllValidVolumesBySeries(series.getNode().getNodeRef());
            for (Volume volume : volumes) {
                if (StringUtils.equalsIgnoreCase(volume.getTitle(), senderName)) {
                    dvkDocVolume = volume;
                    break;
                }
            }
            if (dvkDocVolume == null) {
                dvkDocVolume = volumeService.createVolume(series.getNode().getNodeRef());
                // TODO: standard configuration for creating new dvkWorkflowDocumentFolder
                dvkDocVolume.setValidFrom(new Date());
                dvkDocVolume.setContainsCases(false);
                dvkDocVolume.setTitle(senderName);
                dvkDocVolume.setStatus(DocListUnitStatus.OPEN.getValueName());
                dvkDocVolume.setVolumeMark(dvkWorkflowSeriesMark + "-" + senderName);
                dvkDocVolume.setVolumeTypeEnum(VolumeType.ANNUAL_FILE);
                volumeService.saveOrUpdate(dvkDocVolume, false);
            }

        }
        return new Pair<Location, Boolean>(new Location(dvkDocVolume.getNode().getNodeRef()), Boolean.TRUE);
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
            DvkSendWorkflowDocuments sd = new DvkSendWorkflowDocumentsImpl();
            sd.setIsDocumentNode(false);
            sd.setRecipientsRegNr(task.getCreatorInstitutionCode());
            sd.setRecipientDocNode(taskDomNode);
            String dvkId = sendExternalReviewWorkflowData(new ArrayList<ContentToSend>(), sd);
            nodeService.setProperty(task.getNodeRef(), WorkflowSpecificModel.Props.SENT_DVK_ID, dvkId);
            nodeService.setProperty(task.getNodeRef(), WorkflowSpecificModel.Props.SEND_STATUS, SendStatus.SENT);
        }
    }

    @Override
    public String sendInvoiceFileToSap(Node document, File file) {
        Collection<ContentToSend> contentsToSend = einvoiceService.createContentToSend(file);
        String folder = einvoiceService.getTransactionDvkFolder(document);
        final Collection<String> recipientsRegNrs = new ArrayList<String>();
        recipientsRegNrs.add(parametersService.getStringParameter(Parameters.SAP_DVK_CODE));
        verifyEnoughData(contentsToSend, recipientsRegNrs, true);
        final Set<String> sendDocuments = dhlXTeeService.sendDocuments(contentsToSend, getRecipients(recipientsRegNrs), getSenderAddress(),
                new DhsSendInvoiceToSapCallback(), getSendDocumentToFolderRequestCallback(folder));
        Assert.isTrue(1 == sendDocuments.size(), "Supprise! Size of sendDocuments is " + sendDocuments.size());
        String dvkId = sendDocuments.iterator().next();
        sendOutService.addSapSendInfo(document, dvkId);
        return dvkId;
    }

    @Override
    public String generateAndSendInvoiceFileToSap(Node node, List<Transaction> transactions) throws IOException {
        return sendInvoiceFileToSap(node, einvoiceService.generateTransactionXmlFile(node, transactions));
    }

    @Override
    /**
     * compoundWorkflowRef - if not null only this compound workflow recipients get updates; 
     * if null all compound workflows are checked for recipients
     */
    public void sendDvkTasksWithDocument(NodeRef documentNodeRef, NodeRef compoundWorkflowRef, Map<NodeRef, List<String>> additionalRecipients) {

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

        DvkSendWorkflowDocuments sd = new DvkSendWorkflowDocumentsImpl();
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
                            if (workflowService.isInternalTesting() || !getInstitutionCode().equalsIgnoreCase(institutionCode)) {
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
                                    throw new ExternalReviewException(ExceptionType.DVK_CAPABILITY_ERROR, dvkCapabilityErrorMessage);
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
                    throw new ExternalReviewException(ExceptionType.DVK_CAPABILITY_ERROR, dvkCapabilityErrorMessage);
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

    public boolean isDvkCapable(List<Node> dvkCapableOrgs, String recipientOrgCode) {
        boolean isDvkCapable = false;
        for (Node organization : dvkCapableOrgs) {
            String orgCode = (String) organization.getProperties().get(AddressbookModel.Props.ORGANIZATION_CODE.toString());
            if (recipientOrgCode.equalsIgnoreCase(orgCode)) {
                isDvkCapable = true;
            }
        }
        return isDvkCapable;
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

    // END: getters / setters

}
