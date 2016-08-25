package net.unicon.idp.externalauth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.shibboleth.idp.authn.ExternalAuthentication;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jasig.cas.client.validation.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple translation of the principal name from the CAS assertion to the string value used by Shib
 * @author chasegawa@unicon.net
 * @author jgasper@unicon.net
 */
public class AuthenticatedNameTranslator implements CasToShibTranslator {
    private Logger logger = LoggerFactory.getLogger(ShibcasAuthServlet.class);

    @Override
    public void doTranslation(final HttpServletRequest request, final HttpServletResponse response,
            final Assertion assertion) {
        if (assertion == null || assertion.getPrincipal() == null) {
            logger.error("No valida assertion or principal could be found to translate");
            return;
        }
        
        String authenticatedPrincipalName = assertion.getPrincipal().getName(); // i.e. username from CAS
        logger.debug("principalName found and being passed on: {}", authenticatedPrincipalName);

        // Pass authenticated principal back to IdP to finish its part of authentication request processing
        request.setAttribute(ExternalAuthentication.PRINCIPAL_NAME_KEY, authenticatedPrincipalName);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(final Object that) {
        return EqualsBuilder.reflectionEquals(this, that);
    }
}
