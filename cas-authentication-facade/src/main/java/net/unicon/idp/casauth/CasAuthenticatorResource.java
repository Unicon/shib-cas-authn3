package net.unicon.idp.casauth;

import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import java.io.IOException;

/**
 * A root HTTP resource protected by CAS and acting as an external authentication facade used by Shibboleth IdP external authentication LoginHandler.
 * <p/>
 * This root '/facade' resource has four sub-resources representing features of CAS protocol. Namely: <i>renew service</i>,
 * <i>no renew service</i>, <i>renew as a gateway service</i>, and finally <i>no renew as a gateway service</i>
 * <p/>
 * The decision which resource to use will be made on the IdP side based on the SP's configuration trying to authenticate against CAS.
 *
 * Places the authenticated principal identifier (String) into the ServletContext as an attribute keyed by the
 * session identifier.  Shibboleth IdP will be able to retrieve it, keyed by the session identifier, if the
 * servlet container is configured (emptySessionPath equal true) so that the IdP and the CASified resource web
 * application share the same session identifier (JSESSIONID cookie).
 *
 * Configured via context-param idPContextName with context name of IdP (example: /idp ) .  Defaults to /idp.
 *
 * @author Dmitriy Kopylenko
 * @author Andrew Petro
 * @since 1.0
 */

@Component
@Path("/facade")
public class CasAuthenticatorResource {

    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    @Context
    private ServletContext servletContext;


    @GET
    @Path("renew")
    public Response renewAuth(@QueryParam("idp") final String idpCallbackUrl) throws IOException {
        return redirectBackToIdp(idpCallbackUrl);
    }

    @GET
    @Path("norenew")
    public Response noRenewAuth(@QueryParam("idp") final String idpCallbackUrl) throws IOException {
        return redirectBackToIdp(idpCallbackUrl);
    }

    @GET
    @Path("renewgateway")
    public Response renewGatewayAuth(@QueryParam("idp") final String idpCallbackUrl) throws IOException {
        return redirectBackToIdp(idpCallbackUrl);
    }

    @GET
    @Path("norenewgateway")
    public Response noRenewGatewayAuth(@QueryParam("idp") final String idpCallbackUrl) throws IOException {
        return redirectBackToIdp(idpCallbackUrl);
    }

    private Response redirectBackToIdp(String idpCallbackUrl) throws IOException {

        String idpContextName = this.servletContext.getInitParameter("idPContextName");
        // default to /idp
        if (idpContextName == null) {
            idpContextName = "/idp";
        }

        // "cross-context" to share authenticatedPrincipal
        final ServletContext idpContext = this.servletContext.getContext(idpContextName);

        // username from CAS
        final String authenticatedPrincipal = Assertion.class.cast(request.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION)).getPrincipal().getName();

        // SessionId is as index into shared state between /idp and /casauth, requires Tomcat emptySessionPath="true"
        final HttpSession session = this.request.getSession();

        // put the username into the IDP ServletContext (object shared between this external Resource and the IDP)
        // keyed by the session identifier (unique to this user's session) which is the same as the IDP session
        // identifier because the servlet container has been configured to set an empty session path
        idpContext.setAttribute("net.unicon.idp.casauth." + session.getId(), authenticatedPrincipal);

        this.response.sendRedirect(this.response.encodeRedirectURL(idpCallbackUrl));

        // cleanup casauth session
        session.invalidate();

        //HTTP 204
        return Response.noContent().build();
    }
}
