package ee.webmedia.alfresco.dvk.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.classificator.enums.TransmittalMode;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.dvk.model.DvkModel;
import ee.webmedia.alfresco.dvk.model.DvkReceivedDocument;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.xtee.client.service.DhlXTeeService.MetainfoHelper;
import ee.webmedia.xtee.client.service.DhlXTeeService.SendStatus;
import ee.webmedia.xtee.client.service.DhlXTeeService.ReceivedDocumentsWrapper.ReceivedDocument;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.DhlDokumentType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.GetSendStatusResponseTypeUnencoded.Item;

/**
 * @author Ats Uiboupin
 */
public class DvkServiceSimImpl extends DvkServiceImpl {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DvkServiceSimImpl.class);
    private DocumentService documentService;
    private DocumentSearchService documentSearchService;

    @Override
    public int updateDocSendStatuses() {
        // there are as many sendInfoRefs to the same dvkId as there were recipients whom doc were delivered using DVK
        final Map<NodeRef /* sendInfo */, String /* dvkId */> refsAndIds = documentSearchService.searchOutboxDvkIds();
        if (refsAndIds.size() == 0) {
            return 0; // no need to ask statuses
        }
        // get unique dvkIds
        final Set<String> dvkIds = new HashSet<String>(refsAndIds.size());
        for (Entry<NodeRef, String> entry : refsAndIds.entrySet()) {
            dvkIds.add(entry.getValue());
        }
        // get sendStatus for each dvkId
        final List<Item> sendStatuses = dhlXTeeService.getSendStatuses(dvkIds);
        // fill map containing statuses by ids
        final HashMap<String /* dhlId */, SendStatus> statusesByIds = new HashMap<String, SendStatus>(sendStatuses.size());
        for (Item item : sendStatuses) {
            statusesByIds.put(item.getDhlId(), SendStatus.get(item.getOlek()));
        }
        final HashSet<String> dhlIdsStatusChanged = new HashSet<String>(dvkIds.size());
        // update each sendInfoRef if status has changed(from SENT to RECEIVED or CANCELLED)
        for (Entry<NodeRef, String> refAndDvkId : refsAndIds.entrySet()) {
            final NodeRef sendInfoRef = refAndDvkId.getKey();
            final String dvkId = refAndDvkId.getValue();
            final SendStatus status = statusesByIds.get(dvkId);
            if (!status.equals(SendStatus.SENT)) {
                dhlIdsStatusChanged.add(dvkId);
                nodeService.setProperty(sendInfoRef, DocumentCommonModel.Props.SEND_INFO_SEND_STATUS, status.toString());
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("dvk status changed for dvkIds: " + dhlIdsStatusChanged);
        }
        return dhlIdsStatusChanged.size();
    }

    @Override
    protected NodeRef createDocumentNode(DvkReceivedDocument rd, NodeRef dvkIncomingFolder, String nvlDocumentTitle) {
        final Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        fillPropsFromDvkReceivedDocument(rd, props);
        props.put(DocumentCommonModel.Props.DOC_NAME, nvlDocumentTitle);
        props.put(DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.WORKING.getValueName());
        props.put(DocumentCommonModel.Props.STORAGE_TYPE, StorageType.DIGITAL.getValueName());
        props.put(DocumentSpecificModel.Props.TRANSMITTAL_MODE, TransmittalMode.DVK);

        final Node document = documentService.createDocument(DocumentSubtypeModel.Types.INCOMING_LETTER, dvkIncomingFolder, props);

        return document.getNodeRef();
    }

    private void fillPropsFromDvkReceivedDocument(DvkReceivedDocument rd, Map<QName, Serializable> props) {
        // common
        props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION, rd.getLetterAccessRestriction());
        props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, rd.getLetterAccessRestrictionBeginDate());
        props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE, rd.getLetterAccessRestrictionEndDate());
        props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON, rd.getLetterAccessRestrictionReason());
        // specific
        props.put(DocumentSpecificModel.Props.SENDER_DETAILS_NAME, rd.getSenderOrgName());
        props.put(DocumentSpecificModel.Props.SENDER_REG_NUMBER, rd.getSenderRegNr());
        props.put(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL, rd.getSenderEmail());
        props.put(DocumentSpecificModel.Props.SENDER_REG_DATE, rd.getLetterSenderDocSignDate());
        props.put(DocumentSpecificModel.Props.SENDER_REG_NUMBER, rd.getLetterSenderDocNr());
        props.put(DocumentSpecificModel.Props.DUE_DATE, rd.getLetterDeadLine());
        // 
        props.put(DvkModel.Props.DVK_ID, rd.getDvkId());
    }

    @Override
    protected void handleStorageFailure(ReceivedDocument receivedDocument, String dhlId, NodeRef dvkIncomingFolder
            , MetainfoHelper metaInfoHelper, Collection<String> previouslyFailedDvkIds, Exception e) {
        super.handleStorageFailure(receivedDocument, dhlId, dvkIncomingFolder, metaInfoHelper, previouslyFailedDvkIds, e);
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
            try {
                final OutputStream os = writer.getContentOutputStream();
                os.write(dhlDocument.toString().getBytes());
                os.close();
            } catch (IOException e3) {
                throw new RuntimeException("Failed write output to repository: '" + corruptDvkDocumentsPath + "' nodeRef=" + corruptDocNodeRef + " contentUrl="
                        + writer.getContentUrl(), e);
            }
        } catch (Exception e3) {
            final String msg = "Failed to store DVK document and failed to handle storage failure of document with dhlId=" + dhlId + ".";
            log.fatal(msg, e3);
            throw new RuntimeException(msg, e3);
        }
    }

    @Override
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

    // START: getters / setters
    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }
    // END: getters / setters

}
