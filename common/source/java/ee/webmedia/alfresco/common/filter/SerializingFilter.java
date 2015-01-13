package ee.webmedia.alfresco.common.filter;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.alfresco.repo.web.filter.beans.DependencyInjectedFilter;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.lang.SerializationException;
import org.apache.commons.lang.SerializationUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;

/**
 * Serializes all session attributes. This filter helps to be aware of serializing issues during development. If the session does not serialize, exception is
 * thrown. In production configuration, this filter should never be enabled, thus it is disabled by default.
 */
public class SerializingFilter implements DependencyInjectedFilter, InitializingBean {

    static Logger log = Logger.getLogger(SerializingFilter.class);

    private boolean enabled = false;
    private final NumberFormat decimalFormat = new DecimalFormat();

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (enabled && log.isInfoEnabled()) {
            log.info("This filter should NOT be enabled in PRODUCTION environment, as it imposes a performance penalty!");
        }
    }

    @Override
    public void doFilter(ServletContext context, ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (!enabled) {
            chain.doFilter(req, res);
            return;
        }

        HttpServletRequestWrapper1 wrappedRequest = new HttpServletRequestWrapper1((HttpServletRequest) req);
        chain.doFilter(wrappedRequest, res);
        if (!wrappedRequest.hasSession()) {
            log.debug("Session was not accessed during request");
            return;
        }
        long startTime = System.currentTimeMillis();
        HttpSessionWrapper1 session = (HttpSessionWrapper1) wrappedRequest.getSession();
        try {
            byte[] serialized = SerializationUtils.serialize(session.getAttributes());
            SerializationUtils.deserialize(serialized);
            // FIXME Alfresco starts behaving very weird... two separate instances of NavigationBean are used in differenct places...
            // session.setAttributes((HashMap<String, Object>) SerializationUtils.deserialize(serialized));
            if (log.isDebugEnabled()) {
                log.debug("HTTP session id=" + session.getId() + " attributes=" + session.getAttributes().size() + " size="
                        + decimalFormat.format(serialized.length) + " bytes (serializing took " + (System.currentTimeMillis() - startTime) + " ms)");
            }
            StatisticsPhaseListener.add(StatisticsPhaseListenerLogColumn.SESSION_SIZE, Integer.toString(serialized.length / 1024) + " KiB");
        } catch (SerializationException ex) {
            StatisticsPhaseListener.add(StatisticsPhaseListenerLogColumn.SESSION_SIZE, "ERROR");
            log.error("session id=" + session.getId() + "\n" + ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            StatisticsPhaseListener.add(StatisticsPhaseListenerLogColumn.SESSION_SIZE, "ERROR");
            log.error("session id=" + session.getId(), ex);
        } finally {
            session.setAttributesInOriginal();
        }

    }

    public static class HttpServletRequestWrapper1 extends HttpServletRequestWrapper {

        private HttpSessionWrapper1 session;

        public HttpServletRequestWrapper1(HttpServletRequest request) {
            super(request);
        }

        public boolean hasSession() {
            return session != null;
        }

        @Override
        public HttpSession getSession() {
            HttpSession originalSession = super.getSession(false);
            if (originalSession == null) {
                session = null;
            } else if (session == null || !session.getId().equals(originalSession.getId())) {
                session = new HttpSessionWrapper1(originalSession);
            }
            return session;
        }

        @Override
        public HttpSession getSession(boolean create) {
            HttpSession originalSession = super.getSession(false);
            if (originalSession == null) {
                if (create) {
                    originalSession = super.getSession(create);
                    if (originalSession != null) {
                        session = new HttpSessionWrapper1(originalSession);
                    } else {
                        session = null;
                    }
                } else {
                    session = null;
                }
            } else if (session == null || !session.getId().equals(originalSession.getId())) {
                session = new HttpSessionWrapper1(originalSession);
            }
            return session;
        }

    }

    public static class HttpSessionWrapper1 extends HttpSessionWrapper {

        private static final String ATTRIBUTES_KEY = "ee.webmedia.alfresco.common.filter.HttpSessionWrapper.ATTRIBUTES";
        private HashMap<String, Object> attributes;

        @SuppressWarnings("unchecked")
        public HttpSessionWrapper1(HttpSession sess) {
            super(sess);
            attributes = (HashMap<String, Object>) sess.getAttribute(ATTRIBUTES_KEY);
            if (attributes == null) {
                attributes = new HashMap<String, Object>();
            }
        }

        public void setAttributesInOriginal() {
            super.setAttribute(ATTRIBUTES_KEY, attributes);
        }

        public HashMap<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttributes(HashMap<String, Object> attributes) {
            this.attributes = attributes;
        }

        // Attributes

        @Override
        public Object getAttribute(String s) {
            if (attributes.containsKey(s)) {
                return attributes.get(s);
            }
            return super.getAttribute(s);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Enumeration getAttributeNames() {
            Set<String> names = new HashSet(attributes.keySet());
            for (Enumeration e = super.getAttributeNames(); e.hasMoreElements();) {
                names.add((String) e.nextElement());
            }
            return new IteratorEnumeration(names.iterator());
        }

        @Override
        public void removeAttribute(String s) {
            attributes.remove(s);
            super.removeAttribute(s);
        }

        @Override
        public void setAttribute(String s, Object obj) {
            super.removeAttribute(s);
            attributes.put(s, obj);
        }

        // Values

        @Override
        public Object getValue(String s) {
            RuntimeException e = new RuntimeException();
            log.error("Method not supported", e);
            throw e;
            // return super.getValue(s);
        }

        @Override
        public String[] getValueNames() {
            RuntimeException e = new RuntimeException();
            log.error("Method not supported", e);
            throw e;
            // return super.getValueNames();
        }

        @Override
        public void removeValue(String s) {
            RuntimeException e = new RuntimeException();
            log.error("Method not supported", e);
            throw e;
            // super.removeValue(s);
        }

        @Override
        public void putValue(String s, Object obj) {
            RuntimeException e = new RuntimeException();
            log.error("Method not supported", e);
            throw e;
            // super.putValue(s, obj);
        }

    }

}
