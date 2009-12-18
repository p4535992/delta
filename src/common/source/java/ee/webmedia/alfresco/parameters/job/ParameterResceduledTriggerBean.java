package ee.webmedia.alfresco.parameters.job;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.util.AbstractTriggerBean;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;

/**
 * A utility bean to wrap sceduling a parameter-configurable jobs with a scheduler.<br>
 * At the moment supports 2 types of {@link #parameterFormat}s
 * <ul>
 * <li>time based: "H:mm" - triggered daily based on {@link #parameterName} parameter value</li>
 * <li>interval based: one of: S|s|m|H|D - same symbols as used by {@link SimpleDateFormat}("s" for seconds, "m" for minutes)</li>
 * </ul>
 * 
 * @author Ats Uiboupin
 */
public class ParameterResceduledTriggerBean extends AbstractTriggerBean {

    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ParameterResceduledTriggerBean.class);
    //
    private ParametersService parametersService;
    private boolean startupCompleted;
    // 
    private String parameterName;
    private String parameterFormat;
    private String cron;
    private long repeatInterval = 0;
    private int repeatCount = SimpleTrigger.REPEAT_INDEFINITELY;
    private long startDelay = 0;
    private Trigger trigger;
    private boolean fireAtStartup;

    public ParameterResceduledTriggerBean() {
        super();
    }

    @Override
    public void afterPropertiesSet() throws ParseException {
        if (parameterFormat == null || parameterName == null) {
            throw new RuntimeException("You must specify parameter name and the repetition format that values of this parameter represent.");
        }
        parametersService.addParameterResceduledJob(this);
        parametersService.addParameterChangeListener(parameterName, new ParametersService.ParameterChangedCallback() {
            @Override
            public void doWithParameter(Serializable newValue) {
                reschedule(DefaultTypeConverter.INSTANCE.convert(String.class, newValue));
            }
        });
    }

    /**
     * This method should be called after all properties are set and application is ready to resolve parameter values.
     * Resolves the value of {@link #parameterName} according to {@link #parameterFormat},<br>
     * adds parameterChangeListener and schedules job.
     */
    public void resolvePropertyValueAndSchedule() {
        try {
            log.debug("scheduling trigger with name '" + getBeanName() + "'");
            if (startupCompleted) {
                throw new RuntimeException("Job wit trigger name '" + getBeanName() + "' has alredy been started.");
            }
            resolveSchedule(getParamValue(parameterName));
            super.afterPropertiesSet();
            info(getTrigger(), null, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        startupCompleted = true;
    }

    /**
     * @return new Trigger based on {@link #cron} or {@link #repeatInterval}
     */
    @Override
    public Trigger getTrigger() throws ParseException {
        if (cron == null) {
            SimpleTrigger trigger = new SimpleTrigger(getBeanName(), Scheduler.DEFAULT_GROUP);
            long startDelayInMillis = 0;
            if (startDelay > 0) { // ignore fireAtStartup if startDelay is defined 
                startDelayInMillis = this.startDelay;
            } else if (!fireAtStartup) {
                startDelayInMillis = repeatInterval;
            }
            trigger.setStartTime(new Date(System.currentTimeMillis() + startDelayInMillis));
            trigger.setRepeatCount(repeatCount);
            trigger.setRepeatInterval(this.repeatInterval);
            return trigger;
        } else {
            trigger = new CronTrigger(getBeanName(), Scheduler.DEFAULT_GROUP, cron);
            trigger.setJobName(getJobDetail().getName());
        }
        return trigger;
    }

    private void reschedule(String newValue) {
        Scheduler scheduler = getScheduler();
        String thisTriggerName = getBeanName();
        try {
            for (String triggerGroupName : scheduler.getTriggerGroupNames()) {
                String[] triggerNames = scheduler.getTriggerNames(triggerGroupName);
                for (String triggerName : triggerNames) {
                    Trigger trigger = scheduler.getTrigger(triggerName, triggerGroupName);
                    if (trigger != null && trigger.getName().equals(thisTriggerName)) {
                        info(trigger, null, true);
                        resolveSchedule(newValue);
                        if (trigger instanceof CronTrigger) {
                            ((CronTrigger) trigger).setCronExpression(cron);
                        } else if (trigger instanceof SimpleTrigger) {
                            final SimpleTrigger sTrigger = (SimpleTrigger) trigger;
                            trigger.setStartTime(new Date(System.currentTimeMillis() + repeatInterval));
                            sTrigger.setRepeatInterval(repeatInterval);
                        }
                        scheduler.rescheduleJob(triggerName, triggerGroupName, trigger);
                        info(trigger, newValue, false);
                    }
                }
            }
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException("failed to parse cron expression", e);
        }
    }

    private void info(Trigger trigger, String paramValue, boolean isOldTrigger) {
        if (!log.isInfoEnabled()) {
            return;
        }
        int nrOfFiresToShow = 3;
        Date afterTime = new Date();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nrOfFiresToShow; i++) {
            Date nextFire = trigger.getFireTimeAfter(afterTime);
            sb.append("\n\t").append(nextFire);
            afterTime = nextFire;
        }
        String msg = "trigger '" + trigger.getName() + "' ";
        msg += isOldTrigger ? "would have fired" : "fires";
        if (paramValue != null) {
            msg += " based on paramValue '" + paramValue + "' (";
            msg += (cron != null ? "cron=" + cron : "interval=" + repeatInterval / 60000 + "min)");
        }
        msg += " " + sb.toString();
        log.info(msg);
    }

    private String getParamValue(final String paramName) {
        // we need admin privileges if this bean was initialized after the application has started (for example because the bean was defined in some alfresco
        // subsystem - without the privileges of moduleimporter, that would have provided security context otherwise)
        return AuthenticationUtil.runAs(new RunAsWork<String>() {
            @Override
            public String doWork() throws Exception {
                return parametersService.getStringParameter(Parameters.get(paramName));
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    /**
     * Resolves the schedule based on {@link #parameterFormat} and <code>paramValue</code>
     * 
     * @param paramValue value of the {@link #parameterName}
     */
    private void resolveSchedule(String paramValue) {
        long multiplier = 1;
        if (parameterFormat.equals("H:mm") || parameterFormat.equals("H:m")) {// hours and minutes
            SimpleDateFormat df = new SimpleDateFormat(parameterFormat);
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(df.parse(paramValue));
                this.cron = "0 " + cal.get(Calendar.MINUTE) + " " + cal.get(Calendar.HOUR_OF_DAY) + " * * ?";
                // Trigger trigger = new CronTrigger(getBeanName(), Scheduler.DEFAULT_GROUP, cron);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } else { // repeatInterval is set using timeunit specified in format (format notation is similar to SimpleDateFormat)
            if (parameterFormat.equals("S")) {// ms
            } else if (parameterFormat.equals("s")) {// sec
                multiplier *= 1000L;
            } else if (parameterFormat.equals("m")) {// minute
                multiplier *= 1000L * 60L;
            } else if (parameterFormat.equals("H")) {// hour [0;23]
                multiplier *= 1000L * 60L * 60L;
            } else if (parameterFormat.equals("D")) {// day in year/decade/century ...
                multiplier *= 1000L * 60L * 60L * 24L;
            } else {
                throw new RuntimeException("Unknown schedule format '" + parameterFormat + "' for parameter with value '" + paramValue + "'");
            }
            repeatInterval = multiplier * DefaultTypeConverter.INSTANCE.convert(Long.class, paramValue);
        }
    }

    // START: getters / setters
    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public void setParameterFormat(String parameterFormat) {
        this.parameterFormat = parameterFormat;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public void setStartDelay(long startDelay) {
        this.startDelay = startDelay;
    }

    public void setStartDelayMinutes(long startDelayMinutes) {
        this.startDelay = startDelayMinutes * 60L * 1000L;
    }

    /**
     * NB note that this will not be used if startDelay is set!
     * @param fireAtStartup
     */
    public void setFireAtStartup(boolean fireAtStartup) {
        this.fireAtStartup = fireAtStartup;
    }
    // END: getters / setters

}
