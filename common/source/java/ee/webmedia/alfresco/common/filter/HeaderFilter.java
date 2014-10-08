<<<<<<< HEAD
package ee.webmedia.alfresco.common.filter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

/**
 * HeaderFilter to set configured response headers.
 */
public class HeaderFilter implements Filter {

    private FilterConfig filterConfig;
    private TimeZone timeZone;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        timeZone = TimeZone.getTimeZone("UTC");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        Enumeration<String> enu = filterConfig.getInitParameterNames();
        while (enu.hasMoreElements()) {
            String name = enu.nextElement();
            String value = filterConfig.getInitParameter(name);
            boolean addHeader = true;

            if (name.equalsIgnoreCase("Expires") && StringUtils.isNumeric(value)) {
                int seconds = Integer.parseInt(value);
                Calendar cal = new GregorianCalendar();
                cal.add(Calendar.SECOND, seconds);
                DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                dateFormat.setTimeZone(timeZone);
                value = dateFormat.format(cal.getTime());
            } else if (name.equalsIgnoreCase("Cache-Control")) {
                // This is a workaround for a problem with old Apache proxies (v1.2 for example).
                // When setting both cookies and cache headers, it causes incorrect behavioud in those old proxies.
                if (session == null || session.isNew()) {
                    addHeader = false;
                }
            }

            if (addHeader) {
                httpResponse.addHeader(name, value);
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to do
    }

=======
package ee.webmedia.alfresco.common.filter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

/**
 * HeaderFilter to set configured response headers.
 */
public class HeaderFilter implements Filter {

    private FilterConfig filterConfig;
    private TimeZone timeZone;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        timeZone = TimeZone.getTimeZone("UTC");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        Enumeration<String> enu = filterConfig.getInitParameterNames();
        while (enu.hasMoreElements()) {
            String name = enu.nextElement();
            String value = filterConfig.getInitParameter(name);
            boolean addHeader = true;

            if (name.equalsIgnoreCase("Expires") && StringUtils.isNumeric(value)) {
                int seconds = Integer.parseInt(value);
                Calendar cal = new GregorianCalendar();
                cal.add(Calendar.SECOND, seconds);
                DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                dateFormat.setTimeZone(timeZone);
                value = dateFormat.format(cal.getTime());
            } else if (name.equalsIgnoreCase("Cache-Control")) {
                // This is a workaround for a problem with old Apache proxies (v1.2 for example).
                // When setting both cookies and cache headers, it causes incorrect behavioud in those old proxies.
                if (session == null || session.isNew()) {
                    addHeader = false;
                }
            }

            if (addHeader) {
                httpResponse.addHeader(name, value);
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to do
    }

>>>>>>> develop-5.1
}