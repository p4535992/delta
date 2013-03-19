package ee.webmedia.alfresco.utils;

/**
 * Typesafe version of {@link org.apache.commons.collections.Transformer}, but as an abstract class(so that this is instanceof {@link org.apache.commons.collections.Transformer}
 * and to do the trick with generics).<br>
 * Instead of implementing unparameterized {@link org.apache.commons.collections.Transformer#transform(Object)} implement {@link #tr(T)}
 * 
 * @author Ats Uiboupin
 */
public abstract class ComparableTransformer<T> extends Transformer<T, Comparable<?>> {

    @Override
    public abstract Comparable<?> tr(T input);
}
