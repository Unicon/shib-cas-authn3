## A Shibboleth IdP v3.X plugin for authentication via an external CAS Server

> This project was developed as part of Unicon's [Open Source Support program](https://unicon.net/support). Professional support/integration assistance for this module is available. For more information, visit <https://unicon.net/opensource/shibboleth>.

This is a Shibboleth IDP external authentication plugin that delegates the authentication to an external 
Central Authentication Server. The biggest advantage of using this component over the plain 
`REMOTE_USER` header solution provided by Shibboleth is the ability to utilize a full range 
of native CAS protocol features such as `renew` and `gateway`, plus the ability to share with CAS the 
EntityID of the relying application.

The plugin consists of 2 components:
* A library (.jar) file that provides an IDP side servlet that acts as a bridge between CAS and the IDP
* Spring Webflow definition file (and bean definition file) that invokes the shib-cas-authn3 library.

Build Status
-------------------------------------------------------------
[![Build Status](https://travis-ci.org/Unicon/shib-cas-authn3.svg?branch=master)](https://travis-ci.org/Unicon/shib-cas-authn3)

Software Requirements
-------------------------------------------------------------
This minimum supported version of Shibboleth Identity Provider is `3.3.0`

> A Shibboleth IdP v2.X plugin can be found at <https://github.com/Unicon/shib-cas-authn2>.

Installation
---------------------------------------------------------------

#### Overview

1. Copy the Spring Webflow files, jsp, and included jar files into the IDP_HOME.
1. Update the IdP's `web.xml`. (optional)
1. Update the IdP's `idp.properties` file.
1. Update the IdP's `general-authn.xml` file.
1. Rebuild the war file.

#### Copy the Spring Webflow files into the IDP_HOME
Copy the two xml files from the IDP_HOME directory (in the src tree) to the corresponding layout in your Shibboleth IdP home directory.

#### Update the IdP's `web.xml` (optional)
> The servlet will register itself with the container when running under a Servlet 3.0 compliant container (such as Jetty 9).
This step is provided for legacy reasons.

Add the ShibCas Auth Servlet entry in `IDP_HOME/edit-webapp/WEB-INF/web.xml` (Copy from `IDP_HOME/webapp/WEB-INF/web.xml`, if necessary.)

Example snippet `web.xml`:

```xml
...
    <!-- Servlet for receiving a callback from an external CAS Server and continues the IdP login flow -->
    <servlet>
        <servlet-name>ShibCas Auth Servlet</servlet-name>
        <servlet-class>net.unicon.idp.externalauth.ShibcasAuthServlet</servlet-class>
        <load-on-startup>2</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>ShibCas Auth Servlet</servlet-name>
        <url-pattern>/Authn/ExtCas/*</url-pattern>
    </servlet-mapping>
...
```

#### Update the IdP's idp.properties file

1. Set the `idp.authn.flows` to `Shibcas`. Or, for advance cases, add `Shibcas` to the list.
1. Add the additional properties.

```
...
# Regular expression matching login flows to enable, e.g. IPAddress|Password
#idp.authn.flows = Password
idp.authn.flows = Shibcas

# CAS Client properties (usage loosely matches that of the Java CAS Client)
## CAS Server Properties
shibcas.casServerUrlPrefix = https://cassserver.example.edu/cas
shibcas.casServerLoginUrl = ${shibcas.casServerUrlPrefix}/login

## Shibboleth Server Properties
shibcas.serverName = https://shibserver.example.edu

# By default you always get the AuthenticatedNameTranslator, add additional code to cover your custom needs.
# Takes a comma separated list of fully qualified class names
# shibcas.casToShibTranslators = com.your.institution.MyCustomNamedTranslatorClass
# shibcas.parameterBuilders = com.your.institution.MyParameterBuilderClass

# Specify CAS validator to use - either 'cas20' or 'cas30' (default)
# shibcas.ticketValidatorName = cas30


# Specify if the Relying Party/Service Provider entityId should be appended as a separate entityId query string parameter
# or embedded in the "service" querystring parameter - `append` (default) or `embed`
# shibcas.entityIdLocation = append
...
```

#### Update the IdP's `general-authn.xml` file.
Register the module with the IdP by adding the `authn/Shibcas` bean in `IDP_HOME/conf/authn/general-authn.xml`:

```xml
...
    <util:list id="shibboleth.AvailableAuthenticationFlows">

        <bean id="authn/Shibcas" parent="shibboleth.AuthenticationFlow"
                p:passiveAuthenticationSupported="true"
                p:forcedAuthenticationSupported="true"
                p:nonBrowserSupported="false" />
...
```


#### Rebuild the war file
From the `IDP_HOME/bin` directory, run `./build.sh` or `build.bat` to rebuild the `idp.war`. Redeploy if necessary.


#### CAS Service Registry
By setting `shibcas.entityIdLocation=embed`, shib-cas-authn will embed the entityId in the service string so that CAS Server
can use the entityId when evaluating a service registry entry match. Using serviceIds of something like: 
`https://shibserver.example.edu/idp/Authn/ExtCas\?conversation=[a-z0-9]*&entityId=http://testsp.school.edu/sp`
or
`https://shibserver.example.edu/idp/Authn/ExtCas\?conversation=[a-z0-9]*&entityId=http://test.unicon.net/sp`
will match as two different entries in the service registry which will allow as CAS admin to enable MFA or use access strategies on an SP by SP basis. 

Release Notes
-------------------------------------------------------------
See [here](https://github.com/Unicon/shib-cas-authn3/releases/).


Developer Notes
-------------------------------------------------------------
The project distributables can be built using `./gradlew`. The artifacts will be in `build/distributions`.

This project includes a Docker environment to assist with development/testing. 

To build and execute: `./gradlew clean; ./gradlew up`
Then browse to: `https://idptestbed/idp/profile/SAML2/Unsolicited/SSO?providerId=https://sp.idptestbed/shibboleth`

> You'll need a `hosts` file entry that points `idptestbed` to your Docker server's IP address. 

The IdP only has a session of 1 minute (to test expired session/conversation key issues), so login into CAS Server quickly.