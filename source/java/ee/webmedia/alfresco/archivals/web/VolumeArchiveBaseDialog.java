package ee.webmedia.alfresco.archivals.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.config.Config;
import org.alfresco.config.ConfigElement;
import org.alfresco.config.ConfigService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.archivals.model.ActivityStatus;
import ee.webmedia.alfresco.archivals.model.ActivityType;
import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO.ConfigItemType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.eventplan.model.EventPlan;
import ee.webmedia.alfresco.eventplan.model.FirstEvent;
import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;
import ee.webmedia.alfresco.volume.search.web.VolumeDynamicSearchDialog;

public abstract class VolumeArchiveBaseDialog extends BaseDialogBean {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(VolumeArchiveBaseDialog.class);
    protected static final String CONFIRM_VOLUME_ARCHIVE_ACTION_OUTCOME = "confirmVolumeArchiveActionDialog";

    private static final long serialVersionUID = 1L;
    protected List<Volume> volumes;
    private Map<NodeRef, Boolean> listCheckboxes;
    protected Node filter;
    private transient UIPropertySheet propertySheet;
    private transient PropertySheetConfigElement propertySheetConfigElement;
    private List<SelectItem> eventPlans;
    private List<SelectItem> booleanSelectItems;
    private List<SelectItem> statuses;
    protected List<SelectItem> stores;
    protected List<NodeRef> storeNodeRefs;
    private List<SelectItem> nextEvents;
    public static final String EMPTY_LABEL = MessageUtil.getMessage("select_default_label");
    protected List<QName> renderedFilterFields;
    private boolean confirmGenerateWordFile;

    protected static final ComparatorChain BASE_COMPARATOR;

    static {
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Volume>() {
            @Override
            public Comparable<Date> tr(Volume volume) {
                return volume.getValidTo();
            }
        }, new NullComparator()));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Volume>() {
            @Override
            public Comparable<String> tr(Volume volume) {
                return volume.getVolumeMark();
            }
        }, new NullComparator(AppConstants.DEFAULT_COLLATOR)));
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Volume>() {
            @Override
            public Comparable<String> tr(Volume volume) {
                return volume.getTitle();
            }
        }, new NullComparator(AppConstants.DEFAULT_COLLATOR)));
        BASE_COMPARATOR = chain;
    }

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        propertySheet = null;
        renderedFilterFields = null;
        filter = null;
        loadSelectItems();
        initFilter();
        if (executeInitialSearch()) {
            searchVolumes(null);
        } else {
            volumes = new ArrayList<Volume>();
            listCheckboxes = new HashMap<NodeRef, Boolean>();
        }
        cancelAction(null);
    }

    protected boolean executeInitialSearch() {
        return false;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    public void searchVolumes(ActionEvent event) {
        searchVolumes();
        listCheckboxes = new HashMap<NodeRef, Boolean>();
        for (Volume volume : volumes) {
            listCheckboxes.put(volume.getNodeRef(), Boolean.FALSE);
        }
    }

    protected void searchVolumes() {
        volumes = BeanHelper.getDocumentSearchService().searchVolumesForArchiveList(filter, storeNodeRefs);
        Comparator<Volume> comparator = getComparator();
        if (volumes != null && comparator != null) {
            Collections.sort(volumes, comparator);
        }
    }

    protected Comparator<Volume> getComparator() {
        return BASE_COMPARATOR;
    }

    public void searchAllVolumes(ActionEvent event) {
        initFilter();
        searchVolumes(event);
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public Map<NodeRef, Boolean> getListCheckboxes() {
        return listCheckboxes;
    }

    public Node getFilter() {
        if (filter == null) {
            initFilter();
        }
        return filter;
    }

    protected void initFilter() {
        filter = new TransientNode(VolumeSearchModel.Types.ARCHIVE_LIST_FILTER, null, null);
        initFilterItems();
    }

    protected void initFilterItems() {
        filter.getProperties().put(VolumeSearchModel.Props.STORE.toString(), Collections.emptyList());
        filter.getProperties().put(VolumeSearchModel.Props.STATUS.toString(), Collections.emptyList());
        filter.getProperties().put(VolumeSearchModel.Props.NEXT_EVENT.toString(), Collections.emptyList());
    }

    protected boolean showStoreFilterField() {
        LinkedHashSet<ArchivalsStoreVO> archivalsStoreVOs = BeanHelper.getGeneralService().getArchivalsStoreVOs();
        return archivalsStoreVOs != null && archivalsStoreVOs.size() > 1;
    }

    protected boolean showNextEventFilterField() {
        return BeanHelper.getArchivalsService().isSimpleDestructionEnabled();
    }

    private void loadSelectItems() {
        eventPlans = new ArrayList<SelectItem>();
        List<EventPlan> eventPlansObjects = BeanHelper.getEventPlanService().getEventPlans();
        eventPlans.add(new SelectItem("", EMPTY_LABEL));
        for (EventPlan eventPlan : eventPlansObjects) {
            eventPlans.add(new SelectItem(eventPlan.getNodeRef(), eventPlan.getName()));
        }
        booleanSelectItems = new ArrayList<SelectItem>();
        booleanSelectItems.add(new SelectItem("", EMPTY_LABEL));
        booleanSelectItems.add(new SelectItem(Boolean.TRUE, MessageUtil.getMessage("yes")));
        booleanSelectItems.add(new SelectItem(Boolean.FALSE, MessageUtil.getMessage("no")));

        statuses = new ArrayList<SelectItem>();
        statuses.add(new SelectItem(DocListUnitStatus.OPEN.getValueName()));
        statuses.add(new SelectItem(DocListUnitStatus.CLOSED.getValueName()));

        loadStores();

        nextEvents = new ArrayList<SelectItem>();
        nextEvents.add(new SelectItem(FirstEvent.DESTRUCTION.name(), MessageUtil.getMessage(FirstEvent.DESTRUCTION)));
        nextEvents.add(new SelectItem(FirstEvent.SIMPLE_DESTRUCTION.name(), MessageUtil.getMessage(FirstEvent.SIMPLE_DESTRUCTION)));
    }

    protected void loadStores() {
        loadArchivalStores();
    }

    protected void loadArchivalStores() {
        stores = new ArrayList<SelectItem>();
        storeNodeRefs = new ArrayList<NodeRef>();
        for (ArchivalsStoreVO archivalsStoreVO : getGeneralService().getArchivalsStoreVOs()) {
            NodeRef nodeRef = archivalsStoreVO.getNodeRef();
            stores.add(new SelectItem(nodeRef, archivalsStoreVO.getTitle()));
            storeNodeRefs.add(nodeRef);
        }
    }

    protected void loadAllStores() {
        stores = VolumeDynamicSearchDialog.getVolumeSearchStores();
        storeNodeRefs = new ArrayList<NodeRef>();
        for (SelectItem selectItem : stores) {
            storeNodeRefs.add((NodeRef) selectItem.getValue());
        }
    }

    protected NodeRef generateActivityAndWordFile(ActivityType activityType, String templateNameMsgKey, ActivityStatus activityStatus, String successMsgKey, boolean fileRequired) {
        return generateActivityAndWordFile(activityType, templateNameMsgKey, activityStatus, successMsgKey, fileRequired,
                "archivals_volume_generate_word_file_error_missing_template");
    }

    protected NodeRef generateActivityAndWordFile(ActivityType activityType, String templateNameMsgKey, ActivityStatus activityStatus, String successMsgKey, boolean fileRequired,
            String missingTemplateErrorMsgKey) {
        String templateName = MessageUtil.getMessage(templateNameMsgKey);
        NodeRef templateRef = BeanHelper.getDocumentTemplateService().getArchivalReportTemplateByName(templateName);
        if (templateRef == null && fileRequired) {
            MessageUtil.addErrorMessage(missingTemplateErrorMsgKey, templateName);
            return null;
        }
        return generateActivityAndWordFile(activityType, activityStatus, successMsgKey, templateRef);
    }

    protected NodeRef generateActivityAndWordFile(ActivityType activityType, ActivityStatus activityStatus, String successMsgKey, NodeRef templateRef) {
        try {
            NodeRef activityRef = BeanHelper.getArchivalsService().addArchivalActivity(activityType, activityStatus,
                    getSelectedVolumes(), templateRef);
            if (StringUtils.isNotBlank(successMsgKey)) {
                MessageUtil.addInfoMessage(successMsgKey);
            }
            return activityRef;
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
        } catch (RuntimeException e) {
            LOG.error("Populate template failed", e);
            MessageUtil.addErrorMessage(DocumentDialog.ERR_TEMPLATE_PROCESSING_FAILED);
        }
        return null;
    }

    public List<SelectItem> getEventPlans(FacesContext context, UIInput input) {
        return eventPlans;
    }

    public List<SelectItem> getStatuses(FacesContext context, UIInput input) {
        return statuses;
    }

    public List<SelectItem> getBooleanSelectItems(FacesContext context, UIInput input) {
        return booleanSelectItems;
    }

    public List<SelectItem> getStores(FacesContext context, UIInput input) {
        return stores;
    }

    public List<SelectItem> getNextEvents(FacesContext context, UIInput input) {
        return nextEvents;
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }

    public ConfigElement getConfig() {
        if (propertySheetConfigElement == null) {
            ConfigService configSvc = Application.getConfigService(FacesContext.getCurrentInstance());
            Config configProps = configSvc.getConfig(filter);
            propertySheetConfigElement = (PropertySheetConfigElement) configProps.getConfigElement("property-sheet");
            for (ConfigElement configElement : propertySheetConfigElement.getChildren()) {
                if (configElement instanceof ItemConfigVO) {
                    ItemConfigVO itemConfigVO = (ItemConfigVO) configElement;
                    if (itemConfigVO.getConfigItemType() == ConfigItemType.PROPERTY && !getRenderedFilterFields().contains(configElement.getName())) {
                        itemConfigVO.setRendered(Boolean.FALSE.toString());
                    }
                }
            }
        }
        return propertySheetConfigElement;
    }

    protected boolean checkVolumesSelected() {
        if (!getListCheckboxes().containsValue(Boolean.TRUE)) {
            MessageUtil.addErrorMessage("archivals_volume_archive_no_volume_selected");
            return false;
        }
        return true;
    }

    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }

    public boolean isShowNextEventDateColumn() {
        return false;
    }

    public boolean isShowRetaintionColumns() {
        return false;
    }

    public boolean isShowDestructionColumns() {
        return false;
    }

    public boolean isShowStatusColumn() {
        return false;
    }

    public boolean isShowTransferringDeletingNextEventColumn() {
        return false;
    }

    public boolean isShowTransferringNextEventDateColumn() {
        return false;
    }

    public String getNextEventLabel() {
        return MessageUtil.getMessage("archivals_volume_next_event");
    }

    public boolean isShowMarkedForTransferColumn() {
        return false;
    }

    public boolean isShowExportedForUamColumn() {
        return false;
    }

    public boolean isShowExportedForUamDateTimeColumn() {
        return false;
    }

    public boolean isShowExportToUamButton() {
        return false;
    }

    public boolean isShowStore() {
        return false;
    }

    protected List<QName> getRenderedFilterFields() {
        throw new RuntimeException("Subclasses have to implement this method!");
    }

    public boolean showValidTo() {
        return getRenderedFilterFields().contains(VolumeSearchModel.Props.VALID_TO);
    }

    public boolean showHasArchivalValue() {
        return getRenderedFilterFields().contains(VolumeSearchModel.Props.HAS_ARCHIVAL_VALUE);
    }

    public boolean showStatus() {
        return getRenderedFilterFields().contains(VolumeSearchModel.Props.STATUS);
    }

    public boolean showStore() {
        return getRenderedFilterFields().contains(VolumeSearchModel.Props.STORE);
    }

    public boolean showRetainPermanent() {
        return getRenderedFilterFields().contains(VolumeSearchModel.Props.RETAIN_PERMANENT);
    }

    public boolean showRetainUntilDate() {
        return getRenderedFilterFields().contains(VolumeSearchModel.Props.RETAIN_UNTIL_DATE);
    }

    public boolean showMarkedForTransfer() {
        return getRenderedFilterFields().contains(VolumeSearchModel.Props.MARKED_FOR_TRANSFER);
    }

    public boolean showExportedForUam() {
        return getRenderedFilterFields().contains(VolumeSearchModel.Props.EXPORTED_FOR_UAM);
    }

    public boolean showExportedForUamDateTime() {
        return getRenderedFilterFields().contains(VolumeSearchModel.Props.EXPORTED_FOR_UAM_DATE_TIME);
    }

    public boolean showNextEvent() {
        return getRenderedFilterFields().contains(VolumeSearchModel.Props.NEXT_EVENT);
    }

    public boolean showMarkedForDestruction() {
        return getRenderedFilterFields().contains(VolumeSearchModel.Props.MARKED_FOR_DESTRUCTION);
    }

    public boolean showDisposalActCreated() {
        return getRenderedFilterFields().contains(VolumeSearchModel.Props.DISPOSAL_ACT_CREATED);
    }

    // rendering conditions for dialog buttons' confirmation messages

    public boolean isShowConfirmationMessage() {
        return isConfirmArchive() || isConfirmGenerateWordFile() || isConfirmMarkForTransfer() || isConfirmTransfer() || isConfirmExportToUam() || isConfirmComposeDisposalAct()
                || isConfirmStartDestruction() || isConfirmStartSimpleDestruction();
    }

    public boolean isConfirmArchive() {
        return false;
    }

    public boolean isConfirmMarkForTransfer() {
        return false;
    }

    public boolean isConfirmTransfer() {
        return false;
    }

    public boolean isConfirmExportToUam() {
        return false;
    }

    public boolean isConfirmComposeDisposalAct() {
        return false;
    }

    public boolean isConfirmStartDestruction() {
        return false;
    }

    public boolean isConfirmStartSimpleDestruction() {
        return false;
    }

    public String getGenerateWordFileConfirmationMessage() {
        return "";
    }

    public String getConfirmationMessage() {
        String message = "";
        if (isConfirmArchive()) {
            message = MessageUtil.getMessageAndEscapeJS("archivals_volume_archive_confirm");
        } else if (isConfirmGenerateWordFile()) {
            message = StringEscapeUtils.escapeJavaScript(getGenerateWordFileConfirmationMessage());
        } else if (isConfirmExportToUam()) {
            message = MessageUtil.getMessageAndEscapeJS("archivals_volume_export_to_uam_confirm");
        } else if (isConfirmMarkForTransfer()) {
            message = MessageUtil.getMessageAndEscapeJS("archivals_volume_mark_for_transfer_confirm");
        } else if (isConfirmTransfer()) {
            message = MessageUtil.getMessageAndEscapeJS("archivals_volume_transfer_confirm");
        } else if (isConfirmComposeDisposalAct()) {
            message = MessageUtil.getMessageAndEscapeJS("archivals_volume_compose_disposal_act_confirm");
        } else if (isConfirmStartDestruction()) {
            message = MessageUtil.getMessageAndEscapeJS("archivals_volume_start_destruction_confirm");
        } else if (isConfirmStartSimpleDestruction()) {
            message = MessageUtil.getMessageAndEscapeJS("archivals_volume_start_simple_destruction_confirm");
        }
        return message;
    }

    public String getConfirmationLinkId() {
        if (isConfirmArchive()) {
            return "volume-archive-after-confirmation-accepted-link";
        } else if (isConfirmGenerateWordFile()) {
            return "volume-generate-word-file-after-confirmation-accepted-link";
        } else if (isConfirmMarkForTransfer()) {
            return "volume-mark-for-transfer-after-confirmation-accepted-link";
        } else if (isConfirmExportToUam()) {
            return "volume-export-to-uam-after-confirmation-accepted-link";
        } else if (isConfirmTransfer()) {
            return "volume-transfer-after-confirmation-accepted-link";
        } else if (isConfirmComposeDisposalAct()) {
            return "volume-compose-disposal-act-after-confirmation-accepted-link";
        } else if (isConfirmStartDestruction()) {
            return "volume-start-destruction-after-confirmation-accepted-link";
        } else if (isConfirmStartSimpleDestruction()) {
            return "volume-start-simple-destruction-after-confirmation-accepted-link";
        }
        return null;
    }

    public void cancelAction(ActionEvent actionEvent) {
        setConfirmGenerateWordFile(false);
    }

    public boolean isConfirmGenerateWordFile() {
        return confirmGenerateWordFile;
    }

    public void generateWordFileConfirm() {
        if (checkVolumesSelected()) {
            setConfirmGenerateWordFile(true);
        }
    }

    protected List<NodeRef> getSelectedVolumes() {
        final List<NodeRef> volumesToArchive = new ArrayList<NodeRef>();
        for (Map.Entry<NodeRef, Boolean> entry : getListCheckboxes().entrySet()) {
            if (entry.getValue()) {
                volumesToArchive.add(entry.getKey());
            }
        }
        return volumesToArchive;
    }

    protected void setConfirmGenerateWordFile(boolean confirmGenerateWordFile) {
        this.confirmGenerateWordFile = confirmGenerateWordFile;
    }

    protected void addGenerateWordFileButton(List<DialogButtonConfig> buttons) {
        buttons.add(new DialogButtonConfig("volumeGenerateWordFileButton", null, "archivals_volume_generate_word_file", "#{DialogManager.bean.generateWordFileConfirm}", "false",
                null));
    }

}
