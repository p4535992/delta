package ee.webmedia.alfresco.series.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.numberpattern.NumberPatternParser;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * Form backing bean for Series details
 */
public class SeriesDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "SeriesDetailsDialog";

    private static final String PARAM_FUNCTION_NODEREF = "functionNodeRef";
    private static final String PARAM_SERIES_NODEREF = "seriesNodeRef";

    private LogBlockBean logBlockBean;
    private Series series;
    private boolean newSeries;
    private String initialSeriesIdentifier;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (performPatternChecks(context)) {
            return null;
        }
        if (!validate()) {
            return null;
        }
        BeanHelper.getSeriesService().saveOrUpdate(series);
        resetFields();
        BeanHelper.getMenuService().menuUpdated(); // We need to refresh the left-hand sub-menu
        MessageUtil.addInfoMessage("save_success");
        return outcome;
    }

    private boolean validate() {
        Integer retentionPeriod = series.getRetentionPeriod();
        if (retentionPeriod != null && (retentionPeriod < 0 || retentionPeriod > 999)) {
            MessageUtil.addErrorMessage("series_retentionPeriod_error_invalid_value");
            return false;
        }
        return true;
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
        series = BeanHelper.getSeriesService().getSeriesByNodeRef(seriesNodeRef);
        logBlockBean.init(series.getNode());
    }

    public void addNewSeries(ActionEvent event) {
        newSeries = true;
        NodeRef funcNodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_FUNCTION_NODEREF));
        // create new node for series
        series = BeanHelper.getSeriesService().createSeries(funcNodeRef);
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
            try {
                BeanHelper.getSeriesService().openSeries(series);
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
            boolean wasClosed = BeanHelper.getSeriesService().closeSeries(series);
            if (!wasClosed) {
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "series_validationMsg_closeNotPossible");
                return null;
            }
            MessageUtil.addInfoMessage("series_close_success");
            return getDefaultFinishOutcome();
        }
        return null;
    }

    public boolean isClosed() {
        return BeanHelper.getSeriesService().isClosed(getCurrentNode());
    }

    public boolean isNew() {
        return newSeries;
    }

    // END: jsf actions/accessors

    private void resetFields() {
        initialSeriesIdentifier = null;
        series = null;
        newSeries = false;
        logBlockBean.reset();
    }

    // START: getters / setters

    public void setLogBlockBean(LogBlockBean logBlockBean) {
        this.logBlockBean = logBlockBean;
    }

    public LogBlockBean getLogBlockBean() {
        return logBlockBean;
    }

    public String getInitialSeriesIdentifier() {
        return initialSeriesIdentifier;
    }

    // END: getters / setters
}
