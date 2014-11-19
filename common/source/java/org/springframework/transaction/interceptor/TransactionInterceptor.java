<<<<<<< HEAD
/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.interceptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Properties;

import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.RetryingTransactionManager;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;

import ee.webmedia.alfresco.common.transaction.TransactionHelperWrapperException;

/**
 * AOP Alliance MethodInterceptor for declarative transaction
 * management using the common Spring transaction infrastructure
 * ({@link org.springframework.transaction.PlatformTransactionManager}).
 *
 * <p>Derives from the {@link TransactionAspectSupport} class which
 * contains the integration with Spring's underlying transaction API.
 * TransactionInterceptor simply calls the relevant superclass methods
 * such as {@link #createTransactionIfNecessary} in the correct order.
 *
 * <p>TransactionInterceptors are thread-safe.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see TransactionProxyFactoryBean
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see org.springframework.aop.framework.ProxyFactory
 */
public class TransactionInterceptor extends TransactionAspectSupport implements MethodInterceptor, Serializable {

    private static final Log LOG = LogFactory.getLog(TransactionInterceptor.class);

    /**
     * Create a new TransactionInterceptor.
     * <p>Transaction manager and transaction attributes still need to be set.
     * @see #setTransactionManager
     * @see #setTransactionAttributes(java.util.Properties)
     * @see #setTransactionAttributeSource(TransactionAttributeSource)
     */
    public TransactionInterceptor() {
    }

    /**
     * Create a new TransactionInterceptor.
     * @param ptm the transaction manager to perform the actual transaction management
     * @param attributes the transaction attributes in properties format
     * @see #setTransactionManager
     * @see #setTransactionAttributes(java.util.Properties)
     */
    public TransactionInterceptor(PlatformTransactionManager ptm, Properties attributes) {
        setTransactionManager(ptm);
        setTransactionAttributes(attributes);
    }

    /**
     * Create a new TransactionInterceptor.
     * @param ptm the transaction manager to perform the actual transaction management
     * @param tas the attribute source to be used to find transaction attributes
     * @see #setTransactionManager
     * @see #setTransactionAttributeSource(TransactionAttributeSource)
     */
    public TransactionInterceptor(PlatformTransactionManager ptm, TransactionAttributeSource tas) {
        setTransactionManager(ptm);
        setTransactionAttributeSource(tas);
    }


    public Object invoke(final MethodInvocation invocation) throws Throwable {
        // Work out the target class: may be <code>null</code>.
        // The TransactionAttributeSource should be passed the target class
        // as well as the method, which may be from an interface.
        Class targetClass = (invocation.getThis() != null ? invocation.getThis().getClass() : null);

        // If the transaction attribute is null, the method is non-transactional.
        final TransactionAttribute txAttr =
                getTransactionAttributeSource().getTransactionAttribute(invocation.getMethod(), targetClass);
        final String joinpointIdentification = methodIdentification(invocation.getMethod());

        if (txAttr == null || !(getTransactionManager() instanceof CallbackPreferringPlatformTransactionManager)) {
            // 1) If we have active transaction, let TransactionInterceptor handle transaction commit/rollback (txInfo != null)
            //    in completeTransactionAfterThrowing, cleanupTransactionInfo, commitTransactionAfterReturning. As transaction is not
            //    created by current call, only marking rollback is actually executed. TransactionHelper is not used for processing transaction
            //    because it doesn't mark transaction for rollback in case of runtime exception.
            // 2) If new transaction is needed (no existing transaction), RetryingTransactionManager creates the transaction to be able to retry method invocation
            //    and performs needed commit/rollback operations on transaction:
            //    txInfo == null, calls to completeTransactionAfterThrowing, cleanupTransactionInfo, commitTransactionAfterReturning do nothing;            
            TransactionInfo txInfo = null;
            if (AlfrescoTransactionSupport.getTransactionReadState() != TxnReadState.TXN_NONE) {
                // Standard transaction demarcation with getTransaction and commit/rollback calls.
                txInfo = createTransactionIfNecessary(txAttr, joinpointIdentification);
            }
            boolean canDelegate = getTransactionManager() instanceof RetryingTransactionManager;
            Object retVal = null;
            try {
                // This is an around advice: Invoke the next interceptor in the chain.
                // This will normally result in a target object being invoked.
                if (canDelegate) {
                    boolean isReadOnly = (txInfo != null && txInfo.getTransactionAttribute() != null && txInfo.getTransactionAttribute().isReadOnly())
                            || (txAttr != null && txAttr.isReadOnly())
                            || TxnReadState.TXN_READ_ONLY.equals(AlfrescoTransactionSupport.getTransactionReadState());
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Delegating transaction handling to retryingTransactionHelper");
                    }
                    retVal = ((RetryingTransactionManager) getTransactionManager()).getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>() {
                        @Override
                        public Object execute() throws Throwable {
                            return invocation.proceed();
                        }
                    }, isReadOnly, false, true);
                } else {
                    retVal = invocation.proceed();
                }
            } catch (Throwable ex) {
                // target invocation exception
                if (ex instanceof TransactionHelperWrapperException){
                    ex = ex.getCause();
                }
                completeTransactionAfterThrowing(txInfo, ex);
                throw ex;
            }
            finally {
                cleanupTransactionInfo(txInfo);
            }
            commitTransactionAfterReturning(txInfo);
            return retVal;
        }

        else {
            // It's a CallbackPreferringPlatformTransactionManager: pass a TransactionCallback in.
            try {
                Object result = ((CallbackPreferringPlatformTransactionManager) getTransactionManager()).execute(txAttr,
                        new TransactionCallback() {
                            public Object doInTransaction(TransactionStatus status) {
                                TransactionInfo txInfo = prepareTransactionInfo(txAttr, joinpointIdentification, status);
                                try {
                                    return invocation.proceed();
                                }
                                catch (Throwable ex) {
                                    if (txAttr.rollbackOn(ex)) {
                                        // A RuntimeException: will lead to a rollback.
                                        if (ex instanceof RuntimeException) {
                                            throw (RuntimeException) ex;
                                        }
                                        else {
                                            throw new ThrowableHolderException(ex);
                                        }
                                    }
                                    else {
                                        // A normal return value: will lead to a commit.
                                        return new ThrowableHolder(ex);
                                    }
                                }
                                finally {
                                    cleanupTransactionInfo(txInfo);
                                }
                            }
                        });

                // Check result: It might indicate a Throwable to rethrow.
                if (result instanceof ThrowableHolder) {
                    throw ((ThrowableHolder) result).getThrowable();
                }
                else {
                    return result;
                }
            }
            catch (ThrowableHolderException ex) {
                throw ex.getCause();
            }
        }
    }


    //---------------------------------------------------------------------
    // Serialization support
    //---------------------------------------------------------------------

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Rely on default serialization, although this class itself doesn't carry state anyway...
        ois.defaultReadObject();

        // Serialize all relevant superclass fields.
        // Superclass can't implement Serializable because it also serves as base class
        // for AspectJ aspects (which are not allowed to implement Serializable)!
        setTransactionManager((PlatformTransactionManager) ois.readObject());
        setTransactionAttributeSource((TransactionAttributeSource) ois.readObject());
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        // Rely on default serialization, although this class itself doesn't carry state anyway...
        oos.defaultWriteObject();

        // Deserialize superclass fields.
        oos.writeObject(getTransactionManager());
        oos.writeObject(getTransactionAttributeSource());
    }


    /**
     * Internal holder class for a Throwable, used as a return value
     * from a TransactionCallback (to be subsequently unwrapped again).
     */
    private static class ThrowableHolder {

        private final Throwable throwable;

        public ThrowableHolder(Throwable throwable) {
            this.throwable = throwable;
        }

        public final Throwable getThrowable() {
            return this.throwable;
        }
    }


    /**
     * Internal holder class for a Throwable, used as a RuntimeException to be
     * thrown from a TransactionCallback (and subsequently unwrapped again).
     */
    private static class ThrowableHolderException extends RuntimeException {

        private final Throwable throwable;

        public ThrowableHolderException(Throwable throwable) {
            super(throwable.toString());
            this.throwable = throwable;
        }

        public final Throwable getCause() {
            return this.throwable;
        }

        public String toString() {
            return this.throwable.toString();
        }
    }

}

=======
/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.interceptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Properties;

import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.RetryingTransactionManager;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;

import ee.webmedia.alfresco.common.transaction.TransactionHelperWrapperException;

/**
 * AOP Alliance MethodInterceptor for declarative transaction
 * management using the common Spring transaction infrastructure
 * ({@link org.springframework.transaction.PlatformTransactionManager}).
 *
 * <p>Derives from the {@link TransactionAspectSupport} class which
 * contains the integration with Spring's underlying transaction API.
 * TransactionInterceptor simply calls the relevant superclass methods
 * such as {@link #createTransactionIfNecessary} in the correct order.
 *
 * <p>TransactionInterceptors are thread-safe.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see TransactionProxyFactoryBean
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see org.springframework.aop.framework.ProxyFactory
 */
public class TransactionInterceptor extends TransactionAspectSupport implements MethodInterceptor, Serializable {

    private static final Log LOG = LogFactory.getLog(TransactionInterceptor.class);

    /**
     * Create a new TransactionInterceptor.
     * <p>Transaction manager and transaction attributes still need to be set.
     * @see #setTransactionManager
     * @see #setTransactionAttributes(java.util.Properties)
     * @see #setTransactionAttributeSource(TransactionAttributeSource)
     */
    public TransactionInterceptor() {
    }

    /**
     * Create a new TransactionInterceptor.
     * @param ptm the transaction manager to perform the actual transaction management
     * @param attributes the transaction attributes in properties format
     * @see #setTransactionManager
     * @see #setTransactionAttributes(java.util.Properties)
     */
    public TransactionInterceptor(PlatformTransactionManager ptm, Properties attributes) {
        setTransactionManager(ptm);
        setTransactionAttributes(attributes);
    }

    /**
     * Create a new TransactionInterceptor.
     * @param ptm the transaction manager to perform the actual transaction management
     * @param tas the attribute source to be used to find transaction attributes
     * @see #setTransactionManager
     * @see #setTransactionAttributeSource(TransactionAttributeSource)
     */
    public TransactionInterceptor(PlatformTransactionManager ptm, TransactionAttributeSource tas) {
        setTransactionManager(ptm);
        setTransactionAttributeSource(tas);
    }


    public Object invoke(final MethodInvocation invocation) throws Throwable {
        // Work out the target class: may be <code>null</code>.
        // The TransactionAttributeSource should be passed the target class
        // as well as the method, which may be from an interface.
        Class targetClass = (invocation.getThis() != null ? invocation.getThis().getClass() : null);

        // If the transaction attribute is null, the method is non-transactional.
        final TransactionAttribute txAttr =
                getTransactionAttributeSource().getTransactionAttribute(invocation.getMethod(), targetClass);
        final String joinpointIdentification = methodIdentification(invocation.getMethod());

        if (txAttr == null || !(getTransactionManager() instanceof CallbackPreferringPlatformTransactionManager)) {
            // 1) If we have active transaction, let TransactionInterceptor handle transaction commit/rollback (txInfo != null)
            //    in completeTransactionAfterThrowing, cleanupTransactionInfo, commitTransactionAfterReturning. As transaction is not
            //    created by current call, only marking rollback is actually executed. TransactionHelper is not used for processing transaction
            //    because it doesn't mark transaction for rollback in case of runtime exception.
            // 2) If new transaction is needed (no existing transaction), RetryingTransactionManager creates the transaction to be able to retry method invocation
            //    and performs needed commit/rollback operations on transaction:
            //    txInfo == null, calls to completeTransactionAfterThrowing, cleanupTransactionInfo, commitTransactionAfterReturning do nothing;            
            TransactionInfo txInfo = null;
            if (AlfrescoTransactionSupport.getTransactionReadState() != TxnReadState.TXN_NONE) {
                // Standard transaction demarcation with getTransaction and commit/rollback calls.
                txInfo = createTransactionIfNecessary(txAttr, joinpointIdentification);
            }
            boolean canDelegate = getTransactionManager() instanceof RetryingTransactionManager;
            Object retVal = null;
            try {
                // This is an around advice: Invoke the next interceptor in the chain.
                // This will normally result in a target object being invoked.
                if (canDelegate) {
                    boolean isReadOnly = (txInfo != null && txInfo.getTransactionAttribute() != null && txInfo.getTransactionAttribute().isReadOnly())
                            || (txAttr != null && txAttr.isReadOnly())
                            || TxnReadState.TXN_READ_ONLY.equals(AlfrescoTransactionSupport.getTransactionReadState());
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Delegating transaction handling to retryingTransactionHelper");
                    }
                    retVal = ((RetryingTransactionManager) getTransactionManager()).getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>() {
                        @Override
                        public Object execute() throws Throwable {
                            return invocation.proceed();
                        }
                    }, isReadOnly, false, true);
                } else {
                    retVal = invocation.proceed();
                }
            } catch (Throwable ex) {
                // target invocation exception
                if (ex instanceof TransactionHelperWrapperException){
                    ex = ex.getCause();
                }
                completeTransactionAfterThrowing(txInfo, ex);
                throw ex;
            }
            finally {
                cleanupTransactionInfo(txInfo);
            }
            commitTransactionAfterReturning(txInfo);
            return retVal;
        }

        else {
            // It's a CallbackPreferringPlatformTransactionManager: pass a TransactionCallback in.
            try {
                Object result = ((CallbackPreferringPlatformTransactionManager) getTransactionManager()).execute(txAttr,
                        new TransactionCallback() {
                            public Object doInTransaction(TransactionStatus status) {
                                TransactionInfo txInfo = prepareTransactionInfo(txAttr, joinpointIdentification, status);
                                try {
                                    return invocation.proceed();
                                }
                                catch (Throwable ex) {
                                    if (txAttr.rollbackOn(ex)) {
                                        // A RuntimeException: will lead to a rollback.
                                        if (ex instanceof RuntimeException) {
                                            throw (RuntimeException) ex;
                                        }
                                        else {
                                            throw new ThrowableHolderException(ex);
                                        }
                                    }
                                    else {
                                        // A normal return value: will lead to a commit.
                                        return new ThrowableHolder(ex);
                                    }
                                }
                                finally {
                                    cleanupTransactionInfo(txInfo);
                                }
                            }
                        });

                // Check result: It might indicate a Throwable to rethrow.
                if (result instanceof ThrowableHolder) {
                    throw ((ThrowableHolder) result).getThrowable();
                }
                else {
                    return result;
                }
            }
            catch (ThrowableHolderException ex) {
                throw ex.getCause();
            }
        }
    }


    //---------------------------------------------------------------------
    // Serialization support
    //---------------------------------------------------------------------

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Rely on default serialization, although this class itself doesn't carry state anyway...
        ois.defaultReadObject();

        // Serialize all relevant superclass fields.
        // Superclass can't implement Serializable because it also serves as base class
        // for AspectJ aspects (which are not allowed to implement Serializable)!
        setTransactionManager((PlatformTransactionManager) ois.readObject());
        setTransactionAttributeSource((TransactionAttributeSource) ois.readObject());
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        // Rely on default serialization, although this class itself doesn't carry state anyway...
        oos.defaultWriteObject();

        // Deserialize superclass fields.
        oos.writeObject(getTransactionManager());
        oos.writeObject(getTransactionAttributeSource());
    }


    /**
     * Internal holder class for a Throwable, used as a return value
     * from a TransactionCallback (to be subsequently unwrapped again).
     */
    private static class ThrowableHolder {

        private final Throwable throwable;

        public ThrowableHolder(Throwable throwable) {
            this.throwable = throwable;
        }

        public final Throwable getThrowable() {
            return this.throwable;
        }
    }


    /**
     * Internal holder class for a Throwable, used as a RuntimeException to be
     * thrown from a TransactionCallback (and subsequently unwrapped again).
     */
    private static class ThrowableHolderException extends RuntimeException {

        private final Throwable throwable;

        public ThrowableHolderException(Throwable throwable) {
            super(throwable.toString());
            this.throwable = throwable;
        }

        public final Throwable getCause() {
            return this.throwable;
        }

        public String toString() {
            return this.throwable.toString();
        }
    }

}

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
