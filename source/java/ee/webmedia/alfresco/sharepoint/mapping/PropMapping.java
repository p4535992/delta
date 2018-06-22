package ee.webmedia.alfresco.sharepoint.mapping;

import static ee.smit.common.Utils.castToAnything;
import static org.apache.commons.lang.StringUtils.strip;
import static org.apache.commons.lang.StringUtils.trimToNull;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.convert.ConverterException;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.dom4j.Node;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.TextUtil;

public class PropMapping {

    private static final Map<String, Splitter> SPLITTERS;

    private static final DateFormat DAY_FORMAT = new SimpleDateFormat("dd");
    private static final DateFormat DAY_MONTH_FORMAT = new SimpleDateFormat("dd.MM");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    private final String from;
    private final QName to;
    private final QName toFirst;
    private final QName toSecond;
    private final Splitter splitter;
    private final String expression;
    private final PropertyValue propertyValue;
    private final PropertyValue propertyValue2;

    public PropMapping(Element element, TypeInfo typeInfo) {
        expression = trimToNull(element.attributeValue("expr"));
        from = trimToNull(element.attributeValue("from"));

        Assert.notNull(from, "Element 'prop' attribute 'from' must not be empty!");

        String attrTo = trimToNull(element.attributeValue("to"));
        String attrSplitter = trimToNull(element.attributeValue("splitter"));
        String attrPrefix = trimToNull(element.attributeValue("prefix"));

        if (attrTo != null) {
            PropertyDefinition propDef = typeInfo.requireProperty(attrTo);

            to = getQName(propDef);
            propertyValue = PropertyValue.getHandler(propDef, attrPrefix);
            toFirst = null;
            toSecond = null;
            splitter = null;
            propertyValue2 = null;
        } else if (attrSplitter != null) {
            splitter = SPLITTERS.get(attrSplitter);

            String attrFirst = trimToNull(element.attributeValue("toFirst"));
            String attrSecond = trimToNull(element.attributeValue("toSecond"));

            Assert.notNull(splitter, "Splitter '" + attrSplitter + "' not found!");
            Assert.notNull(attrFirst, "Attribute 'toFirst' not specified together with splitter '" + attrSplitter + "'.");
            Assert.notNull(attrSecond, "Attribute 'toSecond' not specified together with splitter '" + attrSplitter + "'.");

            PropertyDefinition propDef1 = typeInfo.requireProperty(attrFirst);
            PropertyDefinition propDef2 = typeInfo.requireProperty(attrSecond);

            toFirst = getQName(propDef1);
            toSecond = getQName(propDef2);
            propertyValue = PropertyValue.getHandler(propDef1, attrPrefix);
            propertyValue2 = PropertyValue.getHandler(propDef2, attrPrefix);
            to = null;
        } else {
            throw new RuntimeException("Neither 'to' nor 'splitter' are specified for property mapping '" + from + "'.");
        }
    }

    private static QName getQName(PropertyDefinition propDef) {
        if (DocumentCommonModel.DOCCOM_URI.equals(propDef.getName().getNamespaceURI())) {
            return QName.createQName(DocumentCommonModel.URI, propDef.getName().getLocalName());
        }
        return propDef.getName();
    }

    public QName getTo() {
        return to;
    }

    public String getFrom() {
        return from;
    }

    @Override
    public String toString() {
        if (splitter == null) {
            return String.format("[%s -> %s] with value helper %s", from, to, propertyValue);
        }
        return String.format("[%s - %s -> (%s, %s)] with value helpers (%s, %s)", from, splitter, toFirst, toSecond, propertyValue, propertyValue2);
    }

    void addPropValues(Element root, Map<QName, Serializable> propValues) {
        String value;

        if (expression == null) {
            value = root.elementTextTrim(from);
        } else if (expression.equals("xpath")) {
            Node node = root.selectSingleNode(from);
            value = node != null ? StringUtils.trim(node.getText()) : null;
        } else if (expression.equals("xpathContractParty")) {
            List<List<String>> values = new ArrayList<List<String>>();
            for (String fromXpath : from.split(",")) {
                List<Node> nodes = castToAnything(root.selectNodes(fromXpath));
                if (nodes != null) {
                    int i = 1;
                    for (Node node : nodes) {
                        String columnValue = node != null ? StringUtils.trim(node.getText()) : null;
                        List<String> rowValues;
                        if (values.size() < i) {
                            rowValues = new ArrayList<String>();
                            values.add(rowValues);
                        } else {
                            rowValues = values.get(i - 1);
                        }
                        rowValues.add(columnValue);
                        i++;
                    }
                }
            }
            List<String> rows = new ArrayList<String>();
            for (List<String> rowValues : values) {
                String rowValue = TextUtil.joinNonBlankStringsWithComma(rowValues);
                if (StringUtils.isNotBlank(rowValue)) {
                    rows.add("Lepingu osapool: " + rowValue);
                }
            }
            value = TextUtil.joinNonBlankStrings(rows, "\n");
        } else if (expression.equals("const")) {
            value = from;
        } else {
            throw new RuntimeException("Bad expression type " + expression);
        }

        value = trimToNull(value);

        if (value == null) {
            return;
        }

        if (splitter == null) {
            try {
                propValues.put(to, propertyValue.evaluate(propValues.get(to), value));
            } catch (Exception e) {
                if (e.getCause() instanceof NumberFormatException) {
                    propValues.put(DocumentCommonModel.Props.COMMENT, PropertyValue.addComment((String) propValues.get(DocumentCommonModel.Props.COMMENT), "Summa", value));
                }
            }
        } else {
            try {
                Object[] pair = splitter.split(value);
                propValues.put(toFirst, propertyValue.evaluate(propValues.get(toFirst), (String) pair[0]));
                propValues.put(toSecond, propertyValue2.evaluate(propValues.get(toSecond), (String) pair[1]));
            } catch (ConverterException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class PeriodSplitter implements Splitter {
        @Override
        public Object[] split(String s) throws ConverterException {
            Date first = null;
            Date second = null;
            int i = s.indexOf('-');
            if (i != -1) {
                try {
                    String sf = strip(s.substring(0, i), ". ");
                    String ss = strip(s.substring(i + 1), ". ");
                    second = DATE_FORMAT.parse(ss);
                    try {
                        first = DATE_FORMAT.parse(sf);
                    } catch (ParseException e) {
                        try {
                            first = DAY_MONTH_FORMAT.parse(sf);
                            first = combine(first, second, false);
                        } catch (ParseException ee) {
                            first = DAY_FORMAT.parse(sf);
                            first = combine(first, second, true);
                        }
                    }
                    return new Date[] { first, second };
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                first = DATE_FORMAT.parse(s);
                return new Date[] { first, first };
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        private Date combine(Date dayDate, Date yearDate, boolean withMonthes) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(yearDate);
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            calendar.setTime(dayDate);
            calendar.set(Calendar.YEAR, year);
            if (withMonthes) {
                calendar.set(Calendar.MONTH, month);
            }
            return calendar.getTime();
        }

        @Override
        public String toString() {
            return "'period' splitter";
        }
    }

    static {
        DATE_FORMAT.setLenient(false);
        DAY_MONTH_FORMAT.setLenient(false);
        DAY_FORMAT.setLenient(false);

        SPLITTERS = Collections.singletonMap("period", (Splitter) new PeriodSplitter());
    }

}