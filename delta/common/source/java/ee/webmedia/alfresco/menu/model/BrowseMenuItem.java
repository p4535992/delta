package ee.webmedia.alfresco.menu.model;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.servlet.FacesHelper;
import org.springframework.web.jsf.FacesContextUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.ui.component.MenuItemWrapper;
import ee.webmedia.alfresco.menu.ui.component.YahooTreeItem;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * @author Kaarel Jõgeva
 */
@XStreamAlias("browse")
public class BrowseMenuItem extends MenuItem {

    @XStreamOmitField
    private static final long serialVersionUID = 0L;
    @XStreamAsAttribute
    @XStreamAlias("show-children-count")
    private boolean showChildrenCount = true;

    public BrowseMenuItem() {
        super();
    }

    @Override
    public UIComponent createComponent(FacesContext context, String id, UserService userService, WorkflowService workflowService) {
        if (isRestricted() && !hasPermissions(userService)) {
            return null;
        }

        if (isExternalReview() && !isExternalReviewEnabled(workflowService)) {
            return null;
        }

        NodeRef nodeRef = getNodeRefForXPath(context, getOutcome());
        if (nodeRef == null) {
            return null;
        }

        YahooTreeItem ytItem = (YahooTreeItem) context.getApplication().createComponent(YahooTreeItem.class.getCanonicalName());
        FacesHelper.setupComponentId(context, ytItem, null);
        ytItem.setNodeRef(nodeRef);
        if (isShowChildrenCount()) {
            ytItem.setChildrenCount(getNodeChildrenCount(context, nodeRef));
        } else {
            ytItem.setShowChildrenCount(false);
        }

        if (getTitle() == null) {
            setTitle(I18NUtil.getMessage(getTitleId()));
        }
        ytItem.setTitle(getTitle());

        MenuItemWrapper wrap = (MenuItemWrapper) context.getApplication().createComponent(MenuItemWrapper.class.getCanonicalName());
        FacesHelper.setupComponentId(context, wrap, null);
        wrap.setPlain(true);
        @SuppressWarnings("unchecked")
        List<UIComponent> children = wrap.getChildren();
        children.add(ytItem);

        return wrap;
    }

    /**
     * Gets the NodeRef for the node specified by provided XPath
     * 
     * @param XPath path to the requested node
     * @return NodeRef
     */
    private NodeRef getNodeRefForXPath(FacesContext context, String XPath) {
        MenuService menuService = (MenuService) FacesContextUtils.getRequiredWebApplicationContext(context).getBean(
                MenuService.BEAN_NAME);
        return menuService.getNodeRefForXPath(context, XPath);

    }

    /**
     * Returns the number of sub-folders for provided node
     * 
     * @param nodeRef NodeRef of the parent node whose child count is needed
     * @return number of children
     */
    private int getNodeChildrenCount(FacesContext context, NodeRef nodeRef) {
        MenuService menuService = (MenuService) FacesContextUtils.getRequiredWebApplicationContext(context).getBean(
                MenuService.BEAN_NAME);
        return menuService.getNodeChildrenCount(nodeRef);
    }

    public boolean isShowChildrenCount() {
        return showChildrenCount;
    }

    public void setShowChildrenCount(boolean showChildrenCount) {
        this.showChildrenCount = showChildrenCount;
    }
}
