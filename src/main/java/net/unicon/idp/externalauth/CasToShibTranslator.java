package net.unicon.idp.externalauth;

import org.jasig.cas.client.validation.Assertion;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This interface defines the public interface for a class that will translate the information from CAS to Shib. The translator
 * should only push details into the request and should NOT attempt to call
 * AuthenticationEngine.returnToAuthenticationEngine(request, response);
 * <p>
 * Instance of this type should implement hashcode and equals.
 *
 * @author chasegawa @unicon.net
 */
public interface CasToShibTranslator {
    /**
     * Do the needed translation.
     *
     * @param request           The HttpServletRequest object
     * @param response          The HttpServletResponse object
     * @param assertion         The CAS Assertion after validating the CAS ticket
     * @param authenticationKey the authentication key
     * @throws Exception the exception
     */
    void doTranslation(HttpServletRequest request, HttpServletResponse response, Assertion assertion, String authenticationKey) throws Exception;
}
