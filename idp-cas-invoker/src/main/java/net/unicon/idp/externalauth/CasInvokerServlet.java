package net.unicon.idp.externalauth;

import edu.internet2.middleware.shibboleth.idp.authn.provider.ExternalAuthnSystemLoginHandler;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Idp External Authentication Endpoint that redirects to a CAS-protected web resource for authentication
 * <p/>
 * Configured within an IdP deployment and invoked by Servlet Request Dispatcher's forward from
 * IdP's External Authentication Login Handler.
 *
 * @author Dmitriy Kopylenko
 * @since 1.0
 */
public class CasInvokerServlet extends HttpServlet {

    private static final String CAS_PROTECTED_RESOURCE_PARAM = "casProtectedResource";
    private static final String POST_AUTHN_CALLBACK_URL_PARAM = "postAuthnCallbackUrl";

    private String casProtectedResource;
    private String postAuthnCallbackUrl;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.casProtectedResource = config.getInitParameter(CAS_PROTECTED_RESOURCE_PARAM);
        this.postAuthnCallbackUrl = config.getInitParameter(POST_AUTHN_CALLBACK_URL_PARAM);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Boolean force = (Boolean) request.getAttribute(ExternalAuthnSystemLoginHandler.FORCE_AUTHN_PARAM);
        Boolean passive = (Boolean) request.getAttribute(ExternalAuthnSystemLoginHandler.PASSIVE_AUTHN_PARAM);
        String authnType = (force) ? "/renew" : "/norenew";
        if(passive) {
            authnType += "gateway";
        }
        response.sendRedirect(response.encodeRedirectURL(this.casProtectedResource + authnType + "?idp=" + this.postAuthnCallbackUrl));
    }
}
