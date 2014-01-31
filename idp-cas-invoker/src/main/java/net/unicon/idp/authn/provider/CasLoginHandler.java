package net.unicon.idp.authn.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

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
 * CasLoginHandler replaces the {@link CasInvokerServlet} AND {@link CasAuthenticatorResource} (facade) from the v1.x implementations. 
 * Allows simplification of the SHIB-CAS authenticator by removing the need to configure and deploy a separate war.
 * 
 * This LoginHandler handles taking the login request from Shib and translating and sending the request on to the CAS instance.
 * @author chasegawa@unicon.net
 */
public class CasLoginHandler extends AbstractLoginHandler {
    private String callbackUrl;
    private String casLoginUrl;
    private Logger logger = LoggerFactory.getLogger(CasLoginHandler.class);

    /**
     * Create a default CasLoginHandler using the default properties file path and name.
     */
    public CasLoginHandler() {
        this("/opt/shibboleth-idp/conf/cas-shib.properties");
    }

    /**
     * Create a new instance of the login handler. Read the configuration properties from the properties file indicated as 
     * a construction argument. 
     * @param propertiesFile File and path name to the file containing the required properties: 
     * <li>cas.login.url - login URL for the CAS server
     * <li>idp.callback.url - URL to the configured CasCallbackServlet
     */
    public CasLoginHandler(String propertiesFile) {
        Properties props = new Properties();
        try {
            try {
                FileReader reader = new FileReader(new File(propertiesFile));
                props.load(reader);
                reader.close();
            } catch (FileNotFoundException e) {
                logger.debug("Unable to locate properties file: " + propertiesFile);
                throw e;
            } catch (IOException e) {
                logger.debug("Error reading properties file: " + propertiesFile);
                throw e;
            }
            casLoginUrl = props.getProperty("cas.login.url");
            callbackUrl = props.getProperty("idp.callback.url");
        } catch (Exception e) {
            logger.error("Unable to load parameters", e);
        }

        if (isEmpty(casLoginUrl)) {
            logger.error("Unable to create CasLoginHandler - missing cas.login.url property. Please check "
                    + propertiesFile);
            throw new IllegalArgumentException(
                    "CasLoginHandler missing casLoginUrl attribute in handler configuration.");
        }
        if (isEmpty(callbackUrl)) {
            logger.error("Unable to create CasLoginHandler - missing idp.callback.url property. Please check "
                    + propertiesFile);
            throw new IllegalArgumentException(
                    "CasLoginHandler missing callbackUrl attribute in handler configuration.");
        }
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
        force = (null == force) ? Boolean.FALSE : force;
        setSupportsForceAuthentication(force);

        // CAS Protocol - http://www.jasig.org/cas/protocol recommends that when this param is set, to set "true"
        String authnType = (force) ? "&renew=true" : "";

        Boolean passive = (Boolean) request.getAttribute(ExternalAuthnSystemLoginHandler.PASSIVE_AUTHN_PARAM);
        if (null != passive) {
            setSupportsPassive(passive);

            // CAS Protocol - http://www.jasig.org/cas/protocol indicates not setting gateway if renew has been set.
            if (passive && "".equals(authnType)) {
                authnType += "&gateway=true";
            }
        }
        try {
            ServletContext servletContext = request.getSession().getServletContext();
            LoginContext loginContext = HttpServletHelper.getLoginContext(
                    HttpServletHelper.getStorageService(servletContext), servletContext, request);
            String relayingPartyId = loginContext.getRelyingPartyId();

            // CAS protocol doesn't currently handle entityId, but by supplying it now, we are hoping to be able to use that
            // in the next generation of CAS
            response.sendRedirect(response.encodeRedirectURL(casLoginUrl + "?service=" + callbackUrl + authnType
                    + "&entityId=" + relayingPartyId));
        } catch (IOException e) {
            logger.error("Unable to redirect to CAS from LoginHandler", e);
        }
    }
}
