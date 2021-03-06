package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.constant.FieldChangeableIf;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * Not editable search for organization structure units
 */
public class StructUnitGenerator extends BaseTypeFieldGenerator {

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.STRUCT_UNIT };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO editModeItem = generatorResults.getAndAddPreGeneratedItem();
        editModeItem.setStyleClass("expand19-200 medium");
        editModeItem.setEditable(false);
        editModeItem.setDialogTitleId("series_structUnit_popUpInfo");
        editModeItem.setPickerCallback("#{OrganizationStructureListDialog.searchOrgstructs}");
        if (FieldChangeableIf.ALWAYS_NOT_CHANGEABLE.equals(field.getChangeableIfEnum())) {
            editModeItem.setOutputTextPropertyValue(false); // if set to true then value is displayed as concatenated string of all path elements
        }

        if (!field.isForSearch()) {
            editModeItem.setComponentGenerator("StructUnitSearchGenerator");
            editModeItem.setPreprocessCallback("#{OrganizationStructureListDialog.preprocessResultsToPaths}");
            editModeItem.setShowInViewMode(false);

            QName orgStructProp = field.getQName();
            boolean isMultivalueEditorProp = field.getParent() != null && field.getParent() instanceof FieldGroup
                    && SystematicFieldGroupNames.USERS_TABLE.equals(((FieldGroup) field.getParent()).getName());
            QName orgStructLabelProp = RepoUtil.createTransientProp(orgStructProp.getLocalName() + "Label");
            if (!isMultivalueEditorProp) {
                generatorResults.generateAndAddViewModeText(orgStructLabelProp.toString(), field.getName());
            } else {
                editModeItem.setShowInViewMode(true);
            }
            generatorResults.addStateHolder(orgStructProp.getLocalName(), new StructUnitState(orgStructProp, orgStructLabelProp, isMultivalueEditorProp));
        } else {
            // fall back to ordinary multi-search behaviour
            editModeItem.setComponentGenerator("SearchGenerator");
            editModeItem.setPreprocessCallback("#{OrganizationStructureListDialog.preprocessResultsToLongestNames}");
        }

    }

    public static class StructUnitState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        private final QName orgStructProp;
        private final QName orgStructLabelProp;
        private final boolean isMultivalueEditorProp;

        /**
         * @param orgStructProp
         * @param orgStructLabelProp
         * @param isMultivalueProp - indicates that this property contains values for multivalue editor, i.e. contains lists of lists of orgStruct names
         */
        public StructUnitState(QName orgStructProp, QName orgStructLabelProp, boolean isMultivalueEditorProp) {
            this.orgStructProp = orgStructProp;
            this.orgStructLabelProp = orgStructLabelProp;
            this.isMultivalueEditorProp = isMultivalueEditorProp;
        }

        @Override
        public void reset(boolean inEditMode) {
            if (!inEditMode) {
                final Node document = dialogDataProvider.getNode();
                Serializable orgStructPath;
                if (!isMultivalueEditorProp) {
                    @SuppressWarnings("unchecked")
                    List<String> structUnits = (List<String>) document.getProperties().get(orgStructProp);
                    orgStructPath = UserUtil.getDisplayUnit(structUnits);
                } else {
                    @SuppressWarnings("unchecked")
                    List<List<String>> structUnits = (List<List<String>>) document.getProperties().get(orgStructProp);
                    List<String> displayNames = new ArrayList<String>();
                    for (List<String> structUnitList : structUnits) {
                        displayNames.add(UserUtil.getDisplayUnit(structUnitList));
                    }
                    orgStructPath = (Serializable) displayNames;
                }
                document.getProperties().put(orgStructLabelProp.toString(), orgStructPath);
            }
        }
    }
}
