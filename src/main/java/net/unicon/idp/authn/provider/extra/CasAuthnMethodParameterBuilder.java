package net.unicon.idp.authn.provider.extra;

import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

/**
 * Generates a querystring parameter containing the authn_method parameter.
 *
 * @author Misagh Moayyed
 */
public abstract class CasAuthnMethodParameterBuilder implements IParameterBuilder, ApplicationContextAware {
    private final Logger logger = LoggerFactory.getLogger(CasAuthnMethodParameterBuilder.class);
    protected ApplicationContext applicationContext;
    private static final String REFEDS = "https://refeds.org/profile/mfa";

    @Override
    public String getParameterString(final HttpServletRequest request, final String authenticationKey) {
        try {
            final ProfileRequestContext prc = ExternalAuthentication.getProfileRequestContext(authenticationKey, request);
            final AuthenticationContext authnContext = prc.getSubcontext(AuthenticationContext.class, true);
            if (authnContext == null) {
                logger.debug("No authentication context is available");
                return "";
            }
            final RequestedPrincipalContext principalCtx = authnContext.getSubcontext(RequestedPrincipalContext.class, true);
            if (principalCtx == null || principalCtx.getRequestedPrincipals().isEmpty()) {
                logger.debug("No authentication method parameter is found in the request attributes");
                return "";
            }
            final Principal principal = new AuthnContextClassRefPrincipal(REFEDS);
            final Principal attribute = principalCtx.getRequestedPrincipals().stream().filter(p -> p.equals(principal)).findFirst().orElse(null);
            if (attribute == null) {
                return "";
            }
            final String casMethod = getCasAuthenticationMethodFor(REFEDS);
            if (casMethod != null && !casMethod.isEmpty()) {
                return "&authn_method=" + casMethod;
            }
            return "";
        }catch (final Exception e) {
            logger.error(e.getMessage(), e);
            return "";
        }
    }

    protected abstract String getCasAuthenticationMethodFor(final String authnMethod);

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof CasAuthnMethodParameterBuilder;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static boolean isMultifactorRefedsProfile(final String authnMethod) {
        return REFEDS.equals(authnMethod);
    }
}
