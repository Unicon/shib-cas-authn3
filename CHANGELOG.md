
# 1.3.0.1

* Fixes critical security vulnerability in 1.3.0.1, by
* Changes strategy for sharing state between CAS authenticated resource and IdP from passing state via URL parameter to passing state via session-id-keyed attribute in the IdP's `ServletContext` via a cross-context access by the CASified resource context.

# 1.3.0

* Initial tagged release.
* Includes critical security vulnerability.  Do not use this version.
