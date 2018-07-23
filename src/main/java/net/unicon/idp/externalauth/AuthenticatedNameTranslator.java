package net.unicon.idp.externalauth;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple translation of the principal name from the CAS assertion to the string value used by Shib
 *
 * @author chasegawa@unicon.net
 * @author jgasper@unicon.net
 */
public class AuthenticatedNameTranslator implements CasToShibTranslator {
    private final Logger logger = LoggerFactory.getLogger(AuthenticatedNameTranslator.class);

    @Override
    public void doTranslation(final HttpServletRequest request, final HttpServletResponse response,
                              final Assertion assertion, final String authenticationKey) {
        if (assertion == null || assertion.getPrincipal() == null) {
            logger.error("No valid assertion or principal could be found to translate");
            return;
        }
        final AttributePrincipal casPrincipal = assertion.getPrincipal();
        logger.debug("principalName found and being passed on: {}", casPrincipal.getName());

        // Pass authenticated principal back to IdP to finish its part of authentication request processing
        final Collection<IdPAttributePrincipal> assertionAttributes = produceIdpAttributePrincipal(assertion.getAttributes());
        final Collection<IdPAttributePrincipal> principalAttributes = produceIdpAttributePrincipal(casPrincipal.getAttributes());

        if (!assertionAttributes.isEmpty() || !principalAttributes.isEmpty()) {
            logger.debug("Found attributes from CAS. Processing...");
            final Set<Principal> principals = new HashSet<>();

            principals.addAll(assertionAttributes);
            principals.addAll(principalAttributes);
            principals.add(new UsernamePrincipal(casPrincipal.getName()));

            request.setAttribute(ExternalAuthentication.SUBJECT_KEY, new Subject(false, principals,
                Collections.emptySet(), Collections.emptySet()));
            logger.info("Created an IdP subject instance with principals containing attributes for {} ", casPrincipal.getName());

        } else {
            logger.debug("No attributes released from CAS. Creating an IdP principal for {}", casPrincipal.getName());
            request.setAttribute(ExternalAuthentication.PRINCIPAL_NAME_KEY, casPrincipal.getName());
        }
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(final Object that) {
        return EqualsBuilder.reflectionEquals(this, that);
    }


    private Collection<IdPAttributePrincipal> produceIdpAttributePrincipal(final Map<String, Object> casAttributes) {
        final Set<IdPAttributePrincipal> principals = new HashSet<>();
        for (final Map.Entry<String, Object> entry : casAttributes.entrySet()) {
            final IdPAttribute attr = new IdPAttribute(entry.getKey());

            final List<StringAttributeValue> attributeValues = new ArrayList<>();
            if (entry.getValue() instanceof Collection) {
                for (final Object value : (Collection) entry.getValue()) {
                    attributeValues.add(new StringAttributeValue(value.toString()));
                }
            } else {
                attributeValues.add(new StringAttributeValue(entry.getValue().toString()));
            }
            if (!attributeValues.isEmpty()) {
                attr.setValues(attributeValues);
                logger.debug("Added attribute {} with values {}", entry.getKey(), entry.getValue());
                principals.add(new IdPAttributePrincipal(attr));
            } else {
                logger.warn("Skipped attribute {} since it contains no values", entry.getKey());
            }
        }
        return principals;
    }
}
