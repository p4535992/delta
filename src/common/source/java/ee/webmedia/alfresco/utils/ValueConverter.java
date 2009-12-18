package ee.webmedia.alfresco.utils;


/**
 * Class that helps to convert given value to the instance of requeredClass
 * 
 * @author Ats Uiboupin
 * @deprecated use {#DefaultTypeConverter#INSTANCE} where possible.
 */
public class ValueConverter {

    public static <T> T convert(final Object fromValue, Class<T> requeredClass) {
        if (requeredClass.isAssignableFrom(fromValue.getClass())) {
            @SuppressWarnings("unchecked")
            T value = (T) fromValue; // No need to convert
            return value;
        }
        if (fromValue instanceof String) {
            String stringValue = (String) fromValue;
            if (String.class.equals(requeredClass)) {
                @SuppressWarnings("unchecked")
                T value = (T) stringValue;
                return value;
            } else if (Integer.class.equals(requeredClass)) {
                @SuppressWarnings("unchecked")
                T value = (T) Integer.valueOf(stringValue);
                return value;
            } else if (Double.class.equals(requeredClass)) {
                @SuppressWarnings("unchecked")
                T value = (T) Double.valueOf(stringValue);
                return value;
            } else {
                throw new RuntimeException("Not supported geting properties with value type '" + requeredClass.getCanonicalName()//
                        + "'. Property stringvalue: '" + stringValue + "'");
            }
        } else if (fromValue instanceof Long) {
            Long longValue = (Long) fromValue;
            if (Long.class.equals(requeredClass)) {
                @SuppressWarnings("unchecked")
                T value = (T) longValue;
                return value;
            } else if (Integer.class.equals(requeredClass)) {
                @SuppressWarnings("unchecked")
                T value = (T) Integer.valueOf(longValue.toString());
                return value;
            }
        }
        throw new IllegalArgumentException("Converting value from " + fromValue.getClass().getCanonicalName() + " to " + requeredClass
                + " is yet unimplemented.");
    }

}
