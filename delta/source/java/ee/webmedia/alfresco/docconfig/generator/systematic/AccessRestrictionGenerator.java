package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.DOC_STATUS;

import java.util.Date;
import java.util.Map;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;

/**
 * @author Alar Kvell
 */
public class AccessRestrictionGenerator extends BaseSystematicFieldGenerator implements FieldGroupGenerator {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AccessRestrictionGenerator.class);

    // TODO DLSeadist: ensure that everything works according to originalFieldId<-->fieldId

    @Override
    protected String[] getOriginalFieldIds() {
        return new String[] {
                DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.getLocalName(),
                DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.getLocalName(),
                DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.getLocalName() };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        if (field.getOriginalFieldId().equals(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.getLocalName())) {
            item.setMandatoryIf("accessRestriction");
            return;
        } else if (field.getOriginalFieldId().equals(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.getLocalName())) {
            item.setMandatoryIf("accessRestriction");
            return;
        } else if (field.getOriginalFieldId().equals(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.getLocalName())) {
            item.setMandatoryIf("accessRestriction,accessRestrictionEndDesc=null");
            return;
        }
        throw new RuntimeException("Unsupported field: " + field);
    }

    @Override
    public void generateFieldGroup(FieldGroup fieldGroup, GeneratorResults generatorResults) {
        generatorResults.addStateHolder(getStateHolderKey(), new AccessRestrictionState());
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
            final String accessRestriction = (String) docProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString());
            if (StringUtils.isBlank(accessRestriction)) {
                // read serAccessRestriction-related values from series
                final Series series = BeanHelper.getSeriesService().getSeriesByNodeRef(seriesRef);
                final Map<String, Object> seriesProps = series.getNode().getProperties();
                final String serAccessRestriction = (String) seriesProps.get(SeriesModel.Props.ACCESS_RESTRICTION.toString());
                final String serAccessRestrictionReason = (String) seriesProps.get(SeriesModel.Props.ACCESS_RESTRICTION_REASON.toString());
                final Date serAccessRestrictionBeginDate = (Date) seriesProps.get(SeriesModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString());
                final Date serAccessRestrictionEndDate = (Date) seriesProps.get(SeriesModel.Props.ACCESS_RESTRICTION_END_DATE.toString());
                final String serAccessRestrictionEndDesc = (String) seriesProps.get(SeriesModel.Props.ACCESS_RESTRICTION_END_DESC.toString());
                // write them to the document
                docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString(), serAccessRestriction);
                docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.toString(), serAccessRestrictionReason);
                docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString(), serAccessRestrictionBeginDate);
                docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.toString(), serAccessRestrictionEndDate);
                docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC.toString(), serAccessRestrictionEndDesc);
            }
        }

    }

    // ===============================================================================================================================

    private NodeService nodeService;
    private AdrService adrService;
    private DocumentLogService documentLogService;

    @Override
    public void save(DocumentDynamic document) {
        NodeRef docRef = document.getNodeRef();
        Map<String, Object> docProps = document.getNode().getProperties();

        // If accessRestriction changes from OPEN/AK to INTERNAL/LIMITED
        if (AccessRestriction.INTERNAL.equals((String) docProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION))
                || AccessRestriction.LIMITED.equals((String) docProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION))) {
            String oldAccessRestriction = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.ACCESS_RESTRICTION);
            if (!(AccessRestriction.INTERNAL.equals(oldAccessRestriction) || AccessRestriction.LIMITED.equals(oldAccessRestriction))) {

                // And if document was FINISHED
                String oldStatus = (String) nodeService.getProperty(docRef, DOC_STATUS);
                if (DocumentStatus.FINISHED.equals(oldStatus)) {
                    adrService.addDeletedDocument(docRef);
                }
            }
        }

        final String previousAccessrestriction = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.ACCESS_RESTRICTION);
        final String newAccessrestriction = (String) docProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
        if (!document.isDraft() && !StringUtils.equals(previousAccessrestriction, newAccessrestriction)) {
            documentLogService.addDocumentLog(docRef, I18NUtil.getMessage("document_log_status_accessRestrictionChanged"));
        }
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
    // END: setters

}
