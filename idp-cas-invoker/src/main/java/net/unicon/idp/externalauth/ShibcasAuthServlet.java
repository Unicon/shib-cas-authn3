package net.unicon.idp.externalauth;

import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import net.unicon.idp.authn.provider.extra.EntityIdParameterBuilder;
import net.unicon.idp.authn.provider.extra.IParameterBuilder;
import org.apache.commons.lang.StringUtils;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;


/**
 * A Servlet that validates the CAS ticket and then pushes the authenticated principal name into the correct location before
 * handing back control to Shib
 *
 * @author chasegawa@unicon.net
 * @author jgasper@unicon.net
 */
@WebServlet(name = "ShibcasAuthServlet", urlPatterns = {"/Authn/ExtCas/*"})
public class ShibcasAuthServlet extends HttpServlet {
    private Logger logger = LoggerFactory.getLogger(ShibcasAuthServlet.class);
    private static final long serialVersionUID = 1L;
    private static final String artifactParameterName = "ticket";

    private String casToShibTranslatorNames;
    private String casLoginUrl;
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
     * Uses the CAS CommonUtils to build the CAS Redirect URL.
     */
    private String constructRedirectUrl(String serviceUrl, boolean renew, boolean gateway) {
        return CommonUtils.constructRedirectUrl(casLoginUrl, "service", serviceUrl, renew, gateway);
    }

    /**
     * @
     * we can return as well as the best way to know when to do this.
     *
     */
    /**
     * Main entry point of the Servlet
     *
     * @param request  a web request
     * @param response a web response
     * @throws ServletException
     * @throws IOException      TODO: We have the opportunity to give back more to Shib than just the PRINCIPAL_NAME_KEY. Identify additional information
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
            IOException {
        try {
            final String ticket = CommonUtils.safeGetParameter(request, artifactParameterName);
            final String gatewayAttempted = CommonUtils.safeGetParameter(request, "gatewayAttempted");
            final String key = ExternalAuthentication.startExternalAuthentication(request);
            final boolean force = Boolean.parseBoolean(request.getAttribute(ExternalAuthentication.FORCE_AUTHN_PARAM).toString());
            final boolean passive = Boolean.parseBoolean(request.getAttribute(ExternalAuthentication.PASSIVE_AUTHN_PARAM).toString());

            if ((ticket == null || ticket.isEmpty()) && (gatewayAttempted == null || gatewayAttempted.isEmpty())) {
                logger.debug("ticket and gatewayAttempted are not set; initiating CAS login redirect");
                startLoginRequest(request, response, force, passive);
                return;
            }

            if (ticket == null || ticket.isEmpty()) {
                logger.debug("Gateway/Passive returned no ticket, returning NoPassive.");
                request.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, "NoPassive");
                ExternalAuthentication.finishExternalAuthentication(key, request, response);
                return;
            }

            try {
                ticketValidator.setRenew(force);

                logger.debug("validating ticket: {}", ticket);
                Assertion assertion = ticketValidator.validate(ticket, constructServiceUrl(request, response));
                for (CasToShibTranslator casToShibTranslator : translators) {
                    casToShibTranslator.doTranslation(request, response, assertion);
                }

                ExternalAuthentication.finishExternalAuthentication(key, request, response);
            } catch (final TicketValidationException e) {
                logger.error("Ticket validation failed, returning InvalidTicket", e);
                request.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, "InvalidTicket");
                ExternalAuthentication.finishExternalAuthentication(key, request, response);
            }
        } catch (final ExternalAuthenticationException e) {
            throw new ServletException("Error processing ShibCas authentication request", e);
        }
    }

    /**
     * Build addition querystring parameters
     *
     * @param request The original servlet request
     * @return an ampersand delimited list of querystring parameters
     */
    private String getAdditionalParameters(final HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        for (IParameterBuilder paramBuilder : parameterBuilders) {
            builder.append(paramBuilder.getParameterString(request));
        }
        return builder.toString();
    }

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
     *
     * @param environment a Spring Application Context's Environment object (tied to the IdP's root context)
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

    protected void startLoginRequest(final HttpServletRequest request, final HttpServletResponse response, Boolean force, Boolean passive) {
        // CAS Protocol - http://www.jasig.org/cas/protocol indicates not setting gateway if renew has been set.
        // we will set both and let CAS sort it out, but log a warning
        if (Boolean.TRUE.equals(passive) && Boolean.TRUE.equals(force)) {
            logger.warn("Both FORCE AUTHN and PASSIVE AUTHN were set to true, please verify that the requesting system has been properly configured.");
        }

        try {
            String serviceUrl = constructServiceUrl(request, response);
            if (passive) {
                serviceUrl += "&gatewayAttempted=true";
            }

            String loginUrl = constructRedirectUrl(serviceUrl, force, passive)
                    + getAdditionalParameters(request);

            logger.debug("loginUrl: {}", loginUrl);
            response.sendRedirect(loginUrl);

        } catch (final IOException e) {
            logger.error("Unable to redirect to CAS from ShibCas", e);
        }
    }

}
