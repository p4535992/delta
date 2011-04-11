package ee.webmedia.alfresco.webdav;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.webdav.WebDAVMethod;
import org.alfresco.repo.webdav.WebDAVServerException;

public class ForbiddenMethod extends WebDAVMethod {

    @Override
    protected void parseRequestBody() throws WebDAVServerException {
        // do nothing
    }

    @Override
    protected void parseRequestHeaders() throws WebDAVServerException {
        // do nothing
    }

    @Override
    protected void executeImpl() throws WebDAVServerException, Exception {
        // not allowed
        throw new WebDAVServerException(HttpServletResponse.SC_FORBIDDEN);
    }

}
