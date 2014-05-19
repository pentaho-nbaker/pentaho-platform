/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright 2014 Pentaho Corporation. All rights reserved.
 */

package org.pentaho.platform.repository2.unified.fs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.data.simple.SimpleRepositoryFileData;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Created by nbaker on 5/16/14.
 */
public class FileSystemBackedUnifiedRepositoryTest {
  FileSystemBackedUnifiedRepository repo;
  File repoTemp;
  @Before
  public void setup() throws IOException {
    repoTemp = File.createTempFile( "repoTemp", "" + ( Math.random() * 10000 ) );

    repoTemp.delete();
    repoTemp.mkdir();

    repo = new FileSystemBackedUnifiedRepository( repoTemp );
  }
  @After
  public void after(){
    repoTemp.delete();
  }


  @Test
  public void testCreateFile() throws Exception {
    RepositoryFile fileIn = repo.createFile( "", new RepositoryFile.Builder("foo", "some*[]%Name").path( "folder*(!%'\"" ).build(),
      new SimpleRepositoryFileData(new ByteArrayInputStream( "Testing".getBytes( "UTF-8" )), "UTF-8", "text/plain" ), "version 1" );
    RepositoryFile fileOut = repo.getFile( fileIn.getPath() );
    assertEquals("some*[]%Name", fileOut.getName());
  }


  @Test
  public void testCreateFolder() throws Exception {
    RepositoryFile fileIn = repo.createFolder( "", new RepositoryFile.Builder("foo", "some*[]%Name").path( "folder*(!%'\"" ).folder( true ).build(),
       "version 1" );
    RepositoryFile fileOut = repo.getFile( fileIn.getPath() );
    assertEquals("some*[]%Name", fileOut.getName());
    assertTrue( fileOut.isFolder());
  }


  @Test
  public void testDeleteFile() throws Exception {
    RepositoryFile fileIn = repo.createFile( "", new RepositoryFile.Builder("foo", "some*[]%Name").path( "folder*(!%'\"" ).build(),
      new SimpleRepositoryFileData(new ByteArrayInputStream( "Testing".getBytes( "UTF-8" )), "UTF-8", "text/plain" ), "version 1" );
    RepositoryFile fileOut = repo.getFile( fileIn.getPath() );
    assertEquals("some*[]%Name", fileOut.getName());
    repo.deleteFile( fileOut.getId(), "deleting" );
    fileOut = repo.getFile( fileIn.getPath() );
    assertNull( fileOut );
  }


  @Test
  public void testGetChildren() throws Exception {
    RepositoryFile folder = repo.createFolder( "", new RepositoryFile.Builder("foo", "some*[folder]%Name").path( "" ).folder( true ).build(),
      "version 1" );
    RepositoryFile fileIn = repo.createFile( folder.getId(), new RepositoryFile.Builder("foo", "some*[file]%Name").path( "some*[folder]%Name/some*[file]%Name" ).build(),
      new SimpleRepositoryFileData(new ByteArrayInputStream( "Testing".getBytes( "UTF-8" )), "UTF-8", "text/plain" ), "version 1" );
    final List<RepositoryFile> children = repo.getChildren( folder.getId() );
    assertEquals(1, children.size());
  }

  @Test
  public void testGetChildren1() throws Exception {

  }

  @Test
  public void testGetChildren2() throws Exception {

  }

  @Test
  public void testGetChildren3() throws Exception {

  }

  @Test
  public void testGetDataAtVersionForExecute() throws Exception {

  }

  @Test
  public void testGetDataAtVersionForRead() throws Exception {

  }

  @Test
  public void testGetDataForExecute() throws Exception {

  }

  @Test
  public void testGetDataForRead() throws Exception {

  }


  @Test
  public void testGetEffectiveAces() throws Exception {

  }

  @Test
  public void testGetEffectiveAces1() throws Exception {

  }

  @Test
  public void testGetFile() throws Exception {

  }

  @Test
  public void testGetFile1() throws Exception {

  }

  @Test
  public void testGetFileAtVersion() throws Exception {

  }

  @Test
  public void testGetFileById() throws Exception {

  }

  @Test
  public void testGetFileById1() throws Exception {

  }

  @Test
  public void testGetFile2() throws Exception {

  }

  @Test
  public void testGetFileById2() throws Exception {

  }

  @Test
  public void testGetFile3() throws Exception {

  }

  @Test
  public void testGetFileById3() throws Exception {

  }

  @Test
  public void testGetTree() throws Exception {

  }

  @Test
  public void testGetTree1() throws Exception {

  }

  @Test
  public void testGetVersionSummaries() throws Exception {

  }

  @Test
  public void testGetVersionSummary() throws Exception {

  }

  @Test
  public void testSetRootDir() throws Exception {

  }

  @Test
  public void testHasAccess() throws Exception {

  }

  @Test
  public void testLockFile() throws Exception {

  }

  @Test
  public void testMoveFile() throws Exception {

  }

  @Test
  public void testRestoreFileAtVersion() throws Exception {

  }

  @Test
  public void testUndeleteFile() throws Exception {

  }

  @Test
  public void testUnlockFile() throws Exception {

  }

  @Test
  public void testUpdateAcl() throws Exception {

  }

  @Test
  public void testUpdateFile() throws Exception {

  }

  @Test
  public void testGetReferrers() throws Exception {

  }

  @Test
  public void testGetDataForExecuteInBatch() throws Exception {

  }

  @Test
  public void testGetDataForReadInBatch() throws Exception {

  }

  @Test
  public void testGetVersionSummaryInBatch() throws Exception {

  }

  @Test
  public void testSetFileMetadata() throws Exception {

  }

  @Test
  public void testGetFileMetadata() throws Exception {

  }

  @Test
  public void testCopyFile() throws Exception {

  }

  @Test
  public void testGetDeletedFiles3() throws Exception {

  }

  @Test
  public void testGetDeletedFiles4() throws Exception {

  }

  @Test
  public void testGetReservedChars() throws Exception {

  }

}
