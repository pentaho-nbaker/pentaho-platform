package org.pentaho.platform.engine.core.system.objfac;

import org.pentaho.platform.api.engine.IPentahoObjectFactory;
import org.pentaho.platform.api.engine.IPentahoObjectReference;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ObjectFactoryException;
import org.pentaho.platform.engine.core.messages.Messages;

import java.util.*;

/**
 * User: nbaker
 * Date: 1/15/13
 */
public class AggregateObjectFactory implements IPentahoObjectFactory {
  Set<IPentahoObjectFactory> factories = new HashSet<IPentahoObjectFactory>();
  private IPentahoObjectFactory primaryFactory;

  public AggregateObjectFactory(){

  }

  public void registerObjectFactory(IPentahoObjectFactory fact, boolean primary){
    factories.add(fact);
    if(primary){
      this.primaryFactory = fact;
    }
  }

  @Override
  public <T> T get(Class<T> interfaceClass, String key, IPentahoSession session) throws ObjectFactoryException {
    return primaryFactory.get(interfaceClass, key, session);
  }

  @Override
  public <T> T get(Class<T> interfaceClass, IPentahoSession session) throws ObjectFactoryException {

    return get(interfaceClass, session, null);
  }

  private int computePriority(IPentahoObjectReference ref) {
    Object sPri = ref.getProperties().get("priority");
    try{
      return Integer.parseInt(sPri.toString());
    } catch (NumberFormatException e){
      return 20;
    }

  }

  @Override
  public boolean objectDefined(String key) {
    for(IPentahoObjectFactory fact : factories){
      if(fact.objectDefined(key)){
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @deprecated All usage of key methods are deprecated, use object properties instead
   */
  @Override
  public Class<?> getImplementingClass(String key) {
    return primaryFactory.getImplementingClass(key);  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void init(String configFile, Object context) {

  }

  @Override
  public <T> List<T> getAll(Class<T> interfaceClass, IPentahoSession curSession) throws ObjectFactoryException {

    // Use a set to avoid duplicates
    Set<IPentahoObjectReference<T>> referenceSet = new TreeSet<IPentahoObjectReference<T>>();

    for(IPentahoObjectFactory fact : factories){
      IPentahoObjectReference<T> found = fact.getObjectReference(interfaceClass, curSession);
      if(found != null){
        referenceSet.add(found);
      }
    }

    // transform to a list to sort
    List<IPentahoObjectReference<T>> referenceList = new ArrayList<IPentahoObjectReference<T>> ();
    referenceList.addAll(referenceSet);
    Collections.sort(referenceList, referencePriorityComparitor);

    // create final list of impls
    List<T> entryList = new ArrayList<T>();
    for(IPentahoObjectReference<T> ref : referenceList){
      entryList.add(ref.getObject());
    }

    return entryList;
  }

  @Override
  public <T> IPentahoObjectReference<T> getObjectReference(Class<T> clazz, IPentahoSession curSession) {

    Set<IPentahoObjectReference<T>> references = new HashSet<IPentahoObjectReference<T>>();

    for(IPentahoObjectFactory fact : factories){
      IPentahoObjectReference<T> found = fact.getObjectReference(clazz, curSession);
      if(found != null){
        references.add(found);
      }
    }
    IPentahoObjectReference<T> highestRef = null;
    int highestRefPriority = -1;
    for(IPentahoObjectReference<T> ref : references){
      int pri = computePriority(ref);
      if(pri  > highestRefPriority){
        highestRef = ref;
        highestRefPriority = pri;
      }
    }

    return highestRef;
  }

  @Override
  public <T> T get(Class<T> clazz, IPentahoSession session, Map<String, String> properties) throws ObjectFactoryException {

    IPentahoObjectReference<T> highestRef = this.getObjectReference(clazz, session, properties);

    if(highestRef == null){
      String msg = Messages.getInstance().getString("AbstractSpringPentahoObjectFactory.WARN_FAILED_TO_RETRIEVE_OBJECT", clazz.getSimpleName());
      throw new ObjectFactoryException(msg);
    }

    return highestRef.getObject();
  }

  @Override
  public boolean objectDefined(Class<?> clazz) {
    for(IPentahoObjectFactory fact : factories){
      if(fact.objectDefined(clazz)){
        return true;
      }
    }
    return false;
  }

  @Override
  public <T> IPentahoObjectReference<T> getObjectReference(Class<T> interfaceClass, IPentahoSession curSession, Map<String, String> properties) {

    Set<IPentahoObjectReference<T>> references = new HashSet<IPentahoObjectReference<T>>();

    for(IPentahoObjectFactory fact : factories){
      IPentahoObjectReference<T> found = fact.getObjectReference(interfaceClass, curSession, properties);
      if(found != null){
        references.add(found);
      }
    }
    IPentahoObjectReference<T> highestRef = null;
    int highestRefPriority = -1;
    for(IPentahoObjectReference<T> ref : references){
      int pri = computePriority(ref);
      if(pri  > highestRefPriority){
        highestRef = ref;
        highestRefPriority = pri;
      }
    }

    return highestRef;
  }

  private static ReferencePriorityComparitor referencePriorityComparitor = new ReferencePriorityComparitor();
  private static class ReferencePriorityComparitor implements Comparator<IPentahoObjectReference>{
    private static final String PRIORITY = "priority";
    @Override
    public int compare(IPentahoObjectReference ref1, IPentahoObjectReference ref2) {
      int pri1 = extractPriority(ref1);
      int pri2 = extractPriority(ref2);
      if(pri1 == pri2){
        return 0;
      } else if(pri1 > pri2){
        return 1;
      } else {
        return -1;
      }

    }

    private int extractPriority(IPentahoObjectReference ref){
      if(ref == null || ref.getProperties() == null || !ref.getProperties().containsKey(PRIORITY)){
        // return default
        return 20;
      }

      try{
        return Integer.parseInt(ref.getProperties().get(PRIORITY).toString());
      } catch (NumberFormatException e){
        // return default
        return 20;
      }
    }
  }
}
