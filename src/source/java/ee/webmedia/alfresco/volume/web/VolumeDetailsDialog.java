package ee.webmedia.alfresco.volume.web;

import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

public class VolumeDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private static final String PARAM_SERIES_NODEREF = "seriesNodeRef";
    private static final String PARAM_VOLUME_NODEREF = "volumeNodeRef";
    private transient VolumeService volumeService;
    private Volume currentEntry;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        getVolumeService().saveOrUpdate(currentEntry);
        resetFields();
        return outcome;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    // START: jsf actions/accessors
    public void showDetails(ActionEvent event) {
        String volumeNodeRef = ActionUtil.getParam(event, PARAM_VOLUME_NODEREF);
        currentEntry = getVolumeService().getVolumeByNoderef(volumeNodeRef);
    }

    public void addNewVolume(ActionEvent event) {
        NodeRef volumeNodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_SERIES_NODEREF));
        // create new node for currentEntry
        currentEntry = getVolumeService().createVolume(volumeNodeRef);
    }

    public Node getCurrentNode() {
        return currentEntry.getNode();
    }

    public String close() {
        if (!isClosed()) {
            getVolumeService().closeVolume(currentEntry);
            return getDefaultFinishOutcome();
        }
        return null;
    }

    public boolean isClosed() {
        final String currentStatus = (String) getCurrentNode().getProperties().get(VolumeModel.Props.STATUS.toString());
        final boolean closed = DocListUnitStatus.CLOSED.equals(currentStatus);
        return closed;
    }

    // END: jsf actions/accessors

    private void resetFields() {
        currentEntry = null;
    }

    // START: getters / setters
    protected VolumeService getVolumeService() {
        if (volumeService == null) {
            volumeService = (VolumeService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(VolumeService.BEAN_NAME);
        }
        return volumeService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    // END: getters / setters
}
