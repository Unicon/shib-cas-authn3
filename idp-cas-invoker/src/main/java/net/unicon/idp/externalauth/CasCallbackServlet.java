package net.unicon.idp.externalauth;

import edu.internet2.middleware.shibboleth.idp.authn.AuthenticationEngine;
import edu.internet2.middleware.shibboleth.idp.authn.LoginHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A Servlet that retrieves username from where the CAS-protected web resource put it
 * after a successful authentication and continues the IdP login flow by calling
 * <code>AuthenticationEngine#returnToAuthenticationEngine(HttpServletRequest,HttpServletResponse)</code>
 *
 * @author Dmitriy Kopylenko
 * @author Andrew Petro
 */
public class CasCallbackServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // same session ID as that in the CASified external resource
        // because session path set to empty at servlet container layer
        String sessionId = (request.getSession().getId());

        // retrieve the username from the ServletContext keyed by session identifier,
        // where the CASified resource put it
        String authenticatedPrincipalName =
                (String) getServletContext().getAttribute("net.unicon.idp.casauth." + sessionId);

        // clean up the no-longer-needed shared state
        getServletContext().removeAttribute("net.unicon.idp.casauth." + sessionId);

        //Pass authenticated principal back to IdP to finish its part of authentication request processing
        request.setAttribute(LoginHandler.PRINCIPAL_NAME_KEY, authenticatedPrincipalName);
        AuthenticationEngine.returnToAuthenticationEngine(request, response);
    }
}
