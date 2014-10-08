<<<<<<< HEAD
package ee.webmedia.alfresco.utils;

/**
 * Typesafe version of {@link org.apache.commons.collections.Transformer}, but as an abstract class(so that this is instanceof {@link org.apache.commons.collections.Transformer}
 * and to do the trick with generics).<br>
 * Instead of implementing unparameterized {@link org.apache.commons.collections.Transformer#transform(Object)} implement {@link #tr(T)}
 * 
 * @author Ats Uiboupin
 */
public abstract class Transformer<T, R> implements org.apache.commons.collections.Transformer {

    @Override
    @SuppressWarnings("unchecked")
    public R transform(Object input) {
        return tr((T) input);
    }

    public abstract R tr(T input);
}
=======
package ee.webmedia.alfresco.utils;

/**
 * Typesafe version of {@link org.apache.commons.collections.Transformer}, but as an abstract class(so that this is instanceof {@link org.apache.commons.collections.Transformer}
 * and to do the trick with generics).<br>
 * Instead of implementing unparameterized {@link org.apache.commons.collections.Transformer#transform(Object)} implement {@link #tr(T)}
 */
public abstract class Transformer<T, R> implements org.apache.commons.collections.Transformer {

    @Override
    @SuppressWarnings("unchecked")
    public R transform(Object input) {
        return tr((T) input);
    }

    public abstract R tr(T input);
}
>>>>>>> develop-5.1
