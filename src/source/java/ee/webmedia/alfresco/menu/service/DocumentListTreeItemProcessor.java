package ee.webmedia.alfresco.menu.service;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;

import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.service.MenuService.TreeItemProcessor;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.web.SeriesListDialog;
import ee.webmedia.alfresco.volume.web.VolumeListDialog;

public class DocumentListTreeItemProcessor implements TreeItemProcessor {

    private NodeService nodeService;
    private MenuService menuService;
    
    public void init() {
        menuService.setTreeItemProcessor(this);
    }
    
    @Override
    public List<NodeRef> openTreeItem(DropdownMenuItem dd, NodeRef nodeRef) {
        QName type = nodeService.getType(nodeRef);
        if (type.equals(FunctionsModel.Types.FUNCTION)) {
            ((SeriesListDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), SeriesListDialog.BEAN_NAME)).showAll(nodeRef);
        } else if (type.equals(SeriesModel.Types.SERIES)) {
            ((VolumeListDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), VolumeListDialog.BEAN_NAME)).showAll(nodeRef);
            return null;
        }
        
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef);
        List<NodeRef> children = new ArrayList<NodeRef>(childAssocs.size());
        for(ChildAssociationRef caRef : childAssocs) {
            children.add(caRef.getChildRef());
        }
        return children;
    }

    @Override
    public void setupTreeItem(DropdownMenuItem dd, NodeRef nodeRef) {
        QName type = nodeService.getType(nodeRef);
        String title = "";
        if (type.equals(FunctionsModel.Types.FUNCTION)) {
            title = nodeService.getProperty(nodeRef, FunctionsModel.Props.MARK) + " " + nodeService.getProperty(nodeRef, FunctionsModel.Props.TITLE);
            dd.setOutcome("dialog:seriesListDialog");
        } else if (type.equals(SeriesModel.Types.SERIES)) {
            title = nodeService.getProperty(nodeRef, SeriesModel.Props.SERIES_IDENTIFIER) + " " + nodeService.getProperty(nodeRef, SeriesModel.Props.TITLE);
            dd.setOutcome("dialog:volumeListDialog");
        }
        dd.setTitle(title);
    }
    
    // START: getters / setters
    
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }
    // END: getters / setters

    

}
