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
1. Extract the latest release of `shib-cas-authn3` into your IDP_HOME
1. Optionally define the servlet for receiving a callback from CAS
1. Update the IdP's `idp.properties` file
1. Update the IdP's `general-authn.xml` file
1. Rebuild the WAR file

#### Extract the latest release of `shib-cas-authn3` into your IDP_HOME
1. Download and extract the compressed contents of the latest release of `shib-cas-authn3` at https://github.com/Unicon/shib-cas-authn3/releases
1. Copy the extracted folders `edit-webapp` and `flows` into your IDP_HOME.

#### Optionally define the servlet for receiving a callback from CAS
> The servlet will register itself with the container when running under a Servlet 3.0 compliant container (such as Jetty 9).
This step is provided for legacy reasons.

Add the ShibCas Auth Servlet entry in `IDP_HOME/edit-webapp/WEB-INF/web.xml`. If this file does not exist, it may be copied from `IDP_HOME/webapp/WEB-INF/web.xml`.

Example snippet `web.xml`:

```xml
...
    <!-- Servlet for receiving a callback from an external CAS Server and continues the IdP login flow -->
    <servlet>
        <servlet-name>ShibcasAuthServlet</servlet-name>
        <servlet-class>net.unicon.idp.externalauth.ShibcasAuthServlet</servlet-class>
        <load-on-startup>2</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>ShibcasAuthServlet</servlet-name>
        <url-pattern>/Authn/ExtCas/*</url-pattern>
    </servlet-mapping>
...
```

#### Update the IdP's `idp.properties` file

1. Set the `idp.authn.flows` to `Shibcas`. Or, for advance cases, add `Shibcas` to the list.
1. Add the additional properties.

```
...
# Regular expression matching login flows to enable, e.g. IPAddress|Password
#idp.authn.flows = Password
idp.authn.flows = Shibcas

# CAS Client properties (usage loosely matches that of the Java CAS Client)
## CAS Server Properties
shibcas.casServerUrlPrefix = https://cas.example.edu/cas
shibcas.casServerLoginUrl = ${shibcas.casServerUrlPrefix}/login

## Shibboleth Server Properties
shibcas.serverName = https://idp.example.edu

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

#### Update the IdP's `general-authn.xml` file
Register the module with the IdP by adding the `authn/Shibcas` bean in `IDP_HOME/conf/authn/general-authn.xml`:

```xml
...
    <util:list id="shibboleth.AvailableAuthenticationFlows">

        <bean id="authn/Shibcas" parent="shibboleth.AuthenticationFlow"
                p:passiveAuthenticationSupported="true"
                p:forcedAuthenticationSupported="true"
                p:nonBrowserSupported="false" />
        ...
        </util:list>
...
```

#### Rebuild the WAR file
From the `IDP_HOME/bin` directory, run `./build.sh` or `build.bat` to rebuild the `idp.war`. Redeploy and restart the service if necessary.

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
This project includes a Docker environment to assist with development/testing. 

To build and execute: `./gradlew clean; ./gradlew up`
Then browse to: `https://idptestbed/idp/profile/SAML2/Unsolicited/SSO?providerId=https://sp.idptestbed/shibboleth`

> You'll need a `hosts` file entry that points `idptestbed` to your Docker server's IP address. 

The IdP only had a session of 1 minute (to test expired session/conversation key issues), so login into CAS Server quickly.


##### Troubleshooting
If you do not already have Docker Compose, please refer to [Getting Started with Docker Compose](https://docs.docker.com/compose/gettingstarted/).

For historic purposes, refer to the [Build Instructions](https://github.com/Unicon/shib-cas-authn2#to-build) for `shib-cas-authn2`.

If you are having problems building this project due to Docker misconfiguration, try building from the `v3.2.0` tag instead:

````
> git clone https://github.com/Unicon/shib-cas-authn3.git
> cd shib-cas-authn3
> git checkout tags/v3.2.0

> .\gradlew
...
:processTestResources UP-TO-DATE
:testClasses
:test
:check
:build

BUILD SUCCESSFUL

Total time: 23.207 secs
````

If you are missing the `cas-client-core-*.jar` file, see [`Releases`](https://github.com/Unicon/shib-cas-authn3/releases).
