package org.pentaho.platform.engine.core.system.objfac;

import org.springframework.beans.factory.config.BeanDefinition;

import java.util.*;

/**
 * User: nbaker
 * Date: 1/16/13
 */
public class BeanProperties extends HashMap<String, Object> {

  private BeanDefinition definition;

  public BeanProperties(final BeanDefinition definition) {
    this.definition = definition;
    for(String s : definition.attributeNames()){
      this.put(s, definition.getAttribute(s));
    }
  }

}
