package ee.webmedia.alfresco.common.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.alfresco.web.app.servlet.FacesHelper;

/**
 * Sets thread-local FacexContext.
 * @author Alar Kvell
 */
public class FacesContextFilter implements Filter {

    private FilterConfig filterConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // sets thread-local FacesContext
        FacesHelper.getFacesContext(request, response, filterConfig.getServletContext());
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to do
    }

}