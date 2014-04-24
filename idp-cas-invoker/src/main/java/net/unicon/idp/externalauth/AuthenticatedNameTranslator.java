package net.unicon.idp.externalauth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.cas.client.validation.Assertion;

import edu.internet2.middleware.shibboleth.idp.authn.LoginHandler;

/**
 * Simple translation of the principal name from the CAS assertion to the string value used by Shib
 * @author chasegawa@unicon.net
 */
public class AuthenticatedNameTranslator implements ICasToShibTranslator {
    /**
     * @see net.unicon.idp.externalauth.ICasToShibTranslator#doTranslation(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.jasig.cas.client.validation.Assertion)
     */
    public void doTranslation(final HttpServletRequest request, final HttpServletResponse response,
            final Assertion assertion) {
        String authenticatedPrincipalName = assertion.getPrincipal().getName(); // i.e. username from CAS
        // Pass authenticated principal back to IdP to finish its part of authentication request processing
        request.setAttribute(LoginHandler.PRINCIPAL_NAME_KEY, authenticatedPrincipalName);
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof AuthenticatedNameTranslator;
    }
}
