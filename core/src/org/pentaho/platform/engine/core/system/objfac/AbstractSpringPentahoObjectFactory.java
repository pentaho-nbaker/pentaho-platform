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
 * Copyright 2008 Pentaho Corporation.  All rights reserved. 
 * 
 * @created Oct 15, 2008
 * @author Aaron Phillips
 * 
 */
package org.pentaho.platform.engine.core.system.objfac;

import org.pentaho.platform.api.engine.*;
import org.pentaho.platform.engine.core.messages.Messages;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.engine.core.system.objfac.spring.SpringScopeSessionHolder;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.*;

/**
 * Framework for Spring-based object factories.  Subclasses are required only to implement
 * the init method, which is responsible for setting the {@link ApplicationContext}.
 * <p>
 * A note on creation and management of objects:
 * Object creation and scoping is handled by Spring with one exception: in the case of
 * a {@link StandaloneSession}.  Spring's session scope relates a bean to an {@link javax.servlet.http.HttpSession},
 * and as such it does not know about custom sessions.  The correct approach to solve this problem 
 * is to write a custom Spring scope (called something like "pentahosession").  Unfortunately, we 
 * cannot implement a custom scope to handle the {@link StandaloneSession} because the custom scope
 * would not be able to access it.  There is currently no way to statically obtain a reference to a 
 * pentaho session. So we are left with using custom logic in this factory to execute a different non-Spring logic path
 * when the IPentahoSession is of type StandaloneSession.
 * <p>
 *
 * @see IPentahoObjectFactory
 * 
 * @author Aaron Phillips
 */
public abstract class AbstractSpringPentahoObjectFactory implements IPentahoObjectFactory {

  protected GenericApplicationContext beanFactory;
  protected static final Log logger = LogFactory.getLog(AbstractSpringPentahoObjectFactory.class);
  protected static final String PRIORITY = "priority";

  /**
   * @see IPentahoObjectFactory#get(Class, IPentahoSession)
   */
  public <T> T get(Class<T> interfaceClass, final IPentahoSession session) throws ObjectFactoryException {
    return get(interfaceClass, null, session);
  }

  @Override
  public <T> List<T> getAll(Class<T> interfaceClass, IPentahoSession curSession) throws ObjectFactoryException {
    return retreiveObjects(interfaceClass, curSession);
  }

  /**
   * @see IPentahoObjectFactory#get(Class, String, IPentahoSession)
   */
  @SuppressWarnings("unchecked")
  public <T> T get(Class<T> interfaceClass, String key, final IPentahoSession session) throws ObjectFactoryException {
    return retreiveObject(interfaceClass, key, session, null);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(Class<T> interfaceClass, IPentahoSession session, Map<String, String> props) throws ObjectFactoryException {
    return retreiveObject(interfaceClass, null, session, props);
  }

  protected Object instanceClass(String simpleName) throws ObjectFactoryException {
    return instanceClass(simpleName, null);
  }

  protected Object instanceClass(String simpleName, String key) throws ObjectFactoryException {
    Object object = null;
    try {
      if(beanFactory.containsBean(simpleName)){
        object = beanFactory.getType(simpleName).newInstance();
      } else if(key != null){
        object = beanFactory.getType(key).newInstance();
      }
    } catch (Exception e) {
      String msg = Messages.getInstance().getString("AbstractSpringPentahoObjectFactory.WARN_FAILED_TO_CREATE_OBJECT", key); //$NON-NLS-1$
      throw new ObjectFactoryException(msg, e);
    }
    return object;
  }


  protected Object instanceClass(Class<?> interfaceClass, String key) throws ObjectFactoryException {
    Object object = null;
    try {
      String[] beanNames = beanFactory.getBeanNamesForType(interfaceClass);
      if(beanNames.length > 0){

        int highestPriority = -1;
        String highestBeanName = beanNames[0];
        for(String name : beanNames){
          BeanDefinition ref = this.getBeanDefinitionFromFactory(name);

          int priority = computePriority(ref);

          if(priority > highestPriority){
            highestPriority = priority;
            highestBeanName = name;
          }

        }
        object = beanFactory.getType(highestBeanName).newInstance();


      } else if(key != null){
        object = beanFactory.getType(key).newInstance();
      }
    } catch (Exception e) {
      String msg = Messages.getInstance().getString("AbstractSpringPentahoObjectFactory.WARN_FAILED_TO_CREATE_OBJECT", key); //$NON-NLS-1$
      throw new ObjectFactoryException(msg, e);
    }
    return object;
  }

  private int computePriority(BeanDefinition ref) {
    if(ref == null || ref.getAttribute(PRIORITY) == null){
      // return default
      return 20;
    }

    try{
      int val = Integer.parseInt(ref.getAttribute(PRIORITY).toString());
      return val;
    } catch (NumberFormatException e){
      logger.error("bean of type "+ref.getBeanClassName()+" has an invalid priority value, only numeric allowed");
      // return default
      return 20;
    }
  }

  private <T> T retrieveViaSpring(Class<T> interfaceClass) throws ObjectFactoryException {
    return retrieveViaSpring(interfaceClass, null);
  }
  private <T> T retrieveViaSpring(Class<T> interfaceClass, Map<String, String> props) throws ObjectFactoryException {
    Object object = null;
    try {

      String[] beanNames = beanFactory.getBeanNamesForType(interfaceClass);
      if(beanNames == null || beanNames.length == 0){
        throw new IllegalStateException("No bean found for given type");
      }

      // Collection BeanDefinition Metadata map
      Map<String, BeanDefinition> beanDefs = new HashMap<String, BeanDefinition>();
      for(String name : beanNames){
        BeanDefinition ref = getBeanDefinitionFromFactory(name);
        if(ref != null){
          beanDefs.put(name, ref);
        }
      }

      // If this request has properties to filter by, do that now
      if(props != null && props.size() > 0){

        Iterator<Map.Entry<String, BeanDefinition>> iterator = beanDefs.entrySet().iterator();
        outer:
        while (iterator.hasNext()) {
          Map.Entry<String, BeanDefinition> entry = iterator.next();
          BeanDefinition def = entry.getValue();
          for(Map.Entry<String, String> prop : props.entrySet()){
            Object attrVal = def.getAttribute(prop.getKey());
            if(attrVal == null || !attrVal.equals(prop.getValue())){
              iterator.remove();
              continue outer;
            }
          }
        }

      }

      // compute return object based on priority attribute if available
      int highestPriority = -1;
      String highestBeanName = null;
      for(Map.Entry<String, BeanDefinition> entry : beanDefs.entrySet()){
        BeanDefinition def = entry.getValue();

        int priority = computePriority(def);

        if(priority > highestPriority){
          highestPriority = priority;
          highestBeanName = entry.getKey();
        }
      }

      if(highestBeanName == null){
        throw new IllegalStateException("No bean found for given type");
      }
      object = beanFactory.getBean(highestBeanName);

    } catch (Throwable t) {
      String msg = Messages.getInstance().getString("AbstractSpringPentahoObjectFactory.WARN_FAILED_TO_RETRIEVE_OBJECT", interfaceClass.getSimpleName()); //$NON-NLS-1$
      throw new ObjectFactoryException(msg,t);
    }

    // Sanity check
    if(interfaceClass.isAssignableFrom(object.getClass()) == false){
      throw new IllegalStateException("Object retrived from Spring not expected type: "+interfaceClass.getSimpleName());
    }

    return (T) object;
  }

  private BeanDefinition getBeanDefinitionFromFactory(final String name) {
    return beanFactory.getBeanDefinition(name);
  }

  protected Object retrieveViaSpring(String beanId) throws ObjectFactoryException {
    Object object;
    try {
      object = beanFactory.getBean(beanId);
    } catch (Throwable t) {
      String msg = Messages.getInstance().getString("AbstractSpringPentahoObjectFactory.WARN_FAILED_TO_RETRIEVE_OBJECT", beanId); //$NON-NLS-1$
      throw new ObjectFactoryException(msg,t);
    }
    return object;
  }

  private <T> T retreiveObject(Class<T> interfaceClass, String key, IPentahoSession session, Map<String, String> props) throws ObjectFactoryException {
    //cannot access logger here since this object factory provides the logger
    logger.debug("Attempting to get an instance of [" + interfaceClass.getSimpleName() + "] while in session [" + session + "]");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

    Object object;

    if (session != null && session instanceof StandaloneSession) {
      //first ask Spring for the object, if it is session scoped it will fail
      //since Spring doesn't know about StandaloneSessions

      // Save the session off to support Session and Request scope.
      SpringScopeSessionHolder.SESSION.set(session);
      try {
        if(key != null) { // if they want it by id, look for it that way first
          object = retrieveViaSpring(key);
        } else {
          object = retrieveViaSpring(interfaceClass, props);
        }

      } catch (Throwable t) {
        //Spring could not create the object, perhaps due to session scoping, let's try
        //retrieving it from our internal session map
        logger.debug("Retrieving object from Pentaho session map (not Spring).");   //$NON-NLS-1$

        object = session.getAttribute(interfaceClass.getSimpleName());

        if ((object == null)) {
          //our internal session map doesn't have it, let's create it
          object = instanceClass(interfaceClass, key);
          session.setAttribute(interfaceClass.getSimpleName(), object);
        }
      }
    } else {
      // be sure to clear out any session held.
      SpringScopeSessionHolder.SESSION.set(null);
      //Spring can handle the object retrieval since we are not dealing with StandaloneSession

      if(key != null) { // if they want it by id, look for it that way first
        object = retrieveViaSpring(key);
      } else {
        object = retrieveViaSpring(interfaceClass, props);
      }
    }

    //FIXME: what is this doing here??
    if (object instanceof IPentahoInitializer) {
      ((IPentahoInitializer) object).init(session);
    }

    logger.debug(" Got an instance of [" + interfaceClass.getSimpleName() + "]: " + object);   //$NON-NLS-1$ //$NON-NLS-2$
    return (T) object;
  }


  protected  <T> List<T> retreiveObjects(Class<T> type, final IPentahoSession session) throws ObjectFactoryException {
    //cannot access logger here since this object factory provides the logger
    logger.debug("Attempting to get an instance of [" + type.getSimpleName() + "] while in session [" + session + "]");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

    List<T> objects = new ArrayList<T>();

    if (session != null && session instanceof StandaloneSession) {
      //first ask Spring for the object, if it is session scoped it will fail
      //since Spring doesn't know about StandaloneSessions
      try {
        Map beansOfType = beanFactory.getBeansOfType(type);
        objects.addAll(beansOfType.entrySet());

      } catch (Throwable t) {
        //Spring could not create the object, perhaps due to session scoping, let's try
        //retrieving it from our internal session map
        logger.debug("Retrieving object from Pentaho session map (not Spring).");   //$NON-NLS-1$
        Object object = session.getAttribute(type.getSimpleName());

        if ((object == null)) {
          //our internal session map doesn't have it, let's create it
          object = instanceClass(type.getSimpleName());
          session.setAttribute(type.getSimpleName(), object);
        }
      }
    } else {
      //Spring can handle the object retrieval since we are not dealing with StandaloneSession
      T object = retrieveViaSpring(type);
      if(object != null){

        objects.add(object);

        logger.debug(" Got an instance of [" + type.getSimpleName() + "]: " + object);   //$NON-NLS-1$ //$NON-NLS-2$

        //FIXME: what is this doing here??
        if (object instanceof IPentahoInitializer) {
          ((IPentahoInitializer) object).init(session);
        }
      }
    }


    return objects;
  }

  /**
   * @see IPentahoObjectFactory#objectDefined(String)
   */
  public boolean objectDefined(String key) {
    return beanFactory.containsBean(key);
  }

  /**
   *
   * @param clazz  Interface or class literal to search for
   * @return true if a definition exists
   */
  @Override
  public boolean objectDefined(Class<?> clazz) {
    return beanFactory.getBeanNamesForType(clazz).length > 0;
  }

  /**
   * @see IPentahoObjectFactory#getImplementingClass(String)
   */
  @SuppressWarnings("unchecked")
  public Class getImplementingClass(String key) {
    return beanFactory.getType(key);
  }

  protected void setBeanFactory(GenericApplicationContext context){
    beanFactory = context;
  }


  @Override
  public <T> IPentahoObjectReference<T> getObjectReference(Class<T> clazz, IPentahoSession curSession) {
    return getObjectReference(clazz, curSession, null);
  }


  @Override
  public <T> IPentahoObjectReference<T> getObjectReference(Class<T> clazz, IPentahoSession curSession, Map<String, String> properties) {
      String[] beanNames = beanFactory.getBeanNamesForType(clazz);
      if(beanNames == null || beanNames.length == 0){
        return null;
      }

      // order reference
      BeanDefinition highestReference = null;
      int highestPriority = -1;
      String highestBeanName = beanNames[0];
      for(String name : beanNames){
        BeanDefinition ref = this.getBeanDefinitionFromFactory(name);
        int priority = computePriority(ref);

        if(priority > highestPriority){
          highestPriority = priority;
          highestReference = ref;
          highestBeanName = name;
        }

      }

      return new SpringPentahoObjectReference<T>(beanFactory, highestBeanName, clazz, curSession, new BeanProperties(highestReference));
    }

  @Override
  public <T> IPentahoObjectReference<T> getObjectReferences(Class<T> interfaceClass, IPentahoSession curSession) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public <T> IPentahoObjectReference<T> getObjectReferences(Class<T> interfaceClass, IPentahoSession curSession, Map<String, String> properties) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}