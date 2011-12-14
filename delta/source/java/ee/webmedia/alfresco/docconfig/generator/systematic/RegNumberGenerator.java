package ee.webmedia.alfresco.docconfig.generator.systematic;

import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;

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

        super.afterPropertiesSet();
    }

    @Override
    protected String[] getOriginalFieldIds() {
        return new String[] {
                DocumentCommonModel.Props.REG_NUMBER.getLocalName(),
                DocumentCommonModel.Props.REG_DATE_TIME.getLocalName() };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        // Can be used outside systematic field group - then additional functionality is not present
        if (!(field.getParent() instanceof FieldGroup) || !((FieldGroup) field.getParent()).isSystematic()) {
            generatorResults.getAndAddPreGeneratedItem();
            return;
        }

        Map<String, Field> fieldsByOriginalId = ((FieldGroup) field.getParent()).getFieldsByOriginalId();
        Field regNumberField = fieldsByOriginalId.get(DocumentCommonModel.Props.REG_NUMBER.getLocalName());
        Field regDateTimeField = fieldsByOriginalId.get(DocumentCommonModel.Props.REG_DATE_TIME.getLocalName());
        String stateHolderKey = regNumberField.getFieldId();

        ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        item.setShow(getBindingName("showFields", stateHolderKey));
        if (field.getOriginalFieldId().equals(DocumentCommonModel.Props.REG_NUMBER.getLocalName())) {
            if (!(regNumberEditable && userService.isDocumentManager())) {
                item.setReadOnly(true);
            } else {
                item.setForcedMandatory(true);
            }
            generatorResults.addStateHolder(stateHolderKey, new RegNumberState(regNumberField.getQName(), regDateTimeField.getQName()));
            return;
        } else if (field.getOriginalFieldId().equals(DocumentCommonModel.Props.REG_DATE_TIME.getLocalName())) {
            return;
        }
        throw new RuntimeException("Unsupported field: " + field);
    }

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
