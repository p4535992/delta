<<<<<<< HEAD
package ee.webmedia.alfresco.document.register.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.document.service.DocumentServiceImpl;

/**
 * Class that divides whole regNumber, that might contain individualizing number into individualizing number and rest of it
 * 
 * @author Ats Uiboupin
 */
public class RegNrHolder {
    /**
     * regNrWithoutIndividualizingNr - arbitary char-sequence ending with dash and followed by individualizingNr at the end<br>
     * individualizingNr - integer number at the end of regNr
     */
    private static final String REG_NR_PATTERN =
                "(.*-)" // any number of any characters followed by a dash, group 1
                        + "(\\d{1,})" // followed by a number, group 2
                        + "(" // group 3 start
                        + "\\(" // followed by a starting bracket
                        + "(\\d{1,})" // followed by a number, group 4
                        + "\\)" // followed by a closing bracket
                        + "){0,}" // group 3 end, this group is here only for the {0,}
                        + "\\z"; // followed by the end of the string
    private static final Pattern individualizingNrPattern = Pattern.compile(REG_NR_PATTERN);
    private final String regNrWithoutIndividualizingNr; // NB! if individualizingNumber!=null, then this ends with a "-"
    private final Integer individualizingNr;
    private final Integer uniqueNumber;

    public RegNrHolder(String wholeRegNr) {
        if (StringUtils.isNotBlank(wholeRegNr)) {
            Matcher matcher = individualizingNrPattern.matcher(wholeRegNr.trim());
            if (matcher.find()) {
                regNrWithoutIndividualizingNr = matcher.group(1);
                individualizingNr = Integer.valueOf(matcher.group(2));
                if (matcher.group(4) != null) {
                    uniqueNumber = Integer.valueOf(matcher.group(4));
                } else {
                    uniqueNumber = null;
                }
            } else {
                regNrWithoutIndividualizingNr = wholeRegNr;
                individualizingNr = null;
                uniqueNumber = null;
            }
        } else {
            regNrWithoutIndividualizingNr = wholeRegNr;
            individualizingNr = null;
            uniqueNumber = null;
        }
    }

    public String getRegNrWithoutIndividualizingNr() {
        return regNrWithoutIndividualizingNr;
    }

    public String getRegNrWithIndividualizingNr() {
        if (individualizingNr == null) {
            return regNrWithoutIndividualizingNr;
        }
        return regNrWithoutIndividualizingNr + individualizingNr;
    }

    public String getShortRegNrWithoutIndividualizingNr() {
        String shortNr = StringUtils.substringAfterLast(regNrWithoutIndividualizingNr, DocumentServiceImpl.VOLUME_MARK_SEPARATOR);
        if (shortNr.endsWith("-")) {
            return StringUtils.chop(shortNr);
        }
        return shortNr;
    }

    public Integer getIndividualizingNr() {
        return individualizingNr;
    }

    public Integer getUniqueNumber() {
        return uniqueNumber;
    }
}
=======
package ee.webmedia.alfresco.document.register.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.document.service.DocumentServiceImpl;

/**
 * Class that divides whole regNumber, that might contain individualizing number into individualizing number and rest of it
 */
public class RegNrHolder {
    /**
     * regNrWithoutIndividualizingNr - arbitary char-sequence ending with dash and followed by individualizingNr at the end<br>
     * individualizingNr - integer number at the end of regNr
     */
    private static final String REG_NR_PATTERN =
                "(.*-)" // any number of any characters followed by a dash, group 1
                        + "(\\d{1,})" // followed by a number, group 2
                        + "(" // group 3 start
                        + "\\(" // followed by a starting bracket
                        + "(\\d{1,})" // followed by a number, group 4
                        + "\\)" // followed by a closing bracket
                        + "){0,}" // group 3 end, this group is here only for the {0,}
                        + "\\z"; // followed by the end of the string
    private static final Pattern individualizingNrPattern = Pattern.compile(REG_NR_PATTERN);
    private final String regNrWithoutIndividualizingNr; // NB! if individualizingNumber!=null, then this ends with a "-"
    private final Integer individualizingNr;
    private final Integer uniqueNumber;

    public RegNrHolder(String wholeRegNr) {
        if (StringUtils.isNotBlank(wholeRegNr)) {
            Matcher matcher = individualizingNrPattern.matcher(wholeRegNr.trim());
            if (matcher.find()) {
                regNrWithoutIndividualizingNr = matcher.group(1);
                individualizingNr = Integer.valueOf(matcher.group(2));
                if (matcher.group(4) != null) {
                    uniqueNumber = Integer.valueOf(matcher.group(4));
                } else {
                    uniqueNumber = null;
                }
            } else {
                regNrWithoutIndividualizingNr = wholeRegNr;
                individualizingNr = null;
                uniqueNumber = null;
            }
        } else {
            regNrWithoutIndividualizingNr = wholeRegNr;
            individualizingNr = null;
            uniqueNumber = null;
        }
    }

    public String getRegNrWithoutIndividualizingNr() {
        return regNrWithoutIndividualizingNr;
    }

    public String getRegNrWithIndividualizingNr() {
        if (individualizingNr == null) {
            return regNrWithoutIndividualizingNr;
        }
        return regNrWithoutIndividualizingNr + individualizingNr;
    }

    public String getShortRegNrWithoutIndividualizingNr() {
        String shortNr = StringUtils.substringAfterLast(regNrWithoutIndividualizingNr, DocumentServiceImpl.VOLUME_MARK_SEPARATOR);
        if (shortNr.endsWith("-")) {
            return StringUtils.chop(shortNr);
        }
        return shortNr;
    }

    public Integer getIndividualizingNr() {
        return individualizingNr;
    }

    public Integer getUniqueNumber() {
        return uniqueNumber;
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
