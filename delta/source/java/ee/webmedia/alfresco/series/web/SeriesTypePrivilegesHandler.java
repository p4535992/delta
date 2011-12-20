package ee.webmedia.alfresco.series.web;

import javax.faces.event.ValueChangeEvent;

import ee.webmedia.alfresco.privilege.web.PrivilegesHandler;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.web.VolumeTypePrivilegesHandler;

/**
 * {@link PrivilegesHandler} for nodes of type {@link SeriesModel.Types#SERIES}
 * 
 * @author Ats Uiboupin
 */
public class SeriesTypePrivilegesHandler extends PrivilegesHandler {
    private static final long serialVersionUID = 1L;

    protected SeriesTypePrivilegesHandler() {
        super(SeriesModel.Types.SERIES, VolumeTypePrivilegesHandler.getSeriesVolumePrivs());
    }

    @Override
    public Boolean getCheckboxValue() {
        // FIXME ALSeadist Ats - unimplemented. when implemented, remove wrapper element from jsp:
        // <h:panelGroup id="removeMeWhenImplemented" ...>
        return null;
        // return checkboxValue;
    }

    @Override
    protected void checkboxChanged(boolean newValue) {
        MessageUtil.addErrorMessage("unimplmented checkboxChanged"); // FIXME PRIV2 Ats
    }

    @Override
    /** FIXME ALSeadist Ats - vaja realiseerida selle asemel hoops getCheckboxValue() ja checkboxChanged(boolean) meetodid */
    public void checkboxChanged(ValueChangeEvent e) {
        MessageUtil.addErrorMessage("unimplemented: checkboxChanged");
        return;
    }

    @Override
    public boolean isSubmitWhenCheckboxUnchecked() {
        return false;
    }

}