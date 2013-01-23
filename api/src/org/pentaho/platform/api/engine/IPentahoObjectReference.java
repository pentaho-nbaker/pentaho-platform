package org.pentaho.platform.api.engine;

import java.util.Map;

/**
 * User: nbaker
 * Date: 1/16/13
 *
 * IPentahoObjectReference objects represent a pointer to an available object in the IPentahoObjectFactory
 * and contain all Object Properties associated with that object (priority, adhoc properties).
 *
 * Implementations of this class should not simply "wrap" the referenced Object, rather they should
 * defer retrieval of the Object through the getObject() method
 *
 */
public interface IPentahoObjectReference<T> {
  Map<String, Object> getProperties();
  T getObject();
}
