package net.unicon.idp.authn.provider.extra;

public class CasMultifactorRefedsToDuoSecurityAuthnMethodParameterBuilder extends CasAuthnMethodParameterBuilder {
    @Override
    protected String getCasAuthenticationMethodFor(final String authnMethod) {
        if (isMultifactorRefedsProfile(authnMethod)) {
            return "mfa-duo";
        }
        return null;
    }
}
