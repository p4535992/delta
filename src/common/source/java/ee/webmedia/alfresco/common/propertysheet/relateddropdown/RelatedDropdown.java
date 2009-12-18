package ee.webmedia.alfresco.common.propertysheet.relateddropdown;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Selection (dropdown) component that can depend on other components of this type.<br>
 * Values for this component are given using <code>selectionItems</code> attribute.<br>
 * The order how one component depends on another is determined using <code>order</code>(That must be integer).<br>
 * Only component that has the value of <code>order</code> larger by 1 and the same <code>group</code> will be populated with values.<br>
 * Component with larger <code>order</code> values will be disabled and made empty, components with smaller <code>order</code> will not be modified.
 * 
 * @author Ats Uiboupin
 */
public class RelatedDropdown extends HtmlSelectOneMenu {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(RelatedDropdown.class);
    public Integer order;
    public String group;
    public String selectionItems;
    public static String CHANGE_MARKER = "CHANGE_MARKER";

    public RelatedDropdown() {
    }

    @Override
    public void decode(FacesContext context) {
        super.decode(context);
        @SuppressWarnings("unchecked")
        final Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        final String test = requestMap.get(getFilledFieldId(context));
        if (StringUtils.equals(CHANGE_MARKER, test)) {
            final String submittedValue = (String) getSubmittedValue();
            final String group = this.group;
            Integer order = this.order;
            if (StringUtils.isBlank(submittedValue)) {
                log.debug("Submitted value is empty: '" + submittedValue + "'");
            }
            queueEventToRelatedComponents(submittedValue, group, order);
        }
    }

    private void queueEventToRelatedComponents(final String submittedValue, final String group, final Integer order) {
        final UIPropertySheet propertySheet = ComponentUtil.getAncestorComponent(this, UIPropertySheet.class, true);
        final List<? extends RelatedDropdown> relatedDropDownInstances = ComponentUtil.findInputsByClass(propertySheet, this.getClass());
        for (RelatedDropdown relatedDropdown : relatedDropDownInstances) {
            queueEvent(new RelatedSelectEvent(relatedDropdown, group, order, submittedValue));
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        setOnchange(getOnChangeJS(context));
        super.encodeBegin(context);
    }

    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException {
        FacesContext context = FacesContext.getCurrentInstance();
        if (event instanceof RelatedSelectEvent) {
            RelatedSelectEvent rEvent = (RelatedSelectEvent) event;
            final boolean groupsEqual = StringUtils.equals(rEvent.group, group);
            if (groupsEqual) {
                if (rEvent.order + 1 == order) {
                    clearValues();
                    addSelectionItems(context, rEvent.submittedValue);
                } else if (rEvent.order + 1 < order) {
                    clearValues();
                }
            }
        } else {
            super.broadcast(event);
        }
    }

    void clearValues() {
        @SuppressWarnings("unchecked")
        List<UIComponent> selectOptions = this.getChildren();
        selectOptions.removeAll(selectOptions);
        this.setSubmittedValue("");
        this.setDisabled(true);
        UISelectItem selectItem = (UISelectItem) FacesContext.getCurrentInstance().getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
        selectItem.setItemLabel("");
        selectItem.setItemValue("");
        @SuppressWarnings("unchecked")
        final List<UIComponent> children = this.getChildren();
        children.add(selectItem);

    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] state = new Object[4];
        state[0] = super.saveState(context);
        state[1] = order;
        state[2] = group;
        state[3] = selectionItems;
        return state;
    }

    @Override
    public void restoreState(FacesContext context, Object savedState) {
        Object[] state = (Object[]) savedState;
        super.restoreState(context, state[0]);
        order = (Integer) state[1];
        group = (String) state[2];
        selectionItems = (String) state[3];

    }

    private void addSelectionItems(FacesContext context, Object submittedValue) {
        this.setDisabled(false);
        if (submittedValue instanceof String) {
            MethodBinding mb = context.getApplication().createMethodBinding(selectionItems,
                    new Class[] { FacesContext.class, HtmlSelectOneMenu.class, Object.class });
            try {
                mb.invoke(context, new Object[] { context, this, submittedValue });
            } catch (ClassCastException e) {
                throw new RuntimeException("Failed to get values for selection from '" + selectionItems + "'", e);
            }
        } else {
            throw new RuntimeException("submittedValue is not string: '" + submittedValue + "'");
        }
    }

    // public static class RelatedSelectEvent {
    public static class RelatedSelectEvent extends ActionEvent {
        private static final long serialVersionUID = 1L;

        public final int order;
        public final String group;
        public final Object submittedValue;

        /**
         * @param uiComponent
         * @param group - only components in the same group should process this event
         * @param order - determines the component, that has to process this event
         */
        public RelatedSelectEvent(UIComponent uiComponent, String group, int order, Object submittedValue) {
            super(uiComponent);
            this.group = group;
            this.order = order;
            this.submittedValue = submittedValue;
        }

        @Override
        public String toString() {
            return new StringBuilder("RelatedSelectEvent:")
                    .append("\n\tgroup=").append(group)
                    .append("\n\torder=").append(order)
                    .append("\n\tsubmittedValue=").append(submittedValue).toString();
        }

    }

    protected HtmlSelectOneMenu getSelectComponent(FacesContext context) {
        return (HtmlSelectOneMenu) context.getApplication().createComponent(HtmlSelectOneMenu.COMPONENT_TYPE);
    }

    private String getOnChangeJS(FacesContext context) {
        return Utils.generateFormSubmit(context, this, getFilledFieldId(context), CHANGE_MARKER);
    }

    private String getFilledFieldId(FacesContext context) {
        return getClientId(context) + "_hidden";
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setSelectionItems(String selectionItems) {
        this.selectionItems = selectionItems;
    }

    @Override
    public String toString() {
        return new StringBuilder("RelatedDropdown:")
                .append("\n\tgroup=").append(group)
                .append("\n\torder=").append(order)
                .append("\n\tselectionItems=").append(selectionItems).toString();
    }

}
