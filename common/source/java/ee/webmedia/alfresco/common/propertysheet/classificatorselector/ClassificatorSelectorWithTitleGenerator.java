package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.context.FacesContext;

import ee.webmedia.alfresco.utils.ComponentUtil;

public class ClassificatorSelectorWithTitleGenerator extends ClassificatorSelectorGenerator {

    @Override
    public UIComponent generateSelectComponent(FacesContext context, String id, boolean multiValued) {
        UIComponent component = super.generateSelectComponent(context, id, multiValued);
        component.setRendererType(LabelAndValueSelectorRenderer.LABEL_AND_VALUE_SELECTOR_RENDERER_TYPE);
        ComponentUtil.putAttribute(component, LabelAndValueSelectorRenderer.ATTR_DESCRIPTION_AS_TOOLTIP, Boolean.TRUE);
        return component;
    }

    @Override
    protected void setOptionDescriptionAndLabel(ClassificatorSelectorValueProvider classificator, UISelectItem selectItem) {
        selectItem.setItemDescription(classificator.getClassificatorDescription());
        selectItem.setItemLabel(classificator.getSelectorValueName());
    }

}
