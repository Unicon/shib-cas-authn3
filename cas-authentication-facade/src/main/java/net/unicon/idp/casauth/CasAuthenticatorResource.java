package net.unicon.idp.casauth;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
 * The decision which resource to use will be made on the IdP side based on the SP's configuration trying to authenticate against CAS
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
        Assertion casAssertion = (Assertion) this.request.getSession(false).getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION);
        this.response.sendRedirect(this.response.encodeRedirectURL(idpCallbackUrl + "?p=" + casAssertion.getPrincipal().getName()));
        //HTTP 204
        return Response.noContent().build();
    }
}
