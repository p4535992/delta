package ee.webmedia.alfresco.utils;


/**
 * Type safe version of {@link org.apache.commons.collections.Closure}, but as an abstract class(so that this is instanceof {@link org.apache.commons.collections.Closure} and does
 * the trick with generics).<br>
 * Instead of implementing unparameterized {@link org.apache.commons.collections.Closure#execute(Object)} implement {@link #exec(T)}
 */
public abstract class Closure<T> implements org.apache.commons.collections.Closure {
    @Override
    @SuppressWarnings("unchecked")
    public void execute(Object input) {
        exec((T) input);

    }

    public abstract void exec(T input);
}
