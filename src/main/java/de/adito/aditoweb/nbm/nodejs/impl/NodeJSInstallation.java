package de.adito.aditoweb.nbm.nodejs.impl;

import com.google.common.base.Strings;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.options.NodeJSOptions;
import de.adito.aditoweb.nbm.nodejs.impl.options.downloader.INodeJSDownloader;
import de.adito.aditoweb.nbm.nodejs.impl.version.NodeJSEnvironmentFactory;
import lombok.*;
import org.jetbrains.annotations.*;

import java.io.File;

/**
 * Represents a NodeJS installation.
 *
 * @author p.neub, 07.11.2022
 */
@Value
public class NodeJSInstallation
{

  private static final IInstallationProvider _PROVIDER = new OptionsInstallationProvider(new BundledInstallationProvider());

  /**
   * Root folder of the NodeJS installation.
   */
  File rootFolder;

  /**
   * Indicates whether the NodeJS installation is internal or provided by the user.
   * If the installation is internal the {@link NodeJSInstaller}
   * will download and install NodeJS to the root folder if NodeJS is not already installed.
   */
  boolean isInternal;

  /**
   * Returns the current {@link NodeJSInstallation} that should be used to perform NodeJS tasks.
   *
   * @return an instance of the {@link NodeJSInstallation}
   */
  @NonNull
  public static NodeJSInstallation getCurrent()
  {
    return _PROVIDER.get();
  }

  /**
   * Returns a {@link INodeJSExecutor} using the binary provided in the current {@link NodeJSInstallation}.
   *
   * @return an instance of a {@link INodeJSExecutor}
   */
  @NonNull
  public INodeJSExecutor getExecutor()
  {
    return NodeJSExecutorImpl.getInternalUnboundExecutor(rootFolder);
  }

  /**
   * Returns the File representing the NodeJS binary.
   *
   * @return file to the NodeJS binary, or null if not found in the installation directory
   */
  @Nullable
  public File getBinary()
  {
    return INodeJSDownloader.getInstance().findNodeExecutableInInstallation(rootFolder);
  }

  /**
   * Returns a {@link INodeJSEnvironment} for the current {@link NodeJSInstallation}.
   * This can be used for globally used packages e.g. typescript.
   *
   * @return an instance of a {@link INodeJSEnvironment}
   * @throws IllegalStateException in case the environment is invalid
   */
  @NonNull
  public INodeJSEnvironment getEnvironment()
  {
    File binary = getBinary();
    if (binary == null)
      throw new IllegalStateException("global nodejs environment is not available, because node binary could not be found");
    INodeJSEnvironment environment = NodeJSEnvironmentFactory.create(binary);
    if (environment == null || !environment.isValid())
      throw new IllegalStateException("global nodejs environment is invalid");
    return environment;
  }

  /**
   * Checks whether the environment provided by {@link NodeJSInstallation#getEnvironment()} is valid.
   *
   * @return boolean indicating whether the environment is valid
   */
  public boolean isEnvironmentAvailable()
  {
    try
    {
      getEnvironment();
      return true;
    }
    catch (Exception e)
    {
      return false;
    }
  }

  /**
   * Provider that provides a {@link NodeJSInstallation}.
   */
  public interface IInstallationProvider
  {
    /**
     * Method that returns the {@link NodeJSInstallation}.
     * Instances may be shared, since {@link NodeJSInstallation} is immutable.
     *
     * @return an instance of the {@link NodeJSInstallation}
     */
    @NonNull
    NodeJSInstallation get();
  }

  /**
   * Provider that provides the bundled {@link NodeJSInstallation}.
   */
  public static class BundledInstallationProvider implements IInstallationProvider
  {
    @NonNull
    @Override
    public NodeJSInstallation get()
    {
      String userdir = System.getProperty("netbeans.user");
      if (Strings.isNullOrEmpty(userdir))
        userdir = ".";
      return new NodeJSInstallation(new File(userdir, "libraries/bundled_nodejs"), true);
    }
  }

  /**
   * Provider that provides the {@link NodeJSInstallation} specified in the options.
   * If no installation is specified or the specified installation is invalid,
   * it delegates to the {@link OptionsInstallationProvider#defaultRootProvider}.
   */
  @AllArgsConstructor
  public static class OptionsInstallationProvider implements IInstallationProvider
  {
    /**
     * Default {@link IInstallationProvider} that gets used,
     * if no installation is configured in the options, or the configured installation is invalid.
     */
    private final IInstallationProvider defaultRootProvider;

    @NonNull
    @Override
    public NodeJSInstallation get()
    {
      String path = NodeJSOptions.getInstance().getPath();
      if (path == null)
        return defaultRootProvider.get();
      File rootDir = INodeJSDownloader.getInstance().findInstallationFromNodeExecutable(new File(path));
      if (rootDir == null)
        return defaultRootProvider.get();
      return new NodeJSInstallation(rootDir, false);
    }
  }

}
