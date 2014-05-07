package ee.webmedia.alfresco.volume.web;

import java.util.Arrays;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.privilege.web.AbstractInheritingPrivilegesHandler;
import ee.webmedia.alfresco.privilege.web.PrivilegesHandler;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * {@link PrivilegesHandler} for nodes of type {@link VolumeModel.Types#VOLUME}
 */
public class VolumeTypePrivilegesHandler extends AbstractInheritingPrivilegesHandler {
    private static final long serialVersionUID = 1L;

    protected VolumeTypePrivilegesHandler() {
        super(VolumeModel.Types.VOLUME, Arrays.asList(Privileges.VIEW_DOCUMENT_FILES, Privileges.VIEW_DOCUMENT_META_DATA, Privileges.EDIT_DOCUMENT));
    }

    @Override
    public String getConfirmMessage() {
        NodeService nodeService = BeanHelper.getNodeService();
        NodeRef seriesRef = nodeService.getPrimaryParent(state.getManageableRef()).getParentRef();
        if (Boolean.FALSE.equals(nodeService.getProperty(seriesRef, SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS))) {
            return MessageUtil.getMessageAndEscapeJS("volume_manage_permissions_confirm_docsVisibleInheritChanged");
        }
        return super.getConfirmMessage();
    }
}