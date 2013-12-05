package net.unicon.idp.externalauth;

import edu.internet2.middleware.shibboleth.idp.authn.AuthenticationEngine;
import edu.internet2.middleware.shibboleth.idp.authn.LoginHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;

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
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // username from CAS
        String authenticatedPrincipalName = Assertion.class
                .cast(request.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION))
                .getPrincipal().getName();

        //Pass authenticated principal back to IdP to finish its part of authentication request processing
        request.setAttribute(LoginHandler.PRINCIPAL_NAME_KEY, authenticatedPrincipalName);
        AuthenticationEngine.returnToAuthenticationEngine(request, response);
    }
}
