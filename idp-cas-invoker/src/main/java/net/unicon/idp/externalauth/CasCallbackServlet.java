package net.unicon.idp.externalauth;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.opensaml.saml2.core.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.authn.AuthenticationEngine;
import edu.internet2.middleware.shibboleth.idp.authn.LoginHandler;

/**
 * A Servlet that validates the CAS ticket and then pushes the authenticated principal name into the correct location before
 * handing back control to Shib
 *
 * @author chasegawa@unicon.net
 */
public class CasCallbackServlet extends HttpServlet {
    public static final String AUTHN_TYPE = "authnType";
    private static final String DEFAULT_CAS_SHIB_PROPS = "/opt/shibboleth-idp/conf/cas-shib.properties";
    private static final long serialVersionUID = 1L;
    private String artifactParameterName = "ticket";
    private String casPrefix = "/cas";
    private String casProtocol = "https";
    private String casServer;
    private String casToShibTranslatorNames;
    private String idpProtocol = "https";
    private String idpServer;
    private Logger logger = LoggerFactory.getLogger(CasCallbackServlet.class);
    private String serverName;
    private Cas20ServiceTicketValidator ticketValidator;
    private Set<CasToShibTranslator> translators = new HashSet<CasToShibTranslator>();

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
        String ticket = CommonUtils.safeGetParameter(request, artifactParameterName);
        Object authnType = request.getSession().getAttribute(AUTHN_TYPE);
        Assertion assertion = null;
        try {
            ticketValidator.setRenew(null != authnType && authnType.toString().contains("&renew=true"));
            assertion = ticketValidator.validate(ticket, constructServiceUrl(request, response));
        } catch (final TicketValidationException e) {
            logger.error("Unable to validate login attempt.", e);
            boolean wasPassiveAttempt = null != authnType && authnType.toString().contains("&gateway=true");
            // If it was a passive attempt, send back the indicator that the responding provider cannot authenticate 
            // the principal passively, as has been requested. Otherwise, send the generic authn failed code.
            request.setAttribute(LoginHandler.AUTHENTICATION_ERROR_KEY, wasPassiveAttempt ? StatusCode.NO_PASSIVE_URI
                    : StatusCode.AUTHN_FAILED_URI);
            AuthenticationEngine.returnToAuthenticationEngine(request, response);
            return;
        }
        for (CasToShibTranslator casToShibTranslator : translators) {
            casToShibTranslator.doTranslation(request, response, assertion);
        }
        AuthenticationEngine.returnToAuthenticationEngine(request, response);
    }

    /**
     * @return the init param value or empty string if the key/value isn't found
     */
    private String getInitParam(final ServletContext servletConfig, final String key) {
        String result = servletConfig.getInitParameter(key);
        return StringUtils.isEmpty(result) ? "" : result;
    }

    /**
     * @return the property value or empty string if the key/value isn't found
     */
    private String getProperty(final Properties props, final String key) {
        String result = props.getProperty(key);
        return StringUtils.isEmpty(result) ? "" : result;
    }

    /**
     * @see javax.servlet.GenericServlet#init()
     */
    @Override
    public void init() throws ServletException {
        super.init();
        parseProperties();
        buildTranslators();
    }

    /**
     * Check for the externalized properties first. If this hasn't been set, go with the default filename/path
     * If we are unable to load the parameters, we will attempt to load from the init-params. Missing parameters will
     * cause an error - we will not attempt to mix initialization between props and init-params.
     * @throws ServletException
     */
    private void parseProperties() throws ServletException {
        ServletContext servletContext = getServletConfig().getServletContext();
        String casUrlPrefix = null;
        String artifactParamaterName = null;

        String fileName = getInitParam(servletContext, "propertiesFile");
        if (null == fileName || "".equals(fileName.trim())) {
            logger.debug("propertiesFile init-param not set, defaulting to " + DEFAULT_CAS_SHIB_PROPS);
            fileName = DEFAULT_CAS_SHIB_PROPS;
        }

        Properties props = new Properties();
        try {
            try {
                FileReader reader = new FileReader(new File(fileName));
                props.load(reader);
                reader.close();
            } catch (final FileNotFoundException e) {
                logger.debug("Unable to locate file: " + fileName);
                throw e;
            } catch (final IOException e) {
                logger.debug("Error reading file: " + fileName);
                throw e;
            }
            logger.debug("Attempting to load parameters from properties file");
            String temp = getProperty(props, "cas.server.protocol");
            casProtocol = StringUtils.isEmpty(temp) ? casProtocol : temp;
            temp = getProperty(props, "cas.application.prefix");
            casPrefix = StringUtils.isEmpty(temp) ? casPrefix : temp;
            temp = getProperty(props, "cas.server");
            casServer = StringUtils.isEmpty(temp) ? casServer : temp;
            temp = getProperty(props, "idp.server.protocol");
            idpProtocol = StringUtils.isEmpty(temp) ? idpProtocol : temp;
            temp = getProperty(props, "idp.server");
            idpServer = StringUtils.isEmpty(temp) ? idpServer : temp;
            artifactParamaterName = getProperty(props, "artifact.parameter.name");
            casToShibTranslatorNames = getProperty(props, "casToShibTranslators");
        } catch (final Exception e) {
            logger.debug("Error reading properties, attempting to load parameters from servlet init-params");
            String temp = getInitParam(servletContext, "cas.server.protocol");
            casProtocol = StringUtils.isEmpty(temp) ? casProtocol : temp;
            temp = getInitParam(servletContext, "cas.application.prefix");
            casPrefix = StringUtils.isEmpty(temp) ? casPrefix : temp;
            temp = getInitParam(servletContext, "cas.server");
            casServer = StringUtils.isEmpty(temp) ? casServer : temp;
            temp = getInitParam(servletContext, "idp.server.protocol");
            idpProtocol = StringUtils.isEmpty(temp) ? idpProtocol : temp;
            temp = getInitParam(servletContext, "idp.server");
            idpServer = StringUtils.isEmpty(temp) ? idpServer : temp;
            artifactParamaterName = getInitParam(servletContext, "artifact.parameter.name");
            casToShibTranslatorNames = getInitParam(servletContext, "casToShibTranslators");
        }

        if (StringUtils.isEmpty(casServer)) {
            logger.error("Unable to start CasCallbackServlet. Verify that the IDP's web.xml file OR the external property is configured properly.");
            throw new ServletException(
                    "Missing casServer parameter to build the cas server URL - this is a required value");
        }
        casUrlPrefix = casProtocol + "://" + casServer + casPrefix;
        ticketValidator = new Cas20ServiceTicketValidator(casUrlPrefix);

        if (StringUtils.isEmpty(idpServer)) {
            logger.error("Unable to start CasCallbackServlet. Verify that the IDP's web.xml file OR the external property is configured properly.");
            throw new ServletException(
                    "Missing idpServer parameter to build the idp server URL - this is a required value");
        }
        serverName = idpProtocol + "://" + idpServer;
        artifactParameterName = (StringUtils.isEmpty(artifactParamaterName) || "null".equals(artifactParamaterName)) ? "ticket"
                : artifactParamaterName;
    }
}
