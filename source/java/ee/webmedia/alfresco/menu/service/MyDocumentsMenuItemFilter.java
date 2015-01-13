package ee.webmedia.alfresco.menu.service;

import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Repository;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemFilter;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.web.SeriesListDialog;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.volume.web.VolumeListDialog;

public class MyDocumentsMenuItemFilter implements MenuItemFilter {

    private transient UserService userService;
    private transient NodeService nodeService;

    @Override
    public boolean passesFilter(MenuItem menuItem, NodeRef childNodeRef) {
        if (!(menuItem instanceof DropdownMenuItem)) {
            return false;
        }

        NodeRef nodeRef = ((DropdownMenuItem) menuItem).getNodeRef();
        if (nodeRef != null && nodeService.getType(nodeRef).equals(FunctionsModel.Types.FUNCTION)) {
            return isCurrentUsersSeries(childNodeRef);
        }
        if (nodeRef == null && ((DropdownMenuItem) menuItem).getXPath() != null && nodeService.getType(childNodeRef).equals(FunctionsModel.Types.FUNCTION)) {
            List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(childNodeRef);
            for (ChildAssociationRef caRef : childAssocs) {
                if (isCurrentUsersSeries(caRef.getChildRef())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Decides whether currently logged in users department is mentioned in series meta-data.
     * 
     * @param nodeRef
     * @return
     */
    private boolean isCurrentUsersSeries(NodeRef nodeRef) {
        @SuppressWarnings("unchecked")
        List<Integer> structUnits = (List<Integer>) nodeService.getProperty(nodeRef, SeriesModel.Props.STRUCT_UNIT);
        return structUnits != null && structUnits.contains(getCurrentUsersStructUnitId());
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

    private Integer getCurrentUsersStructUnitId() {
        return getUserService().getCurrentUsersStructUnitId();
    }

    // START - getters/setters

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    protected NodeService getNodeService() {
        if (nodeService == null) {
            nodeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
        }
        return nodeService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    // END - getters/setters
}
