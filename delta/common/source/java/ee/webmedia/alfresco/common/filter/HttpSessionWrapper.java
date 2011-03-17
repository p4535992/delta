package ee.webmedia.alfresco.common.filter;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

@SuppressWarnings("deprecation")
public class HttpSessionWrapper implements HttpSession {

    private HttpSession sess;

    public HttpSessionWrapper(HttpSession sess) {
        this.sess = sess;
    }

    @Override
    public Object getAttribute(String s) {
        return sess.getAttribute(s);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Enumeration getAttributeNames() {
        return sess.getAttributeNames();
    }

    @Override
    public long getCreationTime() {
        return sess.getCreationTime();
    }

    @Override
    public String getId() {
        return sess.getId();
    }

    @Override
    public long getLastAccessedTime() {
        return sess.getCreationTime();
    }

    @Override
    public int getMaxInactiveInterval() {
        return sess.getMaxInactiveInterval();
    }

    @Override
    public ServletContext getServletContext() {
        return sess.getServletContext();
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return sess.getSessionContext();
    }

    @Override
    public Object getValue(String s) {
        return sess.getValue(s);
    }

    @Override
    public String[] getValueNames() {
        return sess.getValueNames();
    }

    @Override
    public void invalidate() {
        sess.invalidate();
    }

    @Override
    public boolean isNew() {
        return sess.isNew();
    }

    @Override
    public void putValue(String s, Object obj) {
        sess.putValue(s, obj);
    }

    @Override
    public void removeAttribute(String s) {
        sess.removeAttribute(s);
    }

    @Override
    public void removeValue(String s) {
        sess.removeValue(s);
    }

    @Override
    public void setAttribute(String s, Object obj) {
        sess.setAttribute(s, obj);
    }

    @Override
    public void setMaxInactiveInterval(int i) {
        sess.setMaxInactiveInterval(i);
    }

}
