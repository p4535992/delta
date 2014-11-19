package ee.webmedia.alfresco.user.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter;

/**
 * TicketValidationFilter that breaks redirection loop after unsucessful validation because of ticket validity timeout.
 */
public class Cas20ProxyReceivingRedirectingTicketValidationFilter extends Cas20ProxyReceivingTicketValidationFilter {
    
    public static final String CAS_VALIDATION_ERROR = "CAS_VALIDATION_ERROR";

    @Override
    protected void onSuccessfulValidation(HttpServletRequest request, HttpServletResponse response, Assertion assertion) {
        // After successful validation, we should redirect users to their requested page
        setRedirectAfterValidation(true);
    }

    @Override
    protected void onFailedValidation(HttpServletRequest request, HttpServletResponse response) {
        // Skip redirect and exception propagation and let folowing filters handle this situation.
        setExceptionOnValidationFailure(false);
        setRedirectAfterValidation(false);

        request.getSession().setAttribute(CAS_VALIDATION_ERROR, Boolean.TRUE.toString());
        // Notify AuthenticationFilter about this failure.
        request.getSession().setAttribute(SimpleAuthenticationFilter.AUTHENTICATION_EXCEPTION, Boolean.TRUE.toString());
    }

}