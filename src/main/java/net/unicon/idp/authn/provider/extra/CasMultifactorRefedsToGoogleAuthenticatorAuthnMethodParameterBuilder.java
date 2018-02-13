package net.unicon.idp.authn.provider.extra;

/**
 * This is {@link CasMultifactorRefedsToGoogleAuthenticatorAuthnMethodParameterBuilder}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
public class CasMultifactorRefedsToGoogleAuthenticatorAuthnMethodParameterBuilder extends CasAuthnMethodParameterBuilder {
    @Override
    protected String getCasAuthenticationMethodFor(final String authnMethod) {
        if (isMultifactorRefedsProfile(authnMethod)) {
            return "mfa-gauth";
        }
        return null;
    }
}
