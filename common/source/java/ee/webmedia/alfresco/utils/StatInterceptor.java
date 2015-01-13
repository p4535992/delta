package ee.webmedia.alfresco.utils;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.Auditable;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Statistic interceptor. Used to log method invocation statistics.
 */
public class StatInterceptor implements MethodInterceptor {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(StatInterceptor.class);

    private static final String ANONYMOUS_USERNAME = "anonymous";

    private int thresholdMillis = 50;

    public int getThresholdMillis() {
        return thresholdMillis;
    }

    /**
     * Set <code>thresholdMillis</code> parameter, which defines minimum
     * execution time to stat. Methods with execution time smaller than this are ignored.
     * <P>
     * Default value is 50 milliseconds.
     * 
     * @param thresholdMillis execution time must be longer than this value to get logged out
     */
    public void setThresholdMillis(int thresholdMillis) {
        this.thresholdMillis = thresholdMillis;
    }

    @Override
    public Object invoke(MethodInvocation method) throws Throwable {
        if (LOG.isInfoEnabled()) {
            return proceedWithLogging(method);
        }
        return method.proceed();
    }

    private Object proceedWithLogging(MethodInvocation method) throws Throwable {
        long start = System.currentTimeMillis();
        boolean succeeded = false;
        try {
            Object result = method.proceed();
            succeeded = true;
            return result;
        } finally {
            STAT(method, start, succeeded); // NOPMD - method name: in the interest of noticing it from log
        }
    }

    private void STAT(MethodInvocation method, long start, boolean succeeded) { // NOPMD - method name: in the interest of noticing it from log
        long millis = System.currentTimeMillis() - start;
        boolean readOnly = isReadOnlyInvocation(method.getMethod().getName());
        if (millis >= thresholdMillis) {
            StringBuilder stat = new StringBuilder();
            if (!readOnly) {
                stat.append("[RW] ");
            }
            stat.append(getShortClass(method));
            stat.append(".").append(method.getMethod().getName());
            stat.append("(").append(getArgsString(method)).append(")");
            stat.append(" - ").append(millis).append("ms");
            stat.append(" [").append(getUsername()).append("]");
            if (!succeeded) {
                stat.append(" FAILED");
            }
            LOG.info(stat.toString());
        }
    }

    private static Object getArgsString(MethodInvocation method) {
        StringBuilder result = new StringBuilder();

        if (method.getMethod().isAnnotationPresent(Auditable.class)) {
            for (Object param : method.getArguments()) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                result.append(ObjectUtils.toString(param, "null"));
            }
        } else {
            for (Class<?> param : method.getMethod().getParameterTypes()) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                result.append(param.getSimpleName());
            }
        }

        return result.toString();
    }

    private static String getUsername() {
        String result = AuthenticationUtil.getRunAsUser();
        if (StringUtils.isBlank(result)) {
            result = ANONYMOUS_USERNAME;
        }
        return result;
    }

    private static Object getShortClass(MethodInvocation method) {
        String result = method.getThis().getClass().getName();
        int index = result.lastIndexOf(".", result.lastIndexOf(".") - 1);
        return result.substring(index + 1);
    }

    private static boolean isReadOnlyInvocation(String methodName) {
        // Should be in sync with bean "commonTransactionInterceptor" in "common-context.xml":
        return methodName.startsWith("get")
                || methodName.startsWith("load")
                || methodName.startsWith("exist")
                || methodName.startsWith("has")
                || methodName.startsWith("is")
                || methodName.startsWith("list")
                || methodName.startsWith("resolve")
                || methodName.startsWith("search");
    }
}
