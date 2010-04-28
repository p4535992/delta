package ee.webmedia.alfresco.parameters.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.parameters.job.ParameterRescheduledTriggerBean;
import ee.webmedia.alfresco.parameters.model.Parameter;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.model.ParametersModel;
import ee.webmedia.alfresco.parameters.model.ParametersModel.Repo;

/**
 * Class to retrieve user-configurable parameters
 * 
 * @author Ats Uiboupin
 */
public class ParametersServiceImpl implements ParametersService {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ParametersServiceImpl.class);

    private GeneralService generalService;
    private NodeService nodeService;

    private Boolean applicationStarted = false;
    /**
     * Might happen that after most of the jobs have registered themselves to this service using
     * {@link #addParameterRescheduledJob(ParameterRescheduledTriggerBean)} <br>
     * and {@link #applicationStarted()} is being processed <br>
     * some job get's lazily initialized (i.e lazy=true or using childAppContext, that gets loaded later), job will register itself to this service and
     * resolvePropertyValueAndSchedule would not ever be called for that job
     */
    private Object syncLock = new Object();

    private final Map<String /* parameterName */, List<ParameterChangedCallback>> parameterChangeListeners = new HashMap<String, List<ParameterChangedCallback>>();
    private List<ParameterRescheduledTriggerBean> reschedulableJobs = new ArrayList<ParameterRescheduledTriggerBean>();

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
                for (ParameterRescheduledTriggerBean job : reschedulableJobs) {
                    job.resolvePropertyValueAndSchedule();
                }
                applicationStarted = true;
            }
        }
    }

    public Parameter<?> getParameter(Parameters parameter) {
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

        Parameter<?> par = Parameter.newInstance(parentRef.getQName().getLocalName(), paramValue, nodeType);
        par.setNextFireTime((Date) nodeService.getProperty(nodeRef, ParametersModel.Props.Parameter.NEXT_FIRE_TIME));
        return par;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getParameter(Parameters parameter, Class<T> requiredClazz) {
        String xPath = parameter.toString();
        final NodeRef nodeRef = generalService.getNodeRef(xPath);
        if (nodeRef == null) {
            throw new RuntimeException("Unable to get nodeRef for parameter with xPath: '" + xPath + "'");
        }
        final Serializable parameterValue = nodeService.getProperty(nodeRef, ParametersModel.Props.Parameter.VALUE);
        if (requiredClazz != null) {
            return DefaultTypeConverter.INSTANCE.convert(requiredClazz, parameterValue);
        }
        return (T) parameterValue;
    }

    private <T extends Serializable> T getParameter(Parameter<? extends Serializable> parameter, Class<T> requiredClazz) {
        return getParameter(Parameters.get(parameter), requiredClazz);
    }

    @Override
    public String getStringParameter(Parameters parameter) {
        return getParameter(parameter, String.class);
    }

    @Override
    public Long getLongParameter(Parameters parameter) {
        return getParameter(parameter, Long.class);
    }

    @Override
    public Double getDoubleParameter(Parameters parameter) {
        return getParameter(parameter, Double.class);
    }

    @Override
    public List<Parameter<?>> getAllParameters() {
        String xPath = Repo.PARAMETERS_SPACE;
        final NodeRef parametersRootNodeRef = generalService.getNodeRef(xPath);
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(parametersRootNodeRef);
        List<Parameter<?>> parameters = new ArrayList<Parameter<?>>(childRefs.size());
        for (ChildAssociationRef ref : childRefs) {
            final QName qName = ref.getQName();
            final String paramName = qName.getLocalName();
            NodeRef paramNodeRef = ref.getChildRef();
            final Serializable paramValue = nodeService.getProperty(paramNodeRef, ParametersModel.Props.Parameter.VALUE);
            final Parameter<? extends Serializable> parameter = getParameter(paramName, paramValue, paramNodeRef);
            parameters.add(parameter);
        }
        return parameters;
    }

    private Parameter<? extends Serializable> getParameter(final String paramName, Serializable paramValue, NodeRef paramNodeRef) {
        final QName nodeType = nodeService.getType(paramNodeRef);
        return Parameter.newInstance(paramName, paramValue, nodeType);
    }

    @Override
    public void updateParameters(Collection<Parameter<?>> parameters) {
        for (Parameter<?> parameter : parameters) {
            updateParameter(parameter);
        }
    }

    public void updateParameter(Parameter<? extends Serializable> parameter) {
        final Parameters parameterEnum = Parameters.get(parameter);
        final String xPath = parameterEnum.toString();
        final NodeRef nodeRef = generalService.getNodeRef(xPath);
        if (log.isDebugEnabled()) {
            log.debug("updating parameter: " + parameter);
        }
        Serializable previousValueInRepo = getParameter(parameter, parameter.getParamValue().getClass());
        nodeService.setProperty(nodeRef, ParametersModel.Props.Parameter.VALUE, parameter.getParamValue());
        nodeService.setProperty(nodeRef, ParametersModel.Props.Parameter.NEXT_FIRE_TIME, parameter.getNextFireTime());
        // only reschedule jobs if the parameter value actually changed
        if (!previousValueInRepo.equals(parameter.getParamValue())) {
            log.debug("Parameters value changed:\n\tin repo:" + previousValueInRepo + "'\n\tupdateable parameter: '" + parameter + "'");
            List<ParameterChangedCallback> listeners = parameterChangeListeners.get(parameter.getParamName());
            if (listeners != null) {
                for (ParameterChangedCallback callback : listeners) {
                    callback.doWithParameter(parameter.getParamValue());
                }
            }
        }
    }

    // START: getters / setters
    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
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
