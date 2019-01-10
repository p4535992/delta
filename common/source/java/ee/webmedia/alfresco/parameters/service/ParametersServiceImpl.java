package ee.webmedia.alfresco.parameters.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.parameters.job.ParameterRescheduledTriggerBean;
import ee.webmedia.alfresco.parameters.model.Parameter;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.model.ParametersModel;
import ee.webmedia.alfresco.parameters.model.ParametersModel.Repo;

/**
 * Class to retrieve user-configurable parameters
 */
public class ParametersServiceImpl implements ParametersService {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ParametersServiceImpl.class);
    private static NodeRef parametersRootRef;

    private GeneralService generalService;
    private NodeService nodeService;
    private BulkLoadNodeService bulkLoadNodeService;
    private boolean jobsEnabled = true;

    private Boolean applicationStarted = false;
    /**
     * Might happen that after most of the jobs have registered themselves to this service using {@link #addParameterRescheduledJob(ParameterRescheduledTriggerBean)} <br>
     * and {@link #applicationStarted()} is being processed <br>
     * some job get's lazily initialized (i.e lazy=true or using childAppContext, that gets loaded later), job will register itself to this service and
     * resolvePropertyValueAndSchedule would not ever be called for that job
     */
    private final Object syncLock = new Object();

    private final Map<String /* parameterName */, List<ParameterChangedCallback>> parameterChangeListeners = new HashMap<>();
    private final List<ParameterRescheduledTriggerBean> reschedulableJobs = new ArrayList<>();

    private SimpleCache<String, Parameter<?>> parametersCache;

    @Override
    public void addParameterChangeListener(String paramName, ParameterChangedCallback callback) {
        List<ParameterChangedCallback> listeners = parameterChangeListeners.get(paramName);
        if (listeners == null) {
            listeners = new LinkedList<ParameterChangedCallback>();
            parameterChangeListeners.put(paramName, listeners);
        }
        listeners.add(callback);
    }

    @Override
    public void applicationStarted() {
        synchronized (syncLock) {
            if (!applicationStarted) {
                initParametersCache();
                for (ParameterRescheduledTriggerBean job : reschedulableJobs) {
                    job.resolvePropertyValueAndSchedule();
                }
                applicationStarted = true;
            }
        }
    }

    private void initParametersCache() {
        final NodeRef parametersRootNodeRef = generalService.getNodeRef(Repo.PARAMETERS_SPACE);
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(parametersRootNodeRef);
        for (ChildAssociationRef ref : childRefs) {
            final QName qName = ref.getQName();
            final String paramName = qName.getLocalName();
            NodeRef paramNodeRef = ref.getChildRef();
            final Serializable paramValue = nodeService.getProperty(paramNodeRef, ParametersModel.Props.Parameter.VALUE);
            final String paramDescription = (String) nodeService.getProperty(paramNodeRef, ParametersModel.Props.Parameter.DESCRIPTION);
            final QName nodeType = nodeService.getType(paramNodeRef);
            final Parameter<? extends Serializable> parameter = Parameter.newInstance(paramNodeRef, paramName, paramValue, nodeType, paramDescription);
            if (parameter != null) {
                parametersCache.put(parameter.getParamName(), parameter);
            }
        }
    }

    @Override
    public Parameter<? extends Serializable> getParameter(Parameters parameter) {
        Parameter<? extends Serializable> param = getParameter(parameter.getParameterName(), null);
        if (param == null) {
            String xPath = parameter.toString();
            final NodeRef nodeRef = generalService.getNodeRef(xPath);
            if (nodeRef == null) {
                throw new RuntimeException("Unable to get nodeRef for parameter with xPath: '" + xPath + "'");
            }

            final Serializable paramValue = nodeService.getProperty(nodeRef, ParametersModel.Props.Parameter.VALUE);
            final QName nodeType = nodeService.getType(nodeRef);

            List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(nodeRef);
            if (parentAssocs.size() != 1) {
                throw new RuntimeException("Parameter is expected to have only one parent, but got " + parentAssocs.size() + ".");
            }
            ChildAssociationRef parentRef = parentAssocs.get(0);

            Parameter<?> par = Parameter.newInstance(nodeRef, parentRef.getQName().getLocalName(), paramValue, nodeType);
            par.setNextFireTime((Date) nodeService.getProperty(nodeRef, ParametersModel.Props.Parameter.NEXT_FIRE_TIME));
            return par;
        }

        return param;
    }

    private Parameter<? extends Serializable> getParameter(String parameterName, NodeRef paramRef) {
        Parameter<? extends Serializable> param = parametersCache.get(parameterName);
        if (param == null) {
            if (paramRef != null) {
                final Serializable paramValue = nodeService.getProperty(paramRef, ParametersModel.Props.Parameter.VALUE);
                final QName nodeType = nodeService.getType(paramRef);
                List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(paramRef);
                if (parentAssocs.size() != 1) {
                    throw new RuntimeException("Parameter is expected to have only one parent, but got " + parentAssocs.size() + ".");
                }
                ChildAssociationRef parentRef = parentAssocs.get(0);
                param = Parameter.newInstance(paramRef, parentRef.getQName().getLocalName(), paramValue, nodeType);
                param.setNextFireTime((Date) nodeService.getProperty(paramRef, ParametersModel.Props.Parameter.NEXT_FIRE_TIME));
                parametersCache.put(param.getParamName(), param);
            }
        }
        return param;
    }

    @Override
    public <T> T getParameterValue(Parameters parameter, Class<T> requiredClazz) {
        Parameter<?> param = getParameter(parameter);
        if (param != null && requiredClazz != null) {
            return DefaultTypeConverter.INSTANCE.convert(requiredClazz, param.getParamValue());
        }
        throw new RuntimeException("Unable to find paramter: '" + parameter.getParameterName() + "'");
    }

    @Override
    public Date getDateParameter(Parameters parameter){
        return getParameterValue(parameter, Date.class);
    }
    @Override
    public String getStringParameter(Parameters parameter) {
        return getParameterValue(parameter, String.class);
    }

    @Override
    public Long getLongParameter(Parameters parameter) {
        return getParameterValue(parameter, Long.class);
    }

    @Override
    public Double getDoubleParameter(Parameters parameter) {
        return getParameterValue(parameter, Double.class);
    }

    @Override
    public Map<String, Set<Parameters>> getSwappedStringParameters(List<Parameters> parameters) {
        Map<String, Set<Parameters>> result = new HashMap<>();
        for (Parameters parameter : parameters) {
            String parameterStr = getStringParameter(parameter);
            Set<Parameters> stringParameters = result.get(parameterStr);
            if (stringParameters == null) {
                stringParameters = new HashSet<Parameters>();
                result.put(parameterStr, stringParameters);
            }
            stringParameters.add(parameter);
        }
        return result;
    }

    @Override
    public List<Parameter<? extends Serializable>> getAllParameters() {
        Map<String, NodeRef> parametersWithNodeRefs = bulkLoadNodeService.loadChildElementsNodeRefs(getParametersRootNodeRef(),
                ParametersModel.Props.Parameter.NAME, ParametersModel.Types.PARAMETER_DOUBLE, ParametersModel.Types.PARAMETER_INT, ParametersModel.Types.PARAMETER_STRING);
        List<Parameter<? extends Serializable>> parameters = new ArrayList<>();
        for (Entry<String, NodeRef> param : parametersWithNodeRefs.entrySet()) {
            parameters.add(getParameter(param.getKey(), param.getValue()));
        }
        return parameters;
    }

    @Override
    public void updateParameters(Collection<Parameter<? extends Serializable>> parameters) {
        for (Parameter<?> parameter : parameters) {
            updateParameter(parameter);
        }
    }

    private void updateParameter(Parameter<? extends Serializable> parameter) {
        if (log.isDebugEnabled()) {
            log.debug("updating parameter: " + parameter);
        }
        Serializable previousParamValue = parameter.getPreviousParamValue();
        boolean valueChanged = !EqualsHelper.nullSafeEquals(previousParamValue, parameter.getParamValue());
        if (valueChanged || !StringUtils.equals(parameter.getPreviousParamDescription(), parameter.getParamDescription())) {
            Map<QName, Serializable> newProps = new HashMap<>();
            newProps.put(ParametersModel.Props.Parameter.VALUE, parameter.getParamValue());
            newProps.put(ParametersModel.Props.Parameter.DESCRIPTION, parameter.getParamDescription());
            newProps.put(ParametersModel.Props.Parameter.NEXT_FIRE_TIME, parameter.getNextFireTime());
            parameter.setPreviousParamDescription();
            parameter.setPreviousParamValue();
            nodeService.addProperties(parameter.getNodeRef(), newProps);
            parametersCache.remove(parameter.getParamName());
            parametersCache.put(parameter.getParamName(), parameter);
        }

        // only reschedule jobs if the parameter value actually changed
        if (valueChanged) {
            log.debug("Parameters value changed:\n\tin repo:" + previousParamValue + "'\n\tupdateable parameter: '" + parameter + "'");
            List<ParameterChangedCallback> listeners = parameterChangeListeners.get(parameter.getParamName());
            if (listeners != null) {
                for (ParameterChangedCallback callback : listeners) {
                    callback.doWithParameter(parameter.getParamValue());
                }
            }
        }
    }

    @Override
    public void setParameterNextFireTime(Parameter<? extends Serializable> parameter) {
        if (log.isDebugEnabled()) {
            log.debug("updating parameter next fire time: " + parameter + "; nextFire=" + parameter.getNextFireTime());
        }
        nodeService.setProperty(parameter.getNodeRef(), ParametersModel.Props.Parameter.NEXT_FIRE_TIME, parameter.getNextFireTime());
        parametersCache.remove(parameter.getParamName());
        parametersCache.put(parameter.getParamName(), parameter);
    }

    @Override
    public boolean isJobsEnabled() {
        return jobsEnabled;
    }

    // START: getters / setters
    public void setJobsEnabled(boolean jobsEnabled) {
        this.jobsEnabled = jobsEnabled;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

    public void setParametersCache(SimpleCache<String, Parameter<?>> parametersCache) {
        this.parametersCache = parametersCache;
    }

    private NodeRef getParametersRootNodeRef() {
        if (parametersRootRef == null) {
            parametersRootRef = generalService.getNodeRef(Repo.PARAMETERS_SPACE);
        }
        return parametersRootRef;
    }

    @Override
    public void addParameterRescheduledJob(ParameterRescheduledTriggerBean job) {
        synchronized (syncLock) {
            reschedulableJobs.add(job);
            if (applicationStarted) {
                job.resolvePropertyValueAndSchedule();
            }
        }
    }

    // END: getters / setters
}
