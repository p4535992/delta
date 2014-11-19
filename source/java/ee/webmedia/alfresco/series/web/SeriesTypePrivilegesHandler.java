<<<<<<< HEAD
package ee.webmedia.alfresco.series.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;

import java.util.Collection;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.privilege.web.PrivilegesHandler;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
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
    public boolean isSubmitWhenCheckboxUnchecked() {
        return false;
    }

    @Override
    public String getConfirmMessage() {
        String key = checkboxValue ? "series_manage_permissions_confirm_docsVisibleChanged" : "series_manage_permissions_confirm_docsNotVisibleChanged";
        return MessageUtil.getMessageAndEscapeJS(key);
    }

    @Override
    protected boolean initCheckboxValue() {
        return BeanHelper.getSeriesService().getSeriesByNodeRef(state.getManageableRef()).isDocumentsVisibleForUsersWithoutAccess();
    }

    @Override
    public boolean isPermissionColumnDisabled(String privilege) {
        if (Privileges.VIEW_CASE_FILE.equals(privilege) || Privileges.EDIT_CASE_FILE.equals(privilege)) {
            NodeRef seriesRef = state.getManageableRef();
            @SuppressWarnings("unchecked")
            Collection<String> volumeType = (Collection<String>) getNodeService().getProperty(seriesRef, SeriesModel.Props.VOL_TYPE);
            return !volumeType.contains(VolumeType.CASE_FILE.name());
        }
        return false;
    }

    @Override
    public void save() {
        if (checkboxValueBeforeSave != checkboxValue) {
            SeriesService seriesService = BeanHelper.getSeriesService();
            Series series = seriesService.getSeriesByNodeRef(state.getManageableRef());
            series.setDocumentsVisibleForUsersWithoutAccess(checkboxValue);
            seriesService.saveOrUpdate(series);
        }
    }
=======
package ee.webmedia.alfresco.series.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;

import java.util.Collection;

import javax.faces.event.ValueChangeEvent;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.privilege.web.PrivilegesHandler;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.web.VolumeTypePrivilegesHandler;

/**
 * {@link PrivilegesHandler} for nodes of type {@link SeriesModel.Types#SERIES}
 */
public class SeriesTypePrivilegesHandler extends PrivilegesHandler {
    private static final long serialVersionUID = 1L;

    protected SeriesTypePrivilegesHandler() {
        super(SeriesModel.Types.SERIES, VolumeTypePrivilegesHandler.getSeriesVolumePrivs());
    }

    @Override
    protected boolean initCheckboxValue() {
        /**
         * „Lugemisõiguseta kasutajad näevad dokumentide olemasolu“ – documentsVisibleForUsersWithoutAccess ;
         * vaikimisi true (sarja property)
         */
        // FIXME PRIV2 - unimplemented. when implemented, remove wrapper element from jsp:
        // <h:panelGroup id="removeMeWhenImplemented" ...>
        return true;
    }

    @Override
    /** FIXME PRIV2 - vaja realiseerida selle asemel hoops getCheckboxValue() ja checkboxChanged(boolean) meetodid */
    public void checkboxChanged(ValueChangeEvent e) {
        super.checkboxChanged(e);
        MessageUtil.addErrorMessage("unimplemented: series checkboxChanged");
        return;
    }

    @Override
    public boolean isSubmitWhenCheckboxUnchecked() {
        return false;
    }

    @Override
    public boolean isPermissionColumnDisabled(String privilege) {
        if (Privileges.VIEW_CASE_FILE.equals(privilege) || Privileges.EDIT_CASE_FILE.equals(privilege)) {
            NodeRef seriesRef = state.getManageableRef();
            @SuppressWarnings("unchecked")
            Collection<String> volumeType = (Collection<String>) getNodeService().getProperty(seriesRef, SeriesModel.Props.VOL_TYPE);
            if (!volumeType.contains(VolumeType.CASE_FILE.name())) {
                return true;
            }
        }
        return false;
    }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}