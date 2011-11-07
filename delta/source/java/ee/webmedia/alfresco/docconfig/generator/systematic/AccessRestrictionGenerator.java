package ee.webmedia.alfresco.docconfig.generator.systematic;

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

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
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
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class AccessRestrictionGenerator extends BaseSystematicFieldGenerator {

    public static final String ACCESS_RESTRICTION_CHANGE_REASON_ERROR = "accessRestrictionChangeReasonError";

    @Override
    public void afterPropertiesSet() {
        documentConfigService.registerHiddenFieldDependency(ACCESS_RESTRICTION_CHANGE_REASON.getLocalName(), ACCESS_RESTRICTION.getLocalName());
        super.afterPropertiesSet();
    }

    @Override
    protected String[] getOriginalFieldIds() {
        return new String[] {
                ACCESS_RESTRICTION.getLocalName(),
                ACCESS_RESTRICTION_REASON.getLocalName(),
                ACCESS_RESTRICTION_BEGIN_DATE.getLocalName(),
                ACCESS_RESTRICTION_END_DATE.getLocalName(),
                ACCESS_RESTRICTION_END_DESC.getLocalName() };
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
        QName accessRestrictionEndDescProp = getProp(fieldsByOriginalId, ACCESS_RESTRICTION_END_DESC);

        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        if (field.getQName().equals(accessRestrictionProp)) {
            String stateHolderKey = field.getFieldId();
            generatorResults.addStateHolder(stateHolderKey, new AccessRestrictionState());
            return;
        } else if (field.getOriginalFieldId().equals(ACCESS_RESTRICTION_REASON.getLocalName())) {
            item.setMandatoryIf(accessRestrictionProp.getLocalName());
            return;
        } else if (field.getOriginalFieldId().equals(ACCESS_RESTRICTION_BEGIN_DATE.getLocalName())) {
            item.setMandatoryIf(accessRestrictionProp.getLocalName());
            return;
        } else if (field.getOriginalFieldId().equals(ACCESS_RESTRICTION_END_DATE.getLocalName())) {
            item.setMandatoryIf(accessRestrictionProp.getLocalName() + "," + accessRestrictionEndDescProp.getLocalName() + "=null");
            return;
        } else if (field.getOriginalFieldId().equals(ACCESS_RESTRICTION_END_DESC.getLocalName())) {
            return;
        }
        throw new RuntimeException("Unsupported field: " + field);
    }

    private QName getProp(Map<String, Field> fieldsByOriginalId, QName propName) {
        return fieldsByOriginalId.get(propName.getLocalName()).getQName();
    }

    // ===============================================================================================================================

    public static class AccessRestrictionState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

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
        if (AccessRestriction.INTERNAL.equals((String) newProps.get(ACCESS_RESTRICTION))
                || AccessRestriction.LIMITED.equals((String) newProps.get(ACCESS_RESTRICTION))) {
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
            final List<String> changedAccessRestrictionFieldIds = getChangedAccessRestrictionFieldIds(document, oldProps);
            if (changedAccessRestrictionFieldIds.isEmpty()) {
                return;
            }

            final List<FieldDefinition> fields = documentAdminService.getFieldDefinitions(changedAccessRestrictionFieldIds);
            final String reason = document.getProp(DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON);
            for (FieldDefinition field : fields) {
                Serializable propValue = oldProps.get(field.getQName());
                if (propValue == null || propValue instanceof String && StringUtils.isBlank((String) propValue)) {
                    propValue = MessageUtil.getMessage("document_log_status_empty");
                }
                documentLogService.addDocumentLog(docRef, MessageUtil.getMessage("document_log_status_accessRestrictionChanged"
                        , field.getName(), propValue, reason));
            }
        }
    }

    private List<String> getChangedAccessRestrictionFieldIds(DocumentDynamic document, Map<QName, Serializable> oldProps) {
        List<String> fields = new ArrayList<String>(5);

        if (!StringUtils.equals((String) document.getProp(ACCESS_RESTRICTION), (String) oldProps.get(ACCESS_RESTRICTION))) {
            fields.add(ACCESS_RESTRICTION.getLocalName());
        }
        if (!StringUtils.equals((String) document.getProp(ACCESS_RESTRICTION_REASON), (String) oldProps.get(ACCESS_RESTRICTION_REASON))) {
            fields.add(ACCESS_RESTRICTION_REASON.getLocalName());
        }
        if (!StringUtils.equals((String) document.getProp(ACCESS_RESTRICTION_END_DESC), (String) oldProps.get(ACCESS_RESTRICTION_END_DESC))) {
            fields.add(ACCESS_RESTRICTION_END_DESC.getLocalName());
        }
        if (!ObjectUtils.equals(document.getProp(ACCESS_RESTRICTION_BEGIN_DATE), oldProps.get(ACCESS_RESTRICTION_BEGIN_DATE))) {
            fields.add(ACCESS_RESTRICTION_BEGIN_DATE.getLocalName());
        }
        if (!ObjectUtils.equals(document.getProp(ACCESS_RESTRICTION_END_DATE), oldProps.get(ACCESS_RESTRICTION_END_DATE))) {
            fields.add(ACCESS_RESTRICTION_END_DATE.getLocalName());
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

    // END: setters

}
