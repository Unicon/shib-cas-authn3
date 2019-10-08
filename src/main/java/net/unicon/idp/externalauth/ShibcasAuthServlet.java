package net.unicon.idp.externalauth;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import net.unicon.idp.authn.provider.extra.EntityIdParameterBuilder;
import net.unicon.idp.authn.provider.extra.IParameterBuilder;
import org.apache.commons.lang.StringUtils;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.AbstractCasProtocolUrlBasedTicketValidator;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas10TicketValidator;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.jasig.cas.client.validation.Cas30ServiceTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


/**
 * A Servlet that validates the CAS ticket and then pushes the authenticated principal name into the correct location before
 * handing back control to Shib
 *
 * @author chasegawa@unicon.net
 * @author jgasper@unicon.net
 * @author aremmes (GitHub)
 */
@WebServlet(name = "ShibcasAuthServlet", urlPatterns = {"/Authn/External/*"})
public class ShibcasAuthServlet extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(ShibcasAuthServlet.class);
    private static final long serialVersionUID = 1L;
    private static final String artifactParameterName = "ticket";
    private static final String serviceParameterName = "service";

    private String casLoginUrl;
    private String serverName;
    private String casServerPrefix;
    private String ticketValidatorName;
    private String entityIdLocation;

    private AbstractCasProtocolUrlBasedTicketValidator ticketValidator;

    private final Set<CasToShibTranslator> translators = new HashSet<CasToShibTranslator>();
    private final Set<IParameterBuilder> parameterBuilders = new HashSet<IParameterBuilder>();

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {
        // TODO: We have the opportunity to give back more to Shib than just the PRINCIPAL_NAME_KEY. Identify additional information
        try {
            final String ticket = CommonUtils.safeGetParameter(request, artifactParameterName);
            final String gatewayAttempted = CommonUtils.safeGetParameter(request, "gatewayAttempted");
            final String authenticationKey = ExternalAuthentication.startExternalAuthentication(request);
            final boolean force = Boolean.parseBoolean(request.getAttribute(ExternalAuthentication.FORCE_AUTHN_PARAM).toString());
            final boolean passive = Boolean.parseBoolean(request.getAttribute(ExternalAuthentication.PASSIVE_AUTHN_PARAM).toString());

            if ((ticket == null || ticket.isEmpty()) && (gatewayAttempted == null || gatewayAttempted.isEmpty())) {
                logger.debug("ticket and gatewayAttempted are not set; initiating CAS login redirect");
                startLoginRequest(request, response, force, passive, authenticationKey);
                return;
            }

            if (ticket == null || ticket.isEmpty()) {
                logger.debug("Gateway/Passive returned no ticket, returning NoPassive.");
                request.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, AuthnEventIds.NO_PASSIVE);
                ExternalAuthentication.finishExternalAuthentication(authenticationKey, request, response);
                return;
            }

            validatevalidateCasTicket(request, response, ticket, authenticationKey, force);

        } catch (final ExternalAuthenticationException e) {
            logger.warn("Error processing ShibCas authentication request", e);
            loadErrorPage(request, response);

        } catch (final Exception e) {
            logger.error("Something unexpected happened", e);
            request.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, AuthnEventIds.AUTHN_EXCEPTION);
        }
    }

    private void validatevalidateCasTicket(final HttpServletRequest request, final HttpServletResponse response, final String ticket,
                                           final String authenticationKey, final boolean force) throws ExternalAuthenticationException, IOException {
        try {
            ticketValidator.setRenew(force);
            final String serviceUrl = constructServiceUrl(request, response, true);
            logger.debug("validating ticket: {} with service url: {}", ticket, serviceUrl);
            final Assertion assertion = ticketValidator.validate(ticket, serviceUrl);
            if (assertion == null) {
                throw new TicketValidationException("Validation failed. Assertion could not be retrieved for ticket " + ticket);
            }
            for (final CasToShibTranslator casToShibTranslator : translators) {
                casToShibTranslator.doTranslation(request, response, assertion, authenticationKey);
            }
        } catch (final Exception e) {
            logger.error("Ticket validation failed, returning InvalidTicket", e);
            request.setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, "InvalidTicket");
        }
        ExternalAuthentication.finishExternalAuthentication(authenticationKey, request, response);
    }

    protected void startLoginRequest(final HttpServletRequest request, final HttpServletResponse response,
                                     final Boolean force, final Boolean passive, String authenticationKey) {
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

            final String loginUrl = constructRedirectUrl(serviceUrl, force, passive) + getAdditionalParameters(request, authenticationKey);
            logger.debug("loginUrl: {}", loginUrl);
            response.sendRedirect(loginUrl);
        } catch (final IOException e) {
            logger.error("Unable to redirect to CAS from ShibCas", e);
        }
    }

    /**
     * Uses the CAS CommonUtils to build the CAS Redirect URL.
     */
    private String constructRedirectUrl(final String serviceUrl, final boolean renew, final boolean gateway) {
        return CommonUtils.constructRedirectUrl(casLoginUrl, "service", serviceUrl, renew, gateway, null);
    }

    /**
     * Build addition querystring parameters
     *
     * @param request The original servlet request
     * @return an ampersand delimited list of querystring parameters
     */
    private String getAdditionalParameters(final HttpServletRequest request, final String authenticationKey) {
        final StringBuilder builder = new StringBuilder();
        for (final IParameterBuilder paramBuilder : parameterBuilders) {
            builder.append(paramBuilder.getParameterString(request, authenticationKey));
        }
        return builder.toString();
    }

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        final ApplicationContext ac = (ApplicationContext) config.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        parseProperties(ac.getEnvironment());

        switch (ticketValidatorName) {
            case "cas10":
                ticketValidator = new Cas10TicketValidator(casServerPrefix);
                break;
            case "cas30":
                ticketValidator = new Cas30ServiceTicketValidator(casServerPrefix);
                break;
            case "cas20":
                ticketValidator = new Cas20ServiceTicketValidator(casServerPrefix);
        }

        if (ticketValidator == null) {
            throw new ServletException("Initialization failed. Invalid shibcas.ticketValidatorName property: '"
                + ticketValidatorName + "'");
        }

        if ("append".equalsIgnoreCase(entityIdLocation)) {
            parameterBuilders.add(new EntityIdParameterBuilder());
        }

        buildTranslators(ac.getEnvironment());
        buildParameterBuilders(ac);
    }

    /**
     * Check the idp's idp.properties file for the configuration
     *
     * @param environment a Spring Application Context's Environment object (tied to the IdP's root context)
     */
    private void parseProperties(final Environment environment) {
        logger.debug("reading properties from the idp.properties file");
        casServerPrefix = environment.getRequiredProperty("shibcas.casServerUrlPrefix");
        logger.debug("shibcas.casServerUrlPrefix: {}", casServerPrefix);

        casLoginUrl = environment.getRequiredProperty("shibcas.casServerLoginUrl");
        logger.debug("shibcas.casServerLoginUrl: {}", casLoginUrl);

        serverName = environment.getRequiredProperty("shibcas.serverName");
        logger.debug("shibcas.serverName: {}", serverName);

        ticketValidatorName = environment.getProperty("shibcas.ticketValidatorName", "cas30");
        logger.debug("shibcas.ticketValidatorName: {}", ticketValidatorName);

        entityIdLocation = environment.getProperty("shibcas.entityIdLocation", "append");
        logger.debug("shibcas.entityIdLocation: {}", entityIdLocation);
    }

    private void buildParameterBuilders(final ApplicationContext applicationContext) {
        final Environment environment = applicationContext.getEnvironment();
        final String builders = StringUtils.defaultString(environment.getProperty("shibcas.parameterBuilders", ""));
        for (final String parameterBuilder : StringUtils.split(builders, ";")) {
            try {
                logger.debug("Loading parameter builder class {}", parameterBuilder);
                final Class clazz = Class.forName(parameterBuilder);
                final IParameterBuilder builder = IParameterBuilder.class.cast(clazz.newInstance());
                if (builder instanceof ApplicationContextAware) {
                    ((ApplicationContextAware) builder).setApplicationContext(applicationContext);
                }
                this.parameterBuilders.add(builder);
                logger.debug("Added parameter builder {}", parameterBuilder);
            } catch (final Throwable e) {
                logger.error("Error building parameter builder with name: " + parameterBuilder, e);
            }
        }
    }

    /**
     * Attempt to build the set of translators from the fully qualified class names set in the properties. If nothing has been set
     * then default to the AuthenticatedNameTranslator only.
     */
    private void buildTranslators(final Environment environment) {
        translators.add(new AuthenticatedNameTranslator());

        final String casToShibTranslators = StringUtils.defaultString(environment.getProperty("shibcas.casToShibTranslators", ""));
        for (final String classname : StringUtils.split(casToShibTranslators, ';')) {
            try {
                logger.debug("Loading translator class {}", classname);
                final Class<?> c = Class.forName(classname);
                final CasToShibTranslator e = (CasToShibTranslator) c.newInstance();
                if (e instanceof EnvironmentAware) {
                    ((EnvironmentAware) e).setEnvironment(environment);
                }
                translators.add(e);
                logger.debug("Added translator class {}", classname);
            } catch (final Exception e) {
                logger.error("Error building cas to shib translator with name: " + classname, e);
            }
        }
    }

    /**
     * Use the CAS CommonUtils to build the CAS Service URL.
     */
    protected String constructServiceUrl(final HttpServletRequest request, final HttpServletResponse response) {
        String serviceUrl = CommonUtils.constructServiceUrl(request, response, null, serverName,
            serviceParameterName, artifactParameterName, true);

        if ("embed".equalsIgnoreCase(entityIdLocation)) {
            serviceUrl += (new EntityIdParameterBuilder().getParameterString(request, false));
        }


        return serviceUrl;
    }

    /**
     * Like the above, but with a flag indicating whether we're validating a service ticket,
     * in which case we should not modify the service URL returned by CAS CommonUtils; this
     * avoids appending the entity ID twice when entityIdLocation=embed, since the ID is already
     * embedded in the string during validation.
     */
    protected String constructServiceUrl(final HttpServletRequest request, final HttpServletResponse response, final boolean isValidatingTicket) {
        return isValidatingTicket
            ? CommonUtils.constructServiceUrl(request, response, null, serverName, serviceParameterName, artifactParameterName, true)
            : constructServiceUrl(request, response);
    }

    private void loadErrorPage(final HttpServletRequest request, final HttpServletResponse response) {
        final RequestDispatcher requestDispatcher = request.getRequestDispatcher("/no-conversation-state.jsp");
        try {
            requestDispatcher.forward(request, response);
        } catch (final Exception e) {
            logger.error("Error rendering the empty conversation state (shib-cas-authn3) error view.");
            response.resetBuffer();
            response.setStatus(404);
        }
    }
}
