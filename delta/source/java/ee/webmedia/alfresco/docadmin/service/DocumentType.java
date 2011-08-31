package ee.webmedia.alfresco.docadmin.service;

import java.util.List;

import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

public class DocumentType extends BaseObject {
    private static final long serialVersionUID = 1L;

    public DocumentType(NodeRef parentRef) {
        super(parentRef, DocumentAdminModel.Types.DOCUMENT_TYPE);
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public DocumentType(NodeRef parentNodeRef, WmNode docTypeNode) {
        super(parentNodeRef, docTypeNode);
    }

    @Override
    protected QName getAssocName() {
        return getAssocName(getDocumentTypeId());
    }

    protected static QName getAssocName(String documentTypeId) {
        return QName.createQName(DocumentAdminModel.URI, documentTypeId);
    }

    // ChildrenList

    public DocumentTypeVersion getLatestDocumentTypeVersion() {
        List<? extends DocumentTypeVersion> versionList = getDocumentTypeVersions().getList();
        if (versionList.isEmpty()) {
            return null;
        }
        return versionList.get(versionList.size() - 1);
    }

    public ChildrenList<DocumentTypeVersion> getDocumentTypeVersions() {
        return getChildren(DocumentTypeVersion.class);
    }

    // Properties

    public String getDocumentTypeId() {
        return getProp(DocumentAdminModel.Props.DOCUMENT_TYPE_ID);
    }

    public void setDocumentTypeId(String documentTypeId) {
        setProp(DocumentAdminModel.Props.DOCUMENT_TYPE_ID, documentTypeId);
    }

    public String getName() {
        return getProp(DocumentAdminModel.Props.NAME);
    }

    public void setName(String name) {
        setProp(DocumentAdminModel.Props.NAME, name);
    }

    public boolean isUsed() {
        return getPropBoolean(DocumentAdminModel.Props.USED);
    }

    public void setUsed(boolean used) {
        setProp(DocumentAdminModel.Props.USED, used);
    }

    public Integer getLatestVersion() {
        return getProp(DocumentAdminModel.Props.LATEST_VERSION);
    }

    public void setLatestVersion(Integer latestVersion) {
        setProp(DocumentAdminModel.Props.LATEST_VERSION, latestVersion);
        getLatestDocumentTypeVersion().setVersionNr(latestVersion);
    }

    public boolean isPublicAdr() {
        return getPropBoolean(DocumentAdminModel.Props.PUBLIC_ADR);
    }

    public void setPublicAdr(boolean publicAdr) {
        setProp(DocumentAdminModel.Props.PUBLIC_ADR, publicAdr);
    }

    public String getComment() {
        return getProp(DocumentAdminModel.Props.COMMENT);
    }

    public void setComment(String comment) {
        setProp(DocumentAdminModel.Props.COMMENT, comment);
    }

    public String getSystematicComment() {
        return getProp(DocumentAdminModel.Props.SYSTEMATIC_COMMENT);
    }

    public void setSystematicComment(String systematicComment) {
        setProp(DocumentAdminModel.Props.SYSTEMATIC_COMMENT, systematicComment);
    }

    public void setSystematic(boolean systematic) {
        setProp(DocumentAdminModel.Props.SYSTEMATIC, systematic);
    }

    public boolean isSystematic() {
        return getPropBoolean(DocumentAdminModel.Props.SYSTEMATIC);
    }

    public String getDocumentTypeGroup() {
        return getProp(DocumentAdminModel.Props.DOCUMENT_TYPE_GROUP);
    }

    public void setDocumentTypeGroup(String documentTypeGroup) {
        setProp(DocumentAdminModel.Props.DOCUMENT_TYPE_GROUP, documentTypeGroup);
    }

    @Override
    protected void handleException(RuntimeException e) {
        if (e instanceof DuplicateChildNodeNameException) {
            throw new UnableToPerformException(MessageSeverity.ERROR, "doc_type_error_id_alreadyExists", e);
        }
        super.handleException(e);
    }

    @Override
    public DocumentType clone() {
        return (DocumentType) super.clone(); // just return casted type
    }

    public DocumentTypeVersion addNewLatestDocumentTypeVersion() {
        ChildrenList<DocumentTypeVersion> documentTypeVersions = getDocumentTypeVersions();
        DocumentTypeVersion currentLatestVer = getLatestDocumentTypeVersion();
        if (currentLatestVer == null) {
            DocumentTypeVersion docTypeVer = documentTypeVersions.add();
            setLatestVersion(1);
            BeanHelper.getDocumentAdminService().addSystematicMetadataItems(docTypeVer); // TODO DLSeadist (Alar): temporary solution until Ats finishes proper support for this
            return docTypeVer;
        }
        DocumentTypeVersion newLatestVer = cloneAndMarkChildren(documentTypeVersions, currentLatestVer);
        setLatestVersion(currentLatestVer.getVersionNr() + 1);
        return newLatestVer;
    }

    DocumentTypeVersion cloneAndMarkChildren(ChildrenList<DocumentTypeVersion> documentTypeVersions, DocumentTypeVersion currentLatestVer) {
        DocumentTypeVersion newLatestVer = currentLatestVer.cloneAndResetBaseState();
        documentTypeVersions.addExisting(newLatestVer);
        return newLatestVer;
    }
}
