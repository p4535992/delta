package ee.webmedia.alfresco.utils;

/**
 * @param <T> - type that is given as an argument to the {@link #evaluate(T)} method
 *          Predicate that accepts generic type instance as an argument for {@link #evaluate(T)} method
 * @author Ats Uiboupin
 */
public interface Predicate<T> {
  public boolean evaluate(T object);
}
