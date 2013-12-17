package ee.webmedia.alfresco.volume.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.privilege.web.AbstractInheritingPrivilegesHandler;
import ee.webmedia.alfresco.privilege.web.PrivilegesHandler;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * {@link PrivilegesHandler} for nodes of type {@link VolumeModel.Types#VOLUME}
 * 
 * @author Ats Uiboupin
 */
public class VolumeTypePrivilegesHandler extends AbstractInheritingPrivilegesHandler {
    private static final long serialVersionUID = 1L;
    private static Collection<String> SERIES_VOLUME_MANAGEABLE_PERMISSIONS;

    protected VolumeTypePrivilegesHandler() {
        super(VolumeModel.Types.VOLUME, getSeriesVolumePrivs());
    }

    /** used by series and volume permissions management */
    public static Collection<String> getSeriesVolumePrivs() {
        if (SERIES_VOLUME_MANAGEABLE_PERMISSIONS == null) {
            ArrayList<String> privs = new ArrayList<String>();
            if (BeanHelper.getVolumeService().isCaseVolumeEnabled()) {
                privs.add(Privileges.VIEW_CASE_FILE);
                privs.add(Privileges.EDIT_CASE_FILE);
            }
            privs.add(Privileges.VIEW_DOCUMENT_FILES);
            privs.add(Privileges.VIEW_DOCUMENT_META_DATA);
            privs.add(Privileges.EDIT_DOCUMENT);
            SERIES_VOLUME_MANAGEABLE_PERMISSIONS = Collections.unmodifiableList(privs);
        }
        return SERIES_VOLUME_MANAGEABLE_PERMISSIONS;
    }

    @Override
    public boolean isEditable() {
        if (super.isEditable()) {
            return true;
        }
        return isOwner();
    }

    private boolean isOwner() {
        return false; // regular volume doesn't have owner
    }
}