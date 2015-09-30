/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 2 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright 2006 - 2015 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.platform.repository2.unified.jcr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.management.DataStoreGarbageCollector;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.springframework.extensions.jcr.JcrCallback;
import org.springframework.extensions.jcr.JcrTemplate;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.io.IOException;

/**
 * This class provides a static method {@linkplain #gc()} for running JCR's GC routine.
 *
 * @author Andrey Khayrutdinov
 */
public class RepositoryCleaner {

  private static final Log logger = LogFactory.getLog( RepositoryCleaner.class );

  public static synchronized void gc() {
    Repository jcrRepository = PentahoSystem.get( Repository.class, "jcrRepository", null );
    if ( jcrRepository == null ) {
      logger.error( "Cannot obtain JCR repository. Exiting" );
      return;
    }

    if ( !( jcrRepository instanceof RepositoryImpl ) ) {
      logger.error(
        String.format( "Expected RepositoryImpl, but got: [%s]. Exiting", jcrRepository.getClass().getName() ) );
      return;
    }

    RepositoryImpl repository = (RepositoryImpl) jcrRepository;
    try {
      logger.info( "Creating garbage collector" );
      // JCR's documentation recommends not to use RepositoryImpl.createDataStoreGarbageCollector() and
      // instead invoke RepositoryManager.createDataStoreGarbageCollector()
      // (see it here: http://wiki.apache.org/jackrabbit/DataStore#Data_Store_Garbage_Collection)

      // However, the example from the wiki cannot be applied directly, because
      // RepositoryFactoryImpl accepts only TransientRepository's instances that were created by itself;
      // it creates such instance in "not started" state, and when the instance tries to start, it fails,
      // because Pentaho's JCR repository is already running.

      DataStoreGarbageCollector gc = repository.createDataStoreGarbageCollector();
      try {
//        gc.setPersistenceManagerScan( false );
        logger.debug( "Starting marking stage" );
        gc.mark();
        logger.debug( "Starting sweeping stage" );
        int deleted = gc.sweep();
        logger.info( String.format( "Garbage collecting completed. %d items were deleted", deleted ) );
      } finally {
        gc.close();
      }

    } catch ( RepositoryException e ) {
      logger.error( "Error during garbage collecting", e );
    }
    JcrTemplate jcrTemplate = PentahoSystem.get( JcrTemplate.class, "jcrTemplate", null );
    jcrTemplate.execute( new JcrCallback() {
      @Override public Object doInJcr( Session session ) throws IOException, RepositoryException {
        Node node = session.getNode( "/jcr:system/jcr:versionStorage" );
        findVersionNodesAndPurge( node, session );
        return null;
      }
    } );

  }

  private static void findVersionNodesAndPurge( Node node, Session session ) {
    try {
      if ( node.getName().equals( "jcr:frozenNode" ) && !node.getParent().getName().equals("jcr:rootVersion") ) {
        // Version Node
        if( node.hasProperty( "jcr:frozenUuid" ) ){
          Property property = node.getProperty( "jcr:frozenUuid" );
          Value uuid = property.getValue();
          Node nodeByIdentifier = null;
          try {
            nodeByIdentifier = session.getNodeByIdentifier( uuid.getString() );
          } catch( RepositoryException ex ){
            // ignored
          }
          if ( nodeByIdentifier == null ) {
            // node is gone
            System.out.println( "Removed orphan version: " + nodeByIdentifier.getPath() + " From: " + node.getPath() );
            session.getWorkspace().getVersionManager().getVersionHistory( uuid.toString() );
            node.remove();
          } else {
            System.out.println( "Not orphaned: " + nodeByIdentifier.getPath() + " From: " + node.getPath() );
          }
        }

      }
    } catch (RepositoryException e){
      //      e.printStackTrace();
      // ignored
    }

    NodeIterator nodes = null;
    try {
      nodes = node.getNodes();
    } catch ( RepositoryException e ) {
      //      e.printStackTrace();
      return;
    }
    while ( nodes.hasNext() ) {
      findVersionNodesAndPurge( nodes.nextNode(), session );
    }
  }
}
