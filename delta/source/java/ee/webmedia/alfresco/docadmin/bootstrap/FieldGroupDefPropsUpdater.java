package ee.webmedia.alfresco.docadmin.bootstrap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Add inapplicableForVol=true to {@link FieldGroup}Definitions that need them, but have been created without them
 * 
 * @author Ats Uiboupin
 */
public class FieldGroupDefPropsUpdater extends AbstractNodeUpdater {
    private static final Set<String> INAPPLICABLE_FOR_VOL = new HashSet<String>(Arrays.asList(
            SystematicFieldGroupNames.SENDER_NAME_AND_EMAIL
            , SystematicFieldGroupNames.SENDER_REG_NUMBER_AND_DATE
            , SystematicFieldGroupNames.REGISTRATION_DATA
            , SystematicFieldGroupNames.DOCUMENT_LOCATION
            , SystematicFieldGroupNames.COMPLIENCE
            , SystematicFieldGroupNames.SUPPLIER
            , SystematicFieldGroupNames.SUPPLIER_CONTACT
            , SystematicFieldGroupNames.INVOICE_DATE_AND_NUMBER
            , SystematicFieldGroupNames.INVOICE_DUE_DATE
            , SystematicFieldGroupNames.TOTAL_SUM
            , SystematicFieldGroupNames.ENTRY_DATE_AND_SAP_NUMBER
            , SystematicFieldGroupNames.CONTRACT_PARTIES
            , SystematicFieldGroupNames.LEAVE_REQUEST
            , SystematicFieldGroupNames.LEAVE_CHANGE
            , SystematicFieldGroupNames.LEAVE_CANCEL
            , SystematicFieldGroupNames.SUBSTITUTE
            , SystematicFieldGroupNames.TRAINING_APPLICANT
            , SystematicFieldGroupNames.ERRAND_DOMESTIC_APPLICANT
            , SystematicFieldGroupNames.ERRAND_ABROAD_APPLICANT
            , SystematicFieldGroupNames.FIRST_PARTY_CONTACT_PERSON
            , SystematicFieldGroupNames.ERRAND_EXPENSES
            , SystematicFieldGroupNames.ERRAND_EXPENSES_REPORT
            , SystematicFieldGroupNames.ERRAND_EXPENSES_REPORT_SUMMARY
            , SystematicFieldGroupNames.DRIVE_COMPENSATION
            , SystematicFieldGroupNames.DRIVE_COMPENSATION_RIK
            ));
    private static final String OLD_DOCUMENT_OWNER = "Dokumendi vastutaja";

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentAdminModel.Types.FIELD_GROUP);
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef fieldGroupDefRef) throws Exception {
        String fieldGroupDefName = (String) nodeService.getProperty(fieldGroupDefRef, DocumentAdminModel.Props.NAME);
        boolean updateInapplicableForVol = INAPPLICABLE_FOR_VOL.contains(fieldGroupDefName);
        if (updateInapplicableForVol) {
            nodeService.setProperty(fieldGroupDefRef, DocumentAdminModel.Props.INAPPLICABLE_FOR_VOL, true);
        }
        boolean isDocOwnerGroup = OLD_DOCUMENT_OWNER.equals(fieldGroupDefName);
        if (isDocOwnerGroup) {
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            // update DOCUMENT_OWNER group name ...
            props.put(DocumentAdminModel.Props.NAME, SystematicFieldGroupNames.DOCUMENT_OWNER);
            // ... and make it mandatory for CaseFileTypes
            props.put(DocumentAdminModel.Props.MANDATORY_FOR_VOL, true);
            nodeService.addProperties(fieldGroupDefRef, props);
        }
        boolean hadInapplicableAspect = nodeService.hasAspect(fieldGroupDefRef, DocumentAdminModel.Aspects.INAPPLICABLE_FOR_TYPE);
        if (!hadInapplicableAspect) {
            nodeService.addAspect(fieldGroupDefRef, DocumentAdminModel.Aspects.INAPPLICABLE_FOR_TYPE, null);
        }
        return new String[] { fieldGroupDefName
                , updateInapplicableForVol ? DocumentAdminModel.Props.INAPPLICABLE_FOR_VOL.getLocalName() : ""
                , isDocOwnerGroup ? DocumentAdminModel.Props.MANDATORY_FOR_VOL.getLocalName() : ""
                , hadInapplicableAspect ? "" : DocumentAdminModel.Aspects.INAPPLICABLE_FOR_TYPE.getLocalName() };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "fieldGroupDefinition.nodeRef", "fieldGroupDefinition.name" };
    }

}
