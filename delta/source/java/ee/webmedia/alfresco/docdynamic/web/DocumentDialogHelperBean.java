package ee.webmedia.alfresco.docdynamic.web;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;

/**
 * @author Alar Kvell
 */
public class DocumentDialogHelperBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "DocumentDialogHelperBean";

    private DialogDataProvider dialogDataProvider;

    /**
     * Reset all fields and components
     * 
     * @param provider may be {@code null}
     */
    public void reset(DialogDataProvider provider) {
        dialogDataProvider = provider;
    }

    public Node getNode() {
        return dialogDataProvider == null ? null : dialogDataProvider.getNode();
    }

    public boolean isInEditMode() {
        return dialogDataProvider == null ? false : dialogDataProvider.isInEditMode();
    }

    public void switchMode(boolean inEditMode) {
        dialogDataProvider.switchMode(inEditMode);
    }

    public NodeRef getNodeRef() {
        return getNode().getNodeRef();
    }

    public Map<String, Object> getProps() {
        return getNode().getProperties();
    }

    public boolean isNotEditable() {
        return Boolean.TRUE.equals(getProps().get(DocumentSpecificModel.Props.NOT_EDITABLE));
    }

    public boolean isInprogressCompoundWorkflows() {
        return BeanHelper.getWorkflowService().hasInprogressCompoundWorkflows(getNodeRef());
    }

    public boolean isNotWorkingOrNotEditable() {
        return !DocumentStatus.WORKING.getValueName().equals(getProps().get(DocumentCommonModel.Props.DOC_STATUS)) || isNotEditable();
    }

}
