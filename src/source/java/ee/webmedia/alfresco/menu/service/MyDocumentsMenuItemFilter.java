package ee.webmedia.alfresco.menu.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;

import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemFilter;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.web.SeriesListDialog;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.volume.web.VolumeListDialog;

public class MyDocumentsMenuItemFilter implements MenuItemFilter {

    private UserService userService;
    private NodeService nodeService;
    private Integer currentUsersStructUnitId;

    @Override
    public boolean passesFilter(MenuItem menuItem, NodeRef childNodeRef) {
        if (!(menuItem instanceof DropdownMenuItem))
            return false;

        NodeRef nodeRef = ((DropdownMenuItem) menuItem).getNodeRef();
        if (nodeRef != null && nodeService.getType(nodeRef).equals(FunctionsModel.Types.FUNCTION)) {
            
            @SuppressWarnings("unchecked")
            List<Integer> structUnits = (List<Integer>) nodeService.getProperty(childNodeRef, SeriesModel.Props.STRUCT_UNIT);
            
            return structUnits.contains(getCurrentUsersStructUnitId());
        }
        if (nodeRef == null && ((DropdownMenuItem) menuItem).getXPath() != null) // The very first link defined in menu-structure.xml should always return true
            return true;

        return false;
    }

    @Override
    public String openItemActionsForType(DropdownMenuItem dd, NodeRef nodeRef, QName type) {
        
        if (type.equals(FunctionsModel.Types.FUNCTION)) {
            SeriesListDialog seriesListDialog = (SeriesListDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), SeriesListDialog.BEAN_NAME);
            seriesListDialog.showAllForStructUnit(nodeRef, getCurrentUsersStructUnitId());
            seriesListDialog.setDisableActions(true);
        } else if (type.equals(SeriesModel.Types.SERIES)) {
            ((VolumeListDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), VolumeListDialog.BEAN_NAME)).showAll(nodeRef);
            return null;
        }
        
        return "done";
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public Integer getCurrentUsersStructUnitId() {
        if(currentUsersStructUnitId == null) {
            Map<QName, Serializable> userProperties = userService.getUserProperties(AuthenticationUtil.getRunAsUser());
            currentUsersStructUnitId = Integer.parseInt(userProperties.get(ContentModel.PROP_ORGID).toString());
        }
        return currentUsersStructUnitId;
    }

    public void setCurrentUsersStructUnitId(Integer currentUsersStructUnitId) {
        this.currentUsersStructUnitId = currentUsersStructUnitId;
    }
}
