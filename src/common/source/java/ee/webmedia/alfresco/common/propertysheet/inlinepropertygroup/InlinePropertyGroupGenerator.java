package ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;

public class InlinePropertyGroupGenerator extends BaseComponentGenerator {

    private int propIndex = 0;

    @Override
    public UIComponent generate(FacesContext context, String id) {
        propIndex = 0;
        UIComponent container = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_GRID);
        FacesHelper.setupComponentId(context, container, id);
        return container;
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef, UIComponent component) {
        List<String> props;
        String propsAttribute = getCustomAttributes().get("props");
        if (propsAttribute == null) {
            props = new ArrayList<String>(1);
            props.add(item.getName());
        } else {
            props = Arrays.asList(StringUtils.split(propsAttribute, ','));
        }

        String text = Application.getMessage(FacesContext.getCurrentInstance(), getCustomAttributes().get("textId"));
        List<UIComponent> components = generate(context, propertySheet.getVar(), component.getId(), props, text);
        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        children.addAll(components);
    }

    protected List<UIComponent> generate(FacesContext context, String propertySheetVar, String containerId, List<String> props, String text) {
        List<UIComponent> components = new ArrayList<UIComponent>();
        int i = 0;
        for (String rowText : text.split("\n")) {
            UIComponent container = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_PANELGROUP);
            FacesHelper.setupComponentId(context, container, null);

            List<UIComponent> rowComponents = generateRow(context, propertySheetVar, containerId + "_" + i, props, rowText);

            @SuppressWarnings("unchecked")
            List<UIComponent> children = container.getChildren();
            children.addAll(rowComponents);

            components.add(container);
            i++;
        }
        return components;
    }

    protected List<UIComponent> generateRow(FacesContext context, String propertySheetVar, String containerId, List<String> props, String text) {
        List<UIComponent> components = new ArrayList<UIComponent>();
        List<String> textParts = new ArrayList<String>(Arrays.asList(text.split("#", props.size())));
        String last = textParts.get(textParts.size() - 1);
        if (last.endsWith("#")) {
            textParts.set(textParts.size() - 1, last.substring(0, last.length() - 1));
            textParts.add("");
        }
        for (int i = 0; i < textParts.size(); i++) {
            String currentId = containerId + "_" + i;
            if (StringUtils.isNotEmpty(textParts.get(i))) {
                UIOutput textComponent = createOutputTextComponent(context, currentId);
                textComponent.setValue(textParts.get(i));
                components.add(textComponent);
            }

            if (props.size() > propIndex && i < textParts.size() - 1) {
                UIComponent component = ComponentUtil.generateComponent(context, propertySheetVar, currentId, props.get(propIndex));
                propIndex++;

// TODO add validators to vacation fields
//                MandatoryIfValidator validator = new MandatoryIfValidator("leaveAnnual");
//                ((UIInput) component).addValidator(validator);

                components.add(component);
            }
        }
        return components;
    }

}
