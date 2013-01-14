package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Alar Kvell
 */
public class RegNumberGenerator extends BaseSystematicFieldGenerator {

    private UserService userService;
    private boolean regNumberEditable;

    @Override
    public void afterPropertiesSet() {
        documentConfigService.registerHiddenFieldDependency(DocumentCommonModel.Props.SHORT_REG_NUMBER.getLocalName(), DocumentCommonModel.Props.REG_NUMBER.getLocalName());
        documentConfigService.registerHiddenFieldDependency(DocumentCommonModel.Props.INDIVIDUAL_NUMBER.getLocalName(), DocumentCommonModel.Props.REG_NUMBER.getLocalName());
        documentConfigService.registerHiddenFieldDependency(VolumeModel.Props.VOL_SHORT_REG_NUMBER.getLocalName(), VolumeModel.Props.VOLUME_MARK.getLocalName());

        getDocumentAdminService().registerForbiddenFieldId(VolumeModel.Props.CONTAINS_CASES.getLocalName());
        getDocumentAdminService().registerForbiddenFieldId(VolumeModel.Props.CASES_CREATABLE_BY_USER.getLocalName());
        getDocumentAdminService().registerForbiddenFieldId(VolumeModel.Props.VOLUME_TYPE.getLocalName());
        getDocumentAdminService().registerForbiddenFieldId(VolumeModel.Props.CONTAINING_DOCS_COUNT.getLocalName());
        getDocumentAdminService().registerForbiddenFieldId(VolumeModel.Props.LOCATION.getLocalName());

        super.afterPropertiesSet();
    }

    @Override
    protected String[] getOriginalFieldIds() {
        return new String[] {
                DocumentCommonModel.Props.REG_NUMBER.getLocalName(),
                DocumentCommonModel.Props.REG_DATE_TIME.getLocalName(),
                VolumeModel.Props.VOLUME_MARK.getLocalName() };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        // Can be used outside systematic field group - then additional functionality is not present
        if (!VolumeModel.Props.VOLUME_MARK.getLocalName().equals(field.getOriginalFieldId())
                && (!(field.getParent() instanceof FieldGroup) || !((FieldGroup) field.getParent()).isSystematic())) {
            generatorResults.getAndAddPreGeneratedItem();
            return;
        }

        ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        if (field.getOriginalFieldId().equals(VolumeModel.Props.VOLUME_MARK.getLocalName())) {
            item.getCustomAttributes().put(BaseComponentGenerator.CustomAttributeNames.VALDIATION_DISABLED, "#{CaseFileDialog.isVolumeMarkValidationDisabled}");
            return;
        }

        Map<String, Field> fieldsByOriginalId = ((FieldGroup) field.getParent()).getFieldsByOriginalId();
        Field regNumberField = fieldsByOriginalId.get(DocumentCommonModel.Props.REG_NUMBER.getLocalName());
        Field regDateTimeField = fieldsByOriginalId.get(DocumentCommonModel.Props.REG_DATE_TIME.getLocalName());
        String regNrStateHolderKey = regNumberField.getFieldId();

        item.setShow(getBindingName("showFields", regNrStateHolderKey));
        if (field.getOriginalFieldId().equals(DocumentCommonModel.Props.REG_NUMBER.getLocalName())) {
            if (!(regNumberEditable && userService.isDocumentManager())) {
                item.setReadOnly(true);
            } else {
                item.setForcedMandatory(true);
            }
            generatorResults.addStateHolder(regNrStateHolderKey, new RegNumberState(regNumberField.getQName(), regDateTimeField.getQName()));
            return;
        } else if (field.getOriginalFieldId().equals(DocumentCommonModel.Props.REG_DATE_TIME.getLocalName())) {
            return;
        }
        throw new RuntimeException("Unsupported field: " + field);
    }

    @Override
    public void save(DynamicBase dynamicObject) {
        if (!(dynamicObject instanceof CaseFile)) {
            return;
        }

        // If user has modified the volume mark, then empty short reg number field
        if (!StringUtils.equals((String) dynamicObject.getProp(VolumeModel.Props.VOLUME_MARK),
                (String) BeanHelper.getNodeService().getProperty(dynamicObject.getNodeRef(), VolumeModel.Props.VOLUME_MARK))) {
            dynamicObject.setProp(VolumeModel.Props.VOL_SHORT_REG_NUMBER, null);
        }
    }

    // XXX if user manually changes regNumber of document, then we should update shortRegNumber and individualNumber also
    // but how? this is very difficult. it was decided that these are left as is

    // ===============================================================================================================================

    public static class RegNumberState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        private final QName regNumberProp;
        private final QName regDateTimeProp;

        public RegNumberState(QName regNumberProp, QName regDateTimeProp) {
            this.regNumberProp = regNumberProp;
            this.regDateTimeProp = regDateTimeProp;
        }

        public boolean isShowFields() {
            Node document = dialogDataProvider.getNode();
            final Map<String, Object> docProps = document.getProperties();
            return docProps.get(regDateTimeProp) != null || StringUtils.isNotBlank((String) docProps.get(regNumberProp));
        }
    }

    // ===============================================================================================================================

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setRegNumberEditable(boolean regNumberEditable) {
        this.regNumberEditable = regNumberEditable;
    }

}
