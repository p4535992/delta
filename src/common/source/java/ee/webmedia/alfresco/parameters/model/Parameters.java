package ee.webmedia.alfresco.parameters.model;

import ee.webmedia.alfresco.parameters.model.ParametersModel.Repo;

/**
 * @author Ats Uiboupin
 */
public enum Parameters {
    /** Maximal number of documents that is fetched with single service call(multiple calls will be created if needed) */
    DVK_MAX_RECEIVE_DOCUMENTS_NR("dvkMaxreceiveDocumentsNr"),
    /** Number of days the document will be retained */
    DVK_RETAIN_PERIOD("dvkRetainPeriod"),
    /** Cron expression - expresses when or how ofte */
    DVK_RECEIVE_DOCUMENTS_INTERVAL("dvkReceiveDocumentsInterval"),
    EMPLOYEE_REG_RECEIVE_STRUCT_UNITS_TIME("employeeRegReceiveStructUnitsTime"),
    /** period that is used to update users registry */
    EMPLOYEE_REG_RECEIVE_USERS_PERIOD("employeeRegReceiveUsersPeriod"),
    EMPLOYEE_REG_ORGANISATION_ID("employeeRegOrganisationId");

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
        throw new IllegalArgumentException("Unknown parameterName: " + parameterName);
    }

    public static Parameters get(Parameter<?> parameter) {
        return get(parameter.getParamName());
    }

    @Override
    public String toString() {
        return xPath;
    }

}