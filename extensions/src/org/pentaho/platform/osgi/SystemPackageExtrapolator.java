package org.pentaho.platform.osgi;

import org.apache.commons.lang.StringUtils;
import org.jboss.modules.ModuleClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by nbaker on 2/4/15.
 */
public class SystemPackageExtrapolator {

  public static final String ORG_OSGI_FRAMEWORK_SYSTEM_PACKAGES_EXTRA = "org.osgi.framework.system.packages.extra";
  Logger logger = LoggerFactory.getLogger( getClass() );

  private List<PackageProvider> providers = new ArrayList<>();

  public SystemPackageExtrapolator() {
    providers.add( new JBossModulePackageProvider() );
    providers.add( new UrlClassLoaderPackageProvider() );
  }

  public SystemPackageExtrapolator(
      List<PackageProvider> providers ) {
    this.providers = providers;
  }

  private interface PackageProvider {
    Set<String> getPackages();
  }

  private class JBossModulePackageProvider implements PackageProvider {
    @Override public Set<String> getPackages() {

      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      Set<String> packages = new HashSet<String>();
      if ( classLoader.getClass().getName().equals( "org.jboss.modules.ModuleClassLoader" ) ) {

        Set<String> exportedPaths = ( (ModuleClassLoader) classLoader ).getModule().getExportedPaths();
        for ( String exportedPath : exportedPaths ) {
          packages.add( exportedPath.replaceAll( "/", "." ) );
        }
      }
      return packages;
    }
  }

  private class UrlClassLoaderPackageProvider implements PackageProvider {
    @Override public Set<String> getPackages() {
      Set<String> packages = new HashSet<String>();
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      do {
        if ( !URLClassLoader.class.isAssignableFrom( classLoader.getClass() ) ) {
          continue;
        }
        URL[] urLs = ( (URLClassLoader) classLoader ).getURLs();
        for ( URL url : urLs ) {
          try {
            String fileName = URLDecoder.decode( url.getFile() );
            File file = new File( fileName );
            if ( !file.exists() || file.isDirectory() ) {
              continue;
            }
            JarFile jarFile = new JarFile( file );
            Enumeration<JarEntry> entries = jarFile.entries();
            while ( entries.hasMoreElements() ) {
              JarEntry jarEntry = entries.nextElement();
              String name = jarEntry.getName();

              if ( jarEntry.isDirectory() ) {
                packages.add( name.replaceAll( "\\/", "." ).substring( 0, name.length() - 1 ) );
              }
            }
          } catch ( IOException e ) {
            logger.debug( "Error procesing jar for packages", e );
          }
        }
      } while ( ( classLoader = classLoader.getParent() ) != null );

      return packages;
    }
  }


  public Properties expandProperties( Properties properties ) {

    Set<String> packages = new HashSet<String>();
    for ( PackageProvider provider : providers ) {
      packages.addAll( provider.getPackages() );
    }

    String packagesImports = properties.getProperty( ORG_OSGI_FRAMEWORK_SYSTEM_PACKAGES_EXTRA );
    String[] split = packagesImports.split( "," );

    String[] expanded = expandPackages( split, packages.toArray( new String[packages.size()] ) );

    properties.setProperty( ORG_OSGI_FRAMEWORK_SYSTEM_PACKAGES_EXTRA, StringUtils.join( expanded, "," ) );
    return properties;
  }

  private String[] expandPackages( String[] packages, String[] availablePackages ) {
    Set<String> qualifiedPackages = new HashSet<String>();
    Set<String> qualifiedPackagesWhole = new HashSet<String>();
    Set<String> imports = new HashSet<String>();

    for ( String pack : packages ) {
      pack = pack.trim();
      if ( pack.indexOf( ";" ) > 0 && pack.indexOf( "*" ) == -1 ) {
        qualifiedPackages.add( pack.substring( 0, pack.indexOf( ";" ) ) );
        qualifiedPackagesWhole.add( pack );
      }
    }
    for ( String pack : packages ) {
      if ( StringUtils.isNotEmpty( pack ) && pack.contains( ".*" ) ) {
        // expand out
        String basePackage = pack.substring( 0, pack.indexOf( "*" ) ).trim(); // including "."
        String predicate = pack.substring( pack.indexOf( "*" ) + 1 );
        for ( String aPackage : availablePackages ) {
          if ( aPackage.startsWith( basePackage ) && !qualifiedPackages.contains( aPackage ) ) {
            imports.add( aPackage + predicate );
          }
        }
        imports.add( basePackage.substring( 0, basePackage.length() - 1 ) + predicate );
      } else {
        imports.add( pack );
      }
    }
    imports.addAll( qualifiedPackagesWhole );
    return imports.toArray( new String[ imports.size() ] );
  }
}
