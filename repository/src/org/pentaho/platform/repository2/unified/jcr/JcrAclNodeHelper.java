package org.pentaho.platform.repository2.unified.jcr;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.repository2.unified.IAclNodeHelper;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.RepositoryFileAcl;
import org.pentaho.platform.api.repository2.unified.RepositoryFilePermission;
import org.pentaho.platform.api.repository2.unified.data.node.DataNode;
import org.pentaho.platform.api.repository2.unified.data.node.DataNodeRef;
import org.pentaho.platform.api.repository2.unified.data.node.NodeRepositoryFileData;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.repository.messages.Messages;
import org.pentaho.platform.repository2.unified.ServerRepositoryPaths;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.pentaho.platform.repository.RepositoryFilenameUtils.normalize;

/**
 * @author Andrey Khayrutdinov
 */
public class JcrAclNodeHelper implements IAclNodeHelper {
  private static final Log logger = LogFactory.getLog( JcrAclNodeHelper.class );

  private static final String IS_ACL_NODE = "is_acl_node";
  private static final String TARGET = "target";

  private final IUnifiedRepository unifiedRepository;
  private final String aclNodeFolder;

  public JcrAclNodeHelper( IUnifiedRepository unifiedRepository,
                           String aclNodeFolder ) {
    this.unifiedRepository = unifiedRepository;
    this.aclNodeFolder = StringUtils.defaultIfEmpty( aclNodeFolder, ServerRepositoryPaths.getAclNodeFolderPath() );
  }

  private RepositoryFile getAclNodeRepositoryFolder() {
    RepositoryFile folder;
    try {
      folder = unifiedRepository.getFile( getAclNodeFolder() );
    } catch ( Exception t ) {
      logger.error( Messages.getInstance().getString( "AclNodeHelper.ERROR_0001_ROOT_FOLDER_NOT_AVAILABLE",
          aclNodeFolder, ServerRepositoryPaths.getAclNodeFolderPath() ) );
      folder = unifiedRepository.getFile( ServerRepositoryPaths.getAclNodeFolderPath() );
    }
    return folder;
  }

  private boolean hasAclNode( RepositoryFile file ) {
    List<RepositoryFile> referrers = unifiedRepository.getReferrers( file.getId() );
    return referrers != null && referrers.size() > 0;
  }

  private RepositoryFile getAclNode( RepositoryFile file ) {
    List<RepositoryFile> referrers = unifiedRepository.getReferrers( file.getId() );

    int i = referrers.size();
    while ( i-- > 0 ) {
      RepositoryFile referrer = referrers.get( i );
      NodeRepositoryFileData dataForRead =
          unifiedRepository.getDataForRead( referrer.getId(), NodeRepositoryFileData.class );
      if ( dataForRead != null && dataForRead.getNode().hasProperty( IS_ACL_NODE ) ) {
        return referrer;
      }
    }
    return null;

  }

  @Override public boolean hasAccess( final RepositoryFile repositoryFile, final String principal,
                                      final EnumSet<RepositoryFilePermission> permissions ) {

    boolean hasAclNode = hasAclNode( repositoryFile );
    if ( !hasAclNode ) {
      return true;
    }

    try {
      return SecurityHelper.getInstance().runAsUser( principal, new Callable<Boolean>() {
        @Override public Boolean call() throws Exception {
          return unifiedRepository.hasAccess( repositoryFile.getPath(), permissions );
        }
      } );
    } catch ( Exception e ) {
      logger.error( "Error checking access for " + repositoryFile.getPath(), e );
      return true;
    }

  }

  @Override public boolean hasAccess( RepositoryFile repositoryFile, EnumSet<RepositoryFilePermission> permissions ) {
    return hasAccess( repositoryFile, PentahoSessionHolder.getSession().getName(), permissions );
  }

  /**
   * {@inheritDoc}
   */
  @Override public RepositoryFileAcl getAclFor( final RepositoryFile repositoryFile ) {

    boolean hasAclNode = hasAclNode( repositoryFile );
    if ( !hasAclNode ) {
      return null;
    }

    return unifiedRepository.getAcl( getAclNode( repositoryFile ).getId() );

  }

  /**
   * {@inheritDoc}
   */
  @Override public void setAclFor( RepositoryFile fileToAddAclFor, RepositoryFileAcl acl ) {

    RepositoryFile aclNode = getAclNode( fileToAddAclFor );

    if ( acl == null ) {
      if ( aclNode != null ) {
        unifiedRepository.deleteFile( aclNode.getId(), true,
            Messages.getInstance().getString( "AclNodeHelper.WARN_0001_REMOVE_ACL_NODE", aclNode.getPath() ) );
      }
      // ignore if no ACL node is present.
    } else {
      if ( aclNode == null ) {
        // Create ACL Node with reference to given file.
        aclNode = createAclNode( fileToAddAclFor );
      }
      // Update ACL on file.
      RepositoryFileAcl existing = unifiedRepository.getAcl( aclNode.getId() );
      RepositoryFileAcl updated = new RepositoryFileAcl.Builder( existing ).aces( acl.getAces() ).build();
      unifiedRepository.updateAcl( updated );
    }
  }

  private RepositoryFile createAclNode( RepositoryFile fileToAddAclFor ) {

    DataNode dataNode = new DataNode( "acl node" );
    DataNodeRef dataNodeRef = new DataNodeRef( fileToAddAclFor.getId() );
    dataNode.setProperty( TARGET, dataNodeRef );
    dataNode.setProperty( IS_ACL_NODE, true );
    NodeRepositoryFileData nodeRepositoryFileData = new NodeRepositoryFileData( dataNode );

    return unifiedRepository.createFile(
        getAclNodeRepositoryFolder(),
        new RepositoryFile.Builder( UUID.randomUUID().toString() ).aclNode( true ).build(),
        nodeRepositoryFileData, ""
    );
  }

  @Override public void removeAclFor( RepositoryFile file ) {
    setAclFor( file, null );
  }

  @Override public String getAclNodeFolder() {
    return aclNodeFolder;
  }
}
