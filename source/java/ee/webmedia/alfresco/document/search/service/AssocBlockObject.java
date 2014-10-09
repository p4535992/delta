package ee.webmedia.alfresco.document.search.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.model.SimpleFile;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentListRowLink;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.UnmodifiableVolume;
import ee.webmedia.alfresco.volume.model.Volume;

/**
 * Object representing document, case, volume or casefile.
 * In order to use document list jsps, document properties are represented as AssocBlockObject properties
 * (to use object.property syntax, not object.document.property syntax in jsp)
 */
public class AssocBlockObject implements Serializable, DocumentListRowLink {

    private static final long serialVersionUID = 1L;

    private Document document;
    private Case aCase;
    private Volume volume;

    public AssocBlockObject(Document document) {
        Assert.notNull(document);
        this.document = document;
    }

    public AssocBlockObject(Case aCase) {
        Assert.notNull(aCase);
        this.aCase = aCase;
    }

    public AssocBlockObject(Volume volume) {
        Assert.notNull(volume);
        this.volume = volume;
    }

    public boolean isDocument() {
        return document != null;
    }

    public boolean isCase() {
        return aCase != null;
    }

    public boolean isVolume() {
        return volume != null;
    }

    public Node getNode() {
        return isDocument() ? document.getNode() : (isCase() ? aCase.getNode() : volume.getNode());
    }

    public String getAkString() {
        return isDocument() ? document.getAkString() : "";
    }

    public String getRegNumber() {
        return isDocument() ? document.getRegNumber() : "";
    }

    public Date getRegDateTime() {
        return isDocument() ? document.getRegDateTime() : null;
    }

    public String getRegDateTimeStr() {
        return isDocument() ? document.getRegDateTimeStr() : "";
    }

    public String getDocName() {
        return isDocument() ? document.getDocName() : (isCase() ? aCase.getTitle() : volume.getVolumeMarkAndTitle());
    }

    public String getDocumentTypeName() {
        return isDocument() ? document.getDocumentTypeName() : (isCase() ? MessageUtil.getMessage("case") : volume.getVolumeTypeName());
    }

    public String getSender() {
        return isDocument() ? document.getSender() : "";
    }

    public Date getDueDate() {
        return isDocument() ? document.getDueDate() : (isVolume() ? volume.getWorkflowDueDate() : null);
    }

    public String getDueDateStr() {
        return isDocument() ? document.getDueDateStr() : (isVolume() ? volume.getWorkflowDueDateStr() : "");
    }

    public Date getComplienceDate() {
        return isDocument() ? document.getComplienceDate() : (isVolume() ? volume.getWorkflowEndDate() : null);
    }

    public String getComplienceDateStr() {
        return isDocument() ? document.getComplienceDateStr() : (isVolume() ? volume.getWorkflowEndDateStr() : "");
    }

    public String getOwnerName() {
        return isDocument() ? document.getOwnerName() : (isVolume() ? volume.getOwnerName() : "");
    }

    public UnmodifiableVolume getDocumentVolume() {
        return isDocument() ? document.getDocumentVolume() : null;
    }

    public List<SimpleFile> getFiles() {
        return isDocument() ? document.getFiles(null) : new ArrayList<SimpleFile>();
    }

    public String getCssStyleClass() {
        return isDocument() ? document.getCssStyleClass() : Document.GENERIC_DOCUMENT_STYLECLASS;
    }

    @Override
    public String getAction() {
        return isDocument() ? document.getAction() : (isCase() ? "dialog:documentListDialog" : (volume.isDynamic() ? "dialog:caseFileDialog" : "dialog:caseDocListDialog"));
    }

    public Document getDocument() {
        return document;
    }

    @Override
    public void open(ActionEvent event) {
        if (isDocument()) {
            document.open(event);
        } else if (isCase()) {
            NodeRef caseRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
            BeanHelper.getDocumentListDialog().init(caseRef);
        } else {
            NodeRef volumeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
            ActionUtil.getParams(event).put("volumeNodeRef", volumeRef.toString());
            if (volume.isDynamic()) {
                BeanHelper.getCaseFileDialog().openFromDocumentList(event);
            } else {
                BeanHelper.getCaseDocumentListDialog().showAll(event);
            }
        }
    }

}
