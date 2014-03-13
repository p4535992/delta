package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import java.util.List;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
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

        if (!field.isForSearch()) {
            editModeItem.setComponentGenerator("StructUnitSearchGenerator");
            editModeItem.setPreprocessCallback("#{OrganizationStructureListDialog.preprocessResultsToPaths}");
            editModeItem.setShowInViewMode(false);

            QName orgStructProp = field.getQName();
            QName orgStructLabelProp = RepoUtil.createTransientProp(orgStructProp.getLocalName() + "Label");
            ItemConfigVO viewModeItem = generatorResults.generateAndAddViewModeText(orgStructLabelProp.toString(), field.getName());

            generatorResults.addStateHolder(orgStructProp.getLocalName(), new StructUnitState(orgStructProp, orgStructLabelProp));
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

        public StructUnitState(QName orgStructProp, QName orgStructLabelProp) {
            this.orgStructProp = orgStructProp;
            this.orgStructLabelProp = orgStructLabelProp;
        }

        @Override
        public void reset(boolean inEditMode) {
            if (!inEditMode) {
                final Node document = dialogDataProvider.getNode();
                @SuppressWarnings("unchecked")
                List<String> structUnits = (List<String>) document.getProperties().get(orgStructProp);
                String orgStructPath = UserUtil.getDisplayUnit(structUnits);
                document.getProperties().put(orgStructLabelProp.toString(), orgStructPath);

            }
        }
    }
}
