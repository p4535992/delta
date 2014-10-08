<<<<<<< HEAD
package ee.webmedia.alfresco.menu.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemFilter;
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
        MenuItemFilter filter = null;
        Map<String, MenuItemFilter> menuItemFilters = menuService.getMenuItemFilters();
        if (dd.getChildFilter() != null && menuItemFilters != null && menuItemFilters.containsKey(dd.getChildFilter())) {
            filter = menuItemFilters.get(dd.getChildFilter());
        }

        QName type = nodeService.getType(nodeRef);
        if (filter != null) {
            String result = filter.openItemActionsForType(dd, nodeRef, type);
            if (result == null) {
                return null;
            }
        } else {
            if (type.equals(FunctionsModel.Types.FUNCTION)) {
                ((SeriesListDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), SeriesListDialog.BEAN_NAME)).showAll(nodeRef);
            } else if (type.equals(SeriesModel.Types.SERIES)) {
                ((VolumeListDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), VolumeListDialog.BEAN_NAME)).showAll(nodeRef);
                return null;
            }
        }

        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef);
        List<NodeRef> children = new ArrayList<NodeRef>(childAssocs.size());
        for (ChildAssociationRef caRef : childAssocs) {
            if (filter != null) {
                if (!filter.passesFilter(dd, caRef.getChildRef())) {
                    continue; // Skip this item if it doesn't pass the filter
                }

                children.add(caRef.getChildRef());
                continue;
            }
            // When we have no filter lets add the child
            children.add(caRef.getChildRef());
        }
        return children;
    }

    @Override
    public void setupTreeItem(DropdownMenuItem dd, NodeRef nodeRef) {
        QName type = nodeService.getType(nodeRef);
        String title = "";
        String orderString = "";
        if (type.equals(FunctionsModel.Types.FUNCTION)) {
            title = nodeService.getProperty(nodeRef, FunctionsModel.Props.MARK) + " " + nodeService.getProperty(nodeRef, FunctionsModel.Props.TITLE);
            dd.setOutcome("dialog:seriesListDialog");

            Integer order = (Integer) nodeService.getProperty(nodeRef, FunctionsModel.Props.ORDER);
            if (order == null) {
                order = 0;
            }
            orderString = StringUtils.leftPad(String.valueOf(Math.abs(order)), ((order.intValue() < 0) ? 6 : 5), '0');
            orderString += " " + nodeService.getProperty(nodeRef, FunctionsModel.Props.MARK);
            orderString += " " + nodeService.getProperty(nodeRef, FunctionsModel.Props.TITLE);
        } else if (type.equals(SeriesModel.Types.SERIES)) {
            String containingDocsCount = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, SeriesModel.Props.CONTAINING_DOCS_COUNT));
            if (containingDocsCount == null) {
                containingDocsCount = "?";
            }
            title = nodeService.getProperty(nodeRef, SeriesModel.Props.SERIES_IDENTIFIER) + " " + nodeService.getProperty(nodeRef, SeriesModel.Props.TITLE)
                    + " (" + containingDocsCount + ")";
            dd.setOutcome("dialog:volumeListDialog");

            Integer order = (Integer) nodeService.getProperty(nodeRef, SeriesModel.Props.ORDER);
            if (order == null) {
                order = 0;
            }
            orderString = StringUtils.leftPad(String.valueOf(Math.abs(order)), ((order.intValue() < 0) ? 6 : 5), '0');
            orderString += " " + nodeService.getProperty(nodeRef, SeriesModel.Props.SERIES_IDENTIFIER);
            orderString += " " + nodeService.getProperty(nodeRef, SeriesModel.Props.TITLE);
        }
        dd.setActionListener("#{MenuBean.updateTree}");
        dd.setNodeRef(nodeRef);
        dd.setBrowse(true);
        dd.setTitle(title);
        dd.setTransientOrderString(orderString);
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
=======
package ee.webmedia.alfresco.menu.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.service.MenuService.MenuItemFilter;
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
        MenuItemFilter filter = null;
        Map<String, MenuItemFilter> menuItemFilters = menuService.getMenuItemFilters();
        if (dd.getChildFilter() != null && menuItemFilters != null && menuItemFilters.containsKey(dd.getChildFilter())) {
            filter = menuItemFilters.get(dd.getChildFilter());
        }

        QName type = nodeService.getType(nodeRef);
        if (filter != null) {
            String result = filter.openItemActionsForType(dd, nodeRef, type);
            if (result == null) {
                return null;
            }
        } else {
            if (type.equals(FunctionsModel.Types.FUNCTION)) {
                ((SeriesListDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), SeriesListDialog.BEAN_NAME)).showAll(nodeRef);
            } else if (type.equals(SeriesModel.Types.SERIES)) {
                ((VolumeListDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), VolumeListDialog.BEAN_NAME)).showAll(nodeRef);
                return null;
            }
        }

        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef);
        List<NodeRef> children = new ArrayList<NodeRef>(childAssocs.size());
        for (ChildAssociationRef caRef : childAssocs) {
            if (filter != null) {
                if (!filter.passesFilter(dd, caRef.getChildRef())) {
                    continue; // Skip this item if it doesn't pass the filter
                }

                children.add(caRef.getChildRef());
                continue;
            }
            // When we have no filter lets add the child
            children.add(caRef.getChildRef());
        }
        return children;
    }

    @Override
    public void setupTreeItem(DropdownMenuItem dd, NodeRef nodeRef) {
        QName type = nodeService.getType(nodeRef);
        String title = "";
        String orderString = "";
        if (type.equals(FunctionsModel.Types.FUNCTION)) {
            title = nodeService.getProperty(nodeRef, FunctionsModel.Props.MARK) + " " + nodeService.getProperty(nodeRef, FunctionsModel.Props.TITLE);
            dd.setOutcome("dialog:seriesListDialog");

            Integer order = (Integer) nodeService.getProperty(nodeRef, FunctionsModel.Props.ORDER);
            if (order == null) {
                order = 0;
            }
            orderString = StringUtils.leftPad(String.valueOf(Math.abs(order)), ((order.intValue() < 0) ? 6 : 5), '0');
            orderString += " " + nodeService.getProperty(nodeRef, FunctionsModel.Props.MARK);
            orderString += " " + nodeService.getProperty(nodeRef, FunctionsModel.Props.TITLE);
        } else if (type.equals(SeriesModel.Types.SERIES)) {
            String containingDocsCount = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, SeriesModel.Props.CONTAINING_DOCS_COUNT));
            if (containingDocsCount == null) {
                containingDocsCount = "?";
            }
            title = nodeService.getProperty(nodeRef, SeriesModel.Props.SERIES_IDENTIFIER) + " " + nodeService.getProperty(nodeRef, SeriesModel.Props.TITLE)
                    + " (" + containingDocsCount + ")";
            dd.setOutcome("dialog:volumeListDialog");

            Integer order = (Integer) nodeService.getProperty(nodeRef, SeriesModel.Props.ORDER);
            if (order == null) {
                order = 0;
            }
            orderString = StringUtils.leftPad(String.valueOf(Math.abs(order)), ((order.intValue() < 0) ? 6 : 5), '0');
            orderString += " " + nodeService.getProperty(nodeRef, SeriesModel.Props.SERIES_IDENTIFIER);
            orderString += " " + nodeService.getProperty(nodeRef, SeriesModel.Props.TITLE);
        }
        dd.setActionListener("#{MenuBean.updateTree}");
        dd.setNodeRef(nodeRef);
        dd.setBrowse(true);
        dd.setTitle(title);
        dd.setTransientOrderString(orderString);
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
>>>>>>> develop-5.1
