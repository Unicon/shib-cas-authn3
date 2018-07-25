package net.unicon.idp.externalauth;

import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicate;
import net.shibboleth.idp.authn.principal.PrincipalEvalPredicateFactory;
import net.shibboleth.idp.authn.principal.PrincipalSupportingComponent;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CasDuoSecurityRefedsAuthnMethodTranslator implements CasToShibTranslator, EnvironmentAware {
    private final Logger logger = LoggerFactory.getLogger(CasDuoSecurityRefedsAuthnMethodTranslator.class);

    private static final String REFEDS = "https://refeds.org/profile/mfa";

    private Environment environment;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    @Override
    public void doTranslation(final HttpServletRequest request, final HttpServletResponse response, final Assertion assertion, final String authenticationKey) throws Exception {

        final ProfileRequestContext prc = ExternalAuthentication.getProfileRequestContext(authenticationKey, request);
        final AuthenticationContext authnContext = prc.getSubcontext(AuthenticationContext.class, true);
        if (authnContext == null) {
            logger.debug("No authentication context is available");
            return;
        }
        final RequestedPrincipalContext principalCtx = authnContext.getSubcontext(RequestedPrincipalContext.class, true);
        if (principalCtx == null || principalCtx.getRequestedPrincipals().isEmpty()) {
            logger.debug("No requested principal context is available in the authentication context; Overriding class to {}", AuthnContext.PPT_AUTHN_CTX);
            overrideAuthnContextClass(AuthnContext.PPT_AUTHN_CTX, request, authenticationKey);
            return;
        }

        final Principal principal = new AuthnContextClassRefPrincipal(REFEDS);
        final Principal attribute = principalCtx.getRequestedPrincipals().stream().filter(p -> p.equals(principal)).findFirst().orElse(null);
        if (attribute == null) {
            logger.debug("No authn context class ref principal is found in the requested principals; overriding to {}", AuthnContext.PPT_AUTHN_CTX);
            overrideAuthnContextClass(AuthnContext.PPT_AUTHN_CTX, request, authenticationKey);
            return;
        }
        final String authnMethod = attribute.getName();
        logger.debug("Requested authn method provided by IdP is {}", authnMethod);
        if (!assertion.getPrincipal().getAttributes().containsKey("authnContextClass")) {
            logger.debug("No authentication context class is provided by CAS; Overriding context class to {}", AuthnContext.PPT_AUTHN_CTX);
            overrideAuthnContextClass(AuthnContext.PPT_AUTHN_CTX, request, authenticationKey);
            return;
        }

        final Object clazz = assertion.getPrincipal().getAttributes().get("authnContextClass");
        logger.debug("Located asserted authentication context class [{}]", clazz);

        if (clazz.equals("mfa-duo")) {
            overrideAuthnContextClass(REFEDS, request, authenticationKey);
            logger.info("Validation payload successfully asserts the authentication context class for mfa-duo; Context class is set to {}", REFEDS);
            return;
        }
        logger.debug("Authentication context class [{}] provided by CAS is not one by Duo Security. "
            + "The requested authentication method to be used shall be {} and is left unmodified", clazz, authnMethod);
        overrideAuthnContextClass(clazz.toString(), request, authenticationKey);
    }

    private void overrideAuthnContextClass(final String clazz, final HttpServletRequest request, final String authenticationKey) throws Exception {
        final ProfileRequestContext prc = ExternalAuthentication.getProfileRequestContext(authenticationKey, request);
        final AuthenticationContext authnContext = prc.getSubcontext(AuthenticationContext.class, true);
        if (authnContext == null) {
            throw new IllegalArgumentException("No authentication method parameter is found in the request attributes");
        }
        final RequestedPrincipalContext principalCtx = authnContext.getSubcontext(RequestedPrincipalContext.class, true);
        logger.info("Overriding the principal authn context class ref to {}", clazz);
        if (principalCtx != null) {
            final List<Principal> principals = new ArrayList<>();
            final Principal principal = new AuthnContextClassRefPrincipal(clazz);
            principals.add(principal);
            principalCtx.setRequestedPrincipals(principals);
            principalCtx.setOperator("exact");
            principalCtx.setMatchingPrincipal(principal);

            principalCtx.getPrincipalEvalPredicateFactoryRegistry().register(AuthnContextClassRefPrincipal.class, "exact", new PrincipalEvalPredicateFactory() {
                @Nonnull
                @Override
                public PrincipalEvalPredicate getPredicate(@Nonnull final Principal candidate) {
                    return new PrincipalEvalPredicate() {

                        @Override
                        public Principal getMatchingPrincipal() {
                            return principal;
                        }

                        @Override
                        public boolean apply(@Nullable final PrincipalSupportingComponent input) {
                            final Set supported = input != null
                                ? input.getSupportedPrincipals(principal.getClass())
                                : new HashSet();
                            return supported.stream().anyMatch(p -> principal.equals(p));
                        }
                    };
                }
            });

            logger.info("The final requested authn context class ref principals are {}", principals);
        } else {
            logger.error("No requested principal context class is available");
        }
    }
}
