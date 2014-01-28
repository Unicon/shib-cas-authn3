package net.unicon.idp.externalauth;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
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
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_CAS_SHIB_PROPS = "/opt/shibboleth-idp/conf/cas-shib.properties";
    private String artifactParameterName = "ticket";
    private Logger logger = LoggerFactory.getLogger(CasCallbackServlet.class);
    private String serverName;
    private Cas20ServiceTicketValidator ticketValidator;

    /**
     * Use the CAS CommonUtils to build the CAS Service URL.
     */
    private String constructServiceUrl(final HttpServletRequest request, final HttpServletResponse response) {
        return CommonUtils.constructServiceUrl(request, response, null, serverName, artifactParameterName, true);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String ticket = CommonUtils.safeGetParameter(request, artifactParameterName);
        String authenticatedPrincipalName;  // i.e. username from CAS
        try {
            authenticatedPrincipalName = ticketValidator.validate(ticket, constructServiceUrl(request, response))
                    .getPrincipal().getName();
        } catch (TicketValidationException e) {
            logger.error("Unable to validate login attempt.", e);
            throw new ServletException(e); // Do we throw this or just return control back to SHIB with no auth info?
        }
        // Pass authenticated principal back to IdP to finish its part of authentication request processing
        request.setAttribute(LoginHandler.PRINCIPAL_NAME_KEY, authenticatedPrincipalName);
        AuthenticationEngine.returnToAuthenticationEngine(request, response);
    }

    /**
     * @see javax.servlet.GenericServlet#init()
     */
    @Override
    public void init() throws ServletException {
        super.init();
        ServletConfig servletConfig = getServletConfig();
        String casUrlPrefix = null;
        String apn = null;

        // Check for the externalized properties first. If this hasn't been set, go with the default filename/path
        // If we are unable to load the parameters, we will attempt to load from the init-params. Missing parameters will
        // cause an error - we will not attempt to mix initialization between props and init-params.
        String fileName = servletConfig.getInitParameter("casCallbackServletPropertiesFile");
        if (null == fileName || "".equals(fileName.trim())) {
            logger.debug("casCallbackServletPropertiesFile init-param not set, defaulting to " + DEFAULT_CAS_SHIB_PROPS);
            fileName = DEFAULT_CAS_SHIB_PROPS;
        }

        Properties props = new Properties();
        try {
            try {
                FileReader reader = new FileReader(new File(fileName));
                props.load(reader);
                reader.close();
            } catch (FileNotFoundException e) {
                logger.debug("Unable to locate file: " + fileName);
                throw e;
            } catch (IOException e) {
                logger.debug("Error reading file: " + fileName);
                throw e;
            }
            casUrlPrefix = props.getProperty("casServerUrlPrefix");
            serverName = props.getProperty("serverName");
            apn = props.getProperty("artifactParameterName");
        } catch (Exception e) {
            logger.debug("Attempting to load parameters from servlet init-params");
            casUrlPrefix = servletConfig.getInitParameter("casServerUrlPrefix");
            serverName = servletConfig.getInitParameter("serverName");
            apn = servletConfig.getInitParameter("artifactParameterName");
        }

        if (null == casUrlPrefix) {
            logger.error("Unable to start CasCallbackServlet. Verify that the IDP's web.xml file OR the external property is configured properly and includes the casServerUrlPrefix init param.");
            throw new ServletException(
                    "Missing initParam \"casServerUrlPrefix\" - this is a required configuration value");
        }
        ticketValidator = new Cas20ServiceTicketValidator(casUrlPrefix);
        if (null == serverName) {
            logger.error("Unable to start CasCallbackServlet. Verify that the IDP's web.xml file OR the external property is configured properly and includes the serverName init param.");
            throw new ServletException("Missing initParam \"serverName\" - this is a required configuration value");
        }
        artifactParameterName = (null == apn || "".equals(apn.trim())) ? "ticket" : apn;
    }
}
