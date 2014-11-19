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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.Assert;

/**
 * General utility methods for working with annotations, handling bridge methods
 * (which the compiler generates for generic declarations) as well as super
 * methods (for optional "annotation inheritance"). Note that none of this is
 * provided by the JDK's introspection facilities themselves.
 *
 * <p>As a general rule for runtime-retained annotations (e.g. for transaction
 * control, authorization or service exposure), always use the lookup methods
 * on this class instead of the plain annotation lookup methods in the JDK.
 * You can still explicitly choose between lookup on the given class level only
 * ({@link #getAnnotation}) and lookup in the entire inheritance hierarchy of
 * the given method ({@link #findAnnotation}).
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see java.lang.reflect.Method#getAnnotations()
 * @see java.lang.reflect.Method#getAnnotation(Class)
 */
public abstract class AnnotationUtils {

	/**
	 * Get all {@link Annotation Annotations} from the supplied {@link Method}.
	 * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
	 * @param method the method to look for annotations on
	 * @return the annotations found
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 */
	public static Annotation[] getAnnotations(Method method) {
		return BridgeMethodResolver.findBridgedMethod(method).getAnnotations();
	}

	/**
	 * Get a single {@link Annotation} of <code>annotationType</code> from the
	 * supplied {@link Method}.
	 * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
	 * @param method the method to look for annotations on
	 * @param annotationType the annotation class to look for
	 * @return the annotations found
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 */
	public static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationType) {
		return BridgeMethodResolver.findBridgedMethod(method).getAnnotation(annotationType);
	}

	/**
	 * Get a single {@link Annotation} of <code>annotationType</code> from the
	 * supplied {@link Method}, traversing its super methods if no annotation
	 * can be found on the given method.
	 * <p>Annotations on methods are not inherited by default, so we need to
	 * handle this explicitly.
	 * @param method the method to look for annotations on
	 * @param annotationType the annotation class to look for
	 * @return the annotation of the given type found, or <code>null</code>
	 */
	public static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
		if (!annotationType.isAnnotation()) {
			throw new IllegalArgumentException(annotationType + " is not an annotation");
		}
		A annotation = getAnnotation(method, annotationType);
		Class cl = method.getDeclaringClass();
		while (annotation == null) {
			cl = cl.getSuperclass();
			if (cl == null || cl.equals(Object.class)) {
				break;
			}
			try {
				method = cl.getDeclaredMethod(method.getName(), method.getParameterTypes());
				annotation = getAnnotation(method, annotationType);
			}
			catch (NoSuchMethodException ex) {
				// We're done...
			}
		}
		return annotation;
	}

	/**
     * Find a single {@link Annotation} of <code>annotationType</code> from the
     * supplied {@link Class}, traversing its interfaces and super classes
     * if no annotation can be found on the given class itself.
     * <p>This method explicitly handles class-level annotations which are
     * not declared as {@link java.lang.annotation.Inherited inherited}
     * <i>as well as annotations on interfaces</i>.
     * <p>The algorithm operates as follows: Searches for an annotation on the given
     * class and returns it if found. Else searches all interfaces that the given
     * class declares, returning the annotation from the first matching candidate,
     * if any. Else proceeds with introspection of the superclass of the given class,
     * checking the superclass itself; if no annotation found there, proceeds with
     * the interfaces that the superclass declares. Recursing up through the entire
     * superclass hierarchy if no match is found.
     * @param clazz the class to look for annotations on
     * @param annotationType the annotation class to look for
     * @return the annotation found, or <code>null</code> if none found
     */
    public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
        Assert.notNull(clazz, "Class must not be null");
        A annotation = clazz.getAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }
        for (Class<?> ifc : clazz.getInterfaces()) {
            annotation = findAnnotation(ifc, annotationType);
            if (annotation != null) {
                return annotation;
            }
        }
        Class superClass = clazz.getSuperclass();
        if (superClass == null || superClass == Object.class) {
            return null;
        }
        return findAnnotation(superClass, annotationType);
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.Assert;

/**
 * General utility methods for working with annotations, handling bridge methods
 * (which the compiler generates for generic declarations) as well as super
 * methods (for optional "annotation inheritance"). Note that none of this is
 * provided by the JDK's introspection facilities themselves.
 *
 * <p>As a general rule for runtime-retained annotations (e.g. for transaction
 * control, authorization or service exposure), always use the lookup methods
 * on this class instead of the plain annotation lookup methods in the JDK.
 * You can still explicitly choose between lookup on the given class level only
 * ({@link #getAnnotation}) and lookup in the entire inheritance hierarchy of
 * the given method ({@link #findAnnotation}).
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see java.lang.reflect.Method#getAnnotations()
 * @see java.lang.reflect.Method#getAnnotation(Class)
 */
public abstract class AnnotationUtils {

	/**
	 * Get all {@link Annotation Annotations} from the supplied {@link Method}.
	 * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
	 * @param method the method to look for annotations on
	 * @return the annotations found
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 */
	public static Annotation[] getAnnotations(Method method) {
		return BridgeMethodResolver.findBridgedMethod(method).getAnnotations();
	}

	/**
	 * Get a single {@link Annotation} of <code>annotationType</code> from the
	 * supplied {@link Method}.
	 * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
	 * @param method the method to look for annotations on
	 * @param annotationType the annotation class to look for
	 * @return the annotations found
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 */
	public static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationType) {
		return BridgeMethodResolver.findBridgedMethod(method).getAnnotation(annotationType);
	}

	/**
	 * Get a single {@link Annotation} of <code>annotationType</code> from the
	 * supplied {@link Method}, traversing its super methods if no annotation
	 * can be found on the given method.
	 * <p>Annotations on methods are not inherited by default, so we need to
	 * handle this explicitly.
	 * @param method the method to look for annotations on
	 * @param annotationType the annotation class to look for
	 * @return the annotation of the given type found, or <code>null</code>
	 */
	public static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
		if (!annotationType.isAnnotation()) {
			throw new IllegalArgumentException(annotationType + " is not an annotation");
		}
		A annotation = getAnnotation(method, annotationType);
		Class cl = method.getDeclaringClass();
		while (annotation == null) {
			cl = cl.getSuperclass();
			if (cl == null || cl.equals(Object.class)) {
				break;
			}
			try {
				method = cl.getDeclaredMethod(method.getName(), method.getParameterTypes());
				annotation = getAnnotation(method, annotationType);
			}
			catch (NoSuchMethodException ex) {
				// We're done...
			}
		}
		return annotation;
	}

	/**
     * Find a single {@link Annotation} of <code>annotationType</code> from the
     * supplied {@link Class}, traversing its interfaces and super classes
     * if no annotation can be found on the given class itself.
     * <p>This method explicitly handles class-level annotations which are
     * not declared as {@link java.lang.annotation.Inherited inherited}
     * <i>as well as annotations on interfaces</i>.
     * <p>The algorithm operates as follows: Searches for an annotation on the given
     * class and returns it if found. Else searches all interfaces that the given
     * class declares, returning the annotation from the first matching candidate,
     * if any. Else proceeds with introspection of the superclass of the given class,
     * checking the superclass itself; if no annotation found there, proceeds with
     * the interfaces that the superclass declares. Recursing up through the entire
     * superclass hierarchy if no match is found.
     * @param clazz the class to look for annotations on
     * @param annotationType the annotation class to look for
     * @return the annotation found, or <code>null</code> if none found
     */
    public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
        Assert.notNull(clazz, "Class must not be null");
        A annotation = clazz.getAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }
        for (Class<?> ifc : clazz.getInterfaces()) {
            annotation = findAnnotation(ifc, annotationType);
            if (annotation != null) {
                return annotation;
            }
        }
        Class superClass = clazz.getSuperclass();
        if (superClass == null || superClass == Object.class) {
            return null;
        }
        return findAnnotation(superClass, annotationType);
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
