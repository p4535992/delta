package ee.webmedia.alfresco.eventplan.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getEventPlanLogBlock;
import static ee.webmedia.alfresco.common.web.BeanHelper.getEventPlanService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.eventplan.model.EventPlan;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.eventplan.service.dto.EventPlanSeries;
import ee.webmedia.alfresco.eventplan.service.dto.EventPlanVolume;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;
import ee.webmedia.alfresco.volume.search.web.VolumeDynamicSearchDialog;

/**
 * @author Martti Tamm
 * @author Alar Kvell
 */
public class EventPlanDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "EventPlanDialog";

    private EventPlan predefinedPlan;
    private DocumentConfig volumesFilterConfig;
    private WmNode volumesFilter;
    private List<SelectItem> stores;
    private List<EventPlanSeries> series;
    private List<EventPlanVolume> volumes;
    private boolean volumesEmpty = true;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        getLog().init(predefinedPlan);
    }

    @Override
    public void restored() {
        series = null;
        volumes = null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (predefinedPlan.validate()) {
            getEventPlanService().save(predefinedPlan);
            MessageUtil.addInfoMessage("save_success");
            return getDefaultFinishOutcome();
        }
        return null;
    }

    public boolean isVolumeEventPlanDialog() {
        return false;
    }

    public EventPlan getPredefinedPlan() {
        return predefinedPlan;
    }

    public boolean isNew() {
        return !RepoUtil.isSaved(predefinedPlan.getNode());
    }

    public boolean isInEditMode() {
        return getUserService().isArchivist();
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return isInEditMode();
    }

    public void addEventPlan(@SuppressWarnings("unused") ActionEvent event) {
        predefinedPlan = new EventPlan(getGeneralService().createNewUnSaved(EventPlanModel.Types.EVENT_PLAN, null));
        reset();
    }

    public void editEventPlan(ActionEvent event) {
        NodeRef planRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        predefinedPlan = getEventPlanService().getEventPlan(planRef);
        reset();
    }

    private void reset() {
        volumesFilterConfig = null;
        volumesFilter = null;
        series = null;
        volumes = null;
        volumesEmpty = true;
    }

    @SuppressWarnings("unused")
    public void deleteEventPlan(ActionEvent event) {
        if (!isNew()) {
            byte result = getEventPlanService().deleteEventPlan(predefinedPlan.getNode().getNodeRef());
            if (result == 0) {
                MessageUtil.addInfoMessage("eventplan_delete_success");
                WebUtil.navigateTo("dialog:eventPlanListDialog");
            } else if (result == 1) {
                MessageUtil.addErrorMessage("eventplan_delete_fail1");
            } else if (result == 2) {
                MessageUtil.addErrorMessage("eventplan_delete_fail2");
            }
        }
    }

    public List<EventPlanSeries> getSeries() {
        if (series == null && !isNew()) {
            series = getEventPlanService().getSeries(predefinedPlan.getNode().getNodeRef());
        }
        return series;
    }

    public List<EventPlanVolume> getVolumes() {
        if (volumes == null && !isNew()) {
            if (volumesEmpty) {
                volumes = Collections.emptyList();
            } else {
                WmNode filter = getVolumesFilter();
                String title = (String) filter.getProperties().get(VolumeModel.Props.TITLE);
                @SuppressWarnings("unchecked")
                List<String> status = (List<String>) filter.getProperties().get(VolumeModel.Props.STATUS);
                @SuppressWarnings("unchecked")
                List<NodeRef> store = (List<NodeRef>) filter.getProperties().get(VolumeSearchModel.Props.STORE);
                volumes = getEventPlanService().getVolumes(predefinedPlan.getNode().getNodeRef(), title, status, store);
            }
        }
        return volumes;
    }

    public EventPlanLogBlockBean getLog() {
        return getEventPlanLogBlock();
    }

    public WMPropertySheetConfigElement getVolumesFilterPropertySheetConfigElement() {
        if (volumesFilterConfig == null) {
            volumesFilterConfig = getDocumentConfigService().getEventPlanVolumeSearchFilterConfig();
        }
        return volumesFilterConfig.getPropertySheetConfigElement();
    }

    public WmNode getVolumesFilter() {
        if (volumesFilter == null) {
            HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(VolumeModel.Props.STATUS, new ArrayList<Object>());
            props.put(VolumeSearchModel.Props.STORE, new ArrayList<Object>());
            volumesFilter = new WmNode(RepoUtil.createNewUnsavedNodeRef(), VolumeSearchModel.Types.FILTER, new HashSet<QName>(), props);
        }
        return volumesFilter;
    }

    public List<SelectItem> getStores(@SuppressWarnings("unused") FacesContext context, UIInput selectComponent) {
        ((HtmlSelectManyListbox) selectComponent).setSize(5);
        if (stores == null) {
            stores = VolumeDynamicSearchDialog.getVolumeSearchStores();
        }
        return stores;
    }

    public void volumesSearch(@SuppressWarnings("unused") ActionEvent event) {
        volumes = null;
        volumesEmpty = false;
    }

    public void volumesShowAll(@SuppressWarnings("unused") ActionEvent event) {
        volumes = null;
        volumesFilter = null;
        volumesEmpty = false;
    }

}
