package net.unicon.idp.externalauth;

import edu.internet2.middleware.shibboleth.idp.authn.AuthenticationEngine;
import edu.internet2.middleware.shibboleth.idp.authn.LoginHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A Servlet that receives a callback from CAS-protected web resource after a successful authentication and continues
 * the IdP login flow by calling <code>AuthenticationEngine#returnToAuthenticationEngine(HttpServletRequest,HttpServletResponse)</code>
 *
 * @author Dmitriy Kopylenko
 */
public class CasCallbackServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String authenticatedPrincipalName = request.getParameter("p");
        request.setAttribute(LoginHandler.PRINCIPAL_NAME_KEY, authenticatedPrincipalName);
        AuthenticationEngine.returnToAuthenticationEngine(request, response);
    }
}
