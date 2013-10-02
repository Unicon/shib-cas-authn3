# What is this?

Version `1.3.0.1` of `shib-cas-authenticator` is a **critical security fix**.

Existing `shib-cas-authenticator` adopters can patch their existing deployed usages following these instructions.

# Patch files

In the `patches` directory, pre-built binary files in support of patching are provided.

* `CasAuthenticatorResource.class`
* `idp-cas-invoker-1.3.0.1.jar`

# What versions need the fix?

**All** versions of `shib-cas-authenticator` prior to `1.3.0.1` **must** be fixed to avoid a serious security vulnerability.

# How do I tell if my environment is affected?

Your Shibboleth IdP may not be affected: while all `shib-cas-authenticator` adopters are affected, not all Shibboleth IdPs are also `shib-cas-authenticator` adopters -- in fact, most aren't.

This section steps through all the things that must be true for your Shibboleth IdP to be affected.

## Shibboleth IdP

The vulnerable software is a Unicon-developed plugin for the Shibboleth IdP.  If you're not using the Shibboleth IdP, you're not affected.

## Shibboleth IdP relying on CAS for user login

The vulnerable software is a Shibboleth IdP plugin for making the IdP rely upon CAS for the user login experience and single sign-on session.  If you're not using CAS as the way users log in to your Shibboleth IdP, you're not affected.

## Using `ExternalAuthn`

There are a few ways to configure a Shibboleth IdP to rely upon a  CAS server for the user login experience.  Some of those use `RemoteUser` -- and those ways aren't affected.  Only `ExternalAuthn` integrations are affected.

So.  In your Shibboleth IdP's `handlers.xml`, if the `ExternalAuthn` `LoginHandler` is uncommented, you might be affected.


```xml
<ph:LoginHandler xsi:type="ph:ExternalAuthn" externalAuthnPath="/authn/external" supportsForcedAuthentication="true">
  <ph:AuthenticationMethod>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</ph:AuthenticationMethod>
</ph:LoginHandler>
```   

However, if the `ExternalAuthn` handler is commented out, and you're instead using perhaps `RemoteUser`, you're not affected.

```xml
<ph:LoginHandler xsi:type="ph:RemoteUser">
  <ph:AuthenticationMethod>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</ph:AuthenticationMethod>
</ph:LoginHandler>
```

## Using Unicon's implementation of ExternalAuthn

In your Shibboleth IdP's `web.xml`, if this servlet is included, you're definitely affected by the vulnerability:

    
    <servlet>
      <servlet-name>External Authn</servlet-name>
      <servlet-class>net.unicon.idp.externalauth.CasInvokerServlet</servlet-class>
      <init-param>
      ....
    </servlet>
    

## Using an affected version

Finally, **all versions of `shib-cas-authenticator` earlier than `1.3.0.1`** are affected.  If you're upgraded to `1.3.0.1`, you are not affected by this vulnerability.

#  What to do

## 0. Back up your current IdP and configuration

Of course, you should always make backup copies of anything important that you're going to change.  Your IdP is important, and you're going to be changing it.  Back it up first.  Refamiliarize yourself with your routine backups and perhaps do some ad-hoc copying for extra backup copies of files to be adjusted:

Make backup copies of:

* Tomcat's `context.xml` (as referenced in step 1 below)
* Tomcat's `server.xml` (as referenced in step 1 below)
* The prior `CasAuthenticatorResource.class` (as referenced in step 2 below)
* The prior `idp-cas-invoker-something.jar` (as referenced in step 3 below)
* your entire Shibboleth installer directory, which is something like `/opt/shibboleth-identityprovider-2.4.0`
* your entire Shibboleth installed directory, which is something like `/opt/shibboleth-idp`

## 1. Configure IdP Tomcat 

This solution requires the Servlet Container (Tomcat) to be configured such that

1. Cross-context access is enabled (the `casauth` application can write to the IdP's `ServletContext`.
2. The `casauth` application and the IdP share session identifiers.  They won't share actual servlet sessions, but the session identifiers must be the same, and the way this is accomplished is by configuring the Tomcat `JSESSIONID` cookie path to encompass both applications.
3. The `casauth` application and the IdP are deployed into the same servlet container.  This will already be the case in almost all `shib-cas-authenticator` environments, but if you happened to have separated the deployments, you'll have to combine them for this `1.3.0.1` version to work.



### In Tomcat 6.x

Many IdP implementations are using Tomcat 6.


* Enable Tomcat's *crossContext* in `$CATALINA_HOME/conf/context.xml`

```xml
<Context crossContext="true">
     ...
</Context>
```

* Enable Tomcat's SSL Connector's *emptySessionPath* in `$CATALINA_HOME/conf/server.xml`

```xml
 <Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true" emptySessionPath="true" .../>
```


### In Tomcat 7.x

A few IdP implementations are using Tomcat 7.

Configuration for Tomcat 7 is similar -- you configure `crossContext` in the same way, but the session cookie path configuration is different.

* Enable Tomcat's *crossContext* in `$CATALINA_HOME/conf/context.xml`, just like for Tomcat 6: 

```xml
<Context crossContext="true">
     ...
</Context>
```

* Set *sessionCookiePath* to `/` on that same `Context` element in that same `context.xml` file:

```xml
<Context crossContext="true" sessionCookiePath="/">
     ...
</Context>
```




## 2. Replace `CasAuthenticatorResource.class`

Install the new `CasAuthenticatorResource.class` in `$CATALINA_HOME/webapps/casauth/WEB-INF/classes/net/unicon/idp/casauth/`, replacing the old one.

    cp CasAuthenticatorResource.class /opt/idp-tomcat-6.0.36/webapps/casauth/WEB-INF/classes/net/unicon/idp/casauth/CasAuthenticatorResource.class


## 3. Remove `idp-cas-invoker-1.3.jar`

Remove `/opt/shibboleth-identityprovider-2.4.0/lib/idp-cas-invoker-1.3.jar`

(Note: if you're upgrading from a different version, then your version numbers may differ, e.g. `idp-cas-invoker-1.2.jar` instead.  In any case, all prior versions of the `idp-cas-invoker` `.jar` should be removed.)


## 4. Install `idp-cas-invoker-1.3.0.1.jar`

Copy `idp-cas-invoker-1.3.0.1.jar` to  `/opt/shibboleth-identityprovider-2.4.0/lib`


## 5. Run the Shibboleth IdP installer

Run `/opt/shibboleth-identityprovider-2.4.0/install.sh` preserving configuration.


## 6. Restart Tomcat


# How to get help

If these instructions prove problematic or you otherwise seek assistance applying this security patch, please contact Unicon privately.  Unicon Open Source Support subscribers are encouraged to open a support ticket.  Unicon clients should contact their account representative.  All others, please use http://www.unicon.net/contact.
