package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.PROPERTIES_SEPARATOR;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSeriesService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.DOC_STATUS;

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
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorAndTextGenerator;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.PropsBuilder;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
public class AccessRestrictionGenerator extends BaseSystematicFieldGenerator {

    private static final String VIEW_MODE_PROP_SUFFIX = "_view_mode";
    public static final String ACCESS_RESTRICTION_CHANGE_REASON_ERROR = "accessRestrictionChangeReasonError";

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

        Map<String, Field> fieldsByOriginalId = ((FieldGroup) field.getParent()).getFieldsByOriginalId();
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
            item.getCustomAttributes().put("rendered", getBindingName("renderAllAccessRestrictionFields", stateHolderKey));
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
                        itemLabel,
                        namespaceService);
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
                ItemConfigVO viewModeItem = generatorResults.generateAndAddViewModeText(accessRestrictionBeginDateProp.getLocalName() + VIEW_MODE_PROP_SUFFIX, itemLabel);
                viewModeItem.setRendered(getBindingName("renderAllAccessRestrictionFields", stateHolderKey));
                components = DurationGenerator.generateDurationFields(field, viewModeItem, accessRestrictionBeginDateProp, accessRestrictionEndDateProp,
                        itemLabel,
                        namespaceService);
                viewModeItem.setName(accessRestrictionBeginDateProp.getLocalName() + VIEW_MODE_PROP_SUFFIX);
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
                document.getProperties().put(accessRestrictionBeginDateProp.getLocalName() + VIEW_MODE_PROP_SUFFIX, document.getProperties().get(accessRestrictionBeginDateProp));
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

            final Map<String, Object> docProps = document.getProperties();
            final String accessRestriction = (String) docProps.get(ACCESS_RESTRICTION.toString());
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
                docProps.put(ACCESS_RESTRICTION.toString(), serAccessRestriction);
                docProps.put(ACCESS_RESTRICTION_REASON.toString(), serAccessRestrictionReason);
                docProps.put(ACCESS_RESTRICTION_BEGIN_DATE.toString(), serAccessRestrictionBeginDate);
                docProps.put(ACCESS_RESTRICTION_END_DATE.toString(), serAccessRestrictionEndDate);
                docProps.put(ACCESS_RESTRICTION_END_DESC.toString(), serAccessRestrictionEndDesc);
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
                    String valueData = BeanHelper.getClassificatorService().getClassificatorValuesValueData(accessRestrictionReasonClassificatorName, accessRestrictionReason);
                    Integer monthsToAdd = null;
                    try {
                        monthsToAdd = Integer.parseInt(valueData);
                    } catch (NumberFormatException e) {
                        // use as String value
                    }
                    final Map<String, Object> docProps = dialogDataProvider.getNode().getProperties();
                    if (monthsToAdd != null) {
                        Date restrictionBeginDate = (Date) docProps.get(accessRestrictionBeginDateProp.toString());
                        if (restrictionBeginDate != null) {
                            Date newRestrictionEndDate = DateUtils.addMonths(restrictionBeginDate, monthsToAdd);
                            Date restrictionEndDate = (Date) docProps.get(accessRestrictionEndDateProp.toString());
                            if (restrictionEndDate == null || restrictionEndDate.before(newRestrictionEndDate)) {
                                docProps.put(accessRestrictionEndDateProp.toString(), newRestrictionEndDate);
                                clearPropertySheet();
                                addSelectorValueToContext(accessRestrictionReason);
                                ;
                            }
                        }
                    } else {
                        String accessRestrictionEndDesc = (String) docProps.get(accessRestrictionReasonProp.toString());
                        String newAccessRestrictionEndDesc = StringUtils.isBlank(accessRestrictionEndDesc) ? accessRestrictionReason : accessRestrictionEndDesc + ", "
                                + accessRestrictionReason;
                        docProps.put(accessRestrictionReasonProp.toString(), newAccessRestrictionEndDesc);
                        clearPropertySheet();
                        addSelectorValueToContext(accessRestrictionReason);
                    }
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
            String accessRestriction = (String) dialogDataProvider.getDocument().getProp(ACCESS_RESTRICTION);
            return !AccessRestriction.OPEN.equals(accessRestriction) && !AccessRestriction.INTERNAL.equals(accessRestriction);
        }
    }

    // ===============================================================================================================================

    private NodeService nodeService;
    private AdrService adrService;
    private DocumentLogService documentLogService;
    private DocumentAdminService documentAdminService;

    @Override
    public void validate(DocumentDynamic document, ValidationHelper validationHelper) {
        final NodeRef nodeRef = document.getNodeRef();
        if (document.isDraftOrImapOrDvk()) {
            return;
        }

        final Map<QName, Serializable> oldProps = nodeService.getProperties(nodeRef);
        if (getChangedAccessRestrictionFieldIds(document, oldProps).isEmpty()) {
            return;
        }

        // If user changed the access restriction, verify that reason was also changed
        final String reason = (String) document.getProp(DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON);
        if (StringUtils.isNotBlank(reason) && !StringUtils.equals(reason, (String) oldProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON))) {
            return;
        }

        validationHelper.addErrorMessage(AccessRestrictionGenerator.ACCESS_RESTRICTION_CHANGE_REASON_ERROR);
    }

    @Override
    public void save(DocumentDynamic document) {
        NodeRef docRef = document.getNodeRef();
        Map<String, Object> newProps = document.getNode().getProperties();
        final Map<QName, Serializable> oldProps = nodeService.getProperties(docRef);

        // If accessRestriction changes from OPEN/AK to INTERNAL/LIMITED
        String accessRestriction = (String) newProps.get(ACCESS_RESTRICTION);
        if (AccessRestriction.INTERNAL.equals(accessRestriction) || AccessRestriction.LIMITED.equals(accessRestriction)) {
            String oldAccessRestriction = (String) oldProps.get(ACCESS_RESTRICTION);
            if (!(AccessRestriction.INTERNAL.equals(oldAccessRestriction) || AccessRestriction.LIMITED.equals(oldAccessRestriction))) {

                // And if document was FINISHED
                String oldStatus = (String) oldProps.get(DOC_STATUS);
                if (DocumentStatus.FINISHED.equals(oldStatus)) {
                    adrService.addDeletedDocument(docRef);
                }
            }
        }

        // Log changes
        if (!document.isDraftOrImapOrDvk()) {
            // TODO refactor, so that accessRestriction changes would be logged in DocumentDynamicServiceImpl.logChangedProp, not here
            final List<String> changedAccessRestrictionFieldIds = getChangedAccessRestrictionFieldIds(document, oldProps);
            if (changedAccessRestrictionFieldIds.isEmpty()) {
                return;
            }
            document.setAccessRestrictionPropsChanged(true);
            final List<FieldDefinition> fields = documentAdminService.getFieldDefinitions(changedAccessRestrictionFieldIds);
            final String reason = document.getProp(DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON);
            String emptyValue = MessageUtil.getMessage("document_log_status_empty");
            for (FieldDefinition field : fields) {
                Serializable oldPropValue = oldProps.get(field.getQName());
                if (oldPropValue == null || oldPropValue instanceof String && StringUtils.isBlank((String) oldPropValue)) {
                    oldPropValue = emptyValue;
                }
                Object newPropValue = newProps.get(field.getQName());
                if (newPropValue == null || newPropValue instanceof String && StringUtils.isBlank((String) newPropValue)) {
                    newPropValue = emptyValue;
                }
                documentLogService.addDocumentLog(docRef, MessageUtil.getMessage("document_log_status_accessRestrictionChanged"
                        , field.getName(), oldPropValue, newPropValue, reason));
            }
        }
    }

    private List<String> getChangedAccessRestrictionFieldIds(DocumentDynamic document, Map<QName, Serializable> oldProps) {
        List<String> fields = new ArrayList<String>();
        for (QName propName : ACCESS_RESTRICTION_PROPS) {
            if (!ObjectUtils.equals(document.getProp(propName), oldProps.get(propName))) {
                fields.add(propName.getLocalName());
            }
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

    // END: setters

}
