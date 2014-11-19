<<<<<<< HEAD
package ee.webmedia.alfresco.utils.beanmapper;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;

import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Class that helps to map alfresco properties map to instance of given class using JavaBeans setters and reflection(and vice versa).
 * The most significant URI of the field is at the moment URI defined by the interface where the getters/setters of this field are defined.<br>
 * If field doesn't have getters/setters defined by some implemented interface then URI of the concrete class is used.<br>
 * If concrete class has empty URI (@AlfrescoModelType(uri="")) then the URI of the closest parent class thet defines non-empty URI is used.<br>
 * If URI is not defined by above rules, empty URI is used (resulting value for mappableFieldWithoutUri woult be: qName.toString() ==
 * "{}mappableFieldWithoutUri"<br>
 * TODO: Kui kunagi tekib vajadus eelistada mõne konkreetse välja puhul mingit muud URI't, kui eelnevates reeglites kirjas, võib antud klassi edasi arendada.
 * 
 * @author Ats Uiboupin
 * @param <T> class to be mapped
 */
public class BeanPropertyMapper<T> {

    private static Log log = LogFactory.getLog(BeanPropertyMapper.class);

    private Class<T> mappedClass;

    private String CLASS_URI;

    private HashMap<String, Method> readMethods;
    private HashMap<String, String> methodUris;
    /** Fully qualified QName and Corresponding setter methods */
    private HashMap<QName, Method> qNameWriteMethods;
    private boolean addNullValuedToPropsMap;

    /**
     * @param properties Alfresco properties map
     * @return instance of given class &lt;T&gt; with properties set
     */
    public T toObject(Map<QName, Serializable> properties) {
        return toObject(properties, null);
    }

    /**
     * @param properties Alfresco properties map
     * @param target "template" object that will get properties from the map
     * @return target object with properties set(will override existing properties on given source object, if any)
     */
    public T toObject(Map<QName, Serializable> properties, T target) {
        if (target == null) {
            try {
                target = mappedClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("No target object given to be used for mapping and failed to call default constructor to create target object", e);
            }
        }
        for (QName qName : properties.keySet()) {
            final String propName = qName.getLocalName();
            final Serializable value = properties.get(qName);
            // final Method writer = writeMethods.get(propName);
            final Method writer = qNameWriteMethods.get(qName);

            if (writer != null && RepoUtil.isSystemProperty(qName)) {
                RuntimeException e = new RuntimeException("Handling a system property '"
                        + qName + "' is not allowed; target class " + mappedClass.getCanonicalName());
                log.error(e);
                throw e;
            }

            if (null == writer) {
                if (!RepoUtil.isSystemProperty(qName) && log.isTraceEnabled()) {
                    log.trace("got property '" + qName + "' but didn't find corresponding setter on target class: " + mappedClass.getCanonicalName());
                }
                continue;
            }

            try {
                Class<?> paramClass = writer.getParameterTypes()[0];
                if (value == null && paramClass.isPrimitive()) {
                    writer.invoke(target, 0);
                } else {
                    writer.invoke(target, value);
                }
            } catch (Exception e) {
                RuntimeException e2 = new RuntimeException(
                        "Failed to assign field '" + propName + "' value " + value + " for object " + target + " invoke method " + writer + "", e);
                log.error(e2);
                throw e2;
            }
        }
        return target;
    }

    public Map<QName, Serializable> toProperties(T object) {
        return toProperties(object, null);
    }

    public Map<QName, Serializable> toProperties(T object, Map<QName, Serializable> targetProps) {
        if (targetProps == null) {
            targetProps = new HashMap<QName, Serializable>();
        }

        if (log.isTraceEnabled()) {
            log.trace("Starting to map to properties with CLASS_URI '" + CLASS_URI + "' from object:"
                    + ReflectionToStringBuilder.reflectionToString(object, ToStringStyle.MULTI_LINE_STYLE));
        }
        for (String fieldName : readMethods.keySet()) {
            String uri = methodUris.get(fieldName);
            final Method reader = readMethods.get(fieldName);
            Object value;
            try {
                value = reader.invoke(object);
                if (!addNullValuedToPropsMap && !reader.getReturnType().isPrimitive() && value == null) {
                    log.trace("not adding null value of field '" + fieldName + "' to properties map");
                    continue;
                }
            } catch (Exception e) {
                throw new RuntimeException("failed to get value of field " + fieldName + " using method: " + reader, e);
            }
            if (uri == null) {
                final Class<?> declaringClass = reader.getDeclaringClass();
                final AlfrescoModelType uriSource = declaringClass.getAnnotation(AlfrescoModelType.class);
                if (uriSource != null) {
                    String declaringClassUri = uriSource.uri();
                    // FIXME: see ei pruugi olla päris korrektne lahendus -
                    // võtab küll interface'lt uri, aga võib-olla võiks võtta ka interface parent-interface't?):
                    if (uri == null && StringUtils.isNotBlank(declaringClassUri)) {
                        uri = declaringClassUri;
                    }
                }
                uri = (uri != null) ? uri : CLASS_URI;// prefer URI defined in interface over URI defined in class
            }
            targetProps.put(QName.createQName(uri, fieldName), (Serializable) value);
        }
        return targetProps;
    }

    public static <T> BeanPropertyMapper<T> newInstance(Class<T> mappedClass) {
        return new BeanPropertyMapper<T>().initialize(mappedClass);
    }

    protected BeanPropertyMapper<T> initialize(Class<T> mappedClass) {
        this.mappedClass = mappedClass;
        PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(mappedClass);

        // find fields of the class and parent classes
        Map<String/* fieldName */, LinkedHashMap<Class<?>, Field>> classesAndFieldsByFieldName = new HashMap<String, LinkedHashMap<Class<?>, Field>>();
        qNameWriteMethods = new HashMap<QName, Method>();
        AlfrescoModelType cAnnotation;
        boolean isAlfrescoModelType = false;
        Class<? super T> clazz = mappedClass;
        do {
            cAnnotation = clazz.getAnnotation(AlfrescoModelType.class);
            if (cAnnotation != null) {
                isAlfrescoModelType = true;
                if (StringUtils.isBlank(CLASS_URI)) {
                    CLASS_URI = cAnnotation.uri(); // if class doesn't declare URI, use URI from the nearest superclass as CLASS_URI
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("\tStarting to map " + clazz.getDeclaredFields().length + " fields of class " + clazz.getSimpleName());
            }
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue; // not mapping staitc fields
                }
                final AlfrescoModelProperty filedAnnotation = field.getAnnotation(AlfrescoModelProperty.class);
                if (filedAnnotation != null && !filedAnnotation.isMappable()) {
                    continue; // not mapping field that are annotated with @AlfrescoModelProperty(isMappable=false)
                }
                String fieldName = field.getName();

                QName qName = QName.createQName(cAnnotation != null ? cAnnotation.uri() : "", fieldName);
                Method fieldWriter = qNameWriteMethods.get(qName);
                if (fieldWriter == null) {// only spend time on finding writer if it is already not found from subclass
                    String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1, fieldName.length());
                    try {
                        Method setter = clazz.getDeclaredMethod(setterName, (Class<?>) field.getType());
                        if (cAnnotation != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Mappable class or it's superclass has a field named '" + fieldName + "', QName '" + qName
                                        + "' will be mapped into filed using '" + setter + "' for mappable class: " + mappedClass.getCanonicalName());
                            }
                            qNameWriteMethods.put(qName, setter);
                        }
                    } catch (SecurityException e) {
                        String errMsg = "Can't access setter method with name '" + setterName + "' and argument "
                                + field.getType().getClass().getCanonicalName();
                        log.error(errMsg, e);
                        throw new RuntimeException(errMsg, e);
                    } catch (NoSuchMethodException e) {
                        String errMsg = "No such method found with name '" + setterName + "' and argument " + field.getType().getClass().getCanonicalName();
                        log.error(errMsg, e);
                        throw new RuntimeException(errMsg, e);
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug("\t\tStarting to map field: " + fieldName);
                }
                LinkedHashMap<Class<?>, Field> fieldsByClass = classesAndFieldsByFieldName.get(fieldName);
                if (fieldsByClass == null) {
                    fieldsByClass = new LinkedHashMap<Class<?>, Field>();
                    classesAndFieldsByFieldName.put(fieldName, fieldsByClass);
                }
                fieldsByClass.put(clazz, field);
            }
        } while ((clazz = clazz.getSuperclass()) != null && !clazz.equals(Object.class));

        if (!isAlfrescoModelType) {
            throw new RuntimeException("The class " + mappedClass + " and its superclasses are not annotated with @AlfrescoModelType");
        } else if (StringUtils.isBlank(CLASS_URI)) {
            log.info("Neither superclasses nor mappable class itself " + mappedClass
                    + " has defined non-empty URI for mapping - all URIs will be retrieved from the interfaces for mapping to properties");
        }

        if (log.isDebugEnabled()) {
            log.debug("fields: " + classesAndFieldsByFieldName);
        }

        readMethods = new HashMap<String, Method>();
        for (PropertyDescriptor pd : pds) {
            final String fieldName = pd.getName();
            LinkedHashMap<Class<?>, Field> fieldDeclarations = classesAndFieldsByFieldName.get(fieldName);
            if (fieldDeclarations != null) {
                Method method = pd.getReadMethod();
                method = (method != null ? method : pd.getWriteMethod());
                Assert.assertNotNull(method);
                Field field = fieldDeclarations.get(method.getDeclaringClass());
                field = (field != null ? field : fieldDeclarations.get(method.getDeclaringClass()));

                if (field != null) {
                    final AlfrescoModelProperty filedAnnotation = field.getAnnotation(AlfrescoModelProperty.class);
                    if (filedAnnotation != null && !filedAnnotation.isMappable()) {
                        if (log.isDebugEnabled()) {
                            log.debug("not mapping field '" + fieldName + "' to properties, because it is annotated as not mappable: " + filedAnnotation);
                        }
                        continue;
                    }
                    final Method readMethod = pd.getReadMethod();
                    if (readMethod != null) {
                        readMethods.put(fieldName, readMethod);
                    }
                } else {
                    throw new IllegalArgumentException("Didn't find corresponding field to this propertyDescriptor: " + pd);
                }
            }
        }

        // get URI's from all interfaces and relate them with field names
        final Collection<Class<?>> allInterfaces = getAllInterfaces(mappedClass);
        methodUris = new HashMap<String, String>();
        // first try to associate method name to write methods based on URI mappings defined in interfaces(using @AlfrescoModelType(uri="someURI"))
        for (Class<?> interf : allInterfaces) {
            final AlfrescoModelType iAnnotation = interf.getAnnotation(AlfrescoModelType.class);
            if (iAnnotation != null) {
                final String interfAnnotationUri = iAnnotation.uri();
                final PropertyDescriptor[] pd = BeanUtils.getPropertyDescriptors(interf);
                for (PropertyDescriptor propertyDescriptor : pd) {
                    final String fieldName = propertyDescriptor.getName();
                    // at the moment we don't accept that interfaces have @AlfrescoModelType annotation with blank URI
                    Assert.assertTrue(interf.getCanonicalName() + "Inferfaces with methods should not have empty URI value : @AlfrescoModelType(uri=\"\")",
                            StringUtils.isNotBlank(interfAnnotationUri));
                    qNameWriteMethods.put(QName.createQName(interfAnnotationUri, fieldName), propertyDescriptor.getWriteMethod());
                    if (!methodUris.containsKey(fieldName)) {
                        methodUris.put(fieldName, interfAnnotationUri);
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(methodUris.size() + " methodUris: ");
            for (String methodName : methodUris.keySet()) {
                log.debug("\t" + methodName + "\t" + methodUris.get(methodName));
            }
            log.debug(qNameWriteMethods.size() + " qNameWriteMethods: ");
            for (QName qName : qNameWriteMethods.keySet()) {
                log.debug("\t" + qName + "\t" + qNameWriteMethods.get(qName));
            }
        }
        return this;
    }

    /**
     * @param classOrInterface
     * @return all interfaces (interfaces of the class + interfaces of the superClasses + interfaces of all the interfaces)
     */
    private Collection<Class<?>> getAllInterfaces(Class<?> classOrInterface) {
        final Set<Class<?>> allInterfaces = new HashSet<Class<?>>();
        addInterfacesFrom(classOrInterface, allInterfaces);
        final Class<?> superclass = classOrInterface.getSuperclass();
        if (superclass != null) {
            addInterfacesFrom(superclass, allInterfaces);
        }
        return allInterfaces;
    }

    private void addInterfacesFrom(final Class<?> clazz, final Set<Class<?>> allInterfaces) {
        for (Class<?> superInterf : clazz.getInterfaces()) {
            allInterfaces.add(superInterf);
            allInterfaces.addAll(getAllInterfaces(superInterf));
        }
    }

}
=======
package ee.webmedia.alfresco.utils.beanmapper;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;

import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Class that helps to map alfresco properties map to instance of given class using JavaBeans setters and reflection(and vice versa).
 * The most significant URI of the field is at the moment URI defined by the interface where the getters/setters of this field are defined.<br>
 * If field doesn't have getters/setters defined by some implemented interface then URI of the concrete class is used.<br>
 * If concrete class has empty URI (@AlfrescoModelType(uri="")) then the URI of the closest parent class thet defines non-empty URI is used.<br>
 * If URI is not defined by above rules, empty URI is used (resulting value for mappableFieldWithoutUri woult be: qName.toString() ==
 * "{}mappableFieldWithoutUri"<br>
 * TODO: Kui kunagi tekib vajadus eelistada mõne konkreetse välja puhul mingit muud URI't, kui eelnevates reeglites kirjas, võib antud klassi edasi arendada.
 * @param <T> class to be mapped
 */
public class BeanPropertyMapper<T> {

    private static Log log = LogFactory.getLog(BeanPropertyMapper.class);

    private Class<T> mappedClass;

    private String CLASS_URI;

    private HashMap<String, Method> readMethods;
    private HashMap<String, String> methodUris;
    /** Fully qualified QName and Corresponding setter methods */
    private HashMap<QName, Method> qNameWriteMethods;
    private boolean addNullValuedToPropsMap;

    /**
     * @param properties Alfresco properties map
     * @return instance of given class &lt;T&gt; with properties set
     */
    public T toObject(Map<QName, Serializable> properties) {
        return toObject(properties, null);
    }

    /**
     * @param properties Alfresco properties map
     * @param target "template" object that will get properties from the map
     * @return target object with properties set(will override existing properties on given source object, if any)
     */
    public T toObject(Map<QName, Serializable> properties, T target) {
        if (target == null) {
            try {
                target = mappedClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("No target object given to be used for mapping and failed to call default constructor to create target object", e);
            }
        }
        for (QName qName : properties.keySet()) {
            final String propName = qName.getLocalName();
            final Serializable value = properties.get(qName);
            // final Method writer = writeMethods.get(propName);
            final Method writer = qNameWriteMethods.get(qName);

            if (writer != null && RepoUtil.isSystemProperty(qName)) {
                RuntimeException e = new RuntimeException("Handling a system property '"
                        + qName + "' is not allowed; target class " + mappedClass.getCanonicalName());
                log.error(e);
                throw e;
            }

            if (null == writer) {
                if (!RepoUtil.isSystemProperty(qName) && log.isTraceEnabled()) {
                    log.trace("got property '" + qName + "' but didn't find corresponding setter on target class: " + mappedClass.getCanonicalName());
                }
                continue;
            }

            try {
                Class<?> paramClass = writer.getParameterTypes()[0];
                if (value == null && paramClass.isPrimitive()) {
                    writer.invoke(target, 0);
                } else {
                    writer.invoke(target, value);
                }
            } catch (Exception e) {
                RuntimeException e2 = new RuntimeException(
                        "Failed to assign field '" + propName + "' value " + value + " for object " + target + " invoke method " + writer + "", e);
                log.error(e2);
                throw e2;
            }
        }
        return target;
    }

    public Map<QName, Serializable> toProperties(T object) {
        return toProperties(object, null);
    }

    public Map<QName, Serializable> toProperties(T object, Map<QName, Serializable> targetProps) {
        if (targetProps == null) {
            targetProps = new HashMap<QName, Serializable>();
        }

        if (log.isTraceEnabled()) {
            log.trace("Starting to map to properties with CLASS_URI '" + CLASS_URI + "' from object:"
                    + ReflectionToStringBuilder.reflectionToString(object, ToStringStyle.MULTI_LINE_STYLE));
        }
        for (String fieldName : readMethods.keySet()) {
            String uri = methodUris.get(fieldName);
            final Method reader = readMethods.get(fieldName);
            Object value;
            try {
                value = reader.invoke(object);
                if (!addNullValuedToPropsMap && !reader.getReturnType().isPrimitive() && value == null) {
                    log.trace("not adding null value of field '" + fieldName + "' to properties map");
                    continue;
                }
            } catch (Exception e) {
                throw new RuntimeException("failed to get value of field " + fieldName + " using method: " + reader, e);
            }
            if (uri == null) {
                final Class<?> declaringClass = reader.getDeclaringClass();
                final AlfrescoModelType uriSource = declaringClass.getAnnotation(AlfrescoModelType.class);
                if (uriSource != null) {
                    String declaringClassUri = uriSource.uri();
                    // FIXME: see ei pruugi olla päris korrektne lahendus -
                    // võtab küll interface'lt uri, aga võib-olla võiks võtta ka interface parent-interface't?):
                    if (uri == null && StringUtils.isNotBlank(declaringClassUri)) {
                        uri = declaringClassUri;
                    }
                }
                uri = (uri != null) ? uri : CLASS_URI;// prefer URI defined in interface over URI defined in class
            }
            targetProps.put(QName.createQName(uri, fieldName), (Serializable) value);
        }
        return targetProps;
    }

    public static <T> BeanPropertyMapper<T> newInstance(Class<T> mappedClass) {
        return new BeanPropertyMapper<T>().initialize(mappedClass);
    }

    protected BeanPropertyMapper<T> initialize(Class<T> mappedClass) {
        this.mappedClass = mappedClass;
        PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(mappedClass);

        // find fields of the class and parent classes
        Map<String/* fieldName */, LinkedHashMap<Class<?>, Field>> classesAndFieldsByFieldName = new HashMap<String, LinkedHashMap<Class<?>, Field>>();
        qNameWriteMethods = new HashMap<QName, Method>();
        AlfrescoModelType cAnnotation;
        boolean isAlfrescoModelType = false;
        Class<? super T> clazz = mappedClass;
        do {
            cAnnotation = clazz.getAnnotation(AlfrescoModelType.class);
            if (cAnnotation != null) {
                isAlfrescoModelType = true;
                if (StringUtils.isBlank(CLASS_URI)) {
                    CLASS_URI = cAnnotation.uri(); // if class doesn't declare URI, use URI from the nearest superclass as CLASS_URI
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("\tStarting to map " + clazz.getDeclaredFields().length + " fields of class " + clazz.getSimpleName());
            }
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue; // not mapping staitc fields
                }
                final AlfrescoModelProperty filedAnnotation = field.getAnnotation(AlfrescoModelProperty.class);
                if (filedAnnotation != null && !filedAnnotation.isMappable()) {
                    continue; // not mapping field that are annotated with @AlfrescoModelProperty(isMappable=false)
                }
                String fieldName = field.getName();

                QName qName = QName.createQName(cAnnotation != null ? cAnnotation.uri() : "", fieldName);
                Method fieldWriter = qNameWriteMethods.get(qName);
                if (fieldWriter == null) {// only spend time on finding writer if it is already not found from subclass
                    String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1, fieldName.length());
                    try {
                        Method setter = clazz.getDeclaredMethod(setterName, (Class<?>) field.getType());
                        if (cAnnotation != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Mappable class or it's superclass has a field named '" + fieldName + "', QName '" + qName
                                        + "' will be mapped into filed using '" + setter + "' for mappable class: " + mappedClass.getCanonicalName());
                            }
                            qNameWriteMethods.put(qName, setter);
                        }
                    } catch (SecurityException e) {
                        String errMsg = "Can't access setter method with name '" + setterName + "' and argument "
                                + field.getType().getClass().getCanonicalName();
                        log.error(errMsg, e);
                        throw new RuntimeException(errMsg, e);
                    } catch (NoSuchMethodException e) {
                        String errMsg = "No such method found with name '" + setterName + "' and argument " + field.getType().getClass().getCanonicalName();
                        log.error(errMsg, e);
                        throw new RuntimeException(errMsg, e);
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug("\t\tStarting to map field: " + fieldName);
                }
                LinkedHashMap<Class<?>, Field> fieldsByClass = classesAndFieldsByFieldName.get(fieldName);
                if (fieldsByClass == null) {
                    fieldsByClass = new LinkedHashMap<Class<?>, Field>();
                    classesAndFieldsByFieldName.put(fieldName, fieldsByClass);
                }
                fieldsByClass.put(clazz, field);
            }
        } while ((clazz = clazz.getSuperclass()) != null && !clazz.equals(Object.class));

        if (!isAlfrescoModelType) {
            throw new RuntimeException("The class " + mappedClass + " and its superclasses are not annotated with @AlfrescoModelType");
        } else if (StringUtils.isBlank(CLASS_URI)) {
            log.info("Neither superclasses nor mappable class itself " + mappedClass
                    + " has defined non-empty URI for mapping - all URIs will be retrieved from the interfaces for mapping to properties");
        }

        if (log.isDebugEnabled()) {
            log.debug("fields: " + classesAndFieldsByFieldName);
        }

        readMethods = new HashMap<String, Method>();
        for (PropertyDescriptor pd : pds) {
            final String fieldName = pd.getName();
            LinkedHashMap<Class<?>, Field> fieldDeclarations = classesAndFieldsByFieldName.get(fieldName);
            if (fieldDeclarations != null) {
                Method method = pd.getReadMethod();
                method = (method != null ? method : pd.getWriteMethod());
                Assert.assertNotNull(method);
                Field field = fieldDeclarations.get(method.getDeclaringClass());
                field = (field != null ? field : fieldDeclarations.get(method.getDeclaringClass()));

                if (field != null) {
                    final AlfrescoModelProperty filedAnnotation = field.getAnnotation(AlfrescoModelProperty.class);
                    if (filedAnnotation != null && !filedAnnotation.isMappable()) {
                        if (log.isDebugEnabled()) {
                            log.debug("not mapping field '" + fieldName + "' to properties, because it is annotated as not mappable: " + filedAnnotation);
                        }
                        continue;
                    }
                    final Method readMethod = pd.getReadMethod();
                    if (readMethod != null) {
                        readMethods.put(fieldName, readMethod);
                    }
                } else {
                    throw new IllegalArgumentException("Didn't find corresponding field to this propertyDescriptor: " + pd);
                }
            }
        }

        // get URI's from all interfaces and relate them with field names
        final Collection<Class<?>> allInterfaces = getAllInterfaces(mappedClass);
        methodUris = new HashMap<String, String>();
        // first try to associate method name to write methods based on URI mappings defined in interfaces(using @AlfrescoModelType(uri="someURI"))
        for (Class<?> interf : allInterfaces) {
            final AlfrescoModelType iAnnotation = interf.getAnnotation(AlfrescoModelType.class);
            if (iAnnotation != null) {
                final String interfAnnotationUri = iAnnotation.uri();
                final PropertyDescriptor[] pd = BeanUtils.getPropertyDescriptors(interf);
                for (PropertyDescriptor propertyDescriptor : pd) {
                    final String fieldName = propertyDescriptor.getName();
                    // at the moment we don't accept that interfaces have @AlfrescoModelType annotation with blank URI
                    Assert.assertTrue(interf.getCanonicalName() + "Inferfaces with methods should not have empty URI value : @AlfrescoModelType(uri=\"\")",
                            StringUtils.isNotBlank(interfAnnotationUri));
                    qNameWriteMethods.put(QName.createQName(interfAnnotationUri, fieldName), propertyDescriptor.getWriteMethod());
                    if (!methodUris.containsKey(fieldName)) {
                        methodUris.put(fieldName, interfAnnotationUri);
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(methodUris.size() + " methodUris: ");
            for (String methodName : methodUris.keySet()) {
                log.debug("\t" + methodName + "\t" + methodUris.get(methodName));
            }
            log.debug(qNameWriteMethods.size() + " qNameWriteMethods: ");
            for (QName qName : qNameWriteMethods.keySet()) {
                log.debug("\t" + qName + "\t" + qNameWriteMethods.get(qName));
            }
        }
        return this;
    }

    /**
     * @param classOrInterface
     * @return all interfaces (interfaces of the class + interfaces of the superClasses + interfaces of all the interfaces)
     */
    private Collection<Class<?>> getAllInterfaces(Class<?> classOrInterface) {
        final Set<Class<?>> allInterfaces = new HashSet<Class<?>>();
        addInterfacesFrom(classOrInterface, allInterfaces);
        final Class<?> superclass = classOrInterface.getSuperclass();
        if (superclass != null) {
            addInterfacesFrom(superclass, allInterfaces);
        }
        return allInterfaces;
    }

    private void addInterfacesFrom(final Class<?> clazz, final Set<Class<?>> allInterfaces) {
        for (Class<?> superInterf : clazz.getInterfaces()) {
            allInterfaces.add(superInterf);
            allInterfaces.addAll(getAllInterfaces(superInterf));
        }
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
