package ee.webmedia.alfresco.common.propertysheet.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.PropertySheetConfigElement.ItemConfig;
import org.alfresco.web.ui.repo.RepoConstants;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO.ConfigItemType;
import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;

/**
 * Subclass of UIPropertySheet that copies custom attributes from property-sheet/show-property element to configuration item.
 * 
 * @author Ats Uiboupin
 */
public class WMUIPropertySheet extends UIPropertySheet {

    public static final String SHOW = "show";

    /**
     * If this propertySheet represents a subPropertySheet(meaning it is in another propertySheet), it is associated to the parent using some type of
     * association(a child association for example), that parentPropertySheet can have more than one.<br>
     * Association type is held in closest ancestor component of type {@link SubPropertySheetItem#getAssocTypeQName()}
     */
    private Integer associationIndex;
    private NodeAssocBrand associationBrand;

    /**
     * Default constructor
     */
    public WMUIPropertySheet() {
        super();
    }

    @Override
    protected void changePropSheetItem(ItemConfig item, PropertySheetItem propSheetItem) {
        // if both can have custom attributes, then set them from item to propSheetItem
        if (item instanceof CustomAttributes && propSheetItem instanceof CustomAttributes) {
            CustomAttributes wMPropertyConfig = (CustomAttributes) item;
            CustomAttributes wmPropSheetItem = (CustomAttributes) propSheetItem;
            wmPropSheetItem.setCustomAttributes(wMPropertyConfig.getCustomAttributes());
        }
    }

    @Override
    protected void createComponentsFromConfig(FacesContext context, Collection<ItemConfig> items) throws IOException {
        List<ItemConfig> filteredItems = new ArrayList<ItemConfig>(items.size());
        for (ItemConfig item : items) {
            if (item instanceof CustomAttributes) {
                String show = ((CustomAttributes) item).getCustomAttributes().get(SHOW);
                if (show != null) {
                    ValueBinding vb = context.getApplication().createValueBinding(show);
                    Boolean value = (Boolean) vb.getValue(context);
                    if (value != null && !value) {
                        continue;
                    }
                }
            }
            filteredItems.add(item);
        }
        super.createComponentsFromConfig(context, filteredItems);
    }

    @Override
    protected Pair<PropertySheetItem, String> createPropertySheetItemAndId(ItemConfig item, FacesContext context) {

        final Pair<PropertySheetItem, String> propSheetItemAndId;
        if (item instanceof ItemConfigVO) {
            ItemConfigVO confVO = (ItemConfigVO) item;
            PropertySheetItem propSheetItem;
            String id;
            if (confVO.getConfigItemType().equals(ConfigItemType.PROPERTY)) {
                id = PROP_ID_PREFIX + item.getName();
                propSheetItem = (PropertySheetItem) context.getApplication().
                        createComponent(RepoConstants.ALFRESCO_FACES_PROPERTY);
            } else if (confVO.getConfigItemType().equals(ConfigItemType.ASSOC)) {
                id = ASSOC_ID_PREFIX + item.getName();
                propSheetItem = (PropertySheetItem) context.getApplication().
                        createComponent(RepoConstants.ALFRESCO_FACES_ASSOCIATION);
            } else if (confVO.getConfigItemType().equals(ConfigItemType.CHILD_ASSOC)) {
                id = ASSOC_ID_PREFIX + item.getName();
                propSheetItem = (PropertySheetItem) context.getApplication().
                        createComponent(RepoConstants.ALFRESCO_FACES_CHILD_ASSOCIATION);
            } else if (confVO.getConfigItemType().equals(ConfigItemType.SEPPARATOR)) {
                id = SEP_ID_PREFIX + item.getName();
                propSheetItem = (PropertySheetItem) context.getApplication().
                        createComponent(RepoConstants.ALFRESCO_FACES_SEPARATOR);
            } else if (confVO.getConfigItemType().equals(ConfigItemType.SUB_PROPERTY_SHEET)) {
                id = SubPropertySheetItem.SUB_PROP_SHEET_ID_PREFIX + item.getName();
                propSheetItem = (SubPropertySheetItem) context.getApplication().createComponent(SubPropertySheetItem.SUB_PROPERTY_SHEET_ITEM);
            } else {
                return null; // subclass might have a solution for this item type
            }
            propSheetItemAndId = new Pair<PropertySheetItem, String>(propSheetItem, id);
        } else {
            propSheetItemAndId = null;
        }
        return propSheetItemAndId;
    }

    @Override
    protected void storePropSheetVariable(Node node) {
        if (isSubPropertySheet()) {
            // don't save variable if this is child of another propertySheet.
            // SubPropertySheets should use node saved by outmost propertySheet with associations, to retrieve desired node
        } else {
            super.storePropSheetVariable(node);
        }
    }

    private boolean isSubPropertySheet() {
        return associationBrand != null;
    }

    public void setAssociationIndex(Integer associationIndex) {
        this.associationIndex = associationIndex;
    }

    public Integer getAssociationIndex() {
        return associationIndex;
    }

    public void setAssociationBrand(NodeAssocBrand nodeAssocBrand) {
        this.associationBrand = nodeAssocBrand;
    }

    public NodeAssocBrand getAssociationBrand() {
        return associationBrand;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] state = new Object[3];
        state[0] = super.saveState(context);
        state[1] = this.associationIndex;
        state[2] = this.associationBrand;
        return state;
    }

    @Override
    public void restoreState(FacesContext context, Object stateObj) {
        Object state[] = (Object[]) stateObj;
        super.restoreState(context, state[0]);
        this.associationIndex = (Integer) state[1];
        this.associationBrand = (NodeAssocBrand) state[2];
    }
}
