## A Shibboleth IdP v3.X plugin for External Authentication via an external CAS Server
> A Shibboelth IdP v2.X plugin can be found at <https://github.com/Unicon/shib-cas-authn2>.

This is a Shibboleth IDP external authentication plugin that delegates the authentication to an external 
Central Authentication Server. The biggest advantage of using this component over the plain 
`REMOTE_USER` header solution provided by Shibboleth is the ability to utilize a full range 
of native CAS protocol features such as `renew` and `gateway`, plus the ability to share with CAS the 
EntityID of the relying application.

The plugin consists of 2 components:
* A library (.jar) file that provides an IDP side servlet that acts as a bridge between CAS and the IDP
* Spring Webflow definite file (and bean definition file) that invokes the shib-cas-authn3 library.

Build Status
-------------------------------------------------------------
Travis-CI: ![Travis-CI build status](https://travis-ci.org/Unicon/shib-cas-authn3.png)

Software Requirements
-------------------------------------------------------------
* This plugin will require Shibboleth Identity Provider v3.X.

Installation
---------------------------------------------------------------
> Instructions for building from source can be found in the wiki <>.

The first step is to update your Shib idp deployment with the `CasCallbackServlet`. This can be done prior to building/deploying the idp war file or
if preferred, after the build, the files can be modified/updated in the war file before deploying to Tomcat. Previous instructions
were based on the idea that the files would be modified post-deployment. The recommended installation/deployment of the Shib idp suggest 
not exploding the Shib war, so these instructions assume you will modify the files ahead of time. 

#### Overview

1. Copy the Spring Webflow files into the IDP_HOME.
1. Update the IdP's `web.xml`.
1. Update the IdP's `idp.properties` file.
1. Update the IdP's `general-authn.xml` file.
1. Copy the libraries/jars.

#### Copy the Spring Webflow files into the IDP_HOME
Copy the two xml files from the IDP_HOME directory (in the src tree) to the corresponding layout in your Shibboleth IdP home directory.

#### Update the IdP's `web.xml`
Add the IDP External Authn Callback Servlet entry in `idp/WEB-INF/web.xml`

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

1. Set the `idp.authn.flows` to `ShibCas`. Or, for advance cases, add `ShibCas` to the list.
1. Add the additional properties.

```
...
# Regular expression matching login flows to enable, e.g. IPAddress|Password
#idp.authn.flows= Password
idp.authn.flows= Shibcas

# CAS Client properties (usage loosely matches that of the Java CAS Client)
## CAS Server Properties
shibcas.casServerUrlPrefix= https://cassserver.example.edu/cas
shibcas.casServerLoginUrl= ${shibcas.casServerUrlPrefix}/login

## Shibboleth Server Properties
shibcas.serverName= https://shibserver.exampler.edu

# By default you always get the AuthenticatedNameTranslator, add additional code to cover your custom needs.
# Takes a comma separated list of fully qualified class names
# shibcas.casToShibTranslators= com.your.institution.MyCustomNamedTranslatorClass

...
```

#### Update the IdP's `general-authn.xml` file.
Register the module with the IdP:

```xml
...
    <util:list id="shibboleth.AvailableAuthenticationFlows">

        <bean id="authn/Shibcas" parent="shibboleth.AuthenticationFlow"
                p:passiveAuthenticationSupported="true"
                p:forcedAuthenticationSupported="true"
                p:nonBrowserSupported="false" />
...
```

#### Copy the libraries/jars
Both the shib-cas-authn and cas client library are required. You can download them directly (vs building from source).
- <https://github.com/Unicon/shib-cas-authn3/releases/download/v3.0.0/shib-cas-authnenticator3-3.0.0.jar>
- <http://central.maven.org/maven2/org/jasig/cas/client/cas-client-core/3.3.3/cas-client-core-3.3.3.jar>

> These links are here for demonstration purposes. Please check <https://github.com/Unicon/shib-cas-authn3/releases/latest> and <http://central.maven.org/maven2/org/jasig/cas/client/cas-client-core> for more up-to-date versions.

Shibboleth IdP Upgrades
-------------------------------------------------------------
In order to properly protect the changes to the `web.xml` file of the Shibboleth IdP between upgrades, 
copy the changed version to the `conf` directory of the main Shib IdP directory (e.g. usually `/opt/shibboleth-idp/conf`).
Then, rebuild and redeploy the IdP as usual.

See the following links for additional info:
* https://wiki.shibboleth.net/confluence/display/SHIB2/IdPEnableECP
* https://wiki.shibboleth.net/confluence/display/SHIB2/IdPInstall [section: `Using a customized web.xml`)

Shibboleth SP Apache Configuration
-------------------------------------------------------------
> It hasn't been confirmed that this is required to function in IdP v3.0

* Ensure that the following command is set:
`ShibRequestSetting authnContextClassRef urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified`

New Features 3.0
-------------------------------------------------------------
Support for IdP version 3.0.

Release Notes
-------------------------------------------------------------
v2.0.1
* Re-ordered the parameters sent to CAS. The original ordering meant that parameters would be added to the end of the params (thus looking like they were part of the callback service url). renew and/or gateway should be added first, followed by any additional built parameters, and concluded with the callback service url).

v2.0.2
* Fixed a bug where if the net.unicon.idp.authn.provider.extra.EntityIdParameterBuilder was manually added (this Builder is in the code by default), or added multiple times, the EntityId parameter would appear in the request multiple times.
* Updated the architecture to support developers writing their own extension to a new interface: CasToShibTranslator. This allows custom translation of CAS information for use in Shib. By default, the code will use the standard AuthenticatedNameTranslator which hands off the principal name (only) to Shib. Developers can add additional logic by implementing the net.unicon.idp.externalauth.CasToShibTranslator interface and then adding their class to the configuration thusly:
```
# Takes a comma separated list of fully qualified class names
casToShibTranslators=com.your.institution.MyCustomNamedTranslatorClass
```
v2.0.3
* Fixed a bug where the servlet init-params were not being read correctly.
* CAS login handler now implicitly supports both forced and passive authentication.

2.0.4
* Fixed a bug where the login handler wasn't properly reading whether to force authentication or whether passive (renew and gateway) should be passed to CAS. Previously the code was attempting to read this directly from the request parameters. Now the code is grabbing the login context set by Shib and asking directly.

3.0.0
* Support for IdP v3.0.0
* URL encode the `service` querystring parameter during redirection to CAS Server
* URL encode the `entityID`  querystring parameter during redirection to CAS Server
