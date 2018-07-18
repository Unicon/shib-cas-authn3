package net.unicon.idp.externalauth;

import net.shibboleth.idp.authn.ExternalAuthentication;
import org.jasig.cas.client.validation.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is {@link CasDuoSecurityRefedsAuthnMethodTranslator}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
public class CasDuoSecurityRefedsAuthnMethodTranslator implements CasToShibTranslator, EnvironmentAware {
    private final Logger logger = LoggerFactory.getLogger(CasDuoSecurityRefedsAuthnMethodTranslator.class);

    private static final String REFEDS = "https://refeds.org/profile/mfa";

    private Environment environment;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    @Override
    public void doTranslation(final HttpServletRequest request, final HttpServletResponse response, final Assertion assertion) throws Exception {
        final Object attribute = request.getAttribute(ExternalAuthentication.AUTHN_METHOD_PARAM);
        if (attribute == null) {
            logger.debug("No authentication method parameter is found in the request attributes");
            return;
        }
        final String authnMethod = attribute.toString();
        logger.debug("Requested authn method provided by IdP is {}", authnMethod);
        if (!assertion.getPrincipal().getAttributes().containsKey("authnContextClass")) {
            logger.debug("No authentication context class is provided by CAS; Overriding context class to {}", AuthnContext.PPT_AUTHN_CTX);
            request.setAttribute(ExternalAuthentication.AUTHN_METHOD_PARAM, AuthnContext.PPT_AUTHN_CTX);
            return;
        }

        final Object clazz = assertion.getPrincipal().getAttributes().get("authnContextClass");
        logger.debug("Located asserted authentication context class [{}]", clazz);

        if (clazz.equals("mfa-duo")) {
            request.setAttribute(ExternalAuthentication.AUTHN_METHOD_PARAM, REFEDS);
            logger.info("Validation payload successfully asserts the authentication context class for mfa-duo; Context class is set to {}", REFEDS);
            return;
        }
        logger.debug("Authentication context class [{}] provided by CAS is not one by Duo Security. "
            + "The requested authentication method to be used shall be {}", clazz, authnMethod);
    }
}
