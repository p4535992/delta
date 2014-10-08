<<<<<<< HEAD
package ee.webmedia.alfresco.menu.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.app.servlet.ajax.InvokeCommand.ResponseMimetype;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.ui.MenuBean;

public class MenuItemCountBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(MenuItemCountBean.class);

    public static final String BEAN_NAME = "MenuItemCountBean";
    public static final String MENU_ITEM_ID_PARAM = "menuItemId";

    private transient MenuService menuService;
    private MenuBean menuBean;

    private final Map<String, MenuItemCountVO> map = new HashMap<String, MenuItemCountBean.MenuItemCountVO>();

    public static class MenuItemCountVO implements Serializable {
        private static final long serialVersionUID = 1L;

        public int count;
        public long updated;
    }

    public long getUpdateTimeout() {
        return 60000;
    }

    public Integer getCount(String menuItemId) {
        MenuItemCountVO countVO = map.get(menuItemId);
        if (countVO == null) {
            if (!map.containsKey(menuItemId)) {
                map.put(menuItemId, null);
            }
            return null;
        }
        return countVO.count;
    }

    public Map<String, Long> getNextUpdates() {
        long now = System.currentTimeMillis();
        Map<String, Long> nextUpdates = new HashMap<String, Long>();
        for (Entry<String, MenuItemCountVO> entry : map.entrySet()) {
            // Delayed update disabled - updating is always started immediately after page is rendered
            // if (entry.getValue() == null) {
            nextUpdates.put(entry.getKey(), 0L); // update immediately
            // } else {
            // long timeRemainingToUpdate = entry.getValue().updated + getUpdateTimeout() - now;
            // if (timeRemainingToUpdate < 0) {
            // timeRemainingToUpdate = 0;
            // }
            // nextUpdates.put(entry.getKey(), timeRemainingToUpdate);
            // }
        }
        return nextUpdates;
    }

    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void updateCount() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        @SuppressWarnings("unchecked")
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        String menuItemId = params.get(MENU_ITEM_ID_PARAM);
        Assert.hasLength(menuItemId, "menuItemId was not found in request");

        MenuItem menuItem = findMenuItemById(menuItemId, getMenuBean().getMenu().getSubItems());
        Assert.notNull(menuItem, "No MenuItem found with menuItemId=" + menuItemId);

        MenuItemCountVO countVO = map.get(menuItemId);
        if (countVO == null) {
            countVO = new MenuItemCountVO();
            map.put(menuItemId, countVO);
        }
        MenuItemCountHandler countHandler = getMenuService().getCountHandler(menuItemId);
        Assert.notNull(countHandler, "MenuItemCountHandler does not exist for menuItemId=" + menuItemId);

        long startTime = System.currentTimeMillis();
        countVO.count = countHandler.getCount(menuItem);
        countVO.updated = System.currentTimeMillis();
        log.debug("PERFORMANCE: MenuItemCountHandler " + menuItemId + " - " + (countVO.updated - startTime) + " ms");

        ResponseWriter out = context.getResponseWriter();
        out.write(Integer.toString(countVO.count));
    }

    /**
     * Find MenuItem by id. Returns only the first match. If no match is found, returns {@code null}.
     */
    private static MenuItem findMenuItemById(String menuItemId, List<MenuItem> menuItems) {
        if (menuItems == null) {
            return null;
        }
        for (MenuItem menuItem : menuItems) {
            if (StringUtils.equals(menuItem.getId(), menuItemId)) {
                return menuItem;
            }
            MenuItem result = findMenuItemById(menuItemId, menuItem.getSubItems());
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    // START: getters / setters

    public MenuService getMenuService() {
        if (menuService == null) {
            menuService = (MenuService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(MenuService.BEAN_NAME);
        }
        return menuService;
    }

    public MenuBean getMenuBean() {
        if (menuBean == null) {
            menuBean = (MenuBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MenuBean.BEAN_NAME);
        }
        return menuBean;
    }

    // END: getters / setters
}
=======
package ee.webmedia.alfresco.menu.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.web.app.servlet.ajax.InvokeCommand.ResponseMimetype;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.menu.model.MenuItem;
import ee.webmedia.alfresco.menu.service.MenuItemCountHandler;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.parameters.model.Parameters;

public class MenuItemCountBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(MenuItemCountBean.class);

    public static final String BEAN_NAME = "MenuItemCountBean";
    public static final String MENU_ITEM_ID_PARAM = "menuItemId";

    private transient MenuService menuService;
    private MenuBean menuBean;
    private Integer maxSearchResultRows;

    private final Map<String, MenuItemCountVO> map = new HashMap<String, MenuItemCountBean.MenuItemCountVO>();

    public static class MenuItemCountVO implements Serializable, Cloneable {
        private static final long serialVersionUID = 1L;

        public Integer count;
        public long updated;
        public boolean exceedsMaxSearchResultRows;

        public MenuItemCountVO() {
            // Default constructor
        }

        private MenuItemCountVO(MenuItemCountVO other) {
            count = other.count;
            updated = other.updated;
            exceedsMaxSearchResultRows = other.exceedsMaxSearchResultRows;
        }

        @Override
        public MenuItemCountVO clone() {
            return new MenuItemCountVO(this);
        }
    }

    public long getUpdateTimeout() {
        return 60000;
    }

    public MenuItemCountVO getCount(String menuItemId) {
        MenuItemCountVO countVO = map.get(menuItemId);
        if (countVO == null) {
            countVO = new MenuItemCountVO();
            if (!map.containsKey(menuItemId)) {
                map.put(menuItemId, countVO);
            }
        }
        return countVO.clone();
    }

    public Map<String, Long> getNextUpdates() {
        long now = System.currentTimeMillis();
        Map<String, Long> nextUpdates = new HashMap<String, Long>();
        for (Entry<String, MenuItemCountVO> entry : map.entrySet()) {
            // Delayed update disabled - updating is always started immediately after page is rendered
            // if (entry.getValue() == null) {
            nextUpdates.put(entry.getKey(), 0L); // update immediately
            // } else {
            // long timeRemainingToUpdate = entry.getValue().updated + getUpdateTimeout() - now;
            // if (timeRemainingToUpdate < 0) {
            // timeRemainingToUpdate = 0;
            // }
            // nextUpdates.put(entry.getKey(), timeRemainingToUpdate);
            // }
        }
        return nextUpdates;
    }

    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void updateCount() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        String menuItemId = params.get(MENU_ITEM_ID_PARAM);
        Assert.hasLength(menuItemId, "menuItemId was not found in request");

        MenuItem menuItem = findMenuItemById(menuItemId, getMenuBean().getMenu().getSubItems());
        Assert.notNull(menuItem, "No MenuItem found with menuItemId=" + menuItemId);

        MenuItemCountVO countVO = map.get(menuItemId);
        if (countVO == null) {
            countVO = new MenuItemCountVO();
            map.put(menuItemId, countVO);
        }
        MenuItemCountHandler countHandler = getMenuService().getCountHandler(menuItemId);
        Assert.notNull(countHandler, "MenuItemCountHandler does not exist for menuItemId=" + menuItemId);

        long startTime = System.currentTimeMillis();
        // Skip further updates if the number of items during first request in this session is greater than maxSearchResultRows parameter.
        countVO.exceedsMaxSearchResultRows = countVO.count != null && countVO.count >= getMaxSearchResultRows();
        countVO.count = countVO.exceedsMaxSearchResultRows ? getMaxSearchResultRows() : countHandler.getCount(menuItem);
        countVO.updated = System.currentTimeMillis();
        log.debug("PERFORMANCE: MenuItemCountHandler " + menuItemId + (countVO.exceedsMaxSearchResultRows ? " update skipped" : "") + " - " + (countVO.updated - startTime) + " ms");
        if (MenuItem.MY_TASK_MENU_ITEMS.contains(menuItemId)) {
            log.debug("Updated count of menu_my_tasks subitem " + menuItemId + " to " + countVO.count);
        }

        String result = Integer.toString(countVO.count);
        if (countVO.exceedsMaxSearchResultRows) {
            result += "+";
        }
        context.getResponseWriter().write(result);
    }

    /**
     * Find MenuItem by id. Returns only the first match. If no match is found, returns {@code null}.
     */
    private static MenuItem findMenuItemById(String menuItemId, List<MenuItem> menuItems) {
        if (menuItems == null) {
            return null;
        }
        for (MenuItem menuItem : menuItems) {
            if (StringUtils.equals(menuItem.getId(), menuItemId)) {
                return menuItem;
            }
            MenuItem result = findMenuItemById(menuItemId, menuItem.getSubItems());
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    // START: getters / setters

    public MenuService getMenuService() {
        if (menuService == null) {
            menuService = BeanHelper.getMenuService();
        }
        return menuService;
    }

    public MenuBean getMenuBean() {
        if (menuBean == null) {
            menuBean = BeanHelper.getMenuBean();
        }
        return menuBean;
    }

    public Integer getMaxSearchResultRows() {
        if (maxSearchResultRows == null) {
            maxSearchResultRows = BeanHelper.getParametersService().getLongParameter(Parameters.MAX_SEARCH_RESULT_ROWS).intValue();
        }
        return maxSearchResultRows;
    }

    public Map<String, MenuItemCountVO> getMap() {
        return map;
    }

    // END: getters / setters
}
>>>>>>> develop-5.1
