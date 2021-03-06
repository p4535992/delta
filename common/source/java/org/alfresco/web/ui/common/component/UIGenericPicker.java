/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * and Open Source Software ("FLOSS") applications as described in Alfresco's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.ui.common.component;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAuthorityService;

import java.io.IOException;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.model.SelectItem;

import org.alfresco.web.app.Application;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.ajax.AjaxUpdateable;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Kevin Roast
 */
public class UIGenericPicker extends UICommand implements AjaxUpdateable {
    /** action ids */
    private final static int ACTION_NONE = -1;
    private final static int ACTION_SEARCH = 0;
    public final static int ACTION_CLEAR = 1;
    private final static int ACTION_FILTER = 2;
    private final static int ACTION_ADD = 3;

    /** form field postfixes */
    private final static String FIELD_FILTER = "_filter";
    private final static String FIELD_CONTAINS = "_contains";
    private final static String FIELD_RESULTS = "_results";
    private final static String FIELD_SELECTED_GROUP = "_selectedGroup";
    private final static String FIELD_GROUP_SELECTOR = "_groupSelector";
    private final static String FIELD_SELECTED_GROUP_TEXT = "_selectedGroupText";

    /** I18N message strings */
    private final static String MSG_SEARCH = "search";
    private final static String MSG_CLEAR = "clear";
    private final static String MSG_MODAL_SEARCH_LIMITED = "modal_search_limited";
    private final static String MSG_MODAL_SEARCH_USERGROUP_SELECTED = "modal_search_usergroup_selected";
    private final static String MSG_MODAL_SEARCH_SELECT_USERGROUP = "modal_search_select_usergroup";
    private final static String MSG_ADD = "add";
    private final static String MSG_RESULTS1 = "results_contains";
    private final static String MSG_RESULTS2 = "results_contains_filter";

    private final static int DEFAULT_HEIGHT = 100;
    private final static int DEFAULT_WIDTH = 250;
    private static final int DEFAULT_SIZE = 0;
    private final static int MAX_SIZE = 21;
    private final static int MIN_SIZE = 2;

    private MethodBinding queryCallback = null;
    private Boolean showFilter = null;
    private Boolean showContains = null;
    private Boolean showAddButton = null;
    private Boolean showSelectButton = Boolean.FALSE;
    private Boolean filterByTaskOwnerStructUnit = Boolean.FALSE;
    private Boolean filterRefresh = null;
    private Boolean multiSelect = null;
    private String addButtonLabel;
    private Integer width = null;
    private Integer height = null;
    private Integer size = null;

    private SelectItem[] filters = null;
    private int filterIndex = UserContactGroupSearchBean.USERS_FILTER;
    private int defaultFilterIndex = 0;
    private String contains = "";
    private String selectedUsergroup = "";
    private String[] selectedResults = null;
    private SelectItem[] currentResults = null;

    // ------------------------------------------------------------------------------
    // Component implementation

    /**
     * Default constructor
     */
    public UIGenericPicker() {
        setRendererType(null);
    }

    /**
     * @see javax.faces.component.UIComponent#getFamily()
     */
    @Override
    public String getFamily() {
        return "org.alfresco.faces.GenericPicker";
    }

    /**
     * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
     */
    @Override
    public void restoreState(FacesContext context, Object state) {
        Object values[] = (Object[]) state;
        // standard component attributes are restored by the super class
        super.restoreState(context, values[0]);
        showFilter = (Boolean) values[1];
        showContains = (Boolean) values[2];
        showAddButton = (Boolean) values[3];
        addButtonLabel = (String) values[4];
        width = (Integer) values[5];
        height = (Integer) values[6];
        filterIndex = (Integer) values[7];
        contains = (String) values[8];
        queryCallback = (MethodBinding) restoreAttachedState(context, values[9]);
        selectedResults = (String[]) values[10];
        currentResults = (SelectItem[]) values[11];
        filters = (SelectItem[]) values[12];
        filterRefresh = (Boolean) values[13];
        multiSelect = (Boolean) values[14];
        size = (Integer) values[15];
        defaultFilterIndex = (Integer) values[16];
        selectedUsergroup = (String) values[17];
        showSelectButton = (Boolean) values[18];
        filterByTaskOwnerStructUnit = (Boolean) values[19];
    }

    /**
     * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
     */
    @Override
    public Object saveState(FacesContext context) {
        Object values[] = new Object[20];
        // standard component attributes are saved by the super class
        values[0] = super.saveState(context);
        values[1] = showFilter;
        values[2] = showContains;
        values[3] = showAddButton;
        values[4] = addButtonLabel;
        values[5] = width;
        values[6] = height;
        values[7] = filterIndex;
        values[8] = contains;
        values[9] = saveAttachedState(context, queryCallback);
        values[10] = selectedResults;
        values[11] = currentResults;
        values[12] = filters;
        values[13] = filterRefresh;
        values[14] = multiSelect;
        values[15] = size;
        values[16] = defaultFilterIndex;
        values[17] = selectedUsergroup;
        values[18] = showSelectButton;
        values[19] = filterByTaskOwnerStructUnit;
        return (values);
    }

    /**
     * @see javax.faces.component.UIComponentBase#decode(javax.faces.context.FacesContext)
     */
    @Override
    public void decode(FacesContext context) {
        Map requestMap = context.getExternalContext().getRequestParameterMap();
        Map valuesMap = context.getExternalContext().getRequestParameterValuesMap();
        String fieldId = getHiddenFieldName();
        String value = (String) requestMap.get(fieldId);
        String selectedGroup = (String) requestMap.get(fieldId + FIELD_SELECTED_GROUP);

        int action = ACTION_NONE;
        if (value != null && value.length() != 0) {
            // decode the values - we are expecting an action identifier
            action = Integer.parseInt(value);
        }
        if (action == ACTION_NONE) {
            return; // no need to create an unnecessary event
        }

        // we always process these values to keep the component up-to-date

        // now find the Filter drop-down value
        int filterIndex = defaultFilterIndex;
        String strFilterIndex = (String) requestMap.get(fieldId + FIELD_FILTER);
        if (strFilterIndex != null && strFilterIndex.length() != 0) {
            filterIndex = Integer.parseInt(strFilterIndex);
        }

        // and the Contains text box value
        String contains = (String) requestMap.get(fieldId + FIELD_CONTAINS);

        // and the Results selections
        String[] results = (String[]) valuesMap.get(fieldId + FIELD_RESULTS);

        // queue an event
        PickerEvent event = new PickerEvent(this, action, filterIndex, contains, results, selectedGroup);
        queueEvent(event);
    }

    /**
     * @see javax.faces.component.UIComponentBase#broadcast(javax.faces.event.FacesEvent)
     */
    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException {
        if (event instanceof PickerEvent) {
            PickerEvent pickerEvent = (PickerEvent) event;

            // set component state from event properties
            selectedResults = pickerEvent.Results;

            // delegate to appropriate action logic
            switch (pickerEvent.Action) {
            case ACTION_ADD:
                // We could use filterIndex instance field, BUT when using AjaxSearchBean.searchPickerResults the filter and results are
                // modified client-side, so that ACTION_SEARCH event never happens - that's why we have to get filterIndex from current event
                filterIndex = pickerEvent.FilterIndex;
                contains = pickerEvent.Contains;
                // call super for actionlistener execution
                // it's up to the handler to get the results from the getSelectedResults() method
                super.broadcast(event);
                break;

            case ACTION_CLEAR:
                contains = "";
                filterIndex = defaultFilterIndex;
                selectedResults = null;
                currentResults = null;
                selectedUsergroup = "";
                break;

            case ACTION_FILTER:
                // filter changed then query with new settings
                selectedUsergroup = "";
                //$FALL-THROUGH$
            case ACTION_SEARCH:
                // We want to get "filterIndex" and "contains" values that were used while performing the search,
                // not the ones that user may have modified later
                filterIndex = pickerEvent.FilterIndex;
                contains = pickerEvent.Contains;
                if (pickerEvent.Action != ACTION_FILTER && StringUtils.isNotBlank(pickerEvent.UserGroupSearch)) {
                    selectedUsergroup = pickerEvent.UserGroupSearch;
                }

                // query with current settings
                MethodBinding callback = getQueryCallback();
                if (callback != null) {
                    // use reflection to execute the query callback method and retrieve results
                    Object result = callback.invoke(getFacesContext(),
                            new Object[] { new PickerSearchParams(filterIndex, contains.trim(),
                                    BeanHelper.getParametersService().getLongParameter(Parameters.MAX_MODAL_SEARCH_RESULT_ROWS).intValue(), selectedUsergroup,
                                    filterByTaskOwnerStructUnit) });

                    if (result instanceof SelectItem[]) {
                        currentResults = (SelectItem[]) result;
                    } else {
                        currentResults = null;
                    }
                }
                break;
            }
        } else {
            super.broadcast(event);
        }
    }

    /**
     * @see javax.faces.component.UIComponentBase#encodeBegin(javax.faces.context.FacesContext)
     */
    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (isRendered() == false) {
            return;
        }

        ResponseWriter out = context.getResponseWriter();

        ResourceBundle bundle = Application.getBundle(context);

        String clientId = getClientId(context);

        // start outer table
        out.write("<table id=\"");
        out.write(getAjaxClientId(context));
        out.write("\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\" class=\"generic-picker\">");

        // top row
        out.write("<tr valign=\"top\"><td>");

        // filter drop-down
        String filterId = clientId + FIELD_FILTER;
        if (getShowFilter() == true) {
            out.write("<select class=\"genericpicker-filter ff-margin-right-2\" name='");
            out.write(filterId);
            out.write("' id='");
            out.write(filterId);
            out.write("' size='1'");

            // apply onchange Form submit here if component attributes require it
            if (getFilterRefresh() == true) {
                out.write(" onchange=\"");
                out.write(generateFormSubmit(context, ACTION_FILTER, 0));
                out.write("\"");
            }

            out.write(">");

            // output filter options
            SelectItem[] items = getFilterOptions();
            if (items != null) {
                for (SelectItem item : items) {
                    out.write("<option value=\"");
                    // I checked and it seems that we are using integers as values everywhere.
                    // When this is not the case this cast will fail fast.
                    Integer value = (Integer) item.getValue();
                    out.write(value.toString());
                    // Since select values aren't 0-based integer lists anymore, the selected attribute must be assigned regarding the value.
                    if (filterIndex != value) {
                        out.write("\">");
                    } else {
                        out.write("\" selected=\"true\">");
                    }
                    out.write(Utils.encode(item.getLabel()));
                    out.write("</option>");
                }
            }

            out.write("</select>");
        }

        // Contains textbox
        if (getShowContains()) {
            out.write("<input");
            String pickerCallback = (String) getAttributes().get(Search.PICKER_CALLBACK_KEY);
            if (StringUtils.isBlank(pickerCallback)) {
                pickerCallback = getQueryCallback().getExpressionString();
            }
            if (StringUtils.isNotBlank(pickerCallback)) {
                if (pickerCallback.contains("#{")) {
                    pickerCallback = pickerCallback.substring("#{".length(), pickerCallback.length() - 1);
                }
                out.write(" datasrc='");
                if (!getShowFilter()) {
                    out.write(filterIndex + "|");
                }
                out.write(pickerCallback + "|" + filterByTaskOwnerStructUnit + "'");
            }
            out.write(" name='");
            out.write(clientId + FIELD_CONTAINS);
            out.write("' type='text' class=\"genericpicker-input focus\" maxlength='256' style='width:200px !important;' value=\"");
            out.write(Utils.encode(contains));
            out.write("\">&nbsp;");
        }

        // a hidden field for selected usergroup
        // For some reason, if type=hidden, then the value field was sent to the browser, but the browser cleared it, so no value was sent back
        out.write("<input type='text' id='");
        out.write(clientId + FIELD_SELECTED_GROUP);
        out.write("' name='");
        out.write(clientId + FIELD_SELECTED_GROUP);
        out.write("' value='");
        out.write(selectedUsergroup);
        out.write("' style='display:none;'>");

        // Search button
        out.write("<button class=\"specificAction\" type=\"button\" onclick=\"");
        out.write(generateFormSubmit(context, ACTION_SEARCH, 0));
        out.write("\">");
        out.write(Utils.encode(bundle.getString(MSG_SEARCH)));
        out.write("</button>");
        out.write("</td></tr>");

        int size = getSize();
        int resultRowLimit = BeanHelper.getParametersService().getLongParameter(Parameters.MAX_MODAL_SEARCH_RESULT_ROWS).intValue();
        out.write("<tr><td class=\"modalResultsLimited");
        if (size != resultRowLimit) {
            out.write(" hidden");
        }
        out.write("\">");
        out.write(Utils.encode(MessageUtil.getMessage(MSG_MODAL_SEARCH_LIMITED, resultRowLimit)));
        out.write("</td></tr>");

        // information row
        if (currentResults != null && getShowContains() == true) {
            out.write("<tr><td>");
            out.write("<a href='#' onclick=\"");
            out.write(generateFormSubmit(context, ACTION_CLEAR, 0));
            out.write("\">");
            out.write(Utils.encode(bundle.getString(MSG_CLEAR)));
            out.write("</a></td></tr>");
        }

        // results list row
        out.write("<tr");
        if (size < 1) {
            out.write(" class='hidden'");
        }
        out.write("><td>");
        out.write("<select class=\"genericpicker-results\" size=\"" + getSize() + "\"");
        out.write(" style='width:100%;height:auto;' name='");
        out.write(clientId + FIELD_RESULTS);
        out.write("' id='");
        out.write(clientId + FIELD_RESULTS);
        out.write("'");
        if (getMultiSelect() == true) {
            out.write(" multiple=\"multiple\"");
        }
        out.write(" data-rowlimit=\"" + resultRowLimit + "\"");
        out.write(">");

        // results
        ComponentUtil.renderSelectItems(out, currentResults);

        // end results list
        out.write("</select>");
        out.write("</td></tr>");

        if (!StringUtils.isBlank(selectedUsergroup)) {
            out.write("<tr><td><span id=\"" + clientId + FIELD_SELECTED_GROUP_TEXT + "\" style=\"font-weight:bold\">");
            out.write(Utils.encode(MessageUtil.getMessage(MSG_MODAL_SEARCH_USERGROUP_SELECTED, getAuthorityService().getAuthorityDisplayName(selectedUsergroup))));
            out.write("</span></td></tr>");
        }

        // help text
        if (getMultiSelect() == true) {
            out.write("<tr");
            if (size < 1) {
                out.write(" class='hidden'");
            }
            out.write("><td>");
            out.write(Utils.encode(bundle.getString("help_select_multiple_rows")));
            out.write("</td></tr>");
        }

        // bottom row - add button
        if (getShowAddButton() == true) {
            out.write("<tr");
            if (size < 1) {
                out.write(" class='hidden'");
            }
            out.write("><td>");
            out.write("<input class=\"picker-add\" type='submit' value='");
            String msg = getAddButtonLabel();
            if (msg == null || msg.length() == 0) {
                msg = bundle.getString(MSG_ADD);
            }
            out.write(Utils.encode(msg));
            out.write("' onclick=\"");
            int ajaxParentLevel = 1;
            Integer addition = (Integer) getParent().getAttributes().get(Search.AJAX_PARENT_LEVEL_KEY);
            if (addition != null) {
                ajaxParentLevel += addition;
            }
            out.write(generateFormSubmit(context, ACTION_ADD, ajaxParentLevel));
            out.write("\">");
            // select group for search
            if (showSelectButton != null && showSelectButton) {
                out.write("&nbsp;<input type='submit' id='" + clientId + FIELD_GROUP_SELECTOR + "'");
                out.write("disabled='disabled'");
                out.write(" value='");
                out.write(Utils.encode(MessageUtil.getMessage(MSG_MODAL_SEARCH_SELECT_USERGROUP)));
                out.write("' onclick=\"");
                out.write("selectGroupForModalSearch('" + clientId + FIELD_RESULTS + "', '" + clientId + FIELD_SELECTED_GROUP + "', '" + filterId + "'); "
                        + generateFormSubmit(context, ACTION_SEARCH, 0));
                out.write("\">");
                out.write("<script language=\"javascript\" type=\"text/javascript\">");
                out.write("$jQ(document).ready(function() { $jQ('#'+escapeId4JQ('" + clientId + FIELD_FILTER + "')).change(groupModalFilterChange);});");
                out.write("</script>");
                out.write("</td></tr>");
            }
        }

        // end outer table
        out.write("</table>");
    }

    /**
     * @return the filter options
     */
    public SelectItem[] getFilterOptions() {
        if (filters == null) {
            ValueBinding vb = getValueBinding("filters");
            if (vb != null) {
                filters = (SelectItem[]) vb.getValue(getFacesContext());
            }
        }

        return filters;
    }

    /**
     * @return current filter drop-down selected index value
     */
    public int getFilterIndex() {
        return filterIndex;
    }

    public void setDefaultFilterIndex(int defaultFilterIndex) {
        this.defaultFilterIndex = defaultFilterIndex;
        filterIndex = defaultFilterIndex;
    }
    
    public void setFilters(SelectItem[] filters) {
    	this.filters = filters;
    }

    /**
     * @return Returns the addButtonLabel.
     */
    public String getAddButtonLabel() {
        ValueBinding vb = getValueBinding("addButtonLabel");
        if (vb != null) {
            addButtonLabel = (String) vb.getValue(getFacesContext());
        }

        return addButtonLabel;
    }

    /**
     * @param addButtonLabel The addButtonLabel to set.
     */
    public void setAddButtonLabel(String addButtonLabel) {
        this.addButtonLabel = addButtonLabel;
    }

    /**
     * @return Returns the showAddButton.
     */
    public boolean getShowAddButton() {
        ValueBinding vb = getValueBinding("showAddButton");
        if (vb != null) {
            showAddButton = (Boolean) vb.getValue(getFacesContext());
        }

        return showAddButton != null ? showAddButton.booleanValue() : true;
    }

    /**
     * @param showAddButton The showAddButton to set.
     */
    public void setShowAddButton(boolean showAddButton) {
        this.showAddButton = Boolean.valueOf(showAddButton);
    }

    /**
     * @return Returns the showContains.
     */
    public boolean getShowContains() {
        ValueBinding vb = getValueBinding("showContains");
        if (vb != null) {
            showContains = (Boolean) vb.getValue(getFacesContext());
        }

        return showContains != null ? showContains.booleanValue() : true;
    }

    /**
     * @param showContains The showContains to set.
     */
    public void setShowContains(boolean showContains) {
        this.showContains = Boolean.valueOf(showContains);
    }

    /**
     * @return Returns the showFilter.
     */
    public boolean getShowFilter() {
        ValueBinding vb = getValueBinding("showFilter");
        if (vb != null) {
            showFilter = (Boolean) vb.getValue(getFacesContext());
        }

        return showFilter != null ? showFilter.booleanValue() : true;
    }

    /**
     * @param showFilter The showFilter to set.
     */
    public void setShowFilter(boolean showFilter) {
        this.showFilter = Boolean.valueOf(showFilter);
    }

    /**
     * @return Returns the filterRefresh.
     */
    public boolean getFilterRefresh() {
        ValueBinding vb = getValueBinding("filterRefresh");
        if (vb != null) {
            filterRefresh = (Boolean) vb.getValue(getFacesContext());
        }

        return filterRefresh != null ? filterRefresh.booleanValue() : false;
    }

    /**
     * @param filterRefresh The filterRefresh to set.
     */
    public void setFilterRefresh(boolean filterRefresh) {
        this.filterRefresh = Boolean.valueOf(filterRefresh);
    }

    /**
     * @return true if multi select should be enabled.
     */
    public boolean getMultiSelect() {
        ValueBinding vb = getValueBinding("multiSelect");
        if (vb != null) {
            multiSelect = (Boolean) vb.getValue(getFacesContext());
        }

        return multiSelect != null ? multiSelect.booleanValue() : true;
    }

    /**
     * @param multiSelect Flag to determine whether multi select is enabled
     */
    public void setMultiSelect(boolean multiSelect) {
        this.multiSelect = Boolean.valueOf(multiSelect);
    }

    /**
     * @return Returns the width.
     */
    public int getWidth() {
        ValueBinding vb = getValueBinding("width");
        if (vb != null) {
            width = (Integer) vb.getValue(getFacesContext());
        }

        return width != null ? width.intValue() : DEFAULT_WIDTH;
    }

    /**
     * @param width The width to set.
     */
    public void setWidth(int width) {
        this.width = Integer.valueOf(width);
    }

    /**
     * @return Returns the height.
     */
    public int getHeight() {
        ValueBinding vb = getValueBinding("height");
        if (vb != null) {
            height = (Integer) vb.getValue(getFacesContext());
        }

        return height != null ? height.intValue() : DEFAULT_HEIGHT;
    }

    /**
     * @param height The height to set.
     */
    public void setHeight(int height) {
        this.height = Integer.valueOf(height);
    }

    /**
     * @return Returns the size.
     */
    public int getSize() {
        ValueBinding vb = getValueBinding("size");
        if (vb != null) {
            size = (Integer) vb.getValue(getFacesContext());
        }

        if (currentResults != null) {
            return getResultSize(currentResults);
        }

        return size != null ? size.intValue() : DEFAULT_SIZE;
    }

    public static int getResultSize(SelectItem[] items) {
        if (items != null) {
            if (items.length > MAX_SIZE) {
                return MAX_SIZE;
            }

            if (items.length < MIN_SIZE) {
                return MIN_SIZE;
            }

            return items.length;
        }

        return DEFAULT_SIZE;
    }

    /**
     * @param size The size to set.
     */
    public void setSize(int size) {
        this.size = Integer.valueOf(size);
    }

    /**
     * @return Returns the queryCallback.
     */
    public MethodBinding getQueryCallback() {
        return queryCallback;
    }

    /**
     * @param binding The queryCallback MethodBinding to set.
     */
    public void setQueryCallback(MethodBinding binding) {
        queryCallback = binding;
    }

    /**
     * @return The selected results. An array of whatever string objects were attached to the
     *         SelectItem[] objects supplied as the result of the picker query.
     */
    public String[] getSelectedResults() {
        return selectedResults;
    }

    @Override
    public String getAjaxClientId(FacesContext context) {
        return getClientId(context);
    }

    // ------------------------------------------------------------------------------
    // Private helpers

    /**
     * We use a hidden field per picker instance on the page.
     *
     * @return hidden field name
     */
    private String getHiddenFieldName() {
        return getClientId(getFacesContext());
    }

    /**
     * Generate FORM submit JavaScript for the specified action
     *
     * @param context FacesContext
     * @param action Action index
     * @return FORM submit JavaScript
     */
    private String generateFormSubmit(FacesContext context, int action, int parentLevel) {
        return ComponentUtil.generateAjaxFormSubmit(context, this, getHiddenFieldName(), Integer.toString(action), parentLevel);
    }

    public void setShowSelectButton(Boolean showSelectButton) {
        this.showSelectButton = showSelectButton;
    }

    public void setFilterByTaskOwnerStructUnit(Boolean filterByTaskOwnerStructUnit) {
        this.filterByTaskOwnerStructUnit = filterByTaskOwnerStructUnit;
    }

    // ------------------------------------------------------------------------------
    // Inner classes

    /**
     * Class representing the an action relevant to the Generic Selector component.
     */
    @SuppressWarnings("serial")
    public static class PickerEvent extends ActionEvent {
        public PickerEvent(UIComponent component, int action, int filterIndex, String contains, String[] results, String userGroupSearch) {
            super(component);
            Action = action;
            FilterIndex = filterIndex;
            Contains = contains;
            Results = results;
            UserGroupSearch = userGroupSearch;
        }

        public PickerEvent(UIComponent component, int action, int filterIndex, String contains, String[] results) {
            this(component, action, filterIndex, contains, results, null);
        }

        public String UserGroupSearch;
        public int Action;
        public int FilterIndex;
        public String Contains;
        public String[] Results;
    }
}
