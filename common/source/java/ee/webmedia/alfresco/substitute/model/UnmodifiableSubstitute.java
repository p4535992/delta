package ee.webmedia.alfresco.substitute.model;

import static ee.webmedia.alfresco.document.model.Document.dateFormat;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.utils.MessageUtil;

public class UnmodifiableSubstitute implements Serializable {
    private static final long serialVersionUID = 0L;

    private final String substituteName;
    private final String substituteId;
    private final String label;
    private final Date substitutionStartDate;
    private final Date substitutionEndDate;
    private final boolean active;

    public UnmodifiableSubstitute(Substitute sub) {
        substituteName = sub.getSubstituteName();
        substituteId = sub.getSubstituteId();
        substitutionStartDate = sub.getSubstitutionStartDate();
        substitutionEndDate = sub.getSubstitutionEndDate();
        Date currentDate = DateUtils.truncate(new Date(), Calendar.DATE);
        active = substitutionEndDate != null && substitutionStartDate != null &&
                ((currentDate.after(substitutionStartDate) && currentDate.before(substitutionEndDate))
                        || DateUtils.isSameDay(currentDate, substitutionEndDate)
                        || DateUtils.isSameDay(currentDate, substitutionStartDate));
        label = isActive() ? MessageUtil.getMessage("user_away_has_substitute",
                dateFormat.format(substitutionStartDate), dateFormat.format(substitutionEndDate), getSubstituteName()) : null;
    }

    public boolean isActive() {
        return active;
    }

    public String getLabel() {
        return label;
    }

    public String getSubstituteName() {
        return substituteName;
    }

    public String getSubstituteId() {
        return substituteId;
    }

    public Date getSubstitutionStartDate() {
        return substitutionStartDate;
    }

    public Date getSubstitutionEndDate() {
        return substitutionEndDate;
    }

}
