package net.unicon.idp.externalauth;

import net.shibboleth.idp.authn.ExternalAuthentication;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.TicketValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is {@link CasDuoSecurityAuthnMethodRequiredTranslator}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
public class CasDuoSecurityAuthnMethodRequiredTranslator implements CasToShibTranslator, EnvironmentAware {
    private Logger logger = LoggerFactory.getLogger(CasDuoSecurityAuthnMethodRequiredTranslator.class);

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
        logger.debug("Located authentication method [{}]. Locating assigned CAS authentication method...", authnMethod);
        if (assertion.getPrincipal().getAttributes().containsKey("authnContextClass")) {
            final Object clazz = assertion.getPrincipal().getAttributes().get("authnContextClass");
            logger.debug("Located asserted authentication context class [{}]", clazz);
            if (clazz.equals("mfa-duo")) {
                logger.info("Validation payload successfully asserts the authentication context class for mfa-duo");
                return;
            }
            
            logger.error("CAS payload does not contain the asserted authentication context class mfa-duo. Assertion is invalid");
        } else {
            logger.error("CAS payload does not contain the asserted authentication context class. Assertion is invalid");
        }

        throw new TicketValidationException("Authentication method could not be asserted given the validation payload from CAS");
    }
}
