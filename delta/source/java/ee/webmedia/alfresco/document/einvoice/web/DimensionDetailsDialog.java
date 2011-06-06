package ee.webmedia.alfresco.document.einvoice.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.context.IContextListener;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.Dimension;
import ee.webmedia.alfresco.document.einvoice.model.DimensionModel;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class DimensionDetailsDialog extends BaseDialogBean implements IContextListener {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "DimensionDetailsDialog";

    private static final Log log = LogFactory.getLog(DimensionDetailsDialog.class);

    private transient UIRichList richList;

    private List<DimensionValue> dimensionValues;
    private Dimension selectedDimension;
    private boolean isEditableDimension;

    public DimensionDetailsDialog() {
        UIContextService.getInstance(FacesContext.getCurrentInstance()).registerBean(this);
    }

    /**
     * Used in JSP pages.
     */
    public List<DimensionValue> getDimensionValues() {
        return dimensionValues;
    }

    /**
     * Used in JSP pages.
     */
    public Dimension getDimension() {
        return selectedDimension;
    }

    /**
     * Used in JSP pages.
     */
    public UIRichList getRichList() {
        return richList;
    }

    /**
     * Used in JSP pages.
     */
    public void setRichList(UIRichList richList) {
        this.richList = richList;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate()) {
            BeanHelper.getEInvoiceService().updateDimension(selectedDimension);
            BeanHelper.getEInvoiceService().updateDimensionValues(dimensionValues, selectedDimension.getNode());
            BeanHelper.getDimensionListDialog().reload();
            MessageUtil.addInfoMessage("save_success");
        }
        isFinished = false;
        return null;
    }

    private boolean validate() {
        boolean defaultValueSelected = false;
        for (DimensionValue dimensionValue : dimensionValues) {
            if (Boolean.TRUE.equals(dimensionValue.getDefaultValue())) {
                if (defaultValueSelected) {
                    MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "dimension_validationMsg_moreThanOneDefaultValue");
                    return false;
                }
                defaultValueSelected = true;
            }
        }
        return true;
    }

    @Override
    public String cancel() {
        resetData();
        return super.cancel();
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    @Override
    public String getContainerTitle() {
        if (selectedDimension != null) {
            return selectedDimension.getName();
        }
        return super.getContainerTitle();
    }

    @Override
    public String getActionsConfigId() {
        return null;
    }

    /**
     * JSP event handler.
     * Called before displaying dimension value list.
     * 
     * @param event
     */
    public void select(ActionEvent event) {
        selectedDimension = new Dimension(new Node(new NodeRef(ActionUtil.getParam(event, "nodeRef"))));
        isEditableDimension = BeanHelper.getEInvoiceService().isEditableDimension(selectedDimension.getNode().getNodeRef());
        loadDimensionValues();
    }

    public void addNewValue(ActionEvent event) {
        dimensionValues.add(getNewUnsavedDimensionValue());
    }

    private DimensionValue getNewUnsavedDimensionValue() {
        return new DimensionValue(BeanHelper.getGeneralService().createNewUnSaved(DimensionModel.Types.DIMENSION_VALUE, null));
    }

    @Override
    public void contextUpdated() {
        clearRichList();
    }

    @Override
    public void areaChanged() {
        clearRichList();
    }

    @Override
    public void spaceChanged() {
        clearRichList();
    }

    private void clearRichList() {
        if (getRichList() != null) {
            getRichList().setValue(null);
        }
    }

    private void resetData() {
        dimensionValues = null;
        selectedDimension = null;
        richList = null;
    }

    private void loadDimensionValues() {
        if (selectedDimension != null) {
            dimensionValues = BeanHelper.getEInvoiceService().getAllDimensionValuesFromRepo(selectedDimension.getNode().getNodeRef());
        }
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    public boolean isEditableDimension() {
        return isEditableDimension;
    }

}
