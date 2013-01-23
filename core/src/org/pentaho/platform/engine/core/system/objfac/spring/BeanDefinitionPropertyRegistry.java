package org.pentaho.platform.engine.core.system.objfac.spring;

import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * User: nbaker
 * Date: 1/17/13
 */
public class BeanDefinitionPropertyRegistry {
  private static Map<ApplicationContext, Map<String, Map<String, String>>> contextMap = new WeakHashMap<ApplicationContext, Map<String, Map<String, String>>>();

  public static void storeProperty(ApplicationContext ctx, String beanDefinitionName, String propName, String propValue){
    Map<String, Map<String, String>> beanToPropMap = contextMap.get(ctx);
    if(beanToPropMap == null){
      beanToPropMap = new HashMap<String, Map<String, String>>();
      contextMap.put(ctx, beanToPropMap);
    }
    Map<String, String> beanProps = beanToPropMap.get(beanDefinitionName);
    if(beanProps == null){
      beanProps = new HashMap<String, String>();
      beanToPropMap.put(beanDefinitionName, beanProps);
    }
    beanProps.put(propName, propValue);
  }

  public static Map<String, String> getBeanProperties(ApplicationContext ctx, String beanDefinitionName){
    Map<String, Map<String, String>> beanToPropMap = contextMap.get(ctx);
    if(beanToPropMap == null){
      return null;
    }
    return beanToPropMap.get(beanDefinitionName);

  }
}
