package ee.webmedia.alfresco.parameters.model;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.parameters.model.ParametersModel.Repo;

/**
 * @author Ats Uiboupin
 */
public enum Parameters {
    /** Maximal number of documents that is fetched with single service call(multiple calls will be created if needed) */
    DVK_MAX_RECEIVE_DOCUMENTS_NR("dvkMaxreceiveDocumentsNr"),
    /** Number of days the document will be retained */
    DVK_RETAIN_PERIOD("dvkRetainPeriod"),
    /** Cron expression - expresses when or how often */
    DVK_RECEIVE_DOCUMENTS_INTERVAL("dvkReceiveDocumentsInterval"),
    /** (H:mm) when should dvkCapability of orgainzation(addresbook entry) be updated */
    DVK_RECEIVE_ORGANIZATIONS("dvkReceiveOrganizations"),
    EMPLOYEE_REG_RECEIVE_STRUCT_UNITS_TIME("employeeRegReceiveStructUnitsTime"),
    /** period that is used to update users registry */
    EMPLOYEE_REG_RECEIVE_USERS_PERIOD("employeeRegReceiveUsersPeriod"),
    EMPLOYEE_REG_ORGANISATION_ID("employeeRegOrganisationId"),
    /** Leaving letter */
    LEAVING_LETTER_CONTENT("leavingLetterContent"),
    /** Advisers, Deputies and Officials on the leavingLetter form */
    TRAINING_ADVISER("trainingAdviser"),
    FINANCIAL_DEPARTMENT_HEAD_ON_ACCOUNTING("financialDepartmentHeadOnAccounting"),
    ADVISER_GENERAL_DEPARTMENT_ON_ARCHIVES("adviserGeneralDepartmentOnArchives"),
    ADVISER_GENERAL_DEPARTMENT_ON_CLASSIFIED_RECORDS("adviserGeneralDepartmentOnClassifiedRecords"),
    DEPUTY_HEAD_OF_THE_GENERAL_DEPARTMENT("deputyHeadOfTheGeneralDepartment"),
    DOCUMENTARY_PROCEDURES_RECIPIENT("documentaryProceduresRecipient"),
    ADVISER_ON_ICT("adviserOnICT"),
    DEPUTY_HEAD_OF_THE_ADM_DEPT_ON_MAINTENANCE("deputyHeadOfTheAdmDeptOnMaintenance"),
    OFFICIALS_DIRECT_MANAGER("officialsDirectManager"),
    OFFICIAL_ON_PERSONNEL_DEPARTMENT("officialOnPersonnelDepartment");
    

    private String xPath;
    private String parameterName;

    Parameters(String parameterName) {
        this.xPath = Repo.PARAMETERS_SPACE + "/" + ParametersModel.PREFIX + parameterName;
        this.parameterName = parameterName;
    }

    public static Parameters get(String parameterName) {
        final Parameters[] values = Parameters.values();
        for (Parameters parameter : values) {
            if (parameter.parameterName.equals(parameterName)) {
                return parameter;
            }
        }
        throw new IllegalArgumentException("Unknown parameterName: " + parameterName+". Known values: "+StringUtils.join(values, ", "));
    }

    public static Parameters get(Parameter<?> parameter) {
        return get(parameter.getParamName());
    }

    @Override
    public String toString() {
        return xPath;
    }

}