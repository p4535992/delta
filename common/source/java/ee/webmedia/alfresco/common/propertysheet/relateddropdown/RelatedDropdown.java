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
import javax.faces.event.PhaseId;

import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Selection (dropdown) component that can depend on other components of this type.<br>
 * Values for this component are given using <code>selectionItems</code> attribute.<br>
 * The order how one component depends on another is determined using <code>order</code>(That must be integer).<br>
 * Only component that has the value of <code>order</code> larger by 1 and the same <code>group</code> will be populated with values.<br>
 * Component with larger <code>order</code> values will be disabled and made empty, components with smaller <code>order</code> will not be modified.<br>
 * <br>
 * <code>afterSelect</code> attribute can be used to call method after selection has been done.
 */
public class RelatedDropdown extends HtmlSelectOneMenu {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(RelatedDropdown.class);
    public Integer order;
    public String group;
    /** String for method binding to be excecuted to retrieve values for dropDown */
    public String selectionItems;
    /** String for method binding to be excecuted after selectionItems method binding is excecuted */
    public String afterSelect;
    public static final String CHANGE_MARKER = "CHANGE_MARKER";
    public static final String FIRST_DECODE = "firstDecode";

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
            if (StringUtils.isBlank(submittedValue)) {
                log.debug("Submitted value is empty: '" + submittedValue + "'");
            }
            queueEvent(new RelatedSelectEvent(this, group, order, submittedValue, true));
            queueEventToRelatedComponents(submittedValue);
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = getAttributes();
        }
    }

    private void queueEventToRelatedComponents(final String submittedValue) {
        final UIPropertySheet propertySheet = ComponentUtil.getAncestorComponent(this, UIPropertySheet.class, true);
        final List<? extends RelatedDropdown> relatedDropDownInstances = ComponentUtil.findInputsByClass(propertySheet, this.getClass());
        for (RelatedDropdown relatedDropdown : relatedDropDownInstances) {
            queueEvent(new RelatedSelectEvent(relatedDropdown, group, order, submittedValue));
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = getAttributes();
        setOnchange(getOnChangeJS(context));
        if ((Boolean) attributes.get(FIRST_DECODE) == null) { //
            if (!hasSelectionItems()) {
                initializeItemsBasedOnPreviousRelatedDropdown(context);
            }
            doAfterSelect(context, getSubmittedValue());
            attributes.put(FIRST_DECODE, Boolean.FALSE);
        }
        super.encodeBegin(context);
    }

    private boolean hasSelectionItems() {
        @SuppressWarnings("unchecked")
        List<UIComponent> selectOptions = getChildren();
        if (selectOptions.size() < 1) {
            return false;
        } else if (selectOptions.size() == 1) {
            final UISelectItem uiComponent = (UISelectItem) selectOptions.get(0);
            final String itemValue = (String) uiComponent.getItemValue();
            if (StringUtils.isBlank(itemValue)) {
                return false;
            }
        }
        return true;
    }

    private void initializeItemsBasedOnPreviousRelatedDropdown(FacesContext context) {
        final UIPropertySheet propertySheet = ComponentUtil.getAncestorComponent(this, UIPropertySheet.class, true);
        final List<? extends RelatedDropdown> relatedDropDownInstances = ComponentUtil.findInputsByClass(propertySheet, this.getClass());
        RelatedDropdown previousRelatedDD = findPreviousRelatedDropDown(relatedDropDownInstances);
        if (previousRelatedDD != null) {
            final String previousCompValue = (String) previousRelatedDD.getValue();
            if (StringUtils.isNotBlank(previousCompValue)) {
                clearValues();
                addSelectionItems(context, previousCompValue);
            }
        }
    }

    private RelatedDropdown findPreviousRelatedDropDown(final List<? extends RelatedDropdown> relatedDropDownInstances) {
        RelatedDropdown previousRelatedDD = null;
        for (RelatedDropdown relatedDropdown : relatedDropDownInstances) {
            final boolean groupsEqual = StringUtils.equals(relatedDropdown.group, group);
            if (groupsEqual && relatedDropdown.order.equals(order - 1)) {
                previousRelatedDD = relatedDropdown;
            }
        }
        return previousRelatedDD;
    }

    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException {
        FacesContext context = FacesContext.getCurrentInstance();
        if (event instanceof RelatedSelectEvent) {
            RelatedSelectEvent rEvent = (RelatedSelectEvent) event;
            if (rEvent.doAfterSelect) {
                doAfterSelect(context, rEvent.submittedValue);
                return;
            }
            final boolean groupsEqual = StringUtils.equals(rEvent.group, group);
            if (groupsEqual) {
                if (rEvent.order + 1 == order) {
                    clearValues();
                    addSelectionItems(context, rEvent.submittedValue);
                    // notify other (next) components about changed value
                    queueEventToRelatedComponents((String) getSubmittedValue());
                    // queue doAfterSelect()
                    queueEvent(new RelatedSelectEvent(this, group, order, getSubmittedValue(), true));
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
        List<UIComponent> selectOptions = getChildren();
        selectOptions.clear();
        setSubmittedValue("");
        setDisabled(true);
        UISelectItem selectItem = (UISelectItem) FacesContext.getCurrentInstance().getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
        selectItem.setItemLabel("");
        selectItem.setItemValue("");
        @SuppressWarnings("unchecked")
        final List<UIComponent> children = getChildren();
        children.add(selectItem);

    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] state = new Object[5];
        state[0] = super.saveState(context);
        state[1] = order;
        state[2] = group;
        state[3] = selectionItems;
        state[4] = afterSelect;
        return state;
    }

    @Override
    public void restoreState(FacesContext context, Object savedState) {
        Object[] state = (Object[]) savedState;
        super.restoreState(context, state[0]);
        order = (Integer) state[1];
        group = (String) state[2];
        selectionItems = (String) state[3];
        afterSelect = (String) state[4];
    }

    private void addSelectionItems(FacesContext context, Object submittedValue) {
        if (submittedValue instanceof String) {
            setDisabled(false);
            MethodBinding mb = context.getApplication().createMethodBinding(selectionItems,
                    new Class[] { FacesContext.class, HtmlSelectOneMenu.class, Object.class });
            try {
                mb.invoke(context, new Object[] { context, this, submittedValue });
            } catch (ClassCastException e) {
                throw new RuntimeException("Failed to get values for selection from '" + selectionItems + "'", e);
            }
        } else {
            // this might be ok for some cases, feel free to change it if you need to
            throw new RuntimeException("submittedValue is not string: '" + submittedValue + "'");
        }
    }

    private void doAfterSelect(FacesContext context, Object submittedValue) {
        if (StringUtils.isBlank(afterSelect)) {
            return;
        }
        if (submittedValue instanceof String) {
            MethodBinding mb = context.getApplication().createMethodBinding(afterSelect,
                    new Class[] { Object.class });
            try {
                mb.invoke(context, new Object[] { submittedValue });
            } catch (ClassCastException e) {
                throw new RuntimeException("Failed to call afterSelect method binding '" + afterSelect + "'", e);
            }
        } else {
            // this might be ok for some cases, feel free to change it if you need to
            throw new RuntimeException("submittedValue is not string: '" + submittedValue + "'");
        }
    }

    // public static class RelatedSelectEvent {
    public static class RelatedSelectEvent extends ActionEvent {
        private static final long serialVersionUID = 1L;

        public final int order;
        public final String group;
        public final Object submittedValue;
        public final boolean doAfterSelect;

        public RelatedSelectEvent(UIComponent uiComponent, String group, int order, Object submittedValue) {
            this(uiComponent, group, order, submittedValue, false);
        }

        /**
         * @param uiComponent
         * @param group - only components in the same group should process this event
         * @param order - determines the component, that has to process this event
         */
        public RelatedSelectEvent(UIComponent uiComponent, String group, int order, Object submittedValue, boolean doAfterSelect) {
            super(uiComponent);
            this.group = group;
            this.order = order;
            this.submittedValue = submittedValue;
            this.doAfterSelect = doAfterSelect;
            if (doAfterSelect) {
                setPhaseId(PhaseId.INVOKE_APPLICATION);
            }
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
        return ComponentUtil.generateAjaxFormSubmit(context, this, getFilledFieldId(context), CHANGE_MARKER);
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

    public void setAfterSelect(String afterSelect) {
        this.afterSelect = afterSelect;
    }

    @Override
    public String toString() {
        return new StringBuilder("RelatedDropdown:")
                .append("\n\tgroup=").append(group)
                .append("\n\torder=").append(order)
                .append("\n\tselectionItems=").append(selectionItems).toString();
    }

}
