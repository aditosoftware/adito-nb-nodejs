package de.adito.aditoweb.nbm.nodejs.impl;

import com.google.common.base.Strings;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.options.downloader.INodeJSDownloader;
import de.adito.aditoweb.nbm.nodejs.impl.version.NodeJSEnvironmentFactory;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author w.glanzer, 11.05.2021
 */
public class BundledNodeJS
{

  private static BundledNodeJS _INSTANCE;
  private final INodeJSRootProvider rootProvider;
  private final BehaviorSubject<Long> installFinishedSubject = BehaviorSubject.createDefault(System.currentTimeMillis());

  @NotNull
  public static BundledNodeJS getInstance()
  {
    if (_INSTANCE == null)
      _INSTANCE = new BundledNodeJS(new _RootProvider());
    return _INSTANCE;
  }

  protected BundledNodeJS(@NotNull INodeJSRootProvider pRootProvider)
  {
    rootProvider = pRootProvider;
  }

  /**
   * @return the path where the bundled nodejs installation is located
   */
  @NotNull
  public File getBundledNodeJSContainer()
  {
    return rootProvider.get();
  }

  /**
   * @return the bundled executor
   */
  @NotNull
  public INodeJSExecutor getBundledExecutor()
  {
    return NodeJSExecutorImpl.getInternalUnboundExecutor(getBundledNodeJSContainer());
  }

  /**
   * @return the bundled environment
   * @throws IllegalStateException if environment is invalid or unavailable
   */
  @NotNull
  public INodeJSEnvironment getBundledEnvironment()
  {
    File binary = INodeJSDownloader.getInstance().findNodeExecutableInInstallation(getBundledNodeJSContainer());
    if (binary == null)
      throw new IllegalStateException("bundled environment is not available, because node binary could not be found");
    INodeJSEnvironment environment = NodeJSEnvironmentFactory.create(binary);
    if (environment == null || !environment.isValid())
      throw new IllegalStateException("bundled environment is invalid");
    return environment;
  }

  /**
   * If bundled nodejs environment has changed
   */
  public void fireBundledEnvironmentChanged()
  {
    installFinishedSubject.onNext(System.currentTimeMillis());
  }

  public Observable<Long> observeBundledEnvironment()
  {
    return installFinishedSubject;
  }

  /**
   * @return true if bundled nodejs environment is available
   */
  public boolean isBundledEnvironmentAvailable()
  {
    try
    {
      getBundledEnvironment();
      return true;
    }
    catch (Exception e)
    {
      return false;
    }
  }

  /**
   * @return the bundled environment version
   */
  @NotNull
  public String getBundledVersion()
  {
    return "v16.1.0";
  }

  /**
   * Provider for nodejs bundle root
   */
  protected interface INodeJSRootProvider
  {
    @NotNull
    File get();
  }

  /**
   * RootProvider-Impl
   */
  private static class _RootProvider implements INodeJSRootProvider
  {
    @NotNull
    @Override
    public File get()
    {
      String userdir = System.getProperty("netbeans.user");
      if (Strings.isNullOrEmpty(userdir))
        userdir = ".";
      return new File(userdir, "libraries/bundled_nodejs");
    }
  }

}
