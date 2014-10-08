package ee.webmedia.alfresco.docadmin.bootstrap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Add inapplicableForVol=true to fieldDefinitions that need them, but have been created without them
 */
public class FieldDefPropsUpdater extends AbstractNodeUpdater {
    private static final Set<String> MANDATORY_FOR_VOL = new HashSet<String>(Arrays.asList("function", "series", "ownerName"));
    private static final Set<String> INAPPLICABLE_FOR_VOL = new HashSet<String>(Arrays.asList(
            // systematicFieldDefinitions1.xml
            "docName", "docNameAdr", "docStatus", "storageType", "templateName", "sendDescValue", "keywords", "transmittalMode"
            , "dueDate", "regNumber", "regDateTime", "volume", "case", "complienceNotation", "complienceDate",
            // systematicFieldDefinitions2.xml
            "publishToAdr",
            // systematicFieldDefinitions3.xml
            "financingSource", "firstPartyName", "firstPartyContactPersonName", "contractSum", "firstPartyContactPersonServiceRank", "firstPartyContactPersonJobTitle",
            "firstPartyContactPersonOrgStructUnit", "firstPartyContactPersonWorkAddress", "firstPartyContactPersonEmail", "firstPartyContactPersonPhone",
            "firstPartyContactPersonId",
            // systematicFieldDefinitions6.xml
            "substituteName", "substituteJobTitle", "substitutionBeginDate", "substitutionEndDate",
            // systematicFieldDefinitions7.xml
            "leaveType", "leaveBeginDate", "leaveEndDate", "leaveDays", "leaveWorkYear", "leaveInitialBeginDate", "leaveInitialEndDate", "leaveNewBeginDate", "leaveNewEndDate",
            "leaveChangedDays", "leaveNewWorkYear", "leaveCancelBeginDate", "leaveCancelEndDate", "leaveCancelledDays",
            // systematicFieldDefinitionsAll.xml
            "invoiceType", "paymentReferenceNumber", "additionalInformationContent", "purchaseOrderSapNumber", "docSubType", "sellerPartyName", "sellerPartyRegNumber",
            "sellerPartySapAccount", "sellerPartyContactName", "sellerPartyContactPhoneNumber", "sellerPartyContactEmailAddress", "invoiceDate", "invoiceNumber", "invoiceDueDate",
            "paymentTerm", "totalSum", "currency", "invoiceSum", "vat", "entryDate", "entrySapNumber", "applicantName", "applicantJobTitle", "applicantOrgStructUnit",
            "errandBeginDate", "errandEndDate", "dailyAllowanceDays", "dailyAllowanceRate", "dailyAllowanceSum", "dailyAllowanceTotalSum", "expenseType", "expectedExpenseSum",
            "expensesTotalSum", "advancePaymentSum", "advancePaymentDesc", "errandComment", "costManager", "costCenter", "eventName", "eventOrganizer", "eventBeginDate",
            "eventEndDate", "county", "city", "travelPurpose", "country", "reportDueDate", "expenseTypeChoice", "financingSourceChoice", "payingReasonAndExtent",
            "errandReportPaymentMethod", "errandReportSum", "errandSummaryProject", "errandSummaryDepartment", "errandSummaryExpenseItem", "errandSummaryAccount",
            "errandSummaryDebit", "errandSummaryCredit", "driveRecordKeeping", "driveCompensationRate", "driveBeginDate", "driveEndDate", "driveOdoBegin", "driveOdoEnd",
            "driveKm", "driveCompensationCalculated", "driveTotalKm", "driveTotalCompensation", "driveCompensation", "errandExpectedExpense", "applicantServiceRank",
            "applicantWorkAddress", "applicantEmail", "applicantPhone", "applicantId", "dailyAllowanceCateringCount", "dailyAllowanceFinancingSource", "expensesFinancingSource",
            "purchasingManager", "authorisedCostManager", "budgetAccount"));

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION);
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef fieldDefRef) throws Exception {
        String fieldId = (String) nodeService.getProperty(fieldDefRef, DocumentAdminModel.Props.FIELD_ID);
        boolean updateInapplicableForVol = INAPPLICABLE_FOR_VOL.contains(fieldId);
        if (updateInapplicableForVol) {
            nodeService.setProperty(fieldDefRef, DocumentAdminModel.Props.INAPPLICABLE_FOR_VOL, true);
        }
        boolean updateMandatoryForVol = MANDATORY_FOR_VOL.contains(fieldId);
        if (updateMandatoryForVol) {
            nodeService.setProperty(fieldDefRef, DocumentAdminModel.Props.MANDATORY_FOR_VOL, true);
        }
        boolean removeFormVolSearch = "docName".equals(fieldId);
        if (removeFormVolSearch) {
            nodeService.setProperty(fieldDefRef, DocumentAdminModel.Props.IS_PARAMETER_IN_VOL_SEARCH, false);
        }
        boolean hadInapplicableAspect = nodeService.hasAspect(fieldDefRef, DocumentAdminModel.Aspects.INAPPLICABLE_FOR_TYPE);
        if (!hadInapplicableAspect) {
            nodeService.addAspect(fieldDefRef, DocumentAdminModel.Aspects.INAPPLICABLE_FOR_TYPE, null);
        }
        return new String[] { fieldId
                , updateInapplicableForVol ? DocumentAdminModel.Props.INAPPLICABLE_FOR_VOL.getLocalName() : ""
                , updateMandatoryForVol ? DocumentAdminModel.Props.MANDATORY_FOR_VOL.getLocalName() : ""
                , removeFormVolSearch ? DocumentAdminModel.Props.IS_PARAMETER_IN_VOL_SEARCH.getLocalName() + "=false" : ""
                , hadInapplicableAspect ? "" : DocumentAdminModel.Aspects.INAPPLICABLE_FOR_TYPE.getLocalName() };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "fieldDefinition.nodeRef", "fieldId" };
    }

}
