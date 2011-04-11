package ee.webmedia.alfresco.common.filter;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.app.servlet.FacesHelper;

import ee.webmedia.alfresco.substitute.model.SubstitutionInfo;
import ee.webmedia.alfresco.substitute.web.SubstitutionBean;

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

        FacesContext context = FacesHelper.getFacesContext(request, response, filterConfig.getServletContext());

        SubstitutionInfo substInfo = ((SubstitutionBean) FacesHelper.getManagedBean(context,
                SubstitutionBean.BEAN_NAME)).getSubstitutionInfo();

        if (substInfo.isSubstituting()) {
            AuthenticationUtil.setRunAsUser(substInfo.getSubstitution().getReplacedPersonUserName());
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to do
    }

}