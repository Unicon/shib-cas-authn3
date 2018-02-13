package net.unicon.idp.authn.provider.extra;

/**
 * This is {@link CasMultifactorRefedsToDuoSecurityAuthnMethodParameterBuilder}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
public class CasMultifactorRefedsToDuoSecurityAuthnMethodParameterBuilder extends CasAuthnMethodParameterBuilder {
    @Override
    protected String getCasAuthenticationMethodFor(final String authnMethod) {
        if (isMultifactorRefedsProfile(authnMethod)) {
            return "mfa-duo";
        }
        return null;
    }
}
