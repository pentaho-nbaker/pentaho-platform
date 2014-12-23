package org.pentaho.platform.repository2.unified.jcr;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.platform.api.mt.ITenant;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.RepositoryFileAcl;
import org.pentaho.platform.api.repository2.unified.RepositoryFilePermission;
import org.pentaho.platform.api.repository2.unified.RepositoryFileSid;
import org.pentaho.platform.engine.core.system.TenantUtils;
import org.pentaho.platform.repository2.ClientRepositoryPaths;
import org.pentaho.platform.repository2.unified.DefaultUnifiedRepositoryBase;
import org.pentaho.platform.repository2.unified.ServerRepositoryPaths;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.EnumSet;

import static org.junit.Assert.*;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath:/repository.spring.xml",
  "classpath:/repository-test-override.spring.xml" } )
public class JcrAclNodeHelperTest extends DefaultUnifiedRepositoryBase {

  private static final String DS_NAME = "test.txt";

  private JcrAclNodeHelper helper;
  private ITenant defaultTenant;
  private RepositoryFile targetFile;

  @Before
  public void setUp() throws Exception {
    super.setUp();

    createUsers();
    ensurePublicExists();
    targetFile = createSampleFile( "public", "test.txt", "test", true, 1 );

    helper = new JcrAclNodeHelper( repo, ClientRepositoryPaths.getPublicFolderPath() );
  }

  private void createUsers() {
    loginAsSysTenantAdmin();

    defaultTenant = tenantManager.createTenant( systemTenant, TenantUtils.getDefaultTenant(), tenantAdminRoleName,
      tenantAuthenticatedRoleName, ANONYMOUS_ROLE_NAME );

    createUser( defaultTenant, singleTenantAdminUserName, PASSWORD, tenantAdminRoleName );
    createUser( defaultTenant, USERNAME_SUZY, PASSWORD, tenantAuthenticatedRoleName );
    createUser( defaultTenant, USERNAME_TIFFANY, PASSWORD, tenantAuthenticatedRoleName );

    logout();
  }

  private void ensurePublicExists() {
    ensureFolderExists( ClientRepositoryPaths.getPublicFolderPath() );
  }

  @After
  public void tearDown() throws Exception {
    loginAsSysTenantAdmin();

    ITenant defaultTenant = tenantManager.getTenant( "/" + ServerRepositoryPaths.getPentahoRootFolderName() + "/"
      + TenantUtils.getDefaultTenant() );
    if ( defaultTenant != null ) {
      cleanupUserAndRoles( defaultTenant );
    }

    super.tearDown();
  }


  @Test
  public void jcrAclNodeHelperDefaultLocation() {
    loginAsSysTenantAdmin();

    helper = new JcrAclNodeHelper( repo, null );
    assertEquals( helper.getAclNodeFolder(), ServerRepositoryPaths.getAclNodeFolderPath() );
  }


  @Test
  public void visibleForEveryOne() {
    assertTrue( helper.hasAccess( targetFile, USERNAME_SUZY, EnumSet.of( RepositoryFilePermission.READ ) ) );
  }


  @Test
  public void suzyHasAccess() {
    makeDsPrivate();

    assertTrue( helper.hasAccess( targetFile, USERNAME_SUZY, EnumSet.of( RepositoryFilePermission.READ )) );
  }

  @Test
  public void tiffanyHasNoAccess() {
    makeDsPrivate();
    assertFalse( helper.hasAccess( targetFile, USERNAME_TIFFANY, EnumSet.of( RepositoryFilePermission.READ )) );
  }


  @Test
  public void publish() {
    makeDsPrivate();
    helper.removeAclFor( targetFile );

    loginAsTiffany();
    assertTrue( helper.hasAccess( targetFile, USERNAME_TIFFANY, EnumSet.of( RepositoryFilePermission.READ )) );
  }

  @Test
  public void aclIsReplaced() {
    loginAsRepositoryAdmin();
    RepositoryFileAcl acl = createAclFor( USERNAME_SUZY );
    helper.setAclFor( targetFile, acl );

    loginAsSuzy();
    assertTrue( helper.hasAccess( targetFile, USERNAME_SUZY, EnumSet.of( RepositoryFilePermission.READ )) );

    loginAsTiffany();
    assertFalse( helper.hasAccess( targetFile, USERNAME_TIFFANY, EnumSet.of( RepositoryFilePermission.READ )));

    loginAsRepositoryAdmin();
    acl = createAclFor( USERNAME_TIFFANY );
    helper.setAclFor( targetFile, acl );

    loginAsTiffany();
    assertTrue( helper.hasAccess( targetFile, USERNAME_TIFFANY, EnumSet.of( RepositoryFilePermission.READ )) );
  }


  @Test
  public void aclNodeIsCreated() {
    makeDsPrivate();

    loginAsSuzy();
    assertTrue( "No ACL node was created", repo.getReferrers( targetFile.getId() ).size() > 0 );
  }

  @Test
  public void aclNodeIsRemoved() {
    makeDsPrivate();

    loginAsRepositoryAdmin();
    helper.setAclFor( targetFile, null );
    assertTrue( "Referrers should be null after ACL delete", repo.getReferrers( targetFile.getId() ).size() == 0 );
  }


  private void makeDsPrivate() {
    loginAsRepositoryAdmin();
    RepositoryFileAcl acl = createAclFor( USERNAME_SUZY );
    helper.setAclFor( targetFile, acl );
  }

  private static RepositoryFileAcl createAclFor( String user ) {
    RepositoryFileSid userSid = new RepositoryFileSid( user, RepositoryFileSid.Type.USER );
    return new RepositoryFileAcl.Builder( user )
      .ace( userSid, EnumSet.of( RepositoryFilePermission.ALL ) )
      .entriesInheriting( false )
      .build();
  }


  private RepositoryFile ensureFolderExists( String folderName ) {
    loginAsRepositoryAdmin();
    try {
      RepositoryFile folder = repo.getFile( folderName );
      if ( folder == null ) {
        folder = repo.createFolder( repo.getFile( "/" ).getId(), new RepositoryFile.Builder( folderName ).
          folder( true ).build(), "" );
      }
      return folder;
    } finally {
      logout();
    }
  }

  private void loginAsSuzy() {
    login( USERNAME_SUZY, defaultTenant, new String[] { tenantAuthenticatedRoleName } );
  }

  private void loginAsTiffany() {
    login( USERNAME_TIFFANY, defaultTenant, new String[] { tenantAuthenticatedRoleName } );
  }
}
