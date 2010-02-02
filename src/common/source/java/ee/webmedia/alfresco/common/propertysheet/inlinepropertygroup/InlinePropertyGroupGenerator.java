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
        FacesHelper.setupComponentId(context, container, null);
        return container;
    }

    @Override
    protected void setupMandatoryPropertyIfNecessary(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item,
            PropertyDefinition propertyDef, UIComponent component) {

        // by now, this component has been added to parent's children list

        List<String> props;
        String propsAttribute = getCustomAttributes().get("props");
        if (propsAttribute == null) {
            props = new ArrayList<String>(1);
            props.add(item.getName());
        } else {
            props = Arrays.asList(StringUtils.split(propsAttribute, ','));
        }

        String text = Application.getMessage(FacesContext.getCurrentInstance(), getCustomAttributes().get("textId"));
        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        generate(context, propertySheet, item, children, props, text);

        super.setupMandatoryPropertyIfNecessary(context, propertySheet, item, propertyDef, component);
    }

    protected void generate(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, List<UIComponent> children, List<String> props,
            String text) {

        int i = 0;
        for (String rowText : text.split("\n")) {
            UIComponent container = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_PANELGROUP);
            FacesHelper.setupComponentId(context, container, null);
            children.add(container);

            @SuppressWarnings("unchecked")
            List<UIComponent> rowChildren = container.getChildren();
            generateRow(context, propertySheet, item, rowChildren, props, rowText);

            i++;
        }
    }

    protected void generateRow(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, List<UIComponent> rowChildren, List<String> props,
            String text) {

        List<String> textParts = new ArrayList<String>(Arrays.asList(text.split("#", props.size())));
        String last = textParts.get(textParts.size() - 1);
        if (last.endsWith("#")) {
            textParts.set(textParts.size() - 1, last.substring(0, last.length() - 1));
            textParts.add("");
        }
        for (int i = 0; i < textParts.size(); i++) {
            if (StringUtils.isNotEmpty(textParts.get(i))) {
                UIOutput textComponent = createOutputTextComponent(context, null);
                textComponent.setValue(textParts.get(i));
                rowChildren.add(textComponent);
            }

            if (props.size() > propIndex && i < textParts.size() - 1) {
                ComponentUtil.generateComponent(context, propertySheet.getVar(), props.get(propIndex), propertySheet, item, rowChildren);
                // above method alreadys adds component to children list
                propIndex++;

// TODO add validators to vacation fields
//              MandatoryIfValidator validator = new MandatoryIfValidator("leaveAnnual");
//              ((UIInput) component).addValidator(validator);
            }
        }
    }

}
