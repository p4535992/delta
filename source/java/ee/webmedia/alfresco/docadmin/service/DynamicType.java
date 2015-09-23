package ee.webmedia.alfresco.docadmin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Common base class for {@link DocumentType} and {@link CaseFileType}
 */
public class DynamicType extends BaseObject {
    private static final long serialVersionUID = 1L;

    private static final Map<String, QName> TYPE_ID_TO_ASSOC_QNAME = new HashMap<>();

    public DynamicType(NodeRef parentRef, QName type) {
        super(parentRef, type);
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public DynamicType(NodeRef parentNodeRef, WmNode docTypeNode) {
        super(parentNodeRef, docTypeNode);
    }

    @Override
    protected void nextSaveToParent(NodeRef parentRef) { // widens visibility for DocAdminService
        super.nextSaveToParent(parentRef);
        setLatestVersion(1);
        List<DocumentTypeVersion> docTypeVers = getDocumentTypeVersions();
        Assert.isTrue(docTypeVers.size() == 1, "there should be only one DocumentTypeVersion under DocumentType being imported");
        getLatestDocumentTypeVersion().setVersionNr(1);
    }

    @Override
    protected QName getAssocName() {
        return getAssocName(getId());
    }

    protected static QName getAssocName(String documentTypeId) {
        return RepoUtil.getFromQNamePool(documentTypeId, DocumentAdminModel.URI, TYPE_ID_TO_ASSOC_QNAME);
    }

    @Override
    public NodeRef getParentNodeRef() {
        return super.getParentNodeRef();
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

    public final String getId() {
        return getProp(DocumentAdminModel.Props.ID);
    }

    public final void setId(String documentTypeId) {
        setProp(DocumentAdminModel.Props.ID, documentTypeId);
    }

    public final String getName() {
        return getProp(DocumentAdminModel.Props.NAME);
    }

    public final void setName(String name) {
        setProp(DocumentAdminModel.Props.NAME, name);
    }

    public final boolean isUsed() {
        return getPropBoolean(DocumentAdminModel.Props.USED);
    }

    public final void setUsed(boolean used) {
        setProp(DocumentAdminModel.Props.USED, used);
    }

    public final Integer getLatestVersion() {
        return getProp(DocumentAdminModel.Props.LATEST_VERSION);
    }

    public final void setLatestVersion(Integer latestVersion) {
        setProp(DocumentAdminModel.Props.LATEST_VERSION, latestVersion);
        getLatestDocumentTypeVersion().setVersionNr(latestVersion);
    }

    public final String getComment() {
        return getProp(DocumentAdminModel.Props.COMMENT);
    }

    public final void setComment(String comment) {
        setProp(DocumentAdminModel.Props.COMMENT, comment);
    }

    public final String getMenuGroupName() {
        return getProp(DocumentAdminModel.Props.MENU_GROUP_NAME);
    }

    public final void setMenuGroupName(String documentTypeGroup) {
        setProp(DocumentAdminModel.Props.MENU_GROUP_NAME, documentTypeGroup);
    }

    public final DocumentTypeVersion addNewLatestDocumentTypeVersion() {
        ChildrenList<DocumentTypeVersion> documentTypeVersions = getDocumentTypeVersions();
        DocumentTypeVersion currentLatestVer = getLatestDocumentTypeVersion();
        if (currentLatestVer == null) {
            DocumentTypeVersion docTypeVer = documentTypeVersions.add();
            setLatestVersion(1);
            BeanHelper.getDocumentAdminService().addSystematicMetadataItems(docTypeVer);
            return docTypeVer;
        }
        DocumentTypeVersion newLatestVer = cloneAndMarkChildren(documentTypeVersions, currentLatestVer);
        Integer actual = currentLatestVer.getVersionNr() + 1;
        Assert.isTrue(documentTypeVersions.size() == actual);
        setLatestVersion(actual);
        return newLatestVer;
    }

    final DocumentTypeVersion cloneAndMarkChildren(ChildrenList<DocumentTypeVersion> documentTypeVersions, DocumentTypeVersion currentLatestVer) {
        DocumentTypeVersion newLatestVer = currentLatestVer.cloneAndResetBaseState();
        documentTypeVersions.addExisting(newLatestVer);
        return newLatestVer;
    }

    /** used by DeleteDialog to confirm delete action */
    final public String getNameAndId() {
        return new StringBuilder().append(getName()).append(" (").append(getId()).append(")").toString();
    }

    @Override
    public DynamicType clone() {
        return (DynamicType) super.clone(); // just return casted type
    }
}
