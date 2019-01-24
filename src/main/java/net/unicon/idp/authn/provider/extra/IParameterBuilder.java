package net.unicon.idp.authn.provider.extra;

import javax.servlet.http.HttpServletRequest;

/**
 * This interface defines the interface a custom parameter builder must adopt. The implementing class needs to build a single string
 * that is of the form: paramName=paramValue
 * The resulting param name-value pair will be sent to CAS in the redirect to /login.
 * @author chasegawa@unicon.net
 */
public interface IParameterBuilder {

    /**
     * Builder should build a string to be added to the param list of a new request. The original request should not be modified.
     * @param request The original request.
     * @return a string of the form: paramName=value
     */
    String getParameterString(HttpServletRequest request, String authenticationKey);
}
