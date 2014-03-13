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
import org.apache.commons.lang.time.DateUtils;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import ee.webmedia.alfresco.parameters.model.Parameter;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * A utility bean to wrap scheduling a parameter-configurable jobs with a scheduler.<br>
 * At the moment supports 2 types of {@link #parameterFormat}s
 * <ul>
 * <li>time based: "H:mm" - triggered daily based on {@link #parameterName} parameter value</li>
 * <li>interval based: one of: S|s|m|H|D - same symbols as used by {@link SimpleDateFormat}("s" for seconds, "m" for minutes)</li>
 * </ul>
 * If time and interval are both provided then triggering time is calculated as follows:
 * Start time is current date and time provided by parameter; period is added to the start time.
 * This means that when time changes from summer-time to winter-time and vice versa,
 * the start time given by parameter is shifted by one hour.
 */
public class ParameterRescheduledTriggerBean extends AbstractTriggerBean {

    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ParameterRescheduledTriggerBean.class);
    //
    private ParametersService parametersService;
    private boolean startupCompleted;
    //
    private String parameterName;
    private String parameterFormat;
    private String timeParameterName;
    private String cron;
    private long repeatInterval = 0;
    private long time = 0;
    private int repeatCount = PersistentTrigger.REPEAT_INDEFINITELY;
    private long startDelay = 0;
    private Trigger trigger;
    private boolean fireAtStartup;
    private boolean enabled = true;

    public ParameterRescheduledTriggerBean() {
        super();
    }

    @Override
    public void afterPropertiesSet() throws ParseException {
        if (!enabled) {
            return;
        }
        if (parameterFormat == null || parameterName == null) {
            throw new RuntimeException("You must specify parameter name and the repetition format that values of this parameter represent.");
        }
        parametersService.addParameterRescheduledJob(this);
        parametersService.addParameterChangeListener(parameterName, new ParametersService.ParameterChangedCallback() {
            @Override
            public void doWithParameter(Serializable newValue) {
                reschedule(DefaultTypeConverter.INSTANCE.convert(String.class, newValue), null);
            }
        });
        if (hasTimeParameter()) {
            parametersService.addParameterChangeListener(timeParameterName, new ParametersService.ParameterChangedCallback() {
                @Override
                public void doWithParameter(Serializable newValue) {
                    reschedule(DefaultTypeConverter.INSTANCE.convert(String.class, newValue), timeParameterName);
                }
            });
        }
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
            resolveSchedule(getParamValue(parameterName), parameterName);
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
            PersistentTrigger trigger = new PersistentTrigger(getBeanName(), Scheduler.DEFAULT_GROUP, parameterName, parametersService);
            long startDelayInMillis = 0;
            if (startDelay > 0) { // ignore fireAtStartup if startDelay is defined
                startDelayInMillis = startDelay;
            } else if (!fireAtStartup) {
                startDelayInMillis = getStartDelay();
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format(String.format("Start delay for parameter=%s is %d", parameterName, startDelayInMillis)));
            }
            trigger.setStartTime(new Date(System.currentTimeMillis() + startDelayInMillis));
            trigger.setRepeatCount(repeatCount);
            trigger.setRepeatInterval(repeatInterval);
            return trigger;
        } else {
            trigger = new CronTrigger(getBeanName(), Scheduler.DEFAULT_GROUP, cron);
            trigger.setJobName(getJobDetail().getName());
        }
        return trigger;
    }

    public long getStartDelay() {
        return AuthenticationUtil.runAs(new RunAsWork<Long>() {
            @Override
            public Long doWork() throws Exception {
                Date startTime;
                Parameter param = parametersService.getParameter(Parameters.get(parameterName));
                if (param.getNextFireTime() != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Got next fire time from parameter: " + param.getNextFireTime());
                    }
                    startTime = param.getNextFireTime();
                }
                else {
                    Date date = new Date();
                    if (hasTimeParameter()) {
                        date = truncateTime(date);
                    }
                    startTime = new Date(date.getTime() + time + repeatInterval);
                    if (log.isDebugEnabled()) {
                        log.debug("Storing next fire time to parameter: " + startTime);
                    }
                    param.setNextFireTime(startTime);
                    parametersService.setParameterNextFireTime(param);
                }
                long delay = startTime.getTime() - System.currentTimeMillis();
                return delay > 0 ? delay : 0;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    private void reschedule(String newValue, String triggeringParameterName) {
        Scheduler scheduler = getScheduler();
        String thisTriggerName = getBeanName();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Rescheduling trigger=%s to value=%s", thisTriggerName, newValue));
        }
        try {
            for (String triggerGroupName : scheduler.getTriggerGroupNames()) {
                String[] triggerNames = scheduler.getTriggerNames(triggerGroupName);
                for (String triggerName : triggerNames) {
                    Trigger trigger = scheduler.getTrigger(triggerName, triggerGroupName);
                    if (trigger != null && trigger.getName().equals(thisTriggerName)) {
                        info(trigger, null, true);
                        resolveSchedule(newValue, triggeringParameterName);
                        if (trigger instanceof CronTrigger) {
                            ((CronTrigger) trigger).setCronExpression(cron);
                        } else if (trigger instanceof PersistentTrigger) {
                            final PersistentTrigger sTrigger = (PersistentTrigger) trigger;
                            Date date = new Date();
                            if (hasTimeParameter()) {
                                date = truncateTime(date);
                            }
                            // Calendar calendar = Calendar.getInstance();
                            // int offset = -(calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / (60 * 1000);
                            trigger.setStartTime(new Date(date.getTime() /* + offset */+ time + repeatInterval));
                            sTrigger.setRepeatInterval(repeatInterval);

                            // persist next fire time to repository
                            Parameter parameter = parametersService.getParameter(Parameters.get(parameterName));
                            parameter.setNextFireTime(trigger.getStartTime());
                            parametersService.setParameterNextFireTime(parameter);
                        }
                        scheduler.rescheduleJob(triggerName, triggerGroupName, trigger);
                        info(trigger, newValue, false);
                    }
                }
            }
        } catch (SchedulerException e) {
            final UnableToPerformException unableToPerformException = new UnableToPerformException(MessageSeverity.ERROR, "parameters_error_canNotReschedule");
            unableToPerformException.setMessageValuesForHolders(parameterName, newValue);
            throw unableToPerformException;
        } catch (ParseException e) {
            throw new RuntimeException("failed to parse cron expression", e);
        }
    }

    private Date truncateTime(Date date) {
        return DateUtils.truncate(date, Calendar.DATE);
    }

    private boolean hasTimeParameter() {
        return timeParameterName != null;
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
    private void resolveSchedule(String paramValue, String triggeringParameterName) {
        if (!hasTimeParameter()) {
            if (parameterFormat.equals("H:mm") || parameterFormat.equals("H:m")) {// hours and minutes
                setCron(paramValue, parameterFormat);

            } else { // repeatInterval is set using timeunit specified in format (format notation is similar to SimpleDateFormat)
                setPeriod(paramValue);
            }
        } else {
            String timeVal;
            String periodVal;
            if (timeParameterName.equals(triggeringParameterName)) {
                timeVal = paramValue;
                periodVal = getParamValue(parameterName);
            } else {
                timeVal = getParamValue(timeParameterName);
                periodVal = paramValue;
            }
            Calendar cal = parseTime(timeVal, "H:mm");
            time = cal.get(Calendar.MINUTE) * getMinuteMultiplier() + cal.get(Calendar.HOUR_OF_DAY) * getHourMultiplier();
            setPeriod(periodVal);
        }
    }

    public void setCron(String timeVal, String timeFormat) {
        Calendar cal = parseTime(timeVal, timeFormat);
        cron = "0 " + cal.get(Calendar.MINUTE) + " " + cal.get(Calendar.HOUR_OF_DAY) + " * * ?";
    }

    public Calendar parseTime(String timeVal, String timeFormat) {
        SimpleDateFormat df = new SimpleDateFormat(timeFormat);
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(df.parse(timeVal));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return cal;
    }

    private void setPeriod(String paramValue) {
        long multiplier = 1;
        if (parameterFormat.equals("S")) {// ms
        } else if (parameterFormat.equals("s")) {// sec
            multiplier *= 1000L;
        } else if (parameterFormat.equals("m")) {// minute
            multiplier *= getMinuteMultiplier();
        } else if (parameterFormat.equals("H")) {// hour [0;23]
            multiplier *= getHourMultiplier();
        } else if (parameterFormat.equals("D")) {// day in year/decade/century ...
            multiplier *= 1000L * 60L * 60L * 24L;
        } else {
            throw new RuntimeException("Unknown schedule format '" + parameterFormat + "' for parameter with value '" + paramValue + "'");
        }
        repeatInterval = multiplier * DefaultTypeConverter.INSTANCE.convert(Long.class, paramValue);
    }

    private long getHourMultiplier() {
        return 1000L * 60L * 60L;
    }

    private long getMinuteMultiplier() {
        return 1000L * 60L;
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

    public void setTimeParameterName(String timeParameterName) {
        this.timeParameterName = timeParameterName;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public void setStartDelay(long startDelay) {
        this.startDelay = startDelay;
    }

    public void setStartDelayMinutes(long startDelayMinutes) {
        startDelay = startDelayMinutes * 60L * 1000L;
    }

    /**
     * NB note that this will not be used if startDelay is set!
     * 
     * @param fireAtStartup
     */
    public void setFireAtStartup(boolean fireAtStartup) {
        this.fireAtStartup = fireAtStartup;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    // END: getters / setters

}
