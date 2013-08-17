package net.unicon.idp.casauth;

import org.springframework.stereotype.Component;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
 * @author Dmitriy Kopylenko
 * @since 1.0
 */

@Component
@Path("/facade")
public class CasAuthenticatorResource {

    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    /**
     * Assumes the IDP is in the same servlet container at the path `/idp`.
     * This is a pretty safe assumption, but it would be better to parse it from the request parameter
     * containing the idp redirect URL since we have that handy anyway
     * TODO: parse the IDP context name from the query parameter named idp
     */
    private static final String IDP_CONTEXT_NAME = "/idp";

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

        String sessionId = this.request.getSession().getId();


        ServletContext idpContext = this.request.getServletContext().getContext("/idp");

        // put the username into the IDP ServletContext (object shared between this external Resource and the IDP)
        // keyed by the session identifier (unique to this user's session) which is the same as the IDP session
        // identifier because the servlet container has been configured to set an empty session path
        idpContext.setAttribute( "net.unicon.idp.casauth." + sessionId, this.request.getRemoteUser());

        this.response.sendRedirect(this.response.encodeRedirectURL(idpCallbackUrl));
        //HTTP 204
        return Response.noContent().build();
    }
}
