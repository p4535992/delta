package ee.webmedia.alfresco.parameters.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ee.webmedia.alfresco.parameters.job.ParameterRescheduledTriggerBean;
import ee.webmedia.alfresco.parameters.model.Parameter;
import ee.webmedia.alfresco.parameters.model.Parameters;

/**
 * Class to retrieve user-configurable parameters
 * 
 * @author Ats Uiboupin
 */
public interface ParametersService {

    String BEAN_NAME = "ParametersService";

    public interface ParameterChangedCallback {
        void doWithParameter(Serializable paramValue);
    }

    Parameter<?> getParameter(Parameters parameter);

    /**
     * @param <T> - Type of requiredClazz
     * @param parameter - value of this parameter will be returned
     * @param requiredClazz - parameter is expected to be instance of that class
     * @return value of given parameter converted to requiredClazz
     */
    <T> T getParameter(Parameters parameter, Class<T> requiredClazz);

    String getStringParameter(Parameters parameter);

    Long getLongParameter(Parameters parameter);

    Double getDoubleParameter(Parameters parameter);

    /**
     * @return all parameter objects from repository
     */
    List<Parameter<?>> getAllParameters();

    /**
     * @param parameters - parameters to be updated to the repository
     */
    void updateParameters(Collection<Parameter<? extends Serializable>> parameters);

    void setParameterNextFireTime(Parameter<? extends Serializable> parameter);

    void addParameterChangeListener(String paramName, ParameterChangedCallback callback);

    /**
     * Used to notify, that application has started and we are able to read parameters from repository.<br>
     * Originally needed by jobs that are scheduled based on given parameter
     */
    void applicationStarted();

    void addParameterRescheduledJob(ParameterRescheduledTriggerBean job);

    /**
     * Returns map indexed by parameter values (meaning in case of equal values only one value is returned)
     * 
     * @param collection
     * @return
     */
    Map<String, Set<Parameters>> getSwappedStringParameters(List<Parameters> collection);

    boolean isJobsEnabled();

}
