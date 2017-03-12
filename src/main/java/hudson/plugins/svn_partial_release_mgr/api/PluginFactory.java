package hudson.plugins.svn_partial_release_mgr.api;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

import hudson.plugins.svn_partial_release_mgr.api.constants.PluginUtil;
import hudson.plugins.svn_partial_release_mgr.api.functions.afterbuild.Function1UpdateTagDeploymentsJsonFiles;
import hudson.plugins.svn_partial_release_mgr.api.functions.afterbuild.Function2PartialPatchCreator;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function0GetReleaseDeployInput;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function1GetTagSource;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function2GetPrevDeploymentsFileSources;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function3GetReleaseFileSources;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function4BackupReleaseFilesAsSrcPatches;
import hudson.plugins.svn_partial_release_mgr.api.functions.build.Function5BackupDeploymentInfoJson;
import hudson.plugins.svn_partial_release_mgr.api.functions.initview.Function1TagRevisionResolver;
import hudson.plugins.svn_partial_release_mgr.api.functions.initview.Function2RevisionsAfterTagCollector;
import hudson.plugins.svn_partial_release_mgr.api.functions.initview.Function3PrevDeploymentsCollector;
import hudson.plugins.svn_partial_release_mgr.impl.ReleaseDeploymentService;
import hudson.plugins.svn_partial_release_mgr.impl.functions.afterbuild.Function1UpdateTagDeploymentsJsonFilesImpl;
import hudson.plugins.svn_partial_release_mgr.impl.functions.afterbuild.Function2PartialPatchCreatorImpl;
import hudson.plugins.svn_partial_release_mgr.impl.functions.build.Function0GetReleaseDeployInputImpl;
import hudson.plugins.svn_partial_release_mgr.impl.functions.build.Function1GetTagSourceImpl;
import hudson.plugins.svn_partial_release_mgr.impl.functions.build.Function2GetPrevDeploymentsFileSourcesImpl;
import hudson.plugins.svn_partial_release_mgr.impl.functions.build.Function3GetReleaseFileSourcesImpl;
import hudson.plugins.svn_partial_release_mgr.impl.functions.build.Function4BackupReleaseFilesAsSrcPatchesImpl;
import hudson.plugins.svn_partial_release_mgr.impl.functions.build.Function5BackupDeploymentInfoJsonImpl;
import hudson.plugins.svn_partial_release_mgr.impl.functions.initview.Function1TagRevisionResolverImpl;
import hudson.plugins.svn_partial_release_mgr.impl.functions.initview.Function2RevisionsAfterTagCollectorImpl;
import hudson.plugins.svn_partial_release_mgr.impl.functions.initview.Function3PrevDeploymentsCollectorImpl;

/**
 * @author G.ILIADIS
 *         Have a nice programming day!!!!
 */
public class PluginFactory {
  public static final Map<Class, Class> defaultImplementations = new HashMap<Class, Class>() {
    {
      put(PluginService.class, ReleaseDeploymentService.class);
      put(Function1TagRevisionResolver.class, Function1TagRevisionResolverImpl.class);
      put(Function2RevisionsAfterTagCollector.class, Function2RevisionsAfterTagCollectorImpl.class);
      put(Function3PrevDeploymentsCollector.class, Function3PrevDeploymentsCollectorImpl.class);

      put(Function0GetReleaseDeployInput.class, Function0GetReleaseDeployInputImpl.class);
      put(Function1GetTagSource.class, Function1GetTagSourceImpl.class);
      put(Function2GetPrevDeploymentsFileSources.class,
          Function2GetPrevDeploymentsFileSourcesImpl.class);
      put(Function3GetReleaseFileSources.class, Function3GetReleaseFileSourcesImpl.class);
      put(Function4BackupReleaseFilesAsSrcPatches.class,
          Function4BackupReleaseFilesAsSrcPatchesImpl.class);
      put(Function5BackupDeploymentInfoJson.class, Function5BackupDeploymentInfoJsonImpl.class);

      put(Function1UpdateTagDeploymentsJsonFiles.class,
          Function1UpdateTagDeploymentsJsonFilesImpl.class);
      put(Function2PartialPatchCreator.class, Function2PartialPatchCreatorImpl.class);
    }
  };


  /**
   * Returns the key of the bean to search in the context configuration
   *
   * @param beanClass the class of the bean we are searching the configuration for
   * @return the identifier of the bean
   */
  public static String getBeanIdentifier(Class beanClass) {
    return StringUtils.uncapitalize(beanClass.getSimpleName());
  }

  /**
   * This method returns an instantiated bean implementation for the input class.<br>
   * See the {@link #getBean(Class, Class) getBean} method.<br>
   * If no implementation definition found it will return a new instance of the input class (Obviously will not work for interfaces).<br>
   * NOTE!! the scope of the bean returned will be "singleton".<br>
   *
   * @param beanClass the class to search if has any override configuration
   * @return the instantiated bean implementation
   */
  @SuppressWarnings("unchecked")
  public static <S> S getBean(Class<S> beanClass) {
    Class<? extends S> implementingClass = defaultImplementations.get(beanClass);
    if (implementingClass == null) {
      implementingClass = beanClass;
    }
    return getBean(beanClass, implementingClass);
  }

  /**
   * This method returns an instantiated bean implementation for the input class.<br>
   * See the {@link #getBean(String, Class) getBean} method.<br>
   *
   * @param beanClass         the class to search if has any override configuration
   * @param implementingClass the class that is the default implementation
   * @return the instantiated bean implementation
   */
  @SuppressWarnings("unchecked")
  public static <S> S getBean(Class<S> beanClass,
                              Class<? extends S> implementingClass) {
    String beanClassKey = getBeanIdentifier(beanClass);
    return getBean(beanClassKey, implementingClass);
  }

  /**
   * This method returns an instantiated bean implementation for the input class key.<br>
   *
   * @param beanClassKey      the class to search if has any bean configuration
   * @param implementingClass the class that is the default implementation
   * @return the configured class to implement the class with the given key (or the given if no such configuration)
   */
  @SuppressWarnings("unchecked")
  protected static <S> S getBean(String beanClassKey,
                                 Class<S> implementingClass) {
    String implementationBeanClassName = getOverrideClassForBean(beanClassKey);
    if (StringUtils.isBlank(implementationBeanClassName)) {
      implementationBeanClassName = implementingClass.getName();
    }
    return (S) newBeanInstance(implementationBeanClassName);
  }

  /**
   * This method returns the configured class to override the default implementation.<br>
   *
   * @param beanClassKey the class key to search for override configuration
   * @return the configured class to implement the class with the given key (or the given if no such configuration)
   */
  @SuppressWarnings("unchecked")
  protected static String getOverrideClassForBean(String beanClassKey) {
    String key = "override" + "." + beanClassKey;
    return PluginUtil.getConfiguration(key);
  }


  /**
   * Returns a new instance of the argument class name by calling Class.forName .<br>
   * If the forceNew flag is true the returning class will be a new instance (prototype instead of singleton)
   * otherwise an already instantiated bean inside the memory.<br>
   *
   * @param implementationBeanClassName the class name of the bean to initialize
   * @return a new instance of the bean class
   */
  protected static Object newBeanInstance(String implementationBeanClassName) {
    try {
      return Class.forName(implementationBeanClassName).newInstance();

    } catch (Exception e) {
      String msg = "Class [" + implementationBeanClassName + "] can not be instantiated! " +
          "Make sure to declare an implementation class for this interface or abstract";
      throw new RuntimeException(msg);
    }
  }


}
