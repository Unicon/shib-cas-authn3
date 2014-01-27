package net.unicon.idp.authn.provider;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.authn.LoginContext;
import edu.internet2.middleware.shibboleth.idp.authn.provider.AbstractLoginHandler;
import edu.internet2.middleware.shibboleth.idp.authn.provider.ExternalAuthnSystemLoginHandler;
import edu.internet2.middleware.shibboleth.idp.util.HttpServletHelper;

/**
 * CasLoginHandler replaces the {@link CasInvokerServlet} AND {@link CasAuthenticatorResource} (facade) from the earlier implementations. 
 * Allows simplification of the SHIB-CAS authenticator by removing the need to configure and deploy a separate war.
 * @author chasegawa@unicon.net
 */
public class CasLoginHandler extends AbstractLoginHandler {
    private String callbackUrl;
    private String casLoginUrl;
    private Logger logger = LoggerFactory.getLogger(CasLoginHandler.class);

    /**
     * All attributes/parameters required
     * @param postAuthnCallbackUrl
     * @param casResourceUrl
     */
    public CasLoginHandler(String casLoginUrl, String callbackUrl) {
        if (isEmpty(casLoginUrl)) {
            logger.error("Unable to create CasLoginHandler - missing casLoginUrl parameter. Please check $IDP_HOME/conf/handler.xml");
            throw new IllegalArgumentException(
                    "CasLoginHandler missing casLoginUrl attribute in handler configuration.");
        }
        this.casLoginUrl = casLoginUrl;
        if (isEmpty(callbackUrl)) {
            logger.error("Unable to create CasLoginHandler - missing callbackUrl parameter. Please check $IDP_HOME/conf/handler.xml");
            throw new IllegalArgumentException(
                    "CasLoginHandler missing callbackUrl attribute in handler configuration.");
        }
        this.callbackUrl = callbackUrl;
    }

    /**
     * Essentially StringUtils.isEmpty, but put this here to avoid another jar/dependency
     * @param string
     * @return
     */
    private boolean isEmpty(String string) {
        return null == string || "".equals(string.trim());
    }

    /**
     * Translate the SHIB request so that cas renew and/or gateway are set properly before handing off to CAS.
     * @see edu.internet2.middleware.shibboleth.idp.authn.LoginHandler#login(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void login(HttpServletRequest request, HttpServletResponse response) {
        Boolean force = (Boolean) request.getAttribute(ExternalAuthnSystemLoginHandler.FORCE_AUTHN_PARAM);
        if (null == force) {
            force = Boolean.FALSE;
        }
        setSupportsForceAuthentication(force);
        String authnType = (force) ? "&renew=false" : "&renew=true";

        Boolean passive = (Boolean) request.getAttribute(ExternalAuthnSystemLoginHandler.PASSIVE_AUTHN_PARAM);
        if (null != passive) {
            setSupportsPassive(passive);
            if (passive) {
                authnType += "&gateway=true";
            }
        }
        try {
            ServletContext servletContext = request.getSession().getServletContext();
            LoginContext loginContext = HttpServletHelper.getLoginContext(
                    HttpServletHelper.getStorageService(servletContext), servletContext, request);
            String relayingPartyId = loginContext.getRelyingPartyId();

            response.sendRedirect(response.encodeRedirectURL(casLoginUrl + "?service=" + callbackUrl + authnType
                    + "&entityId=" + relayingPartyId));
        } catch (IOException e) {
            logger.error("Unable to redirect to CAS from LoginHandler", e);
        }
    }
}
