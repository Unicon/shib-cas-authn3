package net.unicon.idp.authn.provider.extra;

public class CasMultifactorRefedsToGoogleAuthenticatorAuthnMethodParameterBuilder extends CasAuthnMethodParameterBuilder {
    @Override
    protected String getCasAuthenticationMethodFor(final String authnMethod) {
        if (isMultifactorRefedsProfile(authnMethod)) {
            return "mfa-gauth";
        }
        return null;
    }
}
