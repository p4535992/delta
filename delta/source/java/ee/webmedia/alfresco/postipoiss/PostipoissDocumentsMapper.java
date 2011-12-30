package ee.webmedia.alfresco.postipoiss;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.web.DocAdminUtil;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.TreeNode;

/**
 * @author Aleksei Lissitsin
 */
public class PostipoissDocumentsMapper {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PostipoissDocumentsMapper.class);

    static class Mapping {
        String from;
        QName to;
        QName assoc;
        List<PropMapping> props;
        Set<Mapping> subMappings = new HashSet<Mapping>();
        TypeInfo typeInfo;
        String defaultVolume;

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder(String.format("[%s -> %s %s :\n",
                    from,
                    to == null ? null : to.toPrefixString(getNamespaceService()),
                    assoc == null ? null : assoc.toPrefixString(getNamespaceService())));
            for (PropMapping m : props) {
                s.append("  ");
                s.append(m);
                s.append("\n");
            }
            s.append("]");
            return s.toString();
        }

        public Mapping() {
            props = new ArrayList<PropMapping>();
        }

        public Mapping(Mapping m) {
            props = new ArrayList<PropMapping>(m.props);
        }

        public Mapping(String from, TypeInfo typeInfo) {
            this.from = from;
            this.typeInfo = typeInfo;
            to = typeInfo.qname;
        }

        public Mapping(Mapping m, String from, TypeInfo typeInfo) {
            if (m != null) {
                props = new ArrayList<PropMapping>(m.props);
            } else {
                props = new ArrayList<PropMapping>();
            }
            this.from = from;
            this.typeInfo = typeInfo;
            to = typeInfo.qname;
        }

        public void add(PropMapping pm) {
            props.add(pm);
        }

        public PropMapping requirePropMappingTo(String to) {
            PropMapping result = null;
            for (PropMapping propMapping : props) {
                if (to.equals(propMapping.to)) {
                    if (result != null) {
                        throw new RuntimeException("Multiple <prop> elements with same to='" + to + "' found under <mapping from='" + from + "' to='" + this.to + "'>");
                    }
                    result = propMapping;
                }
            }
            if (result == null) {
                throw new RuntimeException("No <prop> elements with to='" + to + "' found under <mapping from='" + from + "' to='" + this.to + "'>");
            }
            return result;
        }
    }

    static class PropMapping {
        String from;
        String to;
        String toFirst;
        String toSecond;
        String prefix;
        Splitter splitter;
        String expression;

        @Override
        public String toString() {
            if (splitter == null) {
                if (prefix == null) {
                    return String.format("[%s -> %s]", from, to);
                } else {
                    return String.format("[%s -> %s : %s]", from, to, prefix);
                }
            }
            return String.format("[%s - %s -> (%s, %s)]", from, splitter, toFirst, toSecond);
        }

        public PropMapping() {
        }

        public PropMapping(String from, String to, String prefix, String expression) {
            this.from = from;
            this.to = to;
            this.prefix = prefix;
            this.expression = expression;
        }

        public PropMapping(String from, String to, String prefix, String toFirst, String toSecond, Splitter splitter, String expression) {
            this.to = to;
            this.prefix = prefix;
            this.from = from;
            this.toFirst = toFirst;
            this.toSecond = toSecond;
            this.splitter = splitter;
            this.expression = expression;
        }

    }

    private static DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private static DateFormat dateFormatWithoutYear = new SimpleDateFormat("dd.MM");
    private static DateFormat dateFormatOnlyDay = new SimpleDateFormat("dd");
    static {
        dateFormat.setLenient(false);
        dateFormatWithoutYear.setLenient(false);
        dateFormatOnlyDay.setLenient(false);
    }

    @SuppressWarnings("serial")
    static class ConvertException extends Exception {
    }

    static interface Splitter {
        Pair split(String s) throws ConvertException;
    }

    static class PeriodSplitter implements Splitter {
        @Override
        public Pair split(String s) throws ConvertException {
            Date first = null;
            Date second = null;
            int i = s.indexOf('-');
            if (i != -1) {
                try {
                    String sf = StringUtils.strip(s.substring(0, i), ". ");
                    String ss = StringUtils.strip(s.substring(i + 1), ". ");
                    second = dateFormat.parse(ss);
                    try {
                        first = dateFormat.parse(sf);
                    } catch (ParseException e) {
                        try {
                            first = dateFormatWithoutYear.parse(sf);
                            first = combine(first, second, false);
                        } catch (ParseException ee) {
                            first = dateFormatOnlyDay.parse(sf);
                            first = combine(first, second, true);
                        }
                    }
                    return new Pair(first, second);
                } catch (ParseException e) {
                }
            } else {
                try {
                    first = dateFormat.parse(s);
                    return new Pair(first, first);
                } catch (ParseException e) {
                }
            }
            throw new ConvertException();
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

    static class Pair {
        Object first;
        Object second;

        public Pair(Object first, Object second) {
            this.first = first;
            this.second = second;
        }

    }

    static class StringValueAppender {
        String s;
        StringBuilder sb;
        String separator = ", ";

        public StringValueAppender() {
        }

        public StringValueAppender(String separator) {
            this.separator = separator;
        }

        public void add(String s) {
            if (StringUtils.isBlank(s)) {
                return;
            }
            if (this.s == null) {
                this.s = s;
            } else {
                if (sb == null) {
                    sb = new StringBuilder(this.s);
                }
                sb.append(separator).append(s);
            }
        }

        public String get() {
            if (sb == null) {
                return s;
            } else {
                return sb.toString();
            }
        }
    }

    static String join(String separator, String... strings) {
        StringValueAppender app = new StringValueAppender(separator);
        for (String s : strings) {
            app.add(s);
        }
        return app.get();
    }

    static abstract class PropertyValueProvider {
        QName qname;

        PropertyValueProvider withQName(QName qname) {
            this.qname = qname;
            return this;
        }

        protected abstract PropertyValue provide();

        @Override
        public String toString() {
            return ObjectUtils.toString(qname);
        }
    }

    static class StringPropertyValueProvider extends PropertyValueProvider {

        @Override
        public PropertyValue provide() {
            return new StringPropertyValue().withQName(qname);
        }

    }

    static class CommentPropertyValueProvider extends PropertyValueProvider {

        @Override
        public PropertyValue provide() {
            return new CommentPropertyValue().withQName(qname);
        }

    }

    static class DatePropertyValueProvider extends PropertyValueProvider {

        @Override
        public PropertyValue provide() {
            return new DatePropertyValue().withQName(qname);
        }

    }

    static class NumberPropertyValueProvider extends PropertyValueProvider {

        @Override
        public PropertyValue provide() {
            return new NumberPropertyValue().withQName(qname);
        }

    }

    static class DocumentValue {
        TypeInfo typeInfo;
        Map<String, PropertyValue> props = new HashMap<String, PropertyValue>();

        public DocumentValue(TypeInfo typeInfo) {
            this.typeInfo = typeInfo;
        }

        void put(String s, String prop, String prefix) {
            if (StringUtils.isNotBlank(s)) {
                getPropertyValue(prop).put(s, prefix);
            }
        }

        void putObject(Object o, String prop) {
            if (o != null) {
                getPropertyValue(prop).putObject(o);
            }
        }

        private PropertyValue getPropertyValue(String prop) {
            PropertyValue value = props.get(prop);
            if (value == null) {
                PropertyValueProvider valueProvider = typeInfo.props.get(prop);
                Assert.notNull(valueProvider);
                value = valueProvider.provide();
                props.put(prop, value);
            }
            return value;
        }
    }

    static abstract class PropertyValue {
        QName qname;

        PropertyValue withQName(QName qname) {
            this.qname = qname;
            return this;
        }

        abstract void put(String s);

        void putWithPrefix(String s, String prefix) {
            throw new UnsupportedOperationException("Could not put " + s + " with prefix " + prefix);
        }

        void put(String s, String prefix) {
            if (prefix == null) {
                put(s);
            } else {
                putWithPrefix(s, prefix);
            }
        }

        void putObject(Object o) {
            throw new UnsupportedOperationException();
        }

        abstract Serializable get();
    }

    static class StringPropertyValue extends PropertyValue {
        StringValueAppender value = new StringValueAppender();

        @Override
        public void put(String s) {
            value.add(s);
        }

        @Override
        public Serializable get() {
            return value.get();
        }
    }

    static class CommentPropertyValue extends StringPropertyValue {
        final static String SPACE_DELIMED = ".spaced";
        List<String> prefixes = new ArrayList<String>();
        Map<String, StringValueAppender> values = new HashMap<String, StringValueAppender>();
        StringValueAppender spaceDelimedValue = new StringValueAppender(" ");
        {
            value = new StringValueAppender("\n");
        }

        @Override
        public void putWithPrefix(String s, String prefix) {
            Assert.notNull(prefix);
            if (SPACE_DELIMED.equals(prefix)) {
                spaceDelimedValue.add(s);
                return;
            }
            if (!StringUtils.isBlank(s)) {
                StringValueAppender app;
                if (!values.containsKey(prefix)) {
                    prefixes.add(prefix);
                    app = new StringValueAppender();
                    values.put(prefix, app);
                } else {
                    app = values.get(prefix);
                }
                app.add(s);
            }
        }

        @Override
        public Serializable get() {
            value.add(spaceDelimedValue.get());
            for (String prefix : prefixes) {
                String prefixValue = values.get(prefix).get();
                if (StringUtils.isNotBlank(prefixValue)) {
                    value.add(prefix + ": " + prefixValue);
                }
            }
            String string = value.get();
            return string;
        }
    }

    static class DatePropertyValue extends PropertyValue {
        Date value;

        @Override
        public Serializable get() {
            return value;
        }

        @Override
        public void put(String s) {
            try {
                value = dateFormat.parse(s);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void putObject(Object o) {
            if (o == null) {
                return;
            }
            Assert.isInstanceOf(Date.class, o);
            value = (Date) o;
        }

    }

    static class NumberPropertyValue extends PropertyValue {
        String value;

        @Override
        public Serializable get() {
            return value;
        }

        @Override
        public void put(String s) {
            String number = extractNumber(s);
            try {
                Integer.parseInt(number);
            } catch (NumberFormatException e) {
            }
        }

        private static String extractNumber(String s) {
            StringBuilder sb = new StringBuilder();
            for (Character c : s.toCharArray()) {
                if (isGoodNumberCharacter(c)) {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private static boolean isGoodNumberCharacter(Character c) {
            return Character.isDigit(c) || '.' == c;
        }
    }

    static class TypeInfo {
        String name;
        QName qname;
        Map<String, PropertyValueProvider> props = new HashMap<String, PropertyValueProvider>();
        DocumentTypeVersion docVer;
        Map<String, org.alfresco.util.Pair<DynamicPropertyDefinition, Field>> propDefs;
        TreeNode<QName> childAssocTypeQNameTree;
        QName[] hierarchy;

        public TypeInfo(String name, QName qname) {
            this.name = name;
            this.qname = qname;
        }

    }

    TypeInfo createTypeInfo(String name, String prefix, Map<String, org.alfresco.util.Pair<DynamicPropertyDefinition, Field>> propDefs,
            TreeNode<QName> parentChildAssocTypeQNameTree, QName[] parentHierarchy) {

        QName qname = QName.createQName(prefix, name, namespaceService);
        TypeInfo typeInfo = new TypeInfo(name, qname);

        if (propDefs == null) {

            if (!"docdyn".equals(prefix)) {
                Collection<QName> aspects = generalService.getDefaultAspects(qname);
                aspects.add(qname);
                for (QName aspect : aspects) {
                    for (PropertyDefinition propDef : dictionaryService.getPropertyDefs(aspect).values()) {
                        QName prop = propDef.getName();
                        if (DocumentCommonModel.DOCCOM_URI.equals(prop.getNamespaceURI())) {
                            PropertyValueProvider valueProvider = getProvider(prop.getLocalName(), propDef);
                            if (valueProvider == null) {
                                continue;
                            }
                            if ("doccom".equals(prefix) && "common".equals(name)) {
                                prop = QName.createQName(DocumentDynamicModel.URI, prop.getLocalName());
                            }
                            typeInfo.props.put(prop.getLocalName(), valueProvider.withQName(prop));
                            log.debug("valueprovider prop=" + prop.toPrefixString(namespaceService) + " type prefix+name=" + prefix + ":" + name);
                        }
                    }
                }
                return typeInfo;
            }

            DocumentType docType = getDocumentAdminService().getDocumentType(name, DocumentAdminService.DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER);
            Assert.notNull(docType, "Document type doesn't exist: " + name);
            typeInfo.docVer = docType.getLatestDocumentTypeVersion();
            typeInfo.propDefs = getDocumentConfigService().getPropertyDefinitions(DocAdminUtil.getDocTypeIdAndVersionNr(typeInfo.docVer));
            typeInfo.childAssocTypeQNameTree = getDocumentConfigService().getChildAssocTypeQNameTree(typeInfo.docVer);
            typeInfo.hierarchy = new QName[] {};
        } else {
            typeInfo.docVer = null;
            typeInfo.propDefs = propDefs;
            typeInfo.childAssocTypeQNameTree = null;
            for (TreeNode<QName> treeNode : parentChildAssocTypeQNameTree.getChildren()) {
                if (treeNode.getData().equals(qname)) {
                    typeInfo.childAssocTypeQNameTree = treeNode;
                    break;
                }
            }
            QName parentChildAssocTypeQName = parentChildAssocTypeQNameTree.getData();
            Assert.notNull(typeInfo.childAssocTypeQNameTree, "Child node type " + qname.toPrefixString(namespaceService) + " not found for parent node type "
                    + (parentChildAssocTypeQName == null ? null : parentChildAssocTypeQName.toPrefixString(namespaceService)));
            typeInfo.hierarchy = (QName[]) ArrayUtils.add(parentHierarchy, typeInfo.childAssocTypeQNameTree.getData());
        }

        for (org.alfresco.util.Pair<DynamicPropertyDefinition, Field> pair : typeInfo.propDefs.values()) {
            DynamicPropertyDefinition propDef = pair.getFirst();
            QName[] hierarchy = propDef.getChildAssocTypeQNameHierarchy();
            if (hierarchy == null) {
                hierarchy = new QName[] {};
            }
            if (!Arrays.equals(hierarchy, typeInfo.hierarchy)) {
                continue;
            }

            QName prop = propDef.getName();
            PropertyValueProvider valueProvider = getProvider(prop.getLocalName(), propDef);
            if (valueProvider == null) {
                continue;
            }
            typeInfo.props.put(prop.getLocalName(), valueProvider.withQName(prop));
        }
        return typeInfo;
    }

    static PropertyValueProvider getProvider(String name, PropertyDefinition propDef) {
        // SIM "errandComment".equals(name) || "sendDesc".equals(name)
        if ("comment".equals(name) || "content".equals(name) || "price".equals(name)) {
            return new CommentPropertyValueProvider();
        }
        String javaClassName = propDef.getDataType().getJavaClassName();
        if ("java.util.Date".equals(javaClassName)) {
            return new DatePropertyValueProvider();
        }
        if ("java.lang.Double".equals(javaClassName) || "java.lang.Long".equals(javaClassName)) {
            return new NumberPropertyValueProvider();
        }
        if (!javaClassName.equals("java.lang.String")) {
            log.info("Data type " + javaClassName + " is not supported for property " + propDef.getName().toPrefixString(BeanHelper.getNamespaceService())
                    + ", not allowing mapping");
            return null;
        }
        return new StringPropertyValueProvider();
    }

    private final Map<String, Splitter> splitters = new HashMap<String, Splitter>();
    Map<String, TypeInfo> typeInfos = new HashMap<String, TypeInfo>();

    private final SAXReader xmlReader = new SAXReader();

    protected PropMapping createPropMapping(Element el, TypeInfo typeInfo) {
        String from = el.attributeValue("from");
        Assert.notNull(from, "'From' must not be null!");
        String expression = el.attributeValue("expr");
        String to = el.attributeValue("to");
        String prefix = null;
        if (to != null) {
            PropertyValueProvider propertyValueProvider = typeInfo.props.get(to);
            Assert.notNull(propertyValueProvider, "Property " + to + " is not registered for the type " + typeInfo.name + "\n Registered properties are: "
                    + typeInfo.props.keySet());
            prefix = el.attributeValue("prefix");
        }
        String splitterName = el.attributeValue("splitter");
        if (splitterName != null) {
            Splitter splitter = splitters.get(splitterName);
            Assert.notNull(splitter, "Splitter " + splitterName + " not found!");
            String toFirst = el.attributeValue("toFirst");
            PropertyValueProvider toFirstProvider = typeInfo.props.get(toFirst);
            Assert.notNull(toFirstProvider, "Property " + toFirst + " is not registered for the type " + typeInfo.name);
            String toSecond = el.attributeValue("toSecond");
            PropertyValueProvider toSecondProvider = typeInfo.props.get(toSecond);
            Assert.notNull(toSecondProvider, "Property " + toSecond + " is not registered for the type " + typeInfo.name);
            return new PropMapping(from, to, prefix, toFirst, toSecond, splitter, expression);
        } else {
            if (to == null) {
                throw new RuntimeException("Neither to nor splitter are specified for mapping " + from);
            }
            return new PropMapping(from, to, prefix, expression);
        }
    }

    protected Mapping createMapping(Mapping base, Element el) {
        return createMapping(base, el, "docdyn", null, null, null);
    }

    protected Mapping createMapping(Mapping base, Element el, String prefix, Map<String, org.alfresco.util.Pair<DynamicPropertyDefinition, Field>> parentPropDefs,
            TreeNode<QName> parentChildAssocTypeQNameTree, QName[] parentHierarchy) {

        String from = el.attributeValue("from");
        String to = el.attributeValue("to");
        String assoc = el.attributeValue("assoc");
        String defaultVolume = el.attributeValue("defaultVolume");

        if (to == null) {
            prefix = "doccom";
            to = "common"; // doesn't matter which doctype, generalType just has to have a base mapping
        }
        TypeInfo typeInfo = typeInfos.get(to);
        if (typeInfo == null) {
            typeInfo = createTypeInfo(to, prefix, parentPropDefs, parentChildAssocTypeQNameTree, parentHierarchy);
            typeInfos.put(to, typeInfo);
        }

        Mapping m = new Mapping(base, from, typeInfo);
        m.defaultVolume = defaultVolume;

        if (StringUtils.isNotEmpty(assoc)) {
            m.assoc = QName.createQName(prefix, assoc, namespaceService);
        }

        for (Object o : el.elements("prop")) {
            m.add(createPropMapping((Element) o, typeInfo));
        }

        for (Object o : el.elements("child")) {
            Element e = (Element) o;
            // String childPrefix = e.attributeValue("prefix");
            // if (childPrefix == null) {
            // childPrefix = "docspec";
            // }
            Mapping subMapping = createMapping(null, e, "docchild", typeInfo.propDefs, typeInfo.childAssocTypeQNameTree, typeInfo.hierarchy);
            m.subMappings.add(subMapping);
        }

        return m;
    }

    protected Map<String, Mapping> loadMetadataMappings(File mappingsFile) throws Exception {
        log.info("Loading meta-data mappings from file " + mappingsFile);
        typeInfos = new HashMap<String, TypeInfo>();
        Map<String, Mapping> mappings = new HashMap<String, Mapping>();
        splitters.put("period", new PeriodSplitter());
        Document document = xmlReader.read(mappingsFile);
        Element root = document.getRootElement();

        Element generalType = root.element("generalType");

        Mapping base = createMapping(null, generalType);

        Element sendInfoType = root.element("sendInfoType");

        Mapping sendInfo = createMapping(null, sendInfoType, "doccom", null, null, null);

        mappings.put("sendInfo", sendInfo);

        for (Object o : root.elements("documentType")) {
            Mapping m = createMapping(base, (Element) o);
            mappings.put(m.from, m);
        }

        StringBuilder s = new StringBuilder("Loaded " + mappings.size() + " meta-data mappings:");
        for (Mapping m : mappings.values()) {
            s.append("\n").append(m);
        }
        log.info(s.toString());
        return mappings;
    }

    private NamespaceService namespaceService;
    private DictionaryService dictionaryService;
    private GeneralService generalService;

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }
}
