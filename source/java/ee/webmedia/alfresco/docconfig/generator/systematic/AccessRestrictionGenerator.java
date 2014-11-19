package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.PROPERTIES_SEPARATOR;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSeriesService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON;
<<<<<<< HEAD
=======
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.DOC_STATUS;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
<<<<<<< HEAD
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
<<<<<<< HEAD
import ee.webmedia.alfresco.classificator.enums.PublishToAdr;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.model.DynamicBase;
=======
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorAndTextGenerator;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.PropsBuilder;
import ee.webmedia.alfresco.common.web.BeanHelper;
<<<<<<< HEAD
import ee.webmedia.alfresco.common.web.WmNode;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
<<<<<<< HEAD
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
public class AccessRestrictionGenerator extends BaseSystematicFieldGenerator {

    public static final String BEAN_NAME = "accessRestrictionGenerator";

    private static final String VIEW_MODE_PROP_SUFFIX = "View";
    public static final String ACCESS_RESTRICTION_CHANGE_REASON_ERROR = "accessRestrictionChangeReasonError";
=======
public class AccessRestrictionGenerator extends BaseSystematicFieldGenerator {

    private static final String VIEW_MODE_PROP_SUFFIX = "View";
    public static final String ACCESS_RESTRICTION_CHANGE_REASON_ERROR = "access_restriction_change_reason_error";
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    public static final QName[] ACCESS_RESTRICTION_PROPS = {
            ACCESS_RESTRICTION,
            ACCESS_RESTRICTION_REASON,
            ACCESS_RESTRICTION_BEGIN_DATE,
            ACCESS_RESTRICTION_END_DATE,
            ACCESS_RESTRICTION_END_DESC };

    private NamespaceService namespaceService;

    @Override
    public void afterPropertiesSet() {
        documentConfigService.registerHiddenFieldDependency(ACCESS_RESTRICTION_CHANGE_REASON.getLocalName(), ACCESS_RESTRICTION.getLocalName());
        super.afterPropertiesSet();
    }

    @Override
    protected String[] getOriginalFieldIds() {
        ArrayList<String> originalFieldIds = new ArrayList<String>();
        for (QName propName : ACCESS_RESTRICTION_PROPS) {
            originalFieldIds.add(propName.getLocalName());
        }
        return originalFieldIds.toArray(new String[originalFieldIds.size()]);
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        // Actually these fields cannot be used outside systematic group, because they have onlyInGroup=true
        // But let's leave this check in just in case
        if (!(field.getParent() instanceof FieldGroup) || !((FieldGroup) field.getParent()).isSystematic()) {
            generatorResults.getAndAddPreGeneratedItem();
            return;
        }

<<<<<<< HEAD
        Map<String, Field> fieldsByOriginalId = ((FieldGroup) field.getParent()).getFieldsByOriginalId();
=======
        FieldGroup group = (FieldGroup) field.getParent();
        Map<String, Field> fieldsByOriginalId = group.getFieldsByOriginalId();
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        QName accessRestrictionProp = getProp(fieldsByOriginalId, ACCESS_RESTRICTION);
        QName accessRestrictionReasonProp = getProp(fieldsByOriginalId, ACCESS_RESTRICTION_REASON);
        QName accessRestrictionBeginDateProp = getProp(fieldsByOriginalId, ACCESS_RESTRICTION_BEGIN_DATE);
        QName accessRestrictionEndDateProp = getProp(fieldsByOriginalId, ACCESS_RESTRICTION_END_DATE);
        QName accessRestrictionEndDescProp = getProp(fieldsByOriginalId, ACCESS_RESTRICTION_END_DESC);
        QName accessRestrictionReasonSelectorProp = RepoUtil.createTransientProp(accessRestrictionReasonProp.getLocalName() + "_selector_value");

        if (field.getOriginalFieldId().equals(ACCESS_RESTRICTION_END_DATE.getLocalName())) {
            // access restriction end date is generated with access restriction begin date as part of inline group
            return;
        }

        String accessRestrictionPropName = accessRestrictionProp.getLocalName();
        String stateHolderKey = accessRestrictionPropName;

        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        if (field.getQName().equals(accessRestrictionProp)) {
            item.setValueChangeListener(getBindingName("accessRestrictionValueChanged", stateHolderKey));
            item.setAjaxParentLevel(1);
        } else {
<<<<<<< HEAD
            item.getCustomAttributes().put("rendered", getBindingName("renderAllAccessRestrictionFields", stateHolderKey));
=======
            item.setRendered(getBindingName("renderAllAccessRestrictionFields", stateHolderKey));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            if (field.getOriginalFieldId().equals(ACCESS_RESTRICTION_REASON.getLocalName())) {
                item.setValueChangeListener(getBindingName("accessRestrictionReasonValueChanged", stateHolderKey));
                item.setAjaxParentLevel(1);
                item.setMandatoryIf(accessRestrictionPropName);
                item.getCustomAttributes().put(ClassificatorSelectorAndTextGenerator.IS_LABEL_AND_VALUE_SELECT, Boolean.TRUE.toString());
                item.getCustomAttributes().put(ClassificatorSelectorAndTextGenerator.SELECTOR_VALUE_KEY, accessRestrictionReasonSelectorProp.toString());
                generatorResults.addStateHolder(stateHolderKey, new AccessRestrictionState(accessRestrictionProp, accessRestrictionReasonProp, accessRestrictionBeginDateProp,
                        accessRestrictionEndDateProp, accessRestrictionEndDescProp, accessRestrictionReasonSelectorProp, field.getClassificator()));
            } else if (field.getOriginalFieldId().equals(ACCESS_RESTRICTION_BEGIN_DATE.getLocalName())) {
                String itemLabel = MessageUtil.getMessage("document_accessRestrictionDate");
                List<String> components = DurationGenerator.generateDurationFields(field, item, accessRestrictionBeginDateProp, accessRestrictionEndDateProp,
<<<<<<< HEAD
                        itemLabel,
                        namespaceService);
=======
                        itemLabel, group, namespaceService);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                List<String> componentsWithMandatoryIf = new ArrayList<String>();
                componentsWithMandatoryIf.add(components.get(0) + "¤mandatoryIf=" + accessRestrictionPropName);
                componentsWithMandatoryIf.add(components.get(1) + "¤mandatoryIf=" + accessRestrictionPropName + "," + accessRestrictionEndDescProp.getLocalName() + "=null");
                item.setOptionsSeparator(PropsBuilder.DEFAULT_OPTIONS_SEPARATOR);
                String propSeparator = "|";
                item.getCustomAttributes().put(PROPERTIES_SEPARATOR, propSeparator);
                item.setProps(StringUtils.join(componentsWithMandatoryIf, propSeparator));
                item.setTextId("document_eventDates_templateText");
                item.setShowInViewMode(false);

                // same as edit mode item, only difference is template format text and validation not needed in view mode
                ItemConfigVO viewModeItem = generatorResults.generateAndAddViewModeText(
                        RepoUtil.createTransientProp(accessRestrictionBeginDateProp.getLocalName() + VIEW_MODE_PROP_SUFFIX).toString(), itemLabel);
                viewModeItem.setRendered(getBindingName("renderAllAccessRestrictionFields", stateHolderKey));
                components = DurationGenerator.generateDurationFields(field, viewModeItem, accessRestrictionBeginDateProp, accessRestrictionEndDateProp,
<<<<<<< HEAD
                        itemLabel,
                        namespaceService);
=======
                        itemLabel, group, namespaceService);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                viewModeItem.setOptionsSeparator(PropsBuilder.DEFAULT_OPTIONS_SEPARATOR);
                viewModeItem.setProps(StringUtils.join(components, ','));
                viewModeItem.setTextId("document_accessRestrictionDates_templateText");
            } else if (field.getOriginalFieldId().equals(ACCESS_RESTRICTION_END_DESC.getLocalName())) {
                return;
            } else {
                throw new RuntimeException("Unsupported field: " + field);
            }
        }
    }

    private QName getProp(Map<String, Field> fieldsByOriginalId, QName propName) {
        return fieldsByOriginalId.get(propName.getLocalName()).getQName();
    }

    // =======================================================================================================

    public static class AccessRestrictionState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        private final QName accessRestrictionProp;
        private final QName accessRestrictionReasonProp;
        private final QName accessRestrictionBeginDateProp;
        private final QName accessRestrictionEndDateProp;
        private final QName accessRestrictionEndDescProp;
        private final QName accessRestrictionReasonSelectorProp;
        private final String accessRestrictionReasonClassificatorName;

        public AccessRestrictionState(QName accessRestrictionProp, QName accessRestrictionReasonProp, QName accessRestrictionBeginDateProp, QName accessRestrictionEndDateProp,
                                      QName accessRestrictionEndDescProp, QName accessRestrictionReasonSelectorProp, String accessRestrictionReasonClassificatorName) {
            this.accessRestrictionProp = accessRestrictionProp;
            this.accessRestrictionReasonProp = accessRestrictionReasonProp;
            this.accessRestrictionBeginDateProp = accessRestrictionBeginDateProp;
            this.accessRestrictionEndDateProp = accessRestrictionEndDateProp;
            this.accessRestrictionEndDescProp = accessRestrictionEndDescProp;
            this.accessRestrictionReasonClassificatorName = accessRestrictionReasonClassificatorName;
            this.accessRestrictionReasonSelectorProp = accessRestrictionReasonSelectorProp;
        }

        @Override
        public void reset(boolean inEditMode) {
            if (!inEditMode) {
                final Node document = dialogDataProvider.getNode();
                document.getProperties().put(RepoUtil.createTransientProp(accessRestrictionBeginDateProp.getLocalName() + VIEW_MODE_PROP_SUFFIX).toString(),
                        document.getProperties().get(accessRestrictionBeginDateProp));
            }
        }

        /**
         * Called after selection has been made from series dropdown.<br>
         * If accessRestriction is not filled, then values related to accessRestriction are set according to selected series.
         * 
         * @param submittedValue
         */
        public void updateAccessRestrictionProperties(NodeRef seriesRef) {
            Node document = dialogDataProvider.getNode();
<<<<<<< HEAD
            final Map<String, Object> docProps = document.getProperties();
            final String accessRestriction = (String) docProps.get(accessRestrictionProp.toString());
=======

            final Map<String, Object> docProps = document.getProperties();
            final String accessRestriction = (String) docProps.get(ACCESS_RESTRICTION.toString());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            if (StringUtils.isBlank(accessRestriction)) {
                // read serAccessRestriction-related values from series
                final Series series = getSeriesService().getSeriesByNodeRef(seriesRef);
                final Map<String, Object> seriesProps = series.getNode().getProperties();
                final String serAccessRestriction = (String) seriesProps.get(SeriesModel.Props.ACCESS_RESTRICTION.toString());
                final String serAccessRestrictionReason = (String) seriesProps.get(SeriesModel.Props.ACCESS_RESTRICTION_REASON.toString());
                final Date serAccessRestrictionBeginDate = (Date) seriesProps.get(SeriesModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString());
                final Date serAccessRestrictionEndDate = (Date) seriesProps.get(SeriesModel.Props.ACCESS_RESTRICTION_END_DATE.toString());
                final String serAccessRestrictionEndDesc = (String) seriesProps.get(SeriesModel.Props.ACCESS_RESTRICTION_END_DESC.toString());
                // write them to the document
<<<<<<< HEAD
                docProps.put(accessRestrictionProp.toString(), serAccessRestriction);
                docProps.put(accessRestrictionReasonProp.toString(), serAccessRestrictionReason);
                docProps.put(accessRestrictionBeginDateProp.toString(), serAccessRestrictionBeginDate);
                docProps.put(accessRestrictionEndDateProp.toString(), serAccessRestrictionEndDate);
                docProps.put(accessRestrictionEndDescProp.toString(), serAccessRestrictionEndDesc);
=======
                docProps.put(ACCESS_RESTRICTION.toString(), serAccessRestriction);
                if (!(AccessRestriction.INTERNAL.equals(serAccessRestriction) || AccessRestriction.OPEN.equals(serAccessRestriction))) {
                    docProps.put(ACCESS_RESTRICTION_REASON.toString(), serAccessRestrictionReason);
                    docProps.put(ACCESS_RESTRICTION_BEGIN_DATE.toString(), serAccessRestrictionBeginDate);
                    docProps.put(ACCESS_RESTRICTION_END_DATE.toString(), serAccessRestrictionEndDate);
                    docProps.put(ACCESS_RESTRICTION_END_DESC.toString(), serAccessRestrictionEndDesc);
                } else {
                    setHiddenFieldsNull(docProps);
                }
                calculateAccessRestrictionEndDateOrDesc(serAccessRestrictionReason, docProps, accessRestrictionReasonClassificatorName, accessRestrictionBeginDateProp,
                        accessRestrictionEndDateProp, accessRestrictionEndDescProp);
                clearPropertySheet();
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            }
        }

        public void accessRestrictionValueChanged(ValueChangeEvent event) {
            String oldValue = StringUtils.trimToEmpty((String) event.getOldValue());
            String newValue = StringUtils.trimToEmpty((String) event.getNewValue());
            if (ObjectUtils.equals(oldValue, newValue)) {
                return;
            }
            final String accessRestriction = (String) event.getNewValue();
            ComponentUtil.executeLater(PhaseId.INVOKE_APPLICATION, dialogDataProvider.getPropertySheet(), new Closure() {
                @Override
                public void execute(Object input) {
                    if (AccessRestriction.AK.equals(accessRestriction) || AccessRestriction.LIMITED.equals(accessRestriction)) {
                        Node document = dialogDataProvider.getNode();
                        final Map<String, Object> docProps = document.getProperties();
                        if (docProps.get(accessRestrictionBeginDateProp.toString()) == null) {
                            docProps.put(accessRestrictionBeginDateProp.toString(), new Date());
                        }
                    } else if (AccessRestriction.INTERNAL.equals(accessRestriction) || AccessRestriction.OPEN.equals(accessRestriction)) {
                        final Map<String, Object> docProps = dialogDataProvider.getNode().getProperties();
                        docProps.put(accessRestrictionReasonProp.toString(), null);
                        docProps.put(accessRestrictionBeginDateProp.toString(), null);
                        docProps.put(accessRestrictionEndDateProp.toString(), null);
                        docProps.put(accessRestrictionEndDescProp.toString(), null);
                    }
                    clearPropertySheet();
                }
            });
        }

        public void accessRestrictionReasonValueChanged(ValueChangeEvent event) {
            String oldValue = StringUtils.trimToEmpty((String) event.getOldValue());
            String newValue = StringUtils.trimToEmpty((String) event.getNewValue());
            if (ObjectUtils.equals(oldValue, newValue)) {
                return;
            }
            final String accessRestrictionReason = (String) event.getNewValue();
            ComponentUtil.executeLater(PhaseId.INVOKE_APPLICATION, dialogDataProvider.getPropertySheet(), new Closure() {
                @Override
                public void execute(Object input) {
<<<<<<< HEAD
                    String valueData = BeanHelper.getClassificatorService().getClassificatorValuesValueData(accessRestrictionReasonClassificatorName, accessRestrictionReason);
                    Integer monthsToAdd = null;
                    try {
                        monthsToAdd = Integer.parseInt(valueData);
                    } catch (NumberFormatException e) {
                        // no need to add date
                    }
                    final Map<String, Object> docProps = dialogDataProvider.getNode().getProperties();
                    if (monthsToAdd != null) {
                        Date restrictionBeginDate = (Date) docProps.get(accessRestrictionBeginDateProp.toString());
                        if (restrictionBeginDate != null) {
                            Date newRestrictionEndDate = DateUtils.addMonths(restrictionBeginDate, monthsToAdd);
                            Date restrictionEndDate = (Date) docProps.get(accessRestrictionEndDateProp.toString());
                            if (restrictionEndDate == null || restrictionEndDate.before(newRestrictionEndDate)) {
                                docProps.put(accessRestrictionEndDateProp.toString(), newRestrictionEndDate);
                            }
                        }
                    } else if (StringUtils.isNotBlank(valueData)) {
                        String accessRestrictionEndDesc = (String) docProps.get(accessRestrictionEndDescProp.toString());
                        String newAccessRestrictionEndDesc = StringUtils.isBlank(accessRestrictionEndDesc) ? valueData : accessRestrictionEndDesc + ", "
                                + valueData;
                        docProps.put(accessRestrictionEndDescProp.toString(), newAccessRestrictionEndDesc);
                    }
=======
                    final Map<String, Object> docProps = dialogDataProvider.getNode().getProperties();
                    calculateAccessRestrictionEndDateOrDesc(accessRestrictionReason, docProps, accessRestrictionReasonClassificatorName, accessRestrictionBeginDateProp,
                            accessRestrictionEndDateProp, accessRestrictionEndDescProp);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                    String currentAccessRestrictionReason = (String) docProps.get(accessRestrictionReasonProp.toString());
                    String newAccessRestrictionReason = StringUtils.isBlank(currentAccessRestrictionReason) ? accessRestrictionReason : currentAccessRestrictionReason + ", "
                            + accessRestrictionReason;
                    docProps.put(accessRestrictionReasonProp.toString(), newAccessRestrictionReason);
                    clearPropertySheet();
                    addSelectorValueToContext(accessRestrictionReason);
                }

                protected void addSelectorValueToContext(final String accessRestrictionReason) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
                    requestMap.put(accessRestrictionReasonSelectorProp.toString(), new Object[] { accessRestrictionReason });
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

        public boolean isRenderAllAccessRestrictionFields() {
<<<<<<< HEAD
            String accessRestriction = (String) dialogDataProvider.getNode().getProperties().get(accessRestrictionProp);
            return !AccessRestriction.OPEN.equals(accessRestriction) && !AccessRestriction.INTERNAL.equals(accessRestriction);
        }

        /**
         * CaseFileDialog evaluates with org.alfresco.web.bean.generator.BaseComponentGenerator.evaluateBoolean(String, FacesContext, PropertySheetItem)
         */
        public boolean renderAllAccessRestrictionFields(@SuppressWarnings("unused") PropertySheetItem propertySheetItem) {
            return isRenderAllAccessRestrictionFields();
        }
=======
            String accessRestriction = (String) dialogDataProvider.getDocument().getProp(ACCESS_RESTRICTION);
            return !AccessRestriction.OPEN.equals(accessRestriction) && !AccessRestriction.INTERNAL.equals(accessRestriction);
        }
    }

    private static void calculateAccessRestrictionEndDateOrDesc(final String accessRestrictionReason, final Map<String, Object> docProps,
            String accessRestrictionReasonClassificatorName, QName accessRestrictionBeginDateProp, QName accessRestrictionEndDateProp,
            QName accessRestrictionEndDescProp) {
        String valueData = BeanHelper.getClassificatorService().getClassificatorValuesValueData(accessRestrictionReasonClassificatorName, accessRestrictionReason);
        Integer monthsToAdd = null;
        try {
            monthsToAdd = Integer.parseInt(valueData);
        } catch (NumberFormatException e) {
            // no need to add date
        }
        if (monthsToAdd != null) {
            Date restrictionBeginDate = (Date) docProps.get(accessRestrictionBeginDateProp.toString());
            if (restrictionBeginDate != null) {
                Date newRestrictionEndDate = DateUtils.addMonths(restrictionBeginDate, monthsToAdd);
                Date restrictionEndDate = (Date) docProps.get(accessRestrictionEndDateProp.toString());
                if (restrictionEndDate == null || restrictionEndDate.before(newRestrictionEndDate)) {
                    docProps.put(accessRestrictionEndDateProp.toString(), newRestrictionEndDate);
                }
            }
        } else if (StringUtils.isNotBlank(valueData)) {
            String accessRestrictionEndDesc = (String) docProps.get(accessRestrictionEndDescProp.toString());
            String newAccessRestrictionEndDesc = StringUtils.isBlank(accessRestrictionEndDesc) ? valueData : accessRestrictionEndDesc + ", "
                    + valueData;
            docProps.put(accessRestrictionEndDescProp.toString(), newAccessRestrictionEndDesc);
        }
    }

    public static void calculateAccessRestrictionValues(FieldGroup accessRestrictionGroup, Map<String, Object> props) {
        Field accessRestrictionReasonField = accessRestrictionGroup.getFieldsByOriginalId().get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.getLocalName());
        Field accessRestrictionBeginDateField = accessRestrictionGroup.getFieldsByOriginalId().get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.getLocalName());
        Field accessRestrictionEndDateField = accessRestrictionGroup.getFieldsByOriginalId().get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.getLocalName());
        Field accessRestrictionEndDescField = accessRestrictionGroup.getFieldsByOriginalId().get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC.getLocalName());
        if (accessRestrictionReasonField == null || accessRestrictionBeginDateField == null || accessRestrictionEndDateField == null || accessRestrictionEndDescField == null) {
            return;
        }
        String accessRestrictionReasonClassificatorName = accessRestrictionReasonField.getClassificator();
        String accessRestrictionReason = (String) props.get(accessRestrictionReasonField.getQName().toString());
        calculateAccessRestrictionEndDateOrDesc(accessRestrictionReason, props, accessRestrictionReasonClassificatorName,
                accessRestrictionBeginDateField.getQName(), accessRestrictionEndDateField.getQName(), accessRestrictionEndDescField.getQName());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    // ===============================================================================================================================

    private NodeService nodeService;
    private AdrService adrService;
    private DocumentLogService documentLogService;
    private DocumentAdminService documentAdminService;
    private ClassificatorService classificatorService;

    @Override
<<<<<<< HEAD
    public void validate(DynamicBase dynamicObject, ValidationHelper validationHelper) {
        if (!(dynamicObject instanceof DocumentDynamic)) {
            return;
        }
        DocumentDynamic document = (DocumentDynamic) dynamicObject;
=======
    public void validate(DocumentDynamic document, ValidationHelper validationHelper) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

        // Validate accessRestriction value against active classificator values, because if document comes from DVK,
        // then raw value is put into accessRestriction property and document manager must correct this manually now
        List<String> activeValues = new ArrayList<String>();
        boolean foundValidValue = false;
        List<ClassificatorValue> classificatorValues = classificatorService.getAllClassificatorValues("accessRestriction");
        for (ClassificatorValue classificatorValue : classificatorValues) {
            if (classificatorValue.isActive()) {
                activeValues.add(classificatorValue.getValueName());
                if (classificatorValue.getValueName().equals(document.getAccessRestriction())) {
                    foundValidValue = true;
                    break;
                }
            }
        }

        if (!foundValidValue) {
            String fieldName = validationHelper.getPropDefs().get(DocumentCommonModel.Props.ACCESS_RESTRICTION.getLocalName()).getSecond().getName();
            validationHelper.addErrorMessage("docdyn_accessRestriction_notValid", fieldName, TextUtil.joinNonBlankStringsWithComma(activeValues));
            return;
        }

        final NodeRef nodeRef = document.getNodeRef();
        if (document.isDraftOrImapOrDvk()) {
            return;
        }

        final Map<QName, Serializable> oldProps = nodeService.getProperties(nodeRef);
        if (getChangedAccessRestrictionFieldIds(document, oldProps).isEmpty()) {
            return;
        }

        // If user changed the access restriction, verify that reason was also changed
        final String reason = (String) document.getProp(DocumentDynamicDialog.TEMP_ACCESS_RESTRICTION_CHANGE_REASON);
        if (StringUtils.isNotBlank(reason)) {
<<<<<<< HEAD
            if (!Boolean.TRUE.equals(document.getProp(DocumentDynamicDialog.TEMP_VALIDATE_WITHOUT_SAVE))) {
                // Reset the reason in repository so DocumentPropertyChangeHolder can pick up the change when user enters the same value
                nodeService.removeProperty(nodeRef, ACCESS_RESTRICTION_CHANGE_REASON);
                document.setProp(ACCESS_RESTRICTION_CHANGE_REASON, reason);
                document.getNode().getProperties().remove(DocumentDynamicDialog.TEMP_ACCESS_RESTRICTION_CHANGE_REASON);
            }
=======
            // Reset the reason in repository so DocumentPropertyChangeHolder can pick up the change when user enters the same value
            nodeService.removeProperty(nodeRef, ACCESS_RESTRICTION_CHANGE_REASON);
            document.setProp(ACCESS_RESTRICTION_CHANGE_REASON, reason);
            document.getNode().getProperties().remove(DocumentDynamicDialog.TEMP_ACCESS_RESTRICTION_CHANGE_REASON);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            return;
        }

        validationHelper.addErrorMessage(AccessRestrictionGenerator.ACCESS_RESTRICTION_CHANGE_REASON_ERROR);
    }

    @Override
<<<<<<< HEAD
    public void save(DynamicBase dynamicObject) {
        if (!(dynamicObject instanceof DocumentDynamic)) {
            return;
        }

        DocumentDynamic document = (DocumentDynamic) dynamicObject;
=======
    public void save(DocumentDynamic document) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        NodeRef docRef = document.getNodeRef();
        Map<String, Object> newProps = document.getNode().getProperties();
        final Map<QName, Serializable> oldProps = nodeService.getProperties(docRef);

<<<<<<< HEAD
        boolean markAsDeleted = false;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        // If accessRestriction changes from OPEN/AK to INTERNAL/LIMITED
        String accessRestriction = (String) newProps.get(ACCESS_RESTRICTION);
        if (AccessRestriction.INTERNAL.equals(accessRestriction) || AccessRestriction.LIMITED.equals(accessRestriction)) {
            String oldAccessRestriction = (String) oldProps.get(ACCESS_RESTRICTION);
            if (!(AccessRestriction.INTERNAL.equals(oldAccessRestriction) || AccessRestriction.LIMITED.equals(oldAccessRestriction))) {
<<<<<<< HEAD
                markAsDeleted = true;
            }
        }

        // Mark the document as deleted if publishToAdr value is set to NOT_TO_ADR
        if (!markAsDeleted && PublishToAdr.NOT_TO_ADR.getValueName().equals(newProps.get(DocumentDynamicModel.Props.PUBLISH_TO_ADR))) {
            String oldPublishToAdr = (String) oldProps.get(DocumentDynamicModel.Props.PUBLISH_TO_ADR);
            if (!PublishToAdr.NOT_TO_ADR.getValueName().equals(oldPublishToAdr)) {
                markAsDeleted = true;
            }
        }

        if (markAsDeleted) {
            adrService.addDeletedDocument(docRef);
        }

        String oldAccessRestriction = (String) oldProps.get(ACCESS_RESTRICTION);
        if (oldAccessRestriction != null && !accessRestriction.equals(oldAccessRestriction)
                && (AccessRestriction.INTERNAL.equals(accessRestriction) || AccessRestriction.OPEN.equals(accessRestriction))) {
=======

                // And if document was FINISHED
                String oldStatus = (String) oldProps.get(DOC_STATUS);
                if (DocumentStatus.FINISHED.equals(oldStatus)) {
                    adrService.addDeletedDocument(docRef);
                }
            }
        }

        if (AccessRestriction.INTERNAL.equals(accessRestriction) || AccessRestriction.OPEN.equals(accessRestriction)) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            setHiddenFieldsNull(newProps);
        }

        if (!document.isDraftOrImapOrDvk()) {
            if (!getChangedAccessRestrictionFieldIds(document, oldProps).isEmpty()) {
                document.setAccessRestrictionPropsChanged(true);
            }
        }
    }

<<<<<<< HEAD
    private void setHiddenFieldsNull(Map<String, Object> newProps) {
=======
    public static void setHiddenFieldsNull(Map<String, Object> newProps) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        newProps.put(ACCESS_RESTRICTION_REASON.toString(), null);
        newProps.put(ACCESS_RESTRICTION_BEGIN_DATE.toString(), null);
        newProps.put(ACCESS_RESTRICTION_END_DATE.toString(), null);
        newProps.put(ACCESS_RESTRICTION_END_DESC.toString(), null);
    }

<<<<<<< HEAD
    public void clearHiddenValues(WmNode node) {
        Map<String, Object> properties = node.getProperties();
        String accessRestriction = (String) properties.get(ACCESS_RESTRICTION);
        if (AccessRestriction.INTERNAL.equals(accessRestriction) || AccessRestriction.OPEN.equals(accessRestriction)) {
            setHiddenFieldsNull(properties);
        }
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    private List<String> getChangedAccessRestrictionFieldIds(DocumentDynamic document, Map<QName, Serializable> oldProps) {
        List<String> fields = new ArrayList<String>();
        for (QName propName : ACCESS_RESTRICTION_PROPS) {
            Serializable docProp = document.getProp(propName);
            Serializable oldProp = oldProps.get(propName);

            // Ignore differences between null values as empty strings
            if (docProp instanceof String && StringUtils.isBlank((String) docProp)) {
                docProp = null;
            }
            if (oldProp instanceof String && StringUtils.isBlank((String) oldProp)) {
                oldProp = null;
            }

            if (ObjectUtils.equals(docProp, oldProp)) {
                continue;
            }
            fields.add(propName.getLocalName());
        }
        return fields;
    }

    // START: setters
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setAdrService(AdrService adrService) {
        this.adrService = adrService;
    }

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setClassificatorService(ClassificatorService classificatorService) {
        this.classificatorService = classificatorService;
    }

    // END: setters

}
