package ee.webmedia.alfresco.maais;

import static ee.webmedia.alfresco.common.web.BeanHelper.getMaaisService;

import java.io.IOException;
import java.util.Date;

import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.app.servlet.BaseServlet;
import org.alfresco.web.app.servlet.FacesHelper;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.maais.service.MaaisService;

public class MaaisForwarderServlet extends BaseServlet {
    private static final long serialVersionUID = 0L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        MaaisService maaisService = getMaaisService();
        if (!maaisService.isServiceAvailable()) {
            return;
        }
        // setup the faces context
        final FacesContext facesContext = FacesHelper.getFacesContext(req, res, getServletContext());

        String username = BeanHelper.getUserService().getCurrentUserName();
        if (StringUtils.isBlank(username)) {
            res.getWriter().write("AccessDenied");
            return;
        }

        Date expiry = maaisService.getUserSessionExpiry(username);
        if (expiry != null && expiry.before(new Date())) {
            String url = maaisService.getUserUrl(username);
            facesContext.getExternalContext().redirect(url);
        } else {
            Date updateResult = maaisService.updateAuth(username);
            if (updateResult != null) {
                String url = maaisService.getUserUrl(username);
                facesContext.getExternalContext().redirect(url);
            } else {
                res.getWriter().write(maaisService.getMaaisName() + " ei ole hetkel kättesaadav.");
            }
        }

    }
}
