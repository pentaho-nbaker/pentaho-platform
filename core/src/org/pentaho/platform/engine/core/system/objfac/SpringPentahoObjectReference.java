package org.pentaho.platform.engine.core.system.objfac;

import org.pentaho.platform.api.engine.IPentahoObjectReference;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * User: nbaker
 * Date: 1/16/13
 */
public class SpringPentahoObjectReference<T> implements IPentahoObjectReference {

  private ApplicationContext context;
  private String name;
  private final Class<T> clazz;
  private IPentahoSession session;
  private final BeanProperties properties;

  public SpringPentahoObjectReference(ApplicationContext context, String name, Class<T> clazz, IPentahoSession session, BeanProperties properties){
    this.context = context;
    this.name = name;
    this.clazz = clazz;
    this.session = session;
    this.properties = properties;
  }

  @Override
  public Object getObject() {
    return context.getBean(name);
  }

  @Override
  public Map<String, Object> getProperties() {
    return properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SpringPentahoObjectReference that = (SpringPentahoObjectReference) o;

    if (!clazz.equals(that.clazz)) return false;
    if (!name.equals(that.name)) return false;
    if (properties != null ? !properties.equals(that.properties) : that.properties != null)
      return false;
    if (session != null ? !session.equals(that.session) : that.session != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + clazz.hashCode();
    result = 31 * result + (session != null ? session.hashCode() : 0);
    result = 31 * result + (properties != null ? properties.hashCode() : 0);
    return result;
  }
}
