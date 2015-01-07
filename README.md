## A Shibboleth IdP v3.X plugin for authentication via an external CAS Server

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
This minimum supported version of Shibboleth Identity Provider is `3.0.0`

> A Shibboleth IdP v2.X plugin can be found at <https://github.com/Unicon/shib-cas-authn2>.

Installation
---------------------------------------------------------------
> Instructions for building from source can be found in the wiki <>.

The first step is to update your Shib idp deployment with the `CasCallbackServlet`. This can be done prior to building/deploying the idp war file or
if preferred, after the build, the files can be modified/updated in the war file before deploying to Tomcat. Previous instructions
were based on the idea that the files would be modified post-deployment. The recommended installation/deployment of the Shib idp suggest 
not exploding the Shib war, so these instructions assume you will modify the files ahead of time. 

#### Overview

1. Copy the Spring Webflow files into the IDP_HOME.
1. Update the IdP's `web.xml`. (optional)
1. Update the IdP's `idp.properties` file.
1. Update the IdP's `general-authn.xml` file.
1. Copy the libraries/jars.
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

1. Set the `idp.authn.flows` to `ShibCas`. Or, for advance cases, add `ShibCas` to the list.
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

#### Copy the libraries/jars
Both the shib-cas-authn and cas client library are required. You can download them directly (vs building from source):
- <https://github.com/Unicon/shib-cas-authn3/releases/download/v3.0.0/shib-cas-authnenticator3-3.0.0.jar>
- <http://central.maven.org/maven2/org/jasig/cas/client/cas-client-core/3.3.3/cas-client-core-3.3.3.jar>

Copy them to `IDP_HOME/edit-webapp/WEB-INF/lib/`.

> These links are here for demonstration purposes. Please check <https://github.com/Unicon/shib-cas-authn3/releases/latest> and <http://central.maven.org/maven2/org/jasig/cas/client/cas-client-core> for more up-to-date versions.

#### Rebuild the war file
From the `IDP_HOME/bin` directory, run `./build.sh` or `build.bat` to rebuild the `idp.war`. Redeploy if necessary.


Shibboleth SP Apache Configuration
-------------------------------------------------------------
> It hasn't been confirmed that this is required to function in IdP v3.0

* Ensure that the following command is set:
`ShibRequestSetting authnContextClassRef urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified`

New Features 3.0
-------------------------------------------------------------
Support for IdP version 3.0.0

Release Notes
-------------------------------------------------------------

3.0.0
* Support for IdP 3.0.0
* URL encode the `service` querystring parameter during redirection to CAS Server
* URL encode the `entityID`  querystring parameter during redirection to CAS Server
