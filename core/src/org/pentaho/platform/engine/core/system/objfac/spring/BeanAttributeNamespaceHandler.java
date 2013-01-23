package org.pentaho.platform.engine.core.system.objfac.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * User: nbaker
 * Date: 1/16/13
 */
public class BeanAttributeNamespaceHandler extends NamespaceHandlerSupport {

  public void init() {
    registerBeanDefinitionDecorator("object-properties", new BeanAttributeHandler());
  }
}