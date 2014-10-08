package ee.webmedia.alfresco.utils.beanmapper;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ee.webmedia.alfresco.dvk.model.DvkModel;
import ee.webmedia.alfresco.dvk.model.DvkReceivedDocument;

public class BeanPropertyMapperTest extends TestCase {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(BeanPropertyMapperTest.class);
    private static final String letterAccessRestrictionReason;
    private static final String dvkId;
    private static final String simpleMappable;
    private static final Date letterDeadLine;
    public static final String TestableModel_URI = "xxx";
    public static final String OtherInterface_URI = "http://uri.of.other.interface/";
    private static final String otherInterfaceField;
    private static BeanPropertyMapper<TestableModel> mapper = BeanPropertyMapper.newInstance(TestableModel.class);
    static {
        dvkId = "123";
        letterAccessRestrictionReason = "some absurdish reason";
        simpleMappable = "simpleMappable value (no interface, etc)";
        letterDeadLine = new Date();
        otherInterfaceField = "otherInterfaceField value";
    }
    private static TestableModel testableModel;
    private static Map<QName, Serializable> mappedProperties;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        testableModel = new TestableModel();
        testableModel.setDvkId(dvkId);// defined by interface, implemented in parent class and overridden in concrete class
        testableModel.setSimpleMappable(simpleMappable);
        testableModel.setOtherInterfaceField(otherInterfaceField);
        testableModel.setNotMappable("---------notMappable----");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testToPropertiesT() {
        mappedProperties = mapper.toProperties(testableModel);
        if (log.isDebugEnabled()) {
            log.debug("=========== got properties with " + mappedProperties.size() + " entries:");
            for (QName qName : mappedProperties.keySet()) {
                log.debug("\t" + qName + " \t" + mappedProperties.get(qName));
            }
            log.debug("=========== object that was mapped, was " + ToStringBuilder.reflectionToString(testableModel, ToStringStyle.MULTI_LINE_STYLE));
        }
        Assert.assertEquals(dvkId, mappedProperties.get(QName.createQName(DvkModel.URI, "dvkId")));
        Assert.assertEquals(letterAccessRestrictionReason, mappedProperties.get(QName.createQName(DvkModel.URI, "letterAccessRestrictionReason")));
        Assert.assertEquals(letterDeadLine, mappedProperties.get(QName.createQName(DvkModel.URI, "letterDeadLine")));
        Assert.assertEquals(otherInterfaceField, mappedProperties.get(QName.createQName(OtherInterface_URI, "otherInterfaceField")));
        Assert.assertEquals(simpleMappable, mappedProperties.get(QName.createQName(TestableModel_URI, "simpleMappable")));
        Assert.assertEquals(null, mappedProperties.get(QName.createQName(DvkModel.URI, "notMappable")));
    }

    public void testToObjectMapOfQNameSerializable() {
        // add aditional property (not present in object)
        mappedProperties.put(QName.createQName("noSuchUri, no such property", "no such property"), "no such value either");

        mappedProperties.put(QName.createQName("noSuchUri, but existing property", "dvkId"), "cmNameValue");
        final TestableModel mappedObject2 = mapper.toObject(mappedProperties);
        if (log.isDebugEnabled()) {
            log.debug("Properties parsed to object: " + ToStringBuilder.reflectionToString(mappedObject2, ToStringStyle.MULTI_LINE_STYLE));
            log.debug("Given properties with " + mappedProperties.size() + " entries were:");
            for (QName qName : mappedProperties.keySet()) {
                log.debug("\t" + qName + " \t" + mappedProperties.get(qName));
            }
        }

        Assert.assertEquals(dvkId, mappedObject2.getDvkId());
        Assert.assertEquals(null, mappedObject2.getNotMappable());
        Assert.assertEquals(otherInterfaceField, mappedObject2.getOtherInterfaceField());
        Assert.assertEquals(simpleMappable, mappedObject2.getSimpleMappable());
    }

}

@AlfrescoModelType(uri = BeanPropertyMapperTest.OtherInterface_URI)
interface OtherInterface {
    public static String someStaticField = "someStaticField value, that should not be serialized";

    public String getOtherInterfaceField();

    public void setOtherInterfaceField(String a);
}

@AlfrescoModelType(uri = BeanPropertyMapperTest.TestableModel_URI)
class TestableModel extends DvkReceivedDocument implements OtherInterface, Serializable {
    private static final long serialVersionUID = 1L;
    @AlfrescoModelProperty(isMappable = false)
    private String notMappable;
    @SuppressWarnings("unused")
    @AlfrescoModelProperty(isMappable = false)
    private final String notMappableField = "this doesn't even have a getter/setter";
    private String simpleMappable;
    private String otherInterfaceField;
    private String dvkId;

    @Override
    public String getDvkId() {
        return dvkId;
    }

    @Override
    public void setDvkId(String dvkId) {
        this.dvkId = dvkId;
    }

    @Override
    public String getOtherInterfaceField() {
        return otherInterfaceField;
    }

    @Override
    public void setOtherInterfaceField(String otherInterfaceField) {
        this.otherInterfaceField = otherInterfaceField;
    }

    public String getSimpleMappable() {
        return simpleMappable;
    }

    public void setSimpleMappable(String simpleMappable) {
        this.simpleMappable = simpleMappable;
    }

    public String getNotMappable() {
        return notMappable;
    }

    public void setNotMappable(String notMappable) {
        this.notMappable = notMappable;
    }

}
