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
    private static final String DEFAULT_CAS_SHIB_PROPS = "/opt/shibboleth-idp/conf/cas-shib.properties";
    private static final long serialVersionUID = 1L;
    private String artifactParameterName = "ticket";
    private String casPrefix = "/cas";
    private String casProtocol = "https";
    private String casServer;
    private String idpProtocol = "https";
    private String idpServer;
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
     * @TODO: We have the opportunity to give back more to Shib than just the PRINCIPAL_NAME_KEY. Identify additional information
     * we can return as well as the best way to know when to do this.
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String ticket = CommonUtils.safeGetParameter(request, artifactParameterName);
        String authenticatedPrincipalName = null;  // i.e. username from CAS
        try {
            authenticatedPrincipalName = ticketValidator.validate(ticket, constructServiceUrl(request, response))
                    .getPrincipal().getName();
        } catch (TicketValidationException e) {
            logger.error("Unable to validate login attempt.", e);
            // At this point, we likely have an error due to configuration issues. Throw it out and let the admins have a better
            // idea of what is going on than if we just have Shib show a failed authentication.
            throw new ServletException(e);
        }
        // Pass authenticated principal back to IdP to finish its part of authentication request processing
        request.setAttribute(LoginHandler.PRINCIPAL_NAME_KEY, authenticatedPrincipalName);
        AuthenticationEngine.returnToAuthenticationEngine(request, response);
    }

    /**
     * @return the init param value or empty string if the key/value isn't found
     */
    private String getInitParam(ServletConfig servletConfig, String key) {
        String result = servletConfig.getInitParameter(key);
        return null == result ? "" : result;
    }

    /**
     * @return the property value or empty string if the key/value isn't found
     */
    private String getProperty(Properties props, String key) {
        String result = props.getProperty(key);
        return null == result ? "" : result;
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
        String fileName = getInitParam(servletConfig, "casCallbackServletPropertiesFile");
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
            String temp = getProperty(props, "cas.server.protocol");
            casProtocol = "".equals(temp) ? casProtocol : temp;
            temp = getProperty(props, "cas.application.prefix");
            casPrefix = "".equals(temp) ? casPrefix : temp;
            temp = getProperty(props, "cas.server");
            casServer = "".equals(temp) ? casServer : temp;
            temp = getProperty(props, "idp.server.protocol");
            idpProtocol = "".equals(temp) ? idpProtocol : temp;
            temp = getProperty(props, "idp.server");
            idpServer = "".equals(temp) ? idpServer : temp;
            apn = getProperty(props, "artifact.parameter.name");
        } catch (Exception e) {
            logger.debug("Attempting to load parameters from servlet init-params");
            String temp = getInitParam(servletConfig, "cas.server.protocol");
            casProtocol = "".equals(temp) ? casProtocol : temp;
            temp = getInitParam(servletConfig, "cas.application.prefix");
            casPrefix = "".equals(temp) ? casPrefix : temp;
            temp = getInitParam(servletConfig, "cas.server");
            casServer = "".equals(temp) ? casServer : temp;
            temp = getInitParam(servletConfig, "idp.server.protocol");
            idpProtocol = "".equals(temp) ? idpProtocol : temp;
            temp = getInitParam(servletConfig, "idp.server");
            idpServer = "".equals(temp) ? idpServer : temp;
            apn = getInitParam(servletConfig, "artifact.parameter.name");
        }

        if (null == casServer || "".equals(casServer.trim())) {
            logger.error("Unable to start CasCallbackServlet. Verify that the IDP's web.xml file OR the external property is configured properly.");
            throw new ServletException(
                    "Missing casServer parameter to build the cas server URL - this is a required value");
        }
        casUrlPrefix = casProtocol + "://" + casServer + casPrefix;
        ticketValidator = new Cas20ServiceTicketValidator(casUrlPrefix);

        if (null == idpServer || "".equals(idpServer.trim())) {
            logger.error("Unable to start CasCallbackServlet. Verify that the IDP's web.xml file OR the external property is configured properly.");
            throw new ServletException(
                    "Missing idpServer parameter to build the idp server URL - this is a required value");
        }
        serverName = idpProtocol + "://" + idpServer;
        artifactParameterName = (null == apn || "".equals(apn.trim()) || "null".equals(apn)) ? "ticket" : apn;
    }
}
