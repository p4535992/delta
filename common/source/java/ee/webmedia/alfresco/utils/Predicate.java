package ee.webmedia.alfresco.utils;

/**
 * Type safe version of {@link org.apache.commons.collections.Predicate}, but as an abstract class(so that this is instanceof {@link org.apache.commons.collections.Predicate} and
 * does the trick with generics).<br>
 * Instead of implementing unparameterized {@link org.apache.commons.collections.Predicate#evaluate(Object)} implement {@link #eval(T)}
 * 
 * @param <T> - type of the object that is given as an argument to the {@link #evaluate(Object)} method
 */
public abstract class Predicate<T> implements org.apache.commons.collections.Predicate {
    @Override
    public final boolean evaluate(Object object) {
        @SuppressWarnings("unchecked")
        T o = (T) object;
        return eval(o);
    }

    public abstract boolean eval(T object);
}
