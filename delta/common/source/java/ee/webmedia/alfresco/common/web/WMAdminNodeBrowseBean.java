package ee.webmedia.alfresco.common.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.ISO9075;
import org.alfresco.web.bean.admin.AdminNodeBrowseBean;
import org.apache.commons.lang.StringUtils;

/**
 * @author Ats Uiboupin
 */
public class WMAdminNodeBrowseBean extends AdminNodeBrowseBean {

    private static final long serialVersionUID = -3757857288967828948L;
    private String targetRef;
    private String assocTypeQName;
    private List<SelectItem> assocTypeQNames;
    transient private DataModel sourceAssocs;

    public List<SelectItem> getAssocTypeQNames() {
        final Collection<QName> allAssociations = getDictionaryService().getAllAssociations();
        assocTypeQNames = new ArrayList<SelectItem>(allAssociations.size() + 1);
        for (QName assocQName : allAssociations) {

            final AssociationDefinition association = getDictionaryService().getAssociation(assocQName);
            final QName sourceName = association.getSourceClass().getName();
            final QName targetName = association.getTargetClass().getName();
            final SelectItem item = new SelectItem(assocQName.toString(), assocQName.getPrefixString() + " (" + sourceName.toPrefixString() + " -> "
                    + targetName.toPrefixString() + ")");
            if (!getDictionaryService().isSubClass(getNodeType(), sourceName)) {
                item.setDisabled(true);
            }
            if (getDictionaryService().isSubClass(getNodeType(), targetName)) {
                item.setLabel("(target) " + item.getLabel());
            }
            assocTypeQNames.add(item);
        }
        assocTypeQNames.add(0, new SelectItem("[defaultSelection]", ""));
        return assocTypeQNames;
    }

    public void setTargetRef(String targetRef) {
        this.targetRef = targetRef;
    }

    public String getTargetRef() {
        return targetRef;
    }

    public void setAssocTypeQName(String assocTypeQName) {
        this.assocTypeQName = assocTypeQName;
    }

    public String getAssocTypeQName() {
        return assocTypeQName;
    }

    public void submitCreateAssoc() {
        if (StringUtils.isBlank(assocTypeQName)) {
            addErrorMessage("nodeBrowser_custom_noSuchAssocTypeQName");
            return;
        }
        final QName newAssocTypeQname = QName.createQName(assocTypeQName);
        final Collection<QName> allAssociations = getDictionaryService().getAllAssociations();
        if (allAssociations.contains(newAssocTypeQname)) {
            getNodeService().createAssociation(getNodeRef(), new NodeRef(targetRef), newAssocTypeQname);
            assocs = null; // reset assocs, so they would be recreated
        } else {
            addErrorMessage("nodeBrowser_custom_noSuchAssocTypeQName");
        }
    }

    private void addErrorMessage(String messageText) {
        // FIXME: kunagi hiljem võiks teha MessageUtil'i peale MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "nodeBrowser_custom_noSuchAssocTypeQName",
        // assocTypeQName);
        FacesContext context = FacesContext.getCurrentInstance();
        FacesMessage message = new FacesMessage();
        message.setSeverity(FacesMessage.SEVERITY_ERROR);
        message.setDetail(messageText);
        context.addMessage("searchForm:query", message);
    }

    /**
     * Action to select association From node
     * 
     * @return next action
     */
    public String selectFromNode() {
        AssociationRef assocRef = (AssociationRef) getFromAssocs().getRowData();
        setNodeRef(assocRef.getSourceRef());
        return "success";
    }

    public DataModel getFromAssocs() {
        List<AssociationRef> assocRefs = getNodeService().getSourceAssocs(getNodeRef(), RegexQNamePattern.MATCH_ALL);
        sourceAssocs = new ListDataModel(assocRefs);
        return sourceAssocs;
    }

    private String primaryPathShort;

    public String getPrimaryPathShort() {
        final String primaryPath = super.getPrimaryPath();
        int startURI = primaryPath.indexOf("{");
        int nextStartURI = primaryPath.indexOf("{");
        String primaryPathShort;
        if (startURI != -1) {
            primaryPathShort = primaryPath.substring(0, startURI);
            while (nextStartURI != -1) {
                final int endIndex = primaryPath.indexOf("}", nextStartURI);
                nextStartURI = primaryPath.indexOf("{", endIndex);
                if (nextStartURI != -1) {
                    primaryPathShort += primaryPath.substring(endIndex + 1, nextStartURI);
                } else {
                    primaryPathShort += primaryPath.substring(endIndex + 1);
                }
            }
        } else {
            primaryPathShort = primaryPath;
        }
        return ISO9075.decode(primaryPathShort.toString());
    }

}
