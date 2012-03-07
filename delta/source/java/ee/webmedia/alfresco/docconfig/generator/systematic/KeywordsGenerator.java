package ee.webmedia.alfresco.docconfig.generator.systematic;

import static org.alfresco.web.ui.common.StringUtils.encode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.ajax.AjaxUpdateable;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIPropertySheet;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.PropsBuilder;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.thesaurus.model.HierarchicalKeyword;
import ee.webmedia.alfresco.thesaurus.model.Thesaurus;
import ee.webmedia.alfresco.utils.Closure;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Generates components for Systematic group Keywords
 * 
 * @author Ats Uiboupin
 */
public class KeywordsGenerator extends BaseSystematicFieldGenerator {

    private String[] originalFieldIds;

    @Override
    public void afterPropertiesSet() {
        Set<String> fields = new HashSet<String>();
        fields.add(DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL.getLocalName());
        fields.add(DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL.getLocalName());

        originalFieldIds = fields.toArray(new String[fields.size()]);
        documentConfigService.registerMultiValuedOverrideInSystematicGroup(originalFieldIds);
        super.afterPropertiesSet();
    }

    @Override
    protected String[] getOriginalFieldIds() {
        return originalFieldIds;
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        // Can be used outside systematic field group - then additional functionality is not present
        if (!(field.getParent() instanceof FieldGroup) || !((FieldGroup) field.getParent()).isSystematic()) {
            // field not in systematic fieldGroup - it might be for example on documents search form
            generatorResults.getAndAddPreGeneratedItem();
            return;
        }
        if (DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL.getLocalName().equals(field.getOriginalFieldId())) {
            return; // keywords group component (including field for SECOND_KEYWORD_LEVEL) was completely generated by FIRST_KEYWORD_LEVEL field
        }
        final FieldGroup group = (FieldGroup) field.getParent();

        List<String> props = new ArrayList<String>();
        List<QName> propNames = new ArrayList<QName>();
        String stateHolderKey = field.getFieldId();
        String generatorName = GeneralSelectorGenerator.class.getSimpleName();
        for (Field child : group.getFields()) {
            QName fieldId = child.getQName();
            PropsBuilder generalSelectorGeneratorBuilder = new PropsBuilder(fieldId, generatorName);
            String originalFieldId = child.getOriginalFieldId();
            if (DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL.getLocalName().equals(originalFieldId)) {
                generalSelectorGeneratorBuilder
                        .addProp(GeneralSelectorGenerator.ATTR_SELECTION_ITEMS, getBindingName("getFirstKeywordLevelSelectItems", stateHolderKey))
                        .addProp(GeneralSelectorGenerator.ATTR_VALUE_CHANGE_LISTENER, getBindingName("firstKeywordLevelChanged", stateHolderKey))
                        .addProp(AjaxUpdateable.AJAX_DISABLED_ATTR, "true");
            } else if (DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL.getLocalName().equals(originalFieldId)) {
                generalSelectorGeneratorBuilder.addProp(GeneralSelectorGenerator.ATTR_SELECTION_ITEMS, getBindingName("getSecondKeywordLevelSelectItems", stateHolderKey));
            } else {
                throw new RuntimeException("Unknown field in keywords group: " + originalFieldId);
            }
            props.add(generalSelectorGeneratorBuilder.build());
            propNames.add(fieldId);
        }

        ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        // TODO when allowing multiple occurences of this group (183445), then enable this
        // item.setName(RepoUtil.createTransientProp(field.getFieldId()).toString());
        item.setComponentGenerator("MultiValueEditorGenerator");
        item.setStyleClass("add-item");

        item.setDisplayLabel(group.getReadonlyFieldsName());
        item.setAddLabelId("keywords_add_keyword");
        item.setInitialRows(1);

        item.setShowInViewMode(false);
        item.setPropsGeneration(StringUtils.join(props, ","));

        // And generate a separate view mode component
        String viewModePropName = RepoUtil.createTransientProp(field.getFieldId() + "Label").toString();
        ItemConfigVO viewModeItem = generatorResults.generateAndAddViewModeText(viewModePropName, group.getReadonlyFieldsName());
        viewModeItem.setComponentGenerator("UnescapedOutputTextGenerator");

        generatorResults.addStateHolder(stateHolderKey, new KeywordsTableState(propNames, viewModePropName, group.getThesaurus()));
    }

    // ===============================================================================================================================

    public static class KeywordsTableState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        private final List<QName> propNames;
        private final String viewModePropName;
        private final String thesaurusName;
        private Map<String/* level1Keywords */, List<String>/* level3Keywords */> hirearchy;

        public KeywordsTableState(List<QName> propNames, String viewModePropName, String thesaurusName) {
            this.propNames = propNames;
            this.viewModePropName = viewModePropName;
            this.thesaurusName = thesaurusName;
            Assert.notNull(thesaurusName, "thesaurusName shouldn't bee null for systematic fields group keywords");
        }

        public List<SelectItem> getFirstKeywordLevelSelectItems(FacesContext context, UIInput selectComponent) {
            if (hirearchy == null) {
                Thesaurus thesaurus = BeanHelper.getThesaurusService().getThesaurus(thesaurusName, true);
                List<HierarchicalKeyword> keywords = thesaurus.getKeywords();
                hirearchy = new HashMap<String, List<String>>();
                for (HierarchicalKeyword kw : keywords) {
                    String keywordLevel1 = kw.getKeywordLevel1();
                    List<String> level2List = hirearchy.get(keywordLevel1);
                    if (level2List == null) {
                        level2List = new ArrayList<String>(5);
                        hirearchy.put(keywordLevel1, level2List);
                    }
                    level2List.add(kw.getKeywordLevel2());
                }
            }
            List<SelectItem> selectItems = new ArrayList<SelectItem>(hirearchy.size());
            for (String keywordLevel1 : hirearchy.keySet()) {
                selectItems.add(new SelectItem(keywordLevel1, keywordLevel1));
            }
            if (selectItems.isEmpty()) {
                ComponentUtil.getAttributes(selectComponent).put("title", MessageUtil.getMessage("thesaurus_empty"));
            } else {
                ComponentUtil.addDefault(selectItems, context);
            }
            WebUtil.sort(selectItems);
            return selectItems;
        }

        public List<SelectItem> getSecondKeywordLevelSelectItems(FacesContext context, UIInput selectComponent) {
            String firstLevelKeyword = getFirstLevelKeyword(context, selectComponent);
            if (StringUtils.isBlank(firstLevelKeyword)) {
                ComponentUtil.putAttribute(selectComponent, "disabled", true);
                return Collections.<SelectItem> emptyList();
            }
            List<String> level2List = hirearchy.get(firstLevelKeyword);
            List<SelectItem> selectItems = new ArrayList<SelectItem>(level2List.size());
            for (String keywordLevel2 : level2List) {
                selectItems.add(new SelectItem(keywordLevel2, keywordLevel2));
            }
            if (!selectItems.isEmpty()) {
                ComponentUtil.addDefault(selectItems, context);
            }
            WebUtil.sort(selectItems);
            return selectItems;
        }

        private String getFirstLevelKeyword(FacesContext context, UIInput selectComponent) {
            ValueBinding secondLevelVB = selectComponent.getValueBinding("value");
            String firstLevelVBExpr = StringUtils.replace(secondLevelVB.getExpressionString(), DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL.toString(),
                    DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL.toString());
            ValueBinding vb = context.getApplication().createValueBinding(firstLevelVBExpr);
            String firstLevelKeyword = (String) vb.getValue(context);
            return firstLevelKeyword;
        }

        public void firstKeywordLevelChanged(ValueChangeEvent e) {
            UIComponent firstLevelComponent = e.getComponent();
            WMUIPropertySheet comp = ComponentUtil.getAncestorComponent(firstLevelComponent, WMUIPropertySheet.class, true);
            // clear children of keywordsGroupProperty so that component for SECOND_KEYWORD_LEVEL would be re-rendered with new selectItems,
            // but do clearing child components after model values are updated, so that changes in secondKeywordLevel column wouldn't be lost -
            // for example if you had at least two rows, and before changing FIRST_KEYWORD_LEVEL of first row you changed SECOND_KEYWORD_LEVEL of other row
            ComponentUtil.executeLater(PhaseId.UPDATE_MODEL_VALUES, comp, new Closure<UIComponent>() {
                @Override
                public void exec(UIComponent nill) {
                    WMUIProperty keywordsGroupProperty = ComponentUtil.getPropSheetItem(dialogDataProvider.getPropertySheet()
                            , WMUIProperty.class, DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL);
                    keywordsGroupProperty.getChildren().clear();
                }
            });
        }

        @Override
        protected void reset(boolean inEditMode) {
            if (!inEditMode) {
                // update view mode property value
                final Node document = dialogDataProvider.getNode();
                int size = 0;
                List<List<String>> all = new ArrayList<List<String>>();
                Map<String, Object> props = document.getProperties();
                for (QName propName : propNames) {
                    @SuppressWarnings("unchecked")
                    List<String> columnValues = (List<String>) props.get(propName);
                    if (columnValues == null) {
                        columnValues = new ArrayList<String>();
                    }
                    size = Math.max(columnValues.size(), size);
                    all.add(columnValues);
                }

                List<String> rows = new ArrayList<String>(size);
                for (int rowIndex = 0; rowIndex < size; rowIndex++) {
                    List<String> rowValues = new ArrayList<String>();
                    for (List<String> columnValues : all) {
                        if (rowIndex < columnValues.size()) {
                            String value = StringUtils.trim(columnValues.get(rowIndex));
                            if (StringUtils.isNotBlank(value)) {
                                rowValues.add(encode(value));
                            }
                        }
                    }
                    if (!rowValues.isEmpty()) {
                        rows.add(StringUtils.join(rowValues, ", "));
                    }
                }
                props.put(viewModePropName, StringUtils.join(rows, "<br/>"));
            }
        }

    }

}
