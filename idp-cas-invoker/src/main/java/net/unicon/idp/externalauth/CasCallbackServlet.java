package net.unicon.idp.externalauth;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;

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
    private Cas20ServiceTicketValidator ticketValidator;
    private String serverName;
    private String artifactParameterName = "ticket";

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
            throw new ServletException(e);
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
        String casUrlPrefix = servletConfig.getInitParameter("casServerUrlPrefix");
        if (null == casUrlPrefix) {
            throw new ServletException(
                    "Missing initParam \"casServerUrlPrefix\" - this is a required configuration value");
        }
        ticketValidator = new Cas20ServiceTicketValidator(casUrlPrefix);
        serverName = servletConfig.getInitParameter("serverName");
        if (null == serverName) {
            throw new ServletException("Missing initParam \"serverName\" - this is a required configuration value");
        }
        String apn = servletConfig.getInitParameter("artifactParameterName");
        artifactParameterName = null == apn ? "ticket" : apn;
    }

    /**
     * Use the CAS CommonUtils to build the CAS Service URL.
     */
    private String constructServiceUrl(final HttpServletRequest request, final HttpServletResponse response) {
        return CommonUtils.constructServiceUrl(request, response, null, serverName, artifactParameterName, true);
    }
}
