package de.adito.aditoweb.nbm.nodejs.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.adito.aditoweb.nbm.metrics.api.IMetricProxyFactory;
import de.adito.aditoweb.nbm.metrics.api.types.Counted;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.options.NodeJSOptions;
import de.adito.aditoweb.nbm.nodejs.impl.options.downloader.INodeJSDownloader;
import de.adito.notification.INotificationFacade;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.*;
import org.openide.windows.OnShowing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * @author w.glanzer, 10.05.2021
 */
@OnShowing // to show progress, must not be too early..
public class NodeJSInstaller implements Runnable
{

  public static final String DEFAULT_VERSION = "v18.14.0";
  static final String IS_INCLUDE_SYMLINKS_PROPERTY = "adito.fs.watcher.symlink.handling.enabled";

  private static final String _INSTALLER_INTEGRITYCHECK_FILE = ".installer_integrity";
  private static final Logger _LOGGER = Logger.getLogger(NodeJSInstaller.class.getName());
  private static final ExecutorService _EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                                                                                         .setDaemon(true)
                                                                                         .setNameFormat("tNodeJSInstaller-%d")
                                                                                         .setPriority(Thread.MIN_PRIORITY)
                                                                                         .build());
  private static final BehaviorSubject<Long> installSubject = BehaviorSubject.createDefault(System.currentTimeMillis());
  private final _NodeJSDownloadRetryHandler retryHandler = IMetricProxyFactory.proxy(new _NodeJSDownloadRetryHandler());

  /**
   * Observes the installation.
   * The Observable is triggert when the installation has changed, e.g. a new package was installed.
   *
   * @return the {@link Observable}
   */
  public static Observable<Long> observeInstallation()
  {
    return installSubject;
  }

  @Override
  public void run()
  {
    //noinspection ResultOfMethodCallIgnored doesn't need to be disponsed since there is only one NodeJSInstaller
    NodeJSOptions.observe().subscribe(pOptions -> _downloadLibraries());
  }

  /**
   * Downloads all necessary libraries asynchronously
   */
  @NbBundle.Messages("LBL_Progress_DownloadLibraries=Downloading Libraries...")
  private void _downloadLibraries()
  {
    _EXECUTOR.execute(() -> {
      try
      {
        // validate and fix the installation (switch to the bundled installation),
        // if the specified installation is invalid
        if (isInvalidInstallation())
        {
          NodeJSOptions.update(NodeJSOptions.getInstance().toBuilder().path(null).build());
          return;
        }

        // download and / or install
        downloadBundledNodeJS();
        downloadRequiredGlobalPackages();

        disableSymlinksIfNodeMeetsTheRequiredVersion();
      }
      catch (Exception e)
      {
        INotificationFacade.INSTANCE.error(e);
      }
    });
  }

  /**
   * Checks whether the current nodejs installations creates symlinks on npm install.
   * If that is not the case, it disables the symlink handling.
   */
  @VisibleForTesting
  void disableSymlinksIfNodeMeetsTheRequiredVersion()
  {
    if (System.getProperty(IS_INCLUDE_SYMLINKS_PROPERTY) == null)
    {
      // .. symlink handling is not explicitly set
      NodeJSInstallation installation = NodeJSInstallation.getCurrent();
      if (installation.isEnvironmentAvailable())
      {
        INodeJSEnvironment env = installation.getEnvironment();
        if (!doesNodeCreateSymlinksOnNpmInstall(env.getVersion()))
          System.setProperty(IS_INCLUDE_SYMLINKS_PROPERTY, Boolean.FALSE.toString());
      }
    }
  }

  /**
   * Check whether the provided nodejs version creates symlinks on npm install.
   * The version should be in the following format vMAJOR.MINOR.PATCH.
   *
   * @param pNodeJsVersion the version of nodejs (node --version)
   * @return whether the nodejs version creates symlinks on npm install
   */
  @VisibleForTesting
  static boolean doesNodeCreateSymlinksOnNpmInstall(@NonNull String pNodeJsVersion)
  {
    String[] version = pNodeJsVersion.substring(1).split("\\.");
    // nodejs version v18.x.x is tested to not create symlinks
    return Integer.parseInt(version[0]) < 18;
  }

  /**
   * Checks whether the current {@link NodeJSInstallation} is invalid.
   *
   * @return boolean indicating if the installation is invalid
   */
  private boolean isInvalidInstallation()
  {
    NodeJSInstallation installation = NodeJSInstallation.getCurrent();

    if (NodeJSOptions.getInstance().getPath() != null)
    {
      // invalidate existing explicit paths to the bundled installation
      String currRoot = installation.getRootFolder().getAbsolutePath();
      String bundledRoot = new NodeJSInstallation.BundledInstallationProvider().get().getRootFolder().getAbsolutePath();
      if (currRoot.equals(bundledRoot))
        return true;

      // the installation is invalid, if no valid environment can be created
      return !installation.isEnvironmentAvailable();
    }

    return false;
  }

  /**
   * Downloads the nodejs version specified via {@link NodeJSInstaller#DEFAULT_VERSION}, if no version is specified
   */
  @NbBundle.Messages("LBL_Progress_Download_Execute=Downloading NodeJS {0}...")
  protected void downloadBundledNodeJS() throws IOException
  {
    NodeJSInstallation installation = NodeJSInstallation.getCurrent();
    if (!installation.isInternal())
      return;

    // do not download or update anything, if the nodejs container folder already exists and integrity is ok
    File rootFolder = installation.getRootFolder();
    if (_isIntegrityOK(rootFolder, DEFAULT_VERSION))
    {
      if (BaseUtilities.isWindows())
      {
        if (new File(rootFolder, "node_modules/npm").exists())
          return;
      }
      else
        return;
    }

    if (rootFolder.exists())
      FileUtils.deleteDirectory(rootFolder);

    try (ProgressHandle handle = ProgressHandle.createSystemHandle(Bundle.LBL_Progress_DownloadLibraries(), null))
    {
      // handle progress
      handle.start();
      handle.switchToIndeterminate();

      // download
      handle.setDisplayName(Bundle.LBL_Progress_Download_Execute(DEFAULT_VERSION));

      INodeJSDownloader downloader = INodeJSDownloader.getInstance();
      File binFile = downloader.downloadVersion(DEFAULT_VERSION, rootFolder.getParentFile());
      File nodeVersionContainer = downloader.findInstallationFromNodeExecutable(binFile);

      // rename to target
      if (nodeVersionContainer != null)
        FileUtils.moveDirectory(nodeVersionContainer, rootFolder);
      else
        throw new IllegalStateException("Could not found nodeVersionContainer in " + binFile);

      // update integrity
      _updateIntegrity(rootFolder, DEFAULT_VERSION);
    }
  }

  /**
   * Downloads the latest typescript-language-server
   */
  @NbBundle.Messages({
      "LBL_Progress_Checking=Checking NodeJS installation...",
      "LBL_Progress_Download=Downloading {0}...",
      "LBL_Progress_Update=Updating {0}...",
      "LBL_Progress_Analyze=Analyzing {0}..."
  })
  protected void downloadRequiredGlobalPackages() throws IOException, InterruptedException, TimeoutException
  {
    try (ProgressHandle handle = ProgressHandle.createSystemHandle(Bundle.LBL_Progress_Checking(), null))
    {
      // handle progress
      handle.start();
      handle.switchToIndeterminate();

      // verify that a node installation is present
      NodeJSInstallation installation = NodeJSInstallation.getCurrent();
      File rootFolder = installation.getRootFolder();
      if (!rootFolder.exists())
        return;

      // Create node_modules folder to install the typescript module in the correct directory
      //noinspection ResultOfMethodCallIgnored
      new File(rootFolder, "node_modules").mkdir();

      // prepare
      INodeJSExecutor executor = installation.getExecutor();

      // try it multiple times, sometimes no NodeJS is available
      if (!installation.isEnvironmentAvailable())
        retryHandler.retryBundledNodeJsDownload();

      INodeJSEnvironment environment = installation.getEnvironment();

      List<String> packagesToInstall = getRequiredGlobalPackages();
      NPMCommandExecutor npm = new NPMCommandExecutor(executor, environment, true, rootFolder.getAbsolutePath());

      boolean install = !npm.list(packagesToInstall.toArray(new String[0]));

      boolean changes = false;

      if (install)
      {
        changes = true;
        String display = String.join(", ", packagesToInstall);
        _LOGGER.info(Bundle.LBL_Progress_Download(display));
        handle.setDisplayName(Bundle.LBL_Progress_Download(display));
        npm.install(packagesToInstall.toArray(new String[0]));
      }

      // download and install all "preinstalled" packages, so they will be available at runtime
      for (String pkg : packagesToInstall)
      {
        // Update if installed but outdated
        if (npm.outdated(pkg))
        {
          changes = true;
          _LOGGER.info(Bundle.LBL_Progress_Update(pkg));
          handle.setDisplayName(Bundle.LBL_Progress_Update(pkg));
          npm.update(pkg);
        }
      }

      if (changes)
      {
        DesignerBusUtils.fireModuleChange();
        installSubject.onNext(System.currentTimeMillis());
      }
    }
  }

  /**
   * Returns the global packages that are required by this plugin.
   * Packages may include versions.
   *
   * @return list of package specifications
   */
  protected static List<String> getRequiredGlobalPackages()
  {
    return List.of("typescript@4.5.2", "typescript-language-server@0.7.1");
  }

  /**
   * Determines, if the integrity of pTarget can be checked and the check is OK
   *
   * @param pTarget  target to verify
   * @param pVersion version to check
   * @return true, if OK
   */
  private boolean _isIntegrityOK(@NonNull File pTarget, @NonNull String pVersion)
  {
    File file = new File(pTarget, _INSTALLER_INTEGRITYCHECK_FILE);
    if (!file.exists() || !file.isFile() || !file.canRead())
      return false;

    try
    {
      String version = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8).get(0);
      return pVersion.equals(version);
    }
    catch (Exception e)
    {
      return false;
    }
  }

  /**
   * Updates the integrity of the given target to pVersion
   *
   * @param pTarget  target to update
   * @param pVersion version to set
   */
  private void _updateIntegrity(@NonNull File pTarget, @NonNull String pVersion)
  {
    File file = new File(pTarget, _INSTALLER_INTEGRITYCHECK_FILE);

    if (file.exists())
      //noinspection ResultOfMethodCallIgnored
      file.delete();
    else
      //noinspection ResultOfMethodCallIgnored
      file.getParentFile().mkdirs();

    try
    {
      Files.write(file.toPath(), pVersion.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * Extra class, so we can count and analyze the retries for downloading bundled nodejs
   */
  private class _NodeJSDownloadRetryHandler
  {
    @Counted(name = "nodejs.bundled.download.retryhandler")
    public void retryBundledNodeJsDownload() throws IOException
    {
      NodeJSInstallation installation = NodeJSInstallation.getCurrent();

      int countRetries = 0;
      while (!installation.isEnvironmentAvailable() && countRetries < 3)
      {
        downloadBundledNodeJS();
        countRetries++;
      }
    }
  }
}
