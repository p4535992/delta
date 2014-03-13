package ee.webmedia.alfresco.utils;

/**
 * Typesafe version of {@link org.apache.commons.collections.Transformer}, but as an abstract class(so that this is instanceof {@link org.apache.commons.collections.Transformer}
 * and to do the trick with generics).<br>
 * Instead of implementing unparameterized {@link org.apache.commons.collections.Transformer#transform(Object)} implement {@link #tr(T)}
 */
public abstract class Transformer<T> implements org.apache.commons.collections.Transformer {

    @Override
    @SuppressWarnings("unchecked")
    public Object transform(Object input) {
        return tr((T) input);
    }

    public abstract Object tr(T input);
}
