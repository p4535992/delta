package ee.webmedia.alfresco.document.type.model;

import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 * @author Ats Uiboupin
 */
public class DocumentType extends NodeBaseVO implements Comparable<DocumentType> {
    private static final long serialVersionUID = 1L;
    private static final QName TMP_ID = RepoUtil.createTransientProp("id");

    public DocumentType(QName id, WmNode node) {
        this.node = node;
        setId(id);
    }

    /**
     * Constructs new unsaved DocumentType
     * 
     * @param node
     */
    public DocumentType(WmNode node) {
        this(null, node);
    }

    // START: getters / setters
    public QName getId() {
        return getProp(DocumentTypeModel.Props.ID);
    }

    public void setId(QName id) {
        setProp(DocumentTypeModel.Props.ID, id);
    }

    public String getTmpId() {
        return getProp(TMP_ID);
    }

    public void setTmpId(String tmpId) {
        setProp(TMP_ID, tmpId);
    }

    public String getName() {
        return getProp(DocumentTypeModel.Props.NAME);
    }

    public void setName(String name) {
        setProp(DocumentTypeModel.Props.NAME, name);
    }

    public boolean isUsed() {
        return getPropBoolean(DocumentTypeModel.Props.USED);
    }

    public void setUsed(boolean used) {
        setProp(DocumentTypeModel.Props.USED, used);
    }

    public boolean isPublicAdr() {
        return getPropBoolean(DocumentTypeModel.Props.PUBLIC_ADR);
    }

    public void setPublicAdr(boolean publicAdr) {
        setProp(DocumentTypeModel.Props.PUBLIC_ADR, publicAdr);
    }

    public String getComment() {
        return getProp(DocumentTypeModel.Props.COMMENT);
    }

    public void setComment(String comment) {
        setProp(DocumentTypeModel.Props.COMMENT, comment);
    }

    public String getSystematicComment() {
        return getProp(DocumentTypeModel.Props.SYSTEMATIC_COMMENT);
    }

    public void setSystematicComment(String systematicComment) {
        setProp(DocumentTypeModel.Props.SYSTEMATIC_COMMENT, systematicComment);
    }

    // END: getters / setters

    @Override
    public int compareTo(DocumentType other) {
        if (StringUtils.equalsIgnoreCase(getName(), other.getName())) {
            return 0;
        }
        if (getName() == null) {
            return -1;
        } else if (other.getName() == null) {
            return 1;
        }
        return AppConstants.DEFAULT_COLLATOR.compare(getName(), other.getName());
    }

}
