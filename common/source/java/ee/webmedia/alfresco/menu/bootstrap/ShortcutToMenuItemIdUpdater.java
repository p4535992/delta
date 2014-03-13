package ee.webmedia.alfresco.menu.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.menu.model.Menu;
import ee.webmedia.alfresco.menu.model.MenuModel;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.ui.MenuBean;

/**
 * Changes shortcut to menuItemId (CL task 158558)
 */
public class ShortcutToMenuItemIdUpdater extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ShortcutToMenuItemIdUpdater.class);

    private MenuService menuService;
    private Menu menu;

    @Override
    protected void executeInternal() throws Throwable {
        menuService.reload();
        menu = menuService.getMenu();
        super.executeInternal();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = generateTypeQuery(ContentModel.TYPE_PERSON);
        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
        String userName = (String) props.get(ContentModel.PROP_USERNAME);
        @SuppressWarnings("unchecked")
        List<String> oldShortcuts = (List<String>) props.get(MenuModel.Props.SHORTCUTS);
        ArrayList<String> newShortcuts = null;
        StringBuilder s = new StringBuilder();
        if (oldShortcuts != null) {
            newShortcuts = new ArrayList<String>(oldShortcuts.size());
            for (String oldShortcut : oldShortcuts) {
                if (s.length() > 0) {
                    s.append(", ");
                }
                s.append(oldShortcut).append(" -> ");

                // MenuItem 'externalReviewTasks' with index 0_0_4 was added in 2.2, so menuItems below that were moved by 1 index
                String[] path = MenuBean.getPathFromShortcut(oldShortcut);
                if (path.length > 2 && "0".equals(path[0]) && "0".equals(path[1])) {
                    int pathLevel2 = Integer.parseInt(path[2]);
                    if (pathLevel2 >= 4) {
                        path[2] = Integer.toString(pathLevel2 + 1);
                        oldShortcut = MenuBean.getShortcutFromPath(path);
                        s.append(oldShortcut).append(" -> ");
                    }
                }

                String newShortcut = MenuBean.getMenuItemIdFromShortcut(oldShortcut, menu);
                s.append(newShortcut);
                if (newShortcut != null) {
                    newShortcuts.add(newShortcut);
                }
            }
            nodeService.setProperty(nodeRef, MenuModel.Props.SHORTCUTS, newShortcuts);
        }
        return new String[] {
                userName,
                oldShortcuts == null ? "null" : Integer.toString(oldShortcuts.size()),
                newShortcuts == null ? "null" : Integer.toString(newShortcuts.size()),
                s.toString() };
    }

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

}
