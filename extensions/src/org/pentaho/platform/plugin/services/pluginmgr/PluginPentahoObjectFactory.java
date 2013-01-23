package org.pentaho.platform.plugin.services.pluginmgr;

import org.pentaho.platform.engine.core.system.objfac.AbstractSpringPentahoObjectFactory;
import org.springframework.context.ApplicationContext;

/**
 * User: nbaker
 * Date: 1/22/13
 */
public class PluginPentahoObjectFactory extends AbstractSpringPentahoObjectFactory {

  public PluginPentahoObjectFactory(ApplicationContext context){
    this.setBeanFactory(context);
  }

  @Override
  public void init(String configFile, Object context) {

  }
}
