package net.unicon.idp.authn.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.unicon.idp.authn.provider.extra.EntityIdParameterBuilder;
import net.unicon.idp.authn.provider.extra.IParameterBuilder;
import net.unicon.idp.externalauth.CasCallbackServlet;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.authn.provider.AbstractLoginHandler;
import edu.internet2.middleware.shibboleth.idp.authn.provider.ExternalAuthnSystemLoginHandler;

/**
 * CasLoginHandler replaces the {@link CasInvokerServlet} AND {@link CasAuthenticatorResource} (facade) from the v1.x implementations. 
 * Allows simplification of the SHIB-CAS authenticator by removing the need to configure and deploy a separate war.
 * 
 * This LoginHandler handles taking the login request from Shib and translating and sending the request on to the CAS instance.
 * @author chasegawa@unicon.net
 */
public class CasLoginHandler extends AbstractLoginHandler {
    private static final String MISSING_CONFIG_MSG = "Unable to create CasLoginHandler - missing {} property. Please check {}";
    private static final String LOGIN = "/login";
    private static final Logger LOGGER = LoggerFactory.getLogger(CasLoginHandler.class);
    private String callbackUrl;
    private String casLoginUrl;
    private String casProtocol = "https";
    private String casPrefix = "/cas";
    private String casServer;
    private String idpProtocol = "https";
    private String idpServer;
    private String idpPrefix = "/idp";
    private String idpCallback = "/Authn/Cas";
    private Set<IParameterBuilder> parameterBuilders = new HashSet<IParameterBuilder>();
    {
        // By default, we start with the entity id param builder included
        parameterBuilders.add(new EntityIdParameterBuilder());
    }

    /**
     * Create a new instance of the login handler. Read the configuration properties from the properties file indicated as 
     * a construction argument. 
     * @param propertiesFile File and path name to the file containing the required properties: 
     * <li>cas.server
     * <li>idp.server
     * @throws FileNotFoundException 
     */
    public CasLoginHandler(final String propertiesFile, final String paramBuilderNames) throws FileNotFoundException {
        this(new FileReader(new File(propertiesFile)), propertiesFile, paramBuilderNames);
    }
    
    /**
     * Construct a new instance using the supplied parameters.
     * @param propertiesFileReader The reader to the properties file
     * @param propertiesFile The name of the properties file (used for logging and error messages)
     * @param paramBuilderNames The list of parameter builder names
     */
    public CasLoginHandler(final Reader propertiesFileReader, final String propertiesFile, final String paramBuilderNames) {
        Properties props = new Properties();
        try {
            if (null == propertiesFileReader) {
                throw new FileNotFoundException("Error reading properties file: " + propertiesFile);
            }
            try {
                props.load(propertiesFileReader);
                propertiesFileReader.close();
            } catch (final IOException e) {
                LOGGER.debug("Error reading properties file: {}", propertiesFile);
                throw e;
            }
            String temp = getProperty(props, "cas.server.protocol");
            casProtocol = StringUtils.isEmpty(temp) ? casProtocol : temp;
            temp = getProperty(props, "cas.application.prefix");
            casPrefix = StringUtils.isEmpty(temp) ? casPrefix : temp;
            temp = getProperty(props, "cas.server");
            casServer = StringUtils.isEmpty(temp) ? casServer : temp;
            casLoginUrl = casProtocol + "://" + casServer + casPrefix + LOGIN;

            temp = getProperty(props, "idp.server.protocol");
            idpProtocol = StringUtils.isEmpty(temp) ? idpProtocol : temp;
            temp = getProperty(props, "idp.server");
            idpServer = StringUtils.isEmpty(temp) ? idpServer : temp;
            temp = getProperty(props, "idp.application.prefix");
            idpPrefix = StringUtils.isEmpty(temp) ? idpPrefix : temp;
            temp = getProperty(props, "idp.server.callback");
            idpCallback = StringUtils.isEmpty(temp) ? idpCallback : temp;
            callbackUrl = idpProtocol + "://" + idpServer + idpPrefix + idpCallback;
        } catch (final Exception e) {
            LOGGER.error("Unable to load parameters", e);
            throw new RuntimeException(e);
        }

        if (StringUtils.isEmpty(casServer)) {
            LOGGER.error(MISSING_CONFIG_MSG, "cas.server", propertiesFile);
            throw new IllegalArgumentException(
                    "CasLoginHandler missing properties needed to build the cas login URL in handler configuration.");
        }
        if (null == idpServer || "".equals(idpServer.trim())) {
            LOGGER.error(MISSING_CONFIG_MSG, "idp.server", propertiesFile);
            throw new IllegalArgumentException(
                    "CasLoginHandler missing properties needed to build the callback URL in handler configuration.");
        }

        createParamBuilders(paramBuilderNames);
    }

    /**
     * @param paramBuilderNames The comma separated list of class names to create.
     */
    private void createParamBuilders(final String paramBuilderNames) {
        for (String className : StringUtils.split(paramBuilderNames, ',')) {
            try {
                Class<?> c = Class.forName(className);
                Constructor<?> cons = c.getConstructor();
                parameterBuilders.add((IParameterBuilder) cons.newInstance());
            } catch (Exception e) {
                LOGGER.warn("Unable to create IParameterBuilder with classname {}", className, e);
            }
        }
    }

    /**
     * @param request The original servlet request
     * @return
     */
    private String getAdditionalParameters(final HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        for (IParameterBuilder paramBuilder : parameterBuilders) {
            builder.append(paramBuilder.getParameterString(request));
        }
        return builder.toString();
    }

    /**
     * @return the property value or empty string if the key/value isn't found
     */
    private String getProperty(final Properties props, final String key) {
        String result = props.getProperty(key);
        return StringUtils.isEmpty(result) ? "" : result;
    }

    /**
     * Translate the SHIB request so that cas renew and/or gateway are set properly before handing off to CAS.
     * @see edu.internet2.middleware.shibboleth.idp.authn.LoginHandler#login(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void login(final HttpServletRequest request, final HttpServletResponse response) {
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
            HttpSession session = request.getSession();

            // Coupled this attribute to the CasCallbackServlet as that is the type that needs this bit of information
            session.setAttribute(CasCallbackServlet.AUTHN_TYPE, authnType);

            response.sendRedirect(response.encodeRedirectURL(casLoginUrl + "?service=" + callbackUrl + authnType
                    + getAdditionalParameters(request)));
        } catch (final IOException e) {
            LOGGER.error("Unable to redirect to CAS from LoginHandler", e);
        }
    }
}
