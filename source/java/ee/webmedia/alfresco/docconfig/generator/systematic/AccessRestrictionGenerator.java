package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.PROPERTIES_SEPARATOR;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSeriesService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON;

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
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.PublishToAdr;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorAndTextGenerator;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.PropsBuilder;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
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

public class AccessRestrictionGenerator extends BaseSystematicFieldGenerator {

    public static final String BEAN_NAME = "accessRestrictionGenerator";

    private static final String VIEW_MODE_PROP_SUFFIX = "View";
    public static final String ACCESS_RESTRICTION_CHANGE_REASON_ERROR = "access_restriction_change_reason_error";

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

        FieldGroup group = (FieldGroup) field.getParent();
        Map<String, Field> fieldsByOriginalId = group.getFieldsByOriginalId();
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
            item.setRendered(getBindingName("renderAllAccessRestrictionFields", stateHolderKey));
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
                        itemLabel, group, namespaceService);
                List<String> componentsWithMandatoryIf = new ArrayList<String>();
                componentsWithMandatoryIf.add(components.get(0) + "¤mandatoryIf=" + accessRestrictionPropName);
                componentsWithMandatoryIf.add(components.get(1) + "¤mandatoryIf=" + accessRestrictionPropName);
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
                        itemLabel, group, namespaceService);
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
        public void updateAccessRestrictionProperties(NodeRef seriesRef, DialogDataProvider otherDialogDataProvider) {
            Node node = dialogDataProvider.getNode();
            if (node == null) { // In some cases (if this state is called via BeanHelper.getPropertySheetStateBean())
                // it can happen that this dialogDataProvider is pointing to wrong object and returns null;
                node = otherDialogDataProvider.getNode();
                if (node == null) {
                    return;
                }
            }
            final Map<String, Object> docProps = node.getProperties();
            setAccessRestrictionFromSeries(seriesRef, docProps, accessRestrictionProp, accessRestrictionReasonProp,
                    accessRestrictionBeginDateProp, accessRestrictionEndDateProp, accessRestrictionEndDescProp, accessRestrictionReasonClassificatorName);
            if (StringUtils.isBlank((String) docProps.get(accessRestrictionProp.toString()))) {
                clearPropertySheet();
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
                    final Map<String, Object> docProps = dialogDataProvider.getNode().getProperties();
                    calculateAccessRestrictionEndDateOrDesc(accessRestrictionReason, docProps, accessRestrictionReasonClassificatorName, accessRestrictionBeginDateProp,
                            accessRestrictionEndDateProp, accessRestrictionEndDescProp);
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
            String accessRestriction = (String) dialogDataProvider.getNode().getProperties().get(accessRestrictionProp);
            return !AccessRestriction.OPEN.equals(accessRestriction) && !AccessRestriction.INTERNAL.equals(accessRestriction);
        }

        /**
         * CaseFileDialog evaluates with org.alfresco.web.bean.generator.BaseComponentGenerator.evaluateBoolean(String, FacesContext, PropertySheetItem)
         */
        public boolean renderAllAccessRestrictionFields(@SuppressWarnings("unused") PropertySheetItem propertySheetItem) {
            return isRenderAllAccessRestrictionFields();
        }
    }

    private static void calculateAccessRestrictionEndDateOrDesc(final String accessRestrictionReason, final Map<String, Object> docProps,
            String accessRestrictionReasonClassificatorName, QName accessRestrictionBeginDateProp, QName accessRestrictionEndDateProp,
            QName accessRestrictionEndDescProp) {
        String valueData = BeanHelper.getClassificatorService().getClassificatorValuesValueData(accessRestrictionReasonClassificatorName, accessRestrictionReason);
        if(valueData == null){
        	return;
        }
        Integer monthsToAdd = null;
        String newAccessRestrictionEndDescData = "";
        
        for(String valueDataPart : valueData.split(";")){
        	monthsToAdd = null;
        	valueDataPart = valueDataPart.trim();
	        try {
	            monthsToAdd = Integer.parseInt(valueDataPart);
	        } catch (NumberFormatException e) {
	            // no need to add date
	        	newAccessRestrictionEndDescData += valueDataPart;
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
	        } else if (StringUtils.isNotBlank(newAccessRestrictionEndDescData)) {
	            String accessRestrictionEndDesc = (String) docProps.get(accessRestrictionEndDescProp.toString());
	            String newAccessRestrictionEndDesc = StringUtils.isBlank(accessRestrictionEndDesc) ? newAccessRestrictionEndDescData : accessRestrictionEndDesc + ", "
	                    + newAccessRestrictionEndDescData;
	            docProps.put(accessRestrictionEndDescProp.toString(), newAccessRestrictionEndDesc);
	        }
        }
    }

    public static void calculateAccessRestrictionValues(FieldGroup accessRestrictionGroup, Map<String, Object> props) {
        Field accessRestrictionReasonField = getFieldFromGroup(accessRestrictionGroup, DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON);
        Field accessRestrictionBeginDateField = getFieldFromGroup(accessRestrictionGroup, DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE);
        Field accessRestrictionEndDateField = getFieldFromGroup(accessRestrictionGroup, DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE);
        Field accessRestrictionEndDescField = getFieldFromGroup(accessRestrictionGroup, DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC);
        if (accessRestrictionReasonField == null || accessRestrictionBeginDateField == null || accessRestrictionEndDateField == null || accessRestrictionEndDescField == null) {
            return;
        }
        String accessRestrictionReasonClassificatorName = accessRestrictionReasonField.getClassificator();
        String accessRestrictionReason = (String) props.get(accessRestrictionReasonField.getQName().toString());
        calculateAccessRestrictionEndDateOrDesc(accessRestrictionReason, props, accessRestrictionReasonClassificatorName,
                accessRestrictionBeginDateField.getQName(), accessRestrictionEndDateField.getQName(), accessRestrictionEndDescField.getQName());
    }

    public static Field getFieldFromGroup(FieldGroup accessRestrictionGroup, QName propQName) {
        return accessRestrictionGroup.getFieldsByOriginalId().get(propQName.getLocalName());
    }

    public static void setAccessRestrictionFromSeries(FieldGroup documentLocationGroup, FieldGroup accessRestrictionGroup, Map<String, Object> docProps) {
        Field seriesField = documentLocationGroup.getFieldsByOriginalId().get(DocumentCommonModel.Props.SERIES.getLocalName());
        if (seriesField == null) {
            return;
        }
        NodeRef seriesRef = (NodeRef) docProps.get(seriesField.getQName().toString());
        if (seriesRef == null || !BeanHelper.getNodeService().exists(seriesRef)) {
            return;
        }
        Field accessRestrictionField = getFieldFromGroup(accessRestrictionGroup, DocumentCommonModel.Props.ACCESS_RESTRICTION);
        Field accessRestrictionReasonField = getFieldFromGroup(accessRestrictionGroup, DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON);
        Field accessRestrictionBeginDateField = getFieldFromGroup(accessRestrictionGroup, DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE);
        Field accessRestrictionEndDateField = getFieldFromGroup(accessRestrictionGroup, DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE);
        Field accessRestrictionEndDescField = getFieldFromGroup(accessRestrictionGroup, DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC);
        if (accessRestrictionField == null || accessRestrictionReasonField == null || accessRestrictionBeginDateField == null || accessRestrictionEndDateField == null
                || accessRestrictionEndDescField == null) {
            return;
        }
        String accessRestrictionReasonClassificatorName = accessRestrictionReasonField.getClassificator();
        setAccessRestrictionFromSeries(seriesRef, docProps, accessRestrictionField.getQName(), accessRestrictionReasonField.getQName(), accessRestrictionBeginDateField.getQName(),
                accessRestrictionEndDateField.getQName(), accessRestrictionEndDescField.getQName(), accessRestrictionReasonClassificatorName);
    }

    private static void setAccessRestrictionFromSeries(NodeRef seriesRef, final Map<String, Object> docProps, QName accessRestrictionProp, QName accessRestrictionReasonProp,
            QName accessRestrictionBeginDateProp, QName accessRestrictionEndDateProp, QName accessRestrictionEndDescProp, String accessRestrictionReasonClassificatorName) {
        if (seriesRef == null) {
            return;
        }
        final String accessRestriction = (String) docProps.get(accessRestrictionProp.toString());
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
            docProps.put(accessRestrictionProp.toString(), serAccessRestriction);
            if (!(AccessRestriction.INTERNAL.equals(serAccessRestriction) || AccessRestriction.OPEN.equals(serAccessRestriction))) {
                docProps.put(accessRestrictionReasonProp.toString(), serAccessRestrictionReason);
                docProps.put(accessRestrictionBeginDateProp.toString(), serAccessRestrictionBeginDate);
                docProps.put(accessRestrictionEndDateProp.toString(), serAccessRestrictionEndDate);
                docProps.put(accessRestrictionEndDescProp.toString(), serAccessRestrictionEndDesc);
            } else {
                setHiddenFieldsNull(docProps);
            }
            calculateAccessRestrictionEndDateOrDesc(serAccessRestrictionReason, docProps, accessRestrictionReasonClassificatorName, accessRestrictionBeginDateProp,
                    accessRestrictionEndDateProp, accessRestrictionEndDescProp);
        }
    }

    // ===============================================================================================================================

    private NodeService nodeService;
    private AdrService adrService;
    private DocumentLogService documentLogService;
    private DocumentAdminService documentAdminService;
    private ClassificatorService classificatorService;

    @Override
    public void validate(DynamicBase dynamicObject, ValidationHelper validationHelper) {
        if (!(dynamicObject instanceof DocumentDynamic)) {
            return;
        }
        DocumentDynamic document = (DocumentDynamic) dynamicObject;

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
            if (!Boolean.TRUE.equals(document.getProp(DocumentDynamicDialog.TEMP_VALIDATE_WITHOUT_SAVE))) {
                // Reset the reason in repository so DocumentPropertyChangeHolder can pick up the change when user enters the same value
                nodeService.removeProperty(nodeRef, ACCESS_RESTRICTION_CHANGE_REASON);
                document.setProp(ACCESS_RESTRICTION_CHANGE_REASON, reason);
                document.getNode().getProperties().remove(DocumentDynamicDialog.TEMP_ACCESS_RESTRICTION_CHANGE_REASON);
            }
            return;
        }

        validationHelper.addErrorMessage(AccessRestrictionGenerator.ACCESS_RESTRICTION_CHANGE_REASON_ERROR);
    }

    @Override
    public void save(DynamicBase dynamicObject) {
        if (!(dynamicObject instanceof DocumentDynamic)) {
            return;
        }

        DocumentDynamic document = (DocumentDynamic) dynamicObject;
        NodeRef docRef = document.getNodeRef();
        Map<String, Object> newProps = document.getNode().getProperties();
        final Map<QName, Serializable> oldProps = nodeService.getProperties(docRef);

        boolean markAsDeleted = false;
        // If accessRestriction changes from OPEN/AK to INTERNAL/LIMITED
        String accessRestriction = (String) newProps.get(ACCESS_RESTRICTION);
        if (AccessRestriction.INTERNAL.equals(accessRestriction) || AccessRestriction.LIMITED.equals(accessRestriction)) {
            String oldAccessRestriction = (String) oldProps.get(ACCESS_RESTRICTION);
            if (!(AccessRestriction.INTERNAL.equals(oldAccessRestriction) || AccessRestriction.LIMITED.equals(oldAccessRestriction))) {
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

        if (AccessRestriction.INTERNAL.equals(accessRestriction) || AccessRestriction.OPEN.equals(accessRestriction)) {
            setHiddenFieldsNull(newProps);
        }

        if (!document.isDraftOrImapOrDvk()) {
            if (!getChangedAccessRestrictionFieldIds(document, oldProps).isEmpty()) {
                document.setAccessRestrictionPropsChanged(true);
            }
        }
    }

    public static void setHiddenFieldsNull(Map<String, Object> newProps) {
        newProps.put(ACCESS_RESTRICTION_REASON.toString(), null);
        newProps.put(ACCESS_RESTRICTION_BEGIN_DATE.toString(), null);
        newProps.put(ACCESS_RESTRICTION_END_DATE.toString(), null);
        newProps.put(ACCESS_RESTRICTION_END_DESC.toString(), null);
    }

    public void clearHiddenValues(WmNode node) {
        Map<String, Object> properties = node.getProperties();
        String accessRestriction = (String) properties.get(ACCESS_RESTRICTION);
        if (AccessRestriction.INTERNAL.equals(accessRestriction) || AccessRestriction.OPEN.equals(accessRestriction)) {
            setHiddenFieldsNull(properties);
        }
    }

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
