package ee.webmedia.alfresco.archivals.web;

import java.util.*;

import javax.faces.event.ActionEvent;

import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.archivals.model.ActivityStatus;
import ee.webmedia.alfresco.archivals.model.ActivityType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.eventplan.model.FirstEvent;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

public class WaitingForDestructionVolumeListDialog extends VolumeArchiveBaseDialog {

    private static final long serialVersionUID = 1L;
    private boolean confirmComposeDisposalAct;
    private boolean confirmStartDestruction;
    private boolean confirmStartSimpleDestruction;

    protected static ComparatorChain comparator;

    static {
        ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new TransformingComparator(new ComparableTransformer<Volume>() {
            @Override
            public Comparable<String> tr(Volume volume) {
                return volume.getRetaintionDescription();
            }
        }, new NullComparator(AppConstants.getNewCollatorInstance())));
        chain.addComparator(TransferringToUamVolumeListDialog.comparator);
        comparator = chain;
    }

    @Override
    protected void initFilterItems() {
        super.initFilterItems();
        getFilter().getAspects().add(VolumeSearchModel.Aspects.PLANNED_DESTRUCTION);
        checkDestructionParametersEmpty();
    }
        
    protected void checkDestructionParametersEmpty() {
    	ParametersService parametersService = BeanHelper.getParametersService();
    	
	    String beginTimeStr = StringUtils.deleteWhitespace(parametersService.getStringParameter(Parameters.DESTRUCTION_BEGIN_TIME));
	    String endTimeStr = StringUtils.deleteWhitespace(parametersService.getStringParameter(Parameters.DESTRUCTION_END_TIME));
	    String overweekendTimeStr = StringUtils.deleteWhitespace(parametersService.getStringParameter(Parameters.CONTINUE_DESTRUCTION_OVER_WEEKEND));
    	
	    if (StringUtils.isBlank(beginTimeStr) || StringUtils.isBlank(endTimeStr) || StringUtils.isBlank(overweekendTimeStr)) {
	    	MessageUtil.addErrorMessage("archivals_volume_start_destruction_error_destruction_parameters_are_empty");
	    	return;
	    }
    }

    @Override
    protected void searchVolumes() {
        volumes = BeanHelper.getDocumentSearchService().searchVolumesForArchiveList(filter, false, true, storeNodeRefs);
    }

    @Override
    protected Comparator<Volume> getComparator() {
        return comparator;
    }

    public String getVolumeListTitle() {
        return MessageUtil.getMessage("archivals_volume_waiting_destruction_list_title");
    }

    @Override
    public boolean isShowExportedForUamDateTimeColumn() {
        return true;
    }

    @Override
    public boolean isShowRetaintionColumns() {
        return true;
    }

    @Override
    public boolean isShowDestructionColumns() {
        return true;
    }

    @Override
    public boolean isShowTransferringDeletingNextEventColumn() {
        return true;
    }

    @Override
    protected List<String> getRenderedFilterFields() {
        if (renderedFilterFields == null) {
            renderedFilterFields = new ArrayList<String>(Arrays.asList(
                    VolumeSearchModel.Props.MARKED_FOR_DESTRUCTION.toPrefixString(),
                    VolumeSearchModel.Props.DISPOSAL_ACT_CREATED.toPrefixString(),
                    VolumeSearchModel.Props.EXPORTED_FOR_UAM_DATE_TIME.toPrefixString(),
                    VolumeSearchModel.Props.EXPORTED_FOR_UAM_DATE_TIME_END_DATE.toPrefixString(),
                    VolumeSearchModel.Props.NEXT_EVENT_DATE.toPrefixString(),
                    VolumeSearchModel.Props.NEXT_EVENT_DATE_END_DATE.toPrefixString(),
                    VolumeSearchModel.Props.HAS_ARCHIVAL_VALUE.toPrefixString(),
                    VolumeSearchModel.Props.RETAIN_PERMANENT.toPrefixString(),
                    VolumeSearchModel.Props.EVENT_PLAN.toPrefixString()));
            if (showStoreFilterField()) {
                renderedFilterFields.add(VolumeSearchModel.Props.STORE.toPrefixString());
            }
            if (showNextEventFilterField()) {
                renderedFilterFields.add(VolumeSearchModel.Props.NEXT_EVENT.toPrefixString());
            }
        }
        return renderedFilterFields;
    }

    @Override
    public boolean isShowStore() {
        return BeanHelper.getGeneralService().hasAdditionalArchivalStores();
    }

    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        List<DialogButtonConfig> buttons = new ArrayList<DialogButtonConfig>(2);
        buttons.add(new DialogButtonConfig("volumeComposeDisposalActButton", null, "archivals_volume_compose_disposal_act", "#{DialogManager.bean.composeDisposalActConfirm}",
                "false", null));
        buttons.add(new DialogButtonConfig("volumeStartDestructionButton", null, "archivals_volume_start_destruction", "#{DialogManager.bean.startDestructionConfirm}",
                "false", null));
        if (BeanHelper.getArchivalsService().isSimpleDestructionEnabled()) {
            buttons.add(new DialogButtonConfig("volumeStartSimpleDestructionButton", null, "archivals_volume_start_simple_destruction",
                    "#{DialogManager.bean.startSimpleDestructionConfirm}", "false", null));
        }
        return buttons;
    }

    @Override
    public void cancelAction(ActionEvent actionEvent) {
        confirmComposeDisposalAct = false;
        confirmStartDestruction = false;
        confirmStartSimpleDestruction = false;
    }

    public void composeDisposalActConfirm() {
        if (checkVolumesSelected()) {
            confirmComposeDisposalAct = true;
        }
    }

    public void composeDisposalAct(ActionEvent event) {
        confirmComposeDisposalAct = false;
        NodeRef activityRef = generateActivityAndExcelFile(ActivityType.DISPOSAL_ACT_DOC,
                "archivals_volume_compose_disposal_act_template", ActivityStatus.IN_PROGRESS, null);
        if (activityRef != null) {
            BeanHelper.getArchivalsService().setDisposalActCreated(getSelectedVolumes(), activityRef);
            MessageUtil.addInfoMessage("archivals_volume_compose_disposal_act_success");
        }
    }

    @Override
    public boolean isConfirmComposeDisposalAct() {
        return confirmComposeDisposalAct;
    }

    public void startDestructionConfirm() {
        if (checkVolumesSelected()) {
            confirmStartDestruction = true;
        }
    }

    public void startDestruction(ActionEvent event) {
        confirmStartDestruction = false;
        disposeVolumes("archivals_volume_start_destruction_template", ActivityType.DESTRUCTION,
                "archivals_volume_start_destruction_success");
    }

    private void disposeVolumes(final String template, final ActivityType activityType, final String successMsgKey) {
        BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() {
                List<NodeRef> selectedVolumes = getSelectedVolumes();
                
                boolean simpleDestruction = false;
                if(activityType.equals(ActivityType.SIMPLE_DESTRUCTION)){
                	simpleDestruction = true;
                }
                if (!validateVolumesForDisposal(selectedVolumes, simpleDestruction)) {
                    return null;
                }
                
                NodeRef activityRef = generateActivityAndExcelFile(activityType, template, ActivityStatus.WAITING, null);
                
                if (activityRef != null) {
	                for (NodeRef volumeRef : selectedVolumes) {
	                	BeanHelper.getArchivalsService().addVolumeOrCaseToDestructingList(volumeRef, activityRef);
	                }
                } else {
                	// warn user about failure!
                	return null;
                }
                
                MessageUtil.addInfoMessage(successMsgKey, selectedVolumes.size(), BeanHelper.getArchivalsService().getNonFinishedDestructionActivities());
 
                return null;
            }
        });
    }

    private boolean validateVolumesForDisposal(List<NodeRef> selectedVolumes, boolean simpleDestruction) {
        VolumeService volumeService = BeanHelper.getVolumeService();
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        for (NodeRef volumeRef : selectedVolumes) {
            Volume volume = volumeService.getVolumeByNodeRef(volumeRef, propertyTypes);
            
            if(volume.isMarkedForDestruction()) {
            	MessageUtil.addErrorMessage("archivals_volume_start_destruction_error_already_in_destruction_queue");
            	return false;
            }
            
            if (!(volume.isAppraised() && StringUtils.isNotBlank(volume.getArchivingNote()))) {
                MessageUtil.addErrorMessage("archivals_volume_start_destruction_error_no_appraise_or_arch_note");
                return false;
            }
            if (!simpleDestruction && !volume.isDisposalActCreated()) {
                MessageUtil.addErrorMessage("archivals_volume_start_destruction_error_no_disposal_act");
                return false;
            }

            FirstEvent nextEvent = getNextEvent(volume);
            
            if(volume.isRetainPermanent()==false && volume.isHasArchivalValue()==false) {
            	
            	Date retainUntil = volume.getRetainUntilDate();
            	if( retainUntil != null && new Date().before(retainUntil)) {
            		MessageUtil.addErrorMessage("archivals_volume_start_destruction_error_retention_not_expired_yet");
            		return false;
            	}
            	
	            boolean notSimpleDestructionEvent = FirstEvent.SIMPLE_DESTRUCTION != nextEvent;
	            if ((FirstEvent.DESTRUCTION != nextEvent && notSimpleDestructionEvent)
	                    || (simpleDestruction && notSimpleDestructionEvent)) {
	                MessageUtil.addErrorMessage("archivals_volume_start_destruction_error_wrong_next_event");
	                return false;
	            }
            }
            
            if(FirstEvent.DESTRUCTION.equals(nextEvent) && simpleDestruction){
            	 MessageUtil.addErrorMessage("archivals_volume_start_destruction_error_simple_destruction_not_allowed");
	             return false;
            }
            
        }
        return true;
    }
    
    private FirstEvent getNextEvent(Volume volume){
    	String nextEventStr = volume.getNextEvent();
        return StringUtils.isNotBlank(nextEventStr) ? FirstEvent.valueOf(nextEventStr) : null;
    }

    @Override
    public boolean isConfirmStartDestruction() {
        return confirmStartDestruction;
    }

    public void startSimpleDestructionConfirm() {
        if (checkVolumesSelected()) {
            confirmStartSimpleDestruction = true;
        }
    }

    public void startSimpleDestruction(ActionEvent event) {
        confirmStartSimpleDestruction = false;
        disposeVolumes("archivals_volume_start_simple_destruction_template", ActivityType.SIMPLE_DESTRUCTION,
                "archivals_volume_start_simple_destruction_success");
    }

    @Override
    public boolean isConfirmStartSimpleDestruction() {
        return confirmStartSimpleDestruction;
    }

}
