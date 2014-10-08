package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.document.model.DocumentSpecificModel.Props.DRIVE_BEGIN_DATE;
import static ee.webmedia.alfresco.document.model.DocumentSpecificModel.Props.DRIVE_COMPENSATION;
import static ee.webmedia.alfresco.document.model.DocumentSpecificModel.Props.DRIVE_COMPENSATION_CALCULATED;
import static ee.webmedia.alfresco.document.model.DocumentSpecificModel.Props.DRIVE_COMPENSATION_RATE;
import static ee.webmedia.alfresco.document.model.DocumentSpecificModel.Props.DRIVE_END_DATE;
import static ee.webmedia.alfresco.document.model.DocumentSpecificModel.Props.DRIVE_KM;
import static ee.webmedia.alfresco.document.model.DocumentSpecificModel.Props.DRIVE_ODO_BEGIN;
import static ee.webmedia.alfresco.document.model.DocumentSpecificModel.Props.DRIVE_ODO_END;
import static ee.webmedia.alfresco.document.model.DocumentSpecificModel.Props.DRIVE_RECORD_KEEPING;
import static ee.webmedia.alfresco.document.model.DocumentSpecificModel.Props.DRIVE_TOTAL_COMPENSATION;
import static ee.webmedia.alfresco.document.model.DocumentSpecificModel.Props.DRIVE_TOTAL_KM;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.BeanNameAware;

import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.PropsBuilder;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicGroupGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

public class DriveCompensationGenerator extends BaseSystematicGroupGenerator implements SaveListener, BeanNameAware {

    public static final QName[] DRIVE_DIARY_PROPS = {
        DRIVE_BEGIN_DATE,
        DRIVE_END_DATE,
        DRIVE_ODO_BEGIN,
        DRIVE_ODO_END,
        DRIVE_KM,
        DRIVE_COMPENSATION_CALCULATED
    };

    public static final QName[] DRIVE_COMPENSATION_PROPS = (QName[]) ArrayUtils.addAll(new QName[] {
            DRIVE_RECORD_KEEPING,
            DRIVE_COMPENSATION_RATE,
            DRIVE_TOTAL_KM,
            DRIVE_TOTAL_COMPENSATION,
            DRIVE_COMPENSATION
    }, DRIVE_DIARY_PROPS);

    private String beanName;

    @Override
    public void afterPropertiesSet() {
        documentConfigService.registerMultiValuedOverrideBySystematicGroupName(SystematicFieldGroupNames.DRIVE_COMPENSATION,
                new HashSet<String>(RepoUtil.getLocalNames(DRIVE_DIARY_PROPS)));
        super.afterPropertiesSet();
    }

    public static class DriveCompensationState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;
        private final Map<QName, QName> fieldsByOriginalQName;

        public DriveCompensationState(Map<String, Field> fieldsByOriginalId) {
            Map<QName, QName> props = new HashMap<QName, QName>();
            for (QName prop : DRIVE_COMPENSATION_PROPS) {
                props.put(prop, fieldsByOriginalId.get(prop.getLocalName()).getQName());
            }
            fieldsByOriginalQName = props;
        }


        public void driveRecordKeepingValueChanged(ValueChangeEvent event) {
            Boolean oldValue = (Boolean) event.getOldValue();
            Boolean newValue = (Boolean) event.getNewValue();
            if (ObjectUtils.equals(oldValue, newValue)) {
                return;
            }
            final Boolean driveRecordKeeping = (Boolean) event.getNewValue();
            ComponentUtil.executeLater(PhaseId.INVOKE_APPLICATION, dialogDataProvider.getPropertySheet(), new Closure() {
                @Override
                public void execute(Object input) {
                    DocumentDynamic document = dialogDataProvider.getDocument();
                    if (document != null) {
                        document.setProp(getQName(DRIVE_RECORD_KEEPING), driveRecordKeeping);
                    }
                    clearPropertySheet();
                }
            });
        }

        private void clearPropertySheet() {
            UIPropertySheet propertySheet = dialogDataProvider.getPropertySheet();
            if (propertySheet != null) {
                propertySheet.getChildren().clear();
                propertySheet.getClientValidations().clear();
            }
        }

        public boolean isRecordKeepingEnabled() {
            return Boolean.TRUE.equals(dialogDataProvider.getDocument().getProp(getQName(DRIVE_RECORD_KEEPING)));
        }

        public boolean isRecordKeepingDisabled() {
            return !isRecordKeepingEnabled();
        }

        public void calculateTotalCompensationAndClearProps(DocumentDynamic document) {
            if (Boolean.TRUE.equals(document.getPropBoolean(getQName(DRIVE_RECORD_KEEPING)))) {
                document.setProp(getQName(DRIVE_COMPENSATION), null);
                BigDecimal rate = new BigDecimal((Double) document.getProp(getQName(DRIVE_COMPENSATION_RATE)));
                BigDecimal km = new BigDecimal((Long) document.getProp(getQName(DRIVE_TOTAL_KM)));
                document.setProp(getQName(DRIVE_TOTAL_COMPENSATION), km.multiply(rate).doubleValue());
            } else {
                for (QName prop : DRIVE_COMPENSATION_PROPS) {
                    if (DRIVE_COMPENSATION.equals(prop)) {
                        continue;
                    }
                    document.setProp(getQName(prop), null);
                }
            }
        }

        private QName getQName(QName originalQName) {
            return fieldsByOriginalQName.get(originalQName);
        }
    }

    @Override
    public void save(DynamicBase document) {
        if (!(document instanceof DocumentDynamic)) {
            return;
        }

        for (PropertySheetStateHolder stateHolder : BeanHelper.getPropertySheetStateBean().getStateHolders().values()) {
            if (stateHolder instanceof DriveCompensationState) {
                ((DriveCompensationState) stateHolder).calculateTotalCompensationAndClearProps((DocumentDynamic) document);
            }
        }
    }

    @Override
    public void generateFieldGroup(FieldGroup group, FieldGroupGeneratorResults generatorResults) {
        ChildrenList<Field> fields = group.getFields();
        for (Field field : fields) {
            String originalFieldId = field.getOriginalFieldId();
            if (!DRIVE_BEGIN_DATE.getLocalName().equals(originalFieldId) && RepoUtil.getLocalNames(DRIVE_DIARY_PROPS).contains(originalFieldId)) {
                // All these fields are generated by DRIVE_BEGIN_DATE field
                continue;
            }

            Map<String, Field> fieldsByOriginalId = ((FieldGroup) field.getParent()).getFieldsByOriginalId();

            final ItemConfigVO item = generatorResults.generateItemBase(field);
            String stateHolderKey = fieldsByOriginalId.get(DRIVE_RECORD_KEEPING.getLocalName()).getQName().getLocalName();
            if (DRIVE_RECORD_KEEPING.getLocalName().equals(originalFieldId)) {
                item.setValueChangeListener(getBindingName("driveRecordKeepingValueChanged", stateHolderKey));
                item.setAjaxParentLevel(1);
                generatorResults.addStateHolder(stateHolderKey, new DriveCompensationState(fieldsByOriginalId));
            } else if (DRIVE_COMPENSATION.getLocalName().equals(originalFieldId)) {
                item.setRendered(getBindingName("recordKeepingDisabled", stateHolderKey));
            } else if (DRIVE_BEGIN_DATE.getLocalName().equals(originalFieldId)) {
                List<String> props = new ArrayList<String>();
                item.setRendered(getBindingName("recordKeepingEnabled", stateHolderKey));
                for (QName prop : DRIVE_DIARY_PROPS) {
                    PropsBuilder propsBuilder = new PropsBuilder(fieldsByOriginalId.get(prop.getLocalName()).getQName(), null);
                    if (!DRIVE_BEGIN_DATE.equals(prop) && !DRIVE_END_DATE.equals(prop)) {
                        propsBuilder.addProp("styleClass", "small " + prop.getLocalName());
                    }
                    props.add(propsBuilder.build());
                }
                String readonlyFieldsName = ((FieldGroup) field.getParent()).getReadonlyFieldsName();
                item.setName(readonlyFieldsName);
                item.setDisplayLabel(readonlyFieldsName);
                item.setComponentGenerator("MultiValueEditorGenerator");
                item.setStyleClass("add-item");
                item.setPropsGeneration(StringUtils.join(props, ","));
                item.setAddLabelId("add");
            } else if (DRIVE_TOTAL_KM.getLocalName().equals(originalFieldId) || DRIVE_TOTAL_COMPENSATION.getLocalName().equals(originalFieldId)
                    || DRIVE_COMPENSATION_RATE.getLocalName().equals(originalFieldId)) {
                item.setRendered(getBindingName("recordKeepingEnabled", stateHolderKey));
                item.setStyleClass(item.getStyleClass() + " " + originalFieldId);
            }

            generatorResults.addItem(item);
        }
    }

    @Override
    public Pair<Field, List<Field>> collectAndRemoveFieldsInOriginalOrderToFakeGroup(List<Field> modifiableFieldsList, Field field, Map<String, Field> fieldsByOriginalId) {
        // Not needed
        return null;
    }

    @Override
    protected String[] getSystematicGroupNames() {
        return new String[] { SystematicFieldGroupNames.DRIVE_COMPENSATION };
    }

    @Override
    public String getBeanName() {
        return beanName;
    }

    @Override
    public void setBeanName(String name) {
        beanName = name;
    }

    @Override
    public void validate(DynamicBase document, ValidationHelper validationHelper) {
        // Not needed
    }
}