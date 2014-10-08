<<<<<<< HEAD
package ee.webmedia.alfresco.common.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationUtil;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.substitute.model.SubstitutionInfo;

/**
 * SubstitutionFilter to set substitution info.
 * 
 * @author Riina Tens
 */
public class SubstitutionFilter implements Filter {

    private FilterConfig filterConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        SubstitutionInfo substInfo = BeanHelper.getSubstitutionBean().getSubstitutionInfo();

        if (substInfo.isSubstituting()) {
            AuthenticationUtil.setRunAsUser(substInfo.getSubstitution().getReplacedPersonUserName());
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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationUtil;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.substitute.model.SubstitutionInfo;

/**
 * SubstitutionFilter to set substitution info.
 */
public class SubstitutionFilter implements Filter {

    private FilterConfig filterConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        SubstitutionInfo substInfo = BeanHelper.getSubstitutionBean().getSubstitutionInfo();

        if (substInfo.isSubstituting()) {
            AuthenticationUtil.setRunAsUser(substInfo.getSubstitution().getReplacedPersonUserName());
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to do
    }

>>>>>>> develop-5.1
}