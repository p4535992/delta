package ee.webmedia.alfresco.series.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getLogBlockBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSeriesService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.eventplan.model.EventPlan;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.numberpattern.NumberPatternParser;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Form backing bean for Series details
 * 
 * @author Ats Uiboupin
 */
public class SeriesDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "SeriesDetailsDialog";

    private static final String PARAM_FUNCTION_NODEREF = "functionNodeRef";
    private static final String PARAM_SERIES_NODEREF = "seriesNodeRef";

    private Series series;
    private boolean newSeries;
    private String initialSeriesIdentifier;
    private transient UIPropertySheet propertySheet;
    private static final String PARAM_NODEREF = "nodeRef";
    private static final String PARAM_CONFIRMATION = "confirmation";

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return finishImpl(context, outcome, false);
    }

    private String finishImpl(FacesContext context, String outcome, boolean registerChecked) throws Throwable {
        if (performPatternChecks(context)) {
            return null;
        }
        if (!registerChecked && performRegisterCheck()) {
            return null;
        }
        BeanHelper.getSeriesService().saveOrUpdate(series);
        resetFields();
        BeanHelper.getMenuService().menuUpdated(); // We need to refresh the left-hand sub-menu
        MessageUtil.addInfoMessage("save_success");
        return outcome;
    }

    private boolean performPatternChecks(FacesContext context) {
        boolean foundErrors = false;
        String patternStr = (String) getCurrentNode().getProperties().get(SeriesModel.Props.DOC_NUMBER_PATTERN);
        NumberPatternParser docNrPatternParsed = new NumberPatternParser(patternStr);
        if (!docNrPatternParsed.isValid()) {
            for (String invalidParam : docNrPatternParsed.getInvalidParams()) {
                MessageUtil.addErrorMessage(context, "series_docNumberPattern_contains_invalid_param", "{" + invalidParam + "}");
                foundErrors = true;
            }
        }

        if (!docNrPatternParsed.containsParam("DN")) {
            MessageUtil.addErrorMessage("series_docNumberPattern_dn_mandatory");
            foundErrors = true;
        }
        if (docNrPatternParsed.containsParam("TN")) {
            MessageUtil.addErrorMessage(context, "series_docNumberPattern_tn_not_allowed");
            foundErrors = true;
        }
        if (BeanHelper.getVolumeService().isCaseVolumeEnabled()) {
            // volRegister && volNumberPattern are visible and enabled
            @SuppressWarnings("unchecked")
            List<String> volType = (List<String>) getCurrentNode().getProperties().get(SeriesModel.Props.VOL_TYPE);
            NumberPatternParser volNrPatternParsed = new NumberPatternParser((String) getCurrentNode().getProperties().get(SeriesModel.Props.VOL_NUMBER_PATTERN));
            if (!volNrPatternParsed.isValid()) {
                for (String invalidParam : volNrPatternParsed.getInvalidParams()) {
                    MessageUtil.addErrorMessage(context, "series_volNumberPattern_contains_invalid_param", "{" + invalidParam + "}");
                    foundErrors = true;
                }
            }
            if (volNrPatternParsed.containsParam("T")) {
                MessageUtil.addErrorMessage(context, "series_volNumberPattern_cannot_contain_itself");
                foundErrors = true;
            }
            if (volNrPatternParsed.containsParam("DA")) {
                MessageUtil.addErrorMessage(context, "series_volNumberPattern_da_not_allowed");
                foundErrors = true;
            }
            if (volNrPatternParsed.containsParam("DN")) {
                MessageUtil.addErrorMessage(context, "series_volNumberPattern_dn_not_allowed");
                foundErrors = true;
            }
            if (volType.contains(VolumeType.CASE_FILE.name())) {
                Integer volRegister = (Integer) getCurrentNode().getProperties().get(SeriesModel.Props.VOL_REGISTER);
                if (volNrPatternParsed.isBlank()) {
                    MessageUtil.addErrorMessage(context, "series_volNrPattern_must_not_be_empty");
                    foundErrors = true;
                } else if (volNrPatternParsed.containsParam("TN") && volRegister == null) {
                    MessageUtil.addErrorMessage(context, "series_vol_register_must_be_chosen");
                    foundErrors = true;
                }
            } else {
                if (volNrPatternParsed.containsParam("TN")) {
                    MessageUtil.addErrorMessage(context, "series_volNumberPattern_tn_not_allowed");
                    foundErrors = true;
                }
                if (volNrPatternParsed.containsParam("TA")) {
                    MessageUtil.addErrorMessage(context, "series_volNumberPattern_ta_not_allowed");
                    foundErrors = true;
                }
                if (docNrPatternParsed.containsParam("TA")) {
                    MessageUtil.addErrorMessage(context, "series_docNumberPattern_ta_not_allowed");
                    foundErrors = true;
                }
            }
        } else {
            if (docNrPatternParsed.containsParam("TA")) {
                MessageUtil.addErrorMessage(context, "series_docNumberPattern_ta_not_allowed");
                foundErrors = true;
            }

        }
        return foundErrors;
    }

    private boolean performRegisterCheck() {
        Register register = BeanHelper.getRegisterService().getRegister((Integer) getCurrentNode().getProperties().get(SeriesModel.Props.REGISTER));
        if (register.isAutoReset()) {
            @SuppressWarnings("unchecked")
            List<String> volTypes = ((List<String>) getCurrentNode().getProperties().get(SeriesModel.Props.VOL_TYPE));
            if (volTypes.contains(VolumeType.CASE_FILE.name()) || volTypes.contains(VolumeType.SUBJECT_FILE.name())) {
                Map<String, String> params = new HashMap<String, String>(2);
                params.put(PARAM_NODEREF, series.getNode().getNodeRefAsString());
                params.put(PARAM_CONFIRMATION, Boolean.TRUE.toString());
                BeanHelper
                        .getUserConfirmHelper()
                        .setup(
                                new MessageDataImpl(
                                        "series_confirmation", register.getName()), null,
                                "#{SeriesDetailsDialog.afterConfirmationAction}", params, null, null, null
                        );
                return true;
            }
        }
        return false;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return series.getNode();
    }

    @Override
    public String getActionsConfigId() {
        if (!(series.getNode() instanceof TransientNode)) {
            return "browse_actions_series_details";
        }
        return null;
    }

    // START: jsf actions/accessors
    public void showDetails(ActionEvent event) {
        String seriesNodeRef = ActionUtil.getParam(event, PARAM_SERIES_NODEREF);
        series = getSeriesService().getSeriesByNodeRef(seriesNodeRef);
        getLogBlockBean().init(series.getNode());
    }

    public void addNewSeries(ActionEvent event) {
        newSeries = true;
        NodeRef funcNodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_FUNCTION_NODEREF));
        // create new node for series
        series = getSeriesService().createSeries(funcNodeRef);
        initialSeriesIdentifier = (String) getCurrentNode().getProperties().get(SeriesModel.Props.SERIES_IDENTIFIER);
    }

    public Node getCurrentNode() {
        return series.getNode();
    }

    public void open(@SuppressWarnings("unused") ActionEvent event) {
        Node currentSeriesNode = series.getNode();
        if (currentSeriesNode instanceof TransientNode || currentSeriesNode == null) {
            return;
        }
        if (isClosed()) {
            propertySheet.getChildren().clear();
            try {
                getSeriesService().openSeries(series);
            } catch (UnableToPerformException e) {
                MessageUtil.addStatusMessage(e);
                return;
            }
            MessageUtil.addInfoMessage("series_open_success");
        }
    }

    public String close() {
        if (series.getNode() instanceof TransientNode) {
            return null;
        }

        if (!isClosed()) {
            propertySheet.getChildren().clear();
            boolean wasClosed = getSeriesService().closeSeries(series);
            if (!wasClosed) {
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "series_validationMsg_closeNotPossible");
                return null;
            }
            MessageUtil.addInfoMessage("series_close_success");
            return null;
        }
        return null;
    }

    public String delete() {
        if (series.getNode() instanceof TransientNode) {
            return null;
        }
        if (isClosed()) {
            try {
                getSeriesService().delete(series);
                MessageUtil.addInfoMessage("series_delete_success");
                return getDefaultCancelOutcome();
            } catch (UnableToPerformException e) {
                MessageUtil.addStatusMessage(e);
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public List<SelectItem> getEventPlans(FacesContext context, UIInput input) {
        List<EventPlan> eventPlans = BeanHelper.getEventPlanService().getEventPlans();

        List<SelectItem> options = new ArrayList<SelectItem>();
        options.add(new SelectItem("", MessageUtil.getMessage("select_default_label")));
        for (EventPlan eventPlan : eventPlans) {
            options.add(new SelectItem(eventPlan.getNode().getNodeRef(), eventPlan.getName()));
        }
        return options;
    }

    public boolean isClosed() {
        return getSeriesService().isClosed(getCurrentNode());
    }

    public boolean isNew() {
        return newSeries;
    }

    public void afterConfirmationAction(ActionEvent event) {
        boolean continueSaving = false;
        if (ActionUtil.hasParam(event, PARAM_CONFIRMATION)) {
            continueSaving = ActionUtil.getParam(event, PARAM_CONFIRMATION, Boolean.class);
        }
        try {
            if (continueSaving) {
                WebUtil.navigateTo(finishImpl(FacesContext.getCurrentInstance(), getDefaultFinishOutcome(), true));
            } else {
                cancel();
            }
        } catch (Throwable e) {
            MessageUtil.addErrorMessage(e.getLocalizedMessage());
        }
    }

    // END: jsf actions/accessors

    private void resetFields() {
        initialSeriesIdentifier = null;
        series = null;
        newSeries = false;
        propertySheet = null;
        getLogBlockBean().reset();
    }

    // START: getters / setters

    public String getInitialSeriesIdentifier() {
        return initialSeriesIdentifier;
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }

    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }

    // END: getters / setters
}
