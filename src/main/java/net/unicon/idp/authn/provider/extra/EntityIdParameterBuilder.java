package net.unicon.idp.authn.provider.extra;

import net.shibboleth.idp.authn.ExternalAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Generates a querystring parameter containing the entityId
 * @author chasegawa@unicon.net
 * @author jgasper@unicon.net
 */
public class EntityIdParameterBuilder implements IParameterBuilder {
    private final Logger logger = LoggerFactory.getLogger(EntityIdParameterBuilder.class);

    @Override
    public String getParameterString(final HttpServletRequest request, final String authenticationKey) {
        return getParameterString(request, true);
    }

    public String getParameterString(final HttpServletRequest request, final boolean encode) {
        final String relayingPartyId = request.getAttribute(ExternalAuthentication.RELYING_PARTY_PARAM).toString();

        String rpId = "error-encoding-rpid";

        if (encode == true) {
            try {
                rpId = URLEncoder.encode(relayingPartyId, "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                logger.error("Error encoding the relying party id.", e);
            }
        } else {
            rpId = relayingPartyId;
        }

        return "&entityId=" + rpId;
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof EntityIdParameterBuilder;
    }

}
