package net.unicon.idp.authn.provider.extra;

import net.shibboleth.idp.authn.ExternalAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Generates a querystring parameter containing the authn_method parameter.
 *
 * @author Misagh Moayyed
 */
public abstract class CasAuthnMethodParameterBuilder implements IParameterBuilder, ApplicationContextAware {
    private final Logger logger = LoggerFactory.getLogger(CasAuthnMethodParameterBuilder.class);
    protected ApplicationContext applicationContext;

    @Override
    public String getParameterString(final HttpServletRequest request) {
        final Object attribute = request.getAttribute(ExternalAuthentication.AUTHN_METHOD_PARAM);
        if (attribute == null) {
            logger.debug("No authentication method parameter is found in the request attributes");
            return "";
        }
        final String authnMethod = attribute.toString();
        logger.debug("Located authentication method [{}]. Locating assigned CAS authentication method...", authnMethod);
        final String casMethod = getCasAuthenticationMethodFor(authnMethod);
        if (casMethod != null && !casMethod.isEmpty()) {
            return "&authn_method=" + casMethod;
        }
        return "";
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
        return "https://refeds.org/profile/mfa".equals(authnMethod);
    }
}
