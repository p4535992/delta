package ee.webmedia.alfresco.common.service;

/**
 * @author Ats Uiboupin
 */
public interface IClonable<T> extends java.lang.Cloneable {
    public T clone();
}
