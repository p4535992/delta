package ee.webmedia.alfresco.common.service;

import java.util.ArrayList;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchParameters;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MethodInvocationLoggingInterceptor implements MethodInterceptor {

    private static Log log = LogFactory.getLog(MethodInvocationLoggingInterceptor.class);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
                StoreRef store = null;
                Object[] args = invocation.getArguments();
                if (args.length >= 1 && args[0] != null) {
                    if (args[0] instanceof NodeRef) {
                        store = ((NodeRef) args[0]).getStoreRef();
                    } else if (args[0] instanceof ChildAssociationRef) {
                        NodeRef parent = ((ChildAssociationRef) args[0]).getParentRef();
                        if (parent != null) {
                            store = parent.getStoreRef();
                        }
                    } else if (args[0] instanceof StoreRef) {
                        store = ((StoreRef) args[0]);
                    } else if (args[0] instanceof SearchParameters) {
                        ArrayList<StoreRef> stores = ((SearchParameters) args[0]).getStores();
                        if (stores != null) {
                            store = stores.get(0);
                        }
                    }
                }
                String language = "";
                if (invocation.getMethod().getName().equals("query")) {
                    if (args.length >= 2 && args[1] != null && args[1] instanceof String) {
                        language = " " + args[1];
                    } else if (args.length >= 1 && args[0] != null && args[0] instanceof SearchParameters) {
                        language = " " + ((SearchParameters) args[0]).getLanguage();
                    }
                }
                log.debug("Method " + invocation.getThis().getClass().getSimpleName() + "." + invocation.getMethod().getName()
                        + " " + duration + " ms" + (store == null ? "" : " " + store.toString()) + language);
            }
        }
    }

}