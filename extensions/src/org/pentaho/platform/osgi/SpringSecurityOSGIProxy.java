package org.pentaho.platform.osgi;

import org.pentaho.osgi.api.IAuthenticationProviderProxy;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by nbaker on 4/14/15.
 */
public class SpringSecurityOSGIProxy implements AuthenticationProvider{
  private Logger logger = LoggerFactory.getLogger( getClass() );

  @Override public Authentication authenticate( Authentication authentication ) throws AuthenticationException {
    IAuthenticationProviderProxy authenticationProviderProxy = PentahoSystem.get( IAuthenticationProviderProxy.class );
    if( authenticationProviderProxy == null ){
      logger.debug( "No IAuthenticationProviderProxy implementation found in PentahoSystem" );
      return null;
    }

    Object authenticationFromOsgi = authenticationProviderProxy.authenticate( authentication );
    if( authenticationFromOsgi != null ){
      // we authenticated successfully
      return new OsgiProxyAuthentication( authenticationFromOsgi );
    }
    return null;
  }

  @Override public boolean supports( Class aClass ) {
    return true; // We will attempt to find a Provider for any class
  }
}
