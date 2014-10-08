package ee.webmedia.alfresco.common.service;

import java.io.Serializable;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.util.EqualsHelper;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.web.context.request.RequestContextHolder;

import ee.webmedia.alfresco.common.model.Cacheable;

public class RepeatingServiceCallInterceptor implements MethodInterceptor, Serializable {
    private static final long serialVersionUID = 1L;

    private RequestCacheBean requestCache;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        boolean isRequest = RequestContextHolder.getRequestAttributes() != null;
        if (!isRequest) {
            return invocation.proceed();
        }
        Object result = null;
        String cacheKey = null;
        boolean resultFromCache = false;
        boolean isCacheable = false;
        try {
            isCacheable = invocation.getMethod().getAnnotation(Cacheable.class) != null;
            if (isCacheable) {
                String runAsUser = AuthenticationUtil.getRunAsUser();
                if (!EqualsHelper.nullSafeEquals(requestCache.getUserName(), runAsUser)) {
                    requestCache.clear();
                    requestCache.setUserName(runAsUser);
                }
                cacheKey = invocation.getThis().getClass().getName() + "." + invocation.getMethod().getName();
                result = requestCache.getResult(cacheKey);
                if (result != null) {
                    resultFromCache = true;
                    return result;
                }
            }
            result = invocation.proceed();
            return result;
        } finally {
            if (isCacheable && !resultFromCache && result != null) {
                requestCache.setResult(cacheKey, result);
            }
        }
    }

    public void setRequestCache(RequestCacheBean requestCache) {
        this.requestCache = requestCache;
    }

}
