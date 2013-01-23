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
 * Copyright 2008 - 2009 Pentaho Corporation. All rights reserved.
 * 
*/
package org.pentaho.platform.engine.core;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pentaho.platform.api.engine.IContentInfo;
import org.pentaho.platform.api.engine.IPentahoObjectReference;
import org.pentaho.platform.api.engine.ObjectFactoryException;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.core.system.objfac.StandaloneSpringPentahoObjectFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;

import junit.framework.TestCase;

@SuppressWarnings({"all"})
public class StandaloneSpringPentahoObjectFactoryTest extends TestCase {

  public void testInitFromXml() throws Exception {
    
    StandaloneSession session = new StandaloneSession();
    StandaloneSession session2 = new StandaloneSession();
    
    StandaloneSpringPentahoObjectFactory factory = new StandaloneSpringPentahoObjectFactory( );
    factory.init("test-res/solution/system/pentahoObjects.spring.xml", null );
    
    GoodObject goodObject = factory.get( GoodObject.class, session);
    assertNotNull( goodObject );
    
    try {
      factory.get(BadObject.class, null);
      assertFalse( true );
    } catch (ObjectFactoryException e) {
      
    }
    
    
    GoodObject goodObject1 = factory.get(GoodObject.class, session);
    GoodObject goodObject2 = factory.get(GoodObject.class, session);
    
    assertEquals( goodObject1, goodObject2 );
    assertEquals( session, goodObject1.initSession );
    
    GoodObject goodObject3 = factory.get(GoodObject.class, session2);
    assertNotSame( goodObject1, goodObject3 );
    assertEquals( session2, goodObject3.initSession );
    
  }

  public void testGetAll() throws Exception {

    StandaloneSession session = new StandaloneSession();

    StandaloneSpringPentahoObjectFactory factory = new StandaloneSpringPentahoObjectFactory( );
    factory.init("test-res/solution/system/pentahoObjects.spring.xml", null );

    MimeTypeListener mime1 = factory.get( MimeTypeListener.class, session);
    assertNotNull( mime1);

    List<MimeTypeListener> mimes = factory.getAll(MimeTypeListener.class, session);

    assertNotNull(mimes);
    assertEquals(5, mimes.size());
    assertNotSame(mimes.get(0), mimes.get(1));


  }

  public void testReferences() throws Exception{

    StandaloneSession session = new StandaloneSession();

    StandaloneSpringPentahoObjectFactory factory = new StandaloneSpringPentahoObjectFactory( );
    factory.init("test-res/solution/system/pentahoObjects.spring.xml", null );

    IPentahoObjectReference reference = factory.getObjectReference(MimeTypeListener.class, session);

    assertEquals("30", reference.getProperties().get("priority"));
    //ensure highest priority won
    assertEquals(((MimeTypeListener)reference.getObject()).name, "Higher Priority MimeTypeListener");
  }

  public void testGetByProperty() throws Exception{

    StandaloneSession session = new StandaloneSession();

    StandaloneSpringPentahoObjectFactory factory = new StandaloneSpringPentahoObjectFactory( );
    factory.init("test-res/solution/system/pentahoObjects.spring.xml", null );


    MimeTypeListener obj = factory.get(MimeTypeListener.class, session, Collections.singletonMap("someKey", "1"));
    assertEquals("Test Attr1", obj.name);

    obj = factory.get(MimeTypeListener.class, session, Collections.singletonMap("someKey", "2"));
    assertEquals("Test Attr2", obj.name);

    // Multiple Attributes
    HashMap map = new HashMap<String, String>();
    map.put("someKey", "3");
    map.put("foo", "bar");
    obj = factory.get(MimeTypeListener.class, session, map);
    assertEquals("Test Attr3", obj.name);


    //Not found, will default to
    map = new HashMap<String, String>();
    map.put("someKey", "3");
    map.put("foo", "bang");
    obj = factory.get(MimeTypeListener.class, session, map);
    assertEquals(null, obj.name);
  }


  public void testPriority() throws Exception{

    StandaloneSession session = new StandaloneSession();

    StandaloneSpringPentahoObjectFactory factory = new StandaloneSpringPentahoObjectFactory( );
    factory.init("test-res/solution/system/pentahoObjects.spring.xml", null );

    MimeTypeListener obj = factory.get(MimeTypeListener.class, session);

    assertEquals("Higher Priority MimeTypeListener", obj.name);
  }


  public void testSessionProperties() throws Exception{

    StandaloneSession session = new StandaloneSession();

    StandaloneSpringPentahoObjectFactory factory = new StandaloneSpringPentahoObjectFactory( );
    factory.init("test-res/solution/system/pentahoObjects.spring.xml", null );

    IContentInfo obj = factory.get(IContentInfo.class, session);
    assertEquals("Test Session", obj.getTitle()); // should be

    IContentInfo obj_again = factory.get(IContentInfo.class, session);
    assertSame(obj_again, obj); // should be

    session = new StandaloneSession();
    IContentInfo obj_newer = factory.get(IContentInfo.class, session);
    assertNotSame(obj, obj_newer); // should be
  }



  public void testInitFromObject() throws Exception {
    
    StandaloneSession session = new StandaloneSession();
    
    StandaloneSpringPentahoObjectFactory factory = new StandaloneSpringPentahoObjectFactory( );
    
    File f = new File("test-res/solution/system/pentahoObjects.spring.xml");
    FileSystemResource fsr = new FileSystemResource(f);
    GenericApplicationContext appCtx = new GenericApplicationContext();
    XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(appCtx);
    xmlReader.loadBeanDefinitions(fsr);
    
    factory.init("test-res/solution/system/pentahoObjects.spring.xml", appCtx );
    
    GoodObject obj = factory.get(GoodObject.class, session);
    assertNotNull( obj );
    
  }
  
  public void testNoInit() throws Exception {
    
    StandaloneSession session = new StandaloneSession();
    
    StandaloneSpringPentahoObjectFactory factory = new StandaloneSpringPentahoObjectFactory( );
    
    try {
      GoodObject obj = factory.get(GoodObject.class, session);
      assertNotNull( obj );
      assertFalse( true );
    } catch (ObjectFactoryException e) {
      assertTrue( true );
    }
    
  }

  public void testBadInit() throws Exception {
    
    StandaloneSession session = new StandaloneSession();
    
    StandaloneSpringPentahoObjectFactory factory = new StandaloneSpringPentahoObjectFactory( );
    
    try {
      factory.init( null, "bogus" );
      assertFalse( true );
    } catch (IllegalArgumentException e) {
      assertTrue( true );
    }
    
    try {
      GoodObject obj = factory.get(GoodObject.class, session);
      assertNotNull( obj );
      assertFalse( true );
    } catch (ObjectFactoryException e) {
      assertTrue( true );
    }
    
  }
  
}
