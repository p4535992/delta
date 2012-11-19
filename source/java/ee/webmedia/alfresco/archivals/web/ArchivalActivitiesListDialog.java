package ee.webmedia.alfresco.archivals.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.archivals.model.ActivityType;
import ee.webmedia.alfresco.archivals.model.ArchivalsModel;
import ee.webmedia.alfresco.common.propertysheet.customchildrencontainer.CustomChildrenCreator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Riina Tens
 */
public class ArchivalActivitiesListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "ArchivalActivitiesListDialog";

    private Node filter;
    private List<SelectItem> activityTypes;
    private List<ArchivalActivity> archivalActivities;
    private UIRichList richList;

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        filter = null;
        loadSelectItems();
        initFilter();
        filter.getProperties().put(ArchivalsModel.Props.FILTER_CREATED.toString(), DateUtils.addYears(new Date(), -1));
        searchArchivalActivities(null);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    @Override
    public void restored() {
        richList = null;
        searchArchivalActivities(null);
    }

    public void searchAllArchivalActivities(ActionEvent event) {
        initFilter();
        searchArchivalActivities(event);
    }

    public void searchArchivalActivities(ActionEvent event) {
        archivalActivities = BeanHelper.getDocumentSearchService().searchArchivalActivities(filter);
    }

    private void loadSelectItems() {
        activityTypes = new ArrayList<SelectItem>();
        activityTypes.add(new SelectItem("", VolumeArchiveBaseDialog.EMPTY_LABEL));
        for (ActivityType activityType : ActivityType.values()) {
            activityTypes.add(new SelectItem(activityType.name(), MessageUtil.getMessage(activityType)));
        }
    }

    private void initFilter() {
        filter = new TransientNode(ArchivalsModel.Types.ARCHIVAL_ACTIVITY_SEARCH_FILTER, null, null);
    }

    public List<SelectItem> getActivityTypes(FacesContext context, UIInput input) {
        return activityTypes;
    }

    public List<ArchivalActivity> getArchivalActivities() {
        return archivalActivities;
    }

    public Node getFilter() {
        return filter;
    }

    public CustomChildrenCreator getArchivalActivityRowFileGenerator() {
        return ComponentUtil.getRowFileGenerator(FacesContext.getCurrentInstance().getApplication());
    }

    public UIRichList getRichList() {
        return richList;
    }

    public void setRichList(UIRichList richList) {
        this.richList = richList;
    }

}
