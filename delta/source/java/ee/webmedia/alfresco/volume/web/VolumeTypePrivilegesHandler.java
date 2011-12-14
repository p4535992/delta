package ee.webmedia.alfresco.volume.web;

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

    protected VolumeTypePrivilegesHandler() {
        super(VolumeModel.Types.VOLUME, PrivilegesHandler.DEFAULT_MANAGEABLE_PERMISSIONS);
    }

}