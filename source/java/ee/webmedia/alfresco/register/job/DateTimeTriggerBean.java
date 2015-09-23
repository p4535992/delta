package ee.webmedia.alfresco.register.job;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.alfresco.util.CronTriggerBean;

public class DateTimeTriggerBean extends CronTriggerBean {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DateTimeTriggerBean.class);

    private String defaultValue;
    private String dateTimePattern;
    private String value;

    public DateTimeTriggerBean() {
        super();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String cron = createCronExpression();
        setCronExpression(cron);
        super.afterPropertiesSet();
    }

    private String createCronExpression() {
        SimpleDateFormat df = new SimpleDateFormat(dateTimePattern);
        String cron = create(df, value);
        if (cron == null) {
            cron = create(df, defaultValue);
        }
        return cron;
    }

    private String create(SimpleDateFormat df, String value) {
        String cron = null;
        try {
            Date date = df.parse(value);
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            cron = c.get(Calendar.SECOND) + " " + c.get(Calendar.MINUTE) + " " + c.get(Calendar.HOUR_OF_DAY) + " "
                    + c.get(Calendar.DAY_OF_MONTH) + " " + (c.get(Calendar.MONTH) + 1) + " ?";
        } catch (ParseException e) {
            LOG.warn("Failed to parse '" + value + "'. Used pattern: " + df.toPattern());
        }
        return cron;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setDateTimePattern(String dateTimePattern) {
        this.dateTimePattern = dateTimePattern;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
