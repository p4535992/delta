package org.hibernate.intercept;

import org.hibernate.engine.SessionImplementor;

import java.util.Set;

/**
 * Helper class for dealing with enhanced entity classes.
 *
 * @author Steve Ebersole
 */
public class FieldInterceptionHelper {

    // Alfresco doesn't use Hibernate instrumentation, so deleting all these expensive checks

	private FieldInterceptionHelper() {
	}

	public static boolean isInstrumented(Class entityClass) {
		return false;
	}

	public static boolean isInstrumented(Object entity) {
		return false;
	}

	public static FieldInterceptor extractFieldInterceptor(Object entity) {
		return null;
	}

	public static FieldInterceptor injectFieldInterceptor(
			Object entity,
	        String entityName,
	        Set uninitializedFieldNames,
	        SessionImplementor session) {
		return null;
	}

	public static void clearDirty(Object entity) {
	}

	public static void markDirty(Object entity) {
	}
}
