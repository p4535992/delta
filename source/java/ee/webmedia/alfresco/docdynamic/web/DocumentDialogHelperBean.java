package ee.webmedia.alfresco.docdynamic.web;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;

import org.alfresco.service.cmr.repository.NodeRef;
<<<<<<< HEAD
=======
import org.alfresco.service.cmr.repository.StoreRef;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
import org.joda.time.LocalDate;

<<<<<<< HEAD
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerWithDueDateGenerator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
<<<<<<< HEAD
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
=======
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.ComponentUtil;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        return getNode().getNodeRef();
=======
        Node node = getNode();
        return node == null ? null : node.getNodeRef();
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public Map<String, Object> getProps() {
        return getNode().getProperties();
    }

    public boolean isNotEditable() {
<<<<<<< HEAD
        return isDocument() && Boolean.TRUE.equals(getProps().get(DocumentCommonModel.Props.NOT_EDITABLE));
=======
        return Boolean.TRUE.equals(getProps().get(DocumentSpecificModel.Props.NOT_EDITABLE));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public boolean isInprogressCompoundWorkflows() {
        return BeanHelper.getWorkflowService().hasInprogressCompoundWorkflows(getNodeRef());
    }

    public boolean isNotWorkingOrNotEditable() {
<<<<<<< HEAD
        return isCaseFileClosedOrDestroyed() || isDocument() && !DocumentStatus.WORKING.getValueName().equals(getProps().get(DocumentCommonModel.Props.DOC_STATUS))
                || isNotEditable();
=======
        return !DocumentStatus.WORKING.getValueName().equals(getProps().get(DocumentCommonModel.Props.DOC_STATUS)) || isNotEditable();
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public void setDocumentDueDate(ValueChangeEvent event) {
        if (getNode() == null || !(event.getNewValue() instanceof List)) {
            return;
        }

        @SuppressWarnings("unchecked")
        final List<Object> newValue = (List<Object>) event.getNewValue();
        // Execute at the end of UPDATE_MODEL_VALUES phase, because during this phase node properties are set from user submitted data.
        // Queue executeLater event on propertySheet, because it supports handling ActionEvents.
<<<<<<< HEAD
        // Find propertySheet from component's hierarchy, do NOT use dialogDataProvider#getPropertySheet,
=======
        //Find propertySheet from component's hierarchy, do NOT use dialogDataProvider#getPropertySheet,
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        // because this AJAX submit is executed only on MultiValueEditor and thus PropertySheet binding to DocumentDynamicDialog has not been updated.
        UIComponent component = event.getComponent();
        UIPropertySheet propertySheet = ComponentUtil.getAncestorComponent(component, UIPropertySheet.class, true);
        final UIProperty ancestorComponent = ComponentUtil.getAncestorComponent(component, UIProperty.class, true);
        ComponentUtil.executeLater(PhaseId.UPDATE_MODEL_VALUES, propertySheet, new Closure() {

            @Override
            public void execute(Object input) {
                Boolean isWorkingDays = (Boolean) newValue.get(1);
                Integer dueDateDays = (Integer) newValue.get(0);
                LocalDate calculateDueDate = DatePickerWithDueDateGenerator.calculateDueDate(isWorkingDays, dueDateDays);
                BeanHelper.getDocumentDialogHelperBean().getNode().getProperties().put(DocumentSpecificModel.Props.DUE_DATE.toString(), calculateDueDate.toDateMidnight().toDate());
                // Let components regenerate to update their values
                ancestorComponent.getChildren().clear();
            }
        });
    }

    public boolean isInWorkspace() {
<<<<<<< HEAD
        return RepoUtil.isInWorkspace(getNode());
    }

    public boolean isDocument() {
        Node node = getNode();
        return node != null && DocumentCommonModel.Types.DOCUMENT.equals(node.getType());
    }

    public boolean isCaseFile() {
        Node node = getNode();
        return node != null && CaseFileModel.Types.CASE_FILE.equals(node.getType());
    }

    public boolean isCaseFileClosedOrDestroyed() {
        String status = (String) getProps().get(DocumentDynamicModel.Props.STATUS);
        return isCaseFile() && (DocListUnitStatus.CLOSED.getValueName().equals(status) || DocListUnitStatus.DESTROYED.equals(status));
    }

    public DialogDataProvider getDataProvider() {
        return dialogDataProvider;
=======
        Node node = getNode();
        return (node == null ? false : node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) ;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }
}
