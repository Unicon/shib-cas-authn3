package net.unicon.idp.externalauth;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;

import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import net.unicon.idp.authn.provider.extra.EntityIdParameterBuilder;
import net.unicon.idp.authn.provider.extra.IParameterBuilder;

import org.apache.commons.lang.StringUtils;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.opensaml.saml2.core.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;


/**
 * A Servlet that validates the CAS ticket and then pushes the authenticated principal name into the correct location before
 * handing back control to Shib
 *
 * @author chasegawa@unicon.net
 * @author jgasper@unicon.net
 */
@WebServlet(name="ShibCasAuthServlet", urlPatterns={"/Authn/ExtCas/*"})
public class ShibcasAuthServlet extends HttpServlet {
    private Logger logger = LoggerFactory.getLogger(ShibcasAuthServlet.class);
    private static final long serialVersionUID = 1L;
    private static final String artifactParameterName = "ticket";

    private String casToShibTranslatorNames;
    private String casLoginUrl;
    private String casValidationUrl;
    private String serverName;
    private String casServerPrefix;

    private Cas20ServiceTicketValidator ticketValidator;

    private Set<CasToShibTranslator> translators = new HashSet<CasToShibTranslator>();
    private Set<IParameterBuilder> parameterBuilders = new HashSet<IParameterBuilder>();
    {
        // By default, we start with the entity id param builder included
        parameterBuilders.add(new EntityIdParameterBuilder());
    }


    /**
     * Attempt to build the set of translators from the fully qualified class names set in the properties. If nothing has been set
     * then default to the AuthenticatedNameTranslator only.
     */
    private void buildTranslators() {
        translators.add(new AuthenticatedNameTranslator());
        for (String classname : StringUtils.split(casToShibTranslatorNames, ';')) {
            try {
                Class<?> c = Class.forName(classname);
                Constructor<?> cons = c.getConstructor();
                CasToShibTranslator casToShibTranslator = (CasToShibTranslator) cons.newInstance();
                translators.add(casToShibTranslator);
            } catch (Exception e) {
                logger.error("Error building cas to shib translator with name: " + classname, e);
            }
        }
    }

    /**
     * Use the CAS CommonUtils to build the CAS Service URL.
     */
    private String constructServiceUrl(final HttpServletRequest request, final HttpServletResponse response) {
        return CommonUtils.constructServiceUrl(request, response, null, serverName, artifactParameterName, true);
    }

    /**
     * @TODO: We have the opportunity to give back more to Shib than just the PRINCIPAL_NAME_KEY. Identify additional information
     * we can return as well as the best way to know when to do this.
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
            IOException {
        try {
            final String ticket = CommonUtils.safeGetParameter(request, artifactParameterName);
            final String gatewayAttempted = CommonUtils.safeGetParameter(request, "gatewayAttempted");
            final String key = ExternalAuthentication.startExternalAuthentication(request);
            final Boolean force = Boolean.parseBoolean(request.getAttribute(ExternalAuthentication.FORCE_AUTHN_PARAM).toString());
            final Boolean passive = Boolean.parseBoolean(request.getAttribute(ExternalAuthentication.PASSIVE_AUTHN_PARAM).toString());

            if ((ticket == null || ticket.isEmpty()) && (gatewayAttempted == null || gatewayAttempted.isEmpty())) {
                startLoginRequest(request, response);
                return;
            }

            Assertion assertion = null;

            try {
                ticketValidator.setRenew(force);
                assertion = ticketValidator.validate(ticket, constructServiceUrl(request, response));
            } catch (final TicketValidationException e) {
                logger.error("Unable to validate startLoginRequest attempt.", e);
                // If it was a passive attempt, send back the indicator that the responding provider cannot authenticate
                // the principal passively, as has been requested. Otherwise, send the generic authn failed code.
                request.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, passive ? StatusCode.NO_PASSIVE_URI
                        : StatusCode.AUTHN_FAILED_URI);
                ExternalAuthentication.finishExternalAuthentication(key, request, response);
                return;
            }

            for (CasToShibTranslator casToShibTranslator : translators) {
                casToShibTranslator.doTranslation(request, response, assertion);
            }

            ExternalAuthentication.finishExternalAuthentication(key, request, response);

        } catch (final ExternalAuthenticationException e) {
            throw new ServletException("Error processing CAS authentication request", e);
        }
    }

    /**
     * Build addition querystring parameters
     * @param request The original servlet request
     * @return
     */
    private String getAdditionalParameters(final HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        for (IParameterBuilder paramBuilder : parameterBuilders) {
            builder.append(paramBuilder.getParameterString(request));
        }
        return builder.toString();
    }

    /**
     * @see javax.servlet.GenericServlet#init()
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        ApplicationContext ac = (ApplicationContext) config.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        parseProperties(ac.getEnvironment());

        ticketValidator = new Cas20ServiceTicketValidator(casServerPrefix);

        buildTranslators();
    }

    /**
     * Check the idp's idp.properties file for the configuration
     * @param environment
     */
    private void parseProperties(Environment environment) {
        logger.debug("reading properties from the idp.properties file");
        casServerPrefix = environment.getRequiredProperty("shibcas.casServerUrlPrefix");
        logger.debug("shibcas.casServerUrlPrefix: {}", casServerPrefix);

        casLoginUrl = environment.getRequiredProperty("shibcas.casServerLoginUrl");
        logger.debug("shibcas.casServerLoginUrl: {}", casLoginUrl);

        serverName = environment.getRequiredProperty("shibcas.serverName");
        logger.debug("shibcas.serverName: {}", serverName);

        casToShibTranslatorNames = environment.getProperty("shibcas.casToShibTranslators");
        logger.debug("shibcas.casToShibTranslators: {}", casToShibTranslatorNames);
        casToShibTranslatorNames = null == casToShibTranslatorNames ? "" : casToShibTranslatorNames;
    }

    public void startLoginRequest(final HttpServletRequest request, final HttpServletResponse response) {
        Boolean force = Boolean.parseBoolean(request.getAttribute(ExternalAuthentication.FORCE_AUTHN_PARAM).toString());
        Boolean passive = Boolean.parseBoolean(request.getAttribute(ExternalAuthentication.PASSIVE_AUTHN_PARAM).toString());

        // CAS Protocol - http://www.jasig.org/cas/protocol recommends that when this param is set, to set "true"
        String authnType = force ? "renew=true" : "";

        // CAS Protocol - http://www.jasig.org/cas/protocol indicates not setting gateway if renew has been set.
        // we will set both and let CAS sort it out, but log a warning
        if (passive) {
            if (Boolean.TRUE.equals(force)) {
                authnType += "&";
                logger.warn("Both FORCE AUTHN and PASSIVE AUTHN were set to true, please verify that the requesting system has been properly configured.");
            }
            authnType += "gateway=true";
        }
        logger.debug("authnType: {}", authnType);

        try {
            // Create the raw startLoginRequest string - Service/Callback URL should always be last
            StringBuilder loginString = new StringBuilder(casLoginUrl + "?");
            loginString.append(authnType);

            String additionalParams = getAdditionalParameters(request);
            if (StringUtils.endsWith(loginString.toString(), "?")) {
                additionalParams = StringUtils.removeStart(additionalParams, "&");
            }
            loginString.append(additionalParams);

            String serviceUrl = constructServiceUrl(request, response);
            if (passive) {
                serviceUrl += "&gatewayAttempted=true";
            }

            loginString.append(StringUtils.endsWith(loginString.toString(), "?") ? "service=" : "&service=");
            loginString.append(URLEncoder.encode(serviceUrl, "UTF-8"));

            logger.debug("loginString: {}", loginString);
            response.sendRedirect(response.encodeRedirectURL(loginString.toString()));
        } catch (final IOException e) {
            logger.error("Unable to redirect to CAS from ShibCas", e);
        }
    }

}
