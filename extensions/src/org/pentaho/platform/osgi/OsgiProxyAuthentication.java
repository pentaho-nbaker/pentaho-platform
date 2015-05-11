package org.pentaho.platform.osgi;

import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by nbaker on 4/20/15.
 */
public class OsgiProxyAuthentication implements Authentication {


  private Object target;

  public OsgiProxyAuthentication( Object target ){
    this.target = target;
  }

  @Override public GrantedAuthority[] getAuthorities() {

    Collection authoritiesFromOsgi = (Collection) callMethod( "getAuthorities" ); // will be a collection
    int len = authoritiesFromOsgi.size();
    GrantedAuthority[] auths = new GrantedAuthority[len];

    Iterator iterator = authoritiesFromOsgi.iterator();
    int i=0;
    while ( iterator.hasNext() ) {
      Object auth = iterator.next();
      Method getAuthority = ReflectionUtils.findMethod( auth.getClass(), "getAuthority" );
      String grantedAuth = ReflectionUtils.invokeMethod( getAuthority, auth ).toString();
      GrantedAuthority authImpl = new GrantedAuthorityImpl( grantedAuth );
      auths[i++] = authImpl;
    }
    return auths;

  }

  private Object callMethod( String methodName ){
    Method method = ReflectionUtils.findMethod( target.getClass(), methodName );
    return ReflectionUtils.invokeMethod( method, target );
  }

  @Override public Object getCredentials() {
    return callMethod( "getCredentials" );
  }

  @Override public Object getDetails() {
    return callMethod( "getDetails" );
  }

  @Override public Object getPrincipal() {
    return callMethod( "getPrincipal" );
  }

  @Override public boolean isAuthenticated() {
    return true;
  }

  @Override public void setAuthenticated( boolean b ) throws IllegalArgumentException {
    // we're always authenticated here.
  }

  @Override public String getName() {
    return callMethod( "getName" ).toString();
  }
}
