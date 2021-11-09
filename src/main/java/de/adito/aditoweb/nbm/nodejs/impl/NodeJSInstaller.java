package de.adito.aditoweb.nbm.nodejs.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.adito.aditoweb.nbm.metrics.api.IMetricProxyFactory;
import de.adito.aditoweb.nbm.metrics.api.types.*;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.options.NodeJSOptions;
import de.adito.aditoweb.nbm.nodejs.impl.options.downloader.INodeJSDownloader;
import org.jetbrains.annotations.*;
import org.netbeans.api.progress.*;
import org.openide.util.NbBundle;
import org.openide.windows.OnShowing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * @author w.glanzer, 10.05.2021
 */
@OnShowing // to show progress, must not be too early..
public class NodeJSInstaller implements Runnable
{

  private static final String _INSTALLER_INTEGRITYCHECK_FILE = ".installer_integrity";
  private static final Logger _LOGGER = Logger.getLogger(NodeJSInstaller.class.getName());
  private static final ExecutorService _EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                                                                                         .setDaemon(true)
                                                                                         .setNameFormat("tNodeJSInstaller-%d")
                                                                                         .setPriority(Thread.MIN_PRIORITY)
                                                                                         .build());
  private final _NodeJSDownloadRetryHandler retryHandler = IMetricProxyFactory.proxy(new _NodeJSDownloadRetryHandler());

  @Override
  public void run()
  {
    _downloadLibraries();
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
        // download and / or install
        downloadBundledNodeJS();
        downloadOrUpdateBundledTypeScript();
      }
      catch (Exception e)
      {
        _LOGGER.log(Level.WARNING, "", e);
      }
    });
  }

  /**
   * Downloads the latest nodejs version, if no version is specified
   */
  @NbBundle.Messages("LBL_Progress_Download_Execute=Downloading NodeJS {0}...")
  protected void downloadBundledNodeJS() throws IOException
  {
    // do not download or update anything, if the nodejs container folder already exists and integrity is ok
    File target = BundledNodeJS.getInstance().getBundledNodeJSContainer();
    String version = BundledNodeJS.getInstance().getBundledVersion();
    if (_isIntegrityOK(target, version))
      return;

    try (ProgressHandle handle = ProgressHandleFactory.createSystemHandle(Bundle.LBL_Progress_DownloadLibraries(), null))
    {
      // handle progress
      handle.start();
      handle.switchToIndeterminate();

      // download
      handle.progress(Bundle.LBL_Progress_Download_Execute(version));

      INodeJSDownloader downloader = INodeJSDownloader.getInstance();
      File binFile = downloader.downloadVersion(version, target.getParentFile());
      File nodeVersionContainer = downloader.findInstallationFromNodeExecutable(binFile);

      // rename to target
      if (nodeVersionContainer != null)
        if (nodeVersionContainer.renameTo(target))
          binFile = downloader.findNodeExecutableInInstallation(target);

      // update integrity
      _updateIntegrity(target, version);

      // update options to use new binary
      if (binFile != null && !NodeJSOptions.getInstance().isPathValid())
        NodeJSOptions.update(NodeJSOptions.getInstance().toBuilder()
                                 .path(binFile.getAbsolutePath())
                                 .build());
    }
  }

  /**
   * Downloads the latest typescript-language-server
   */
  @NbBundle.Messages({
      "LBL_Progress_Download=Downloading {0}...",
      "LBL_Progress_Update=Updating {0}...",
      "LBL_Progress_Analyze=Analyzing {0}..."
  })
  protected void downloadOrUpdateBundledTypeScript() throws IOException, InterruptedException, TimeoutException
  {
    BundledNodeJS bundledNode = BundledNodeJS.getInstance();
    File target = bundledNode.getBundledNodeJSContainer();
    if (!target.exists())
      return;

    // Create node_modules folder to install the typescript module in the correct directory
    //noinspection ResultOfMethodCallIgnored
    new File(target, "node_modules").mkdir();

    // prepare
    List<String> packagesToInstall = IBundledPackages.getPreinstalledPackages();
    INodeJSExecutor executor = bundledNode.getBundledExecutor();

    // try it multiple times, sometimes no NodeJS is available
    if (!bundledNode.isBundledEnvironmentAvailable())
      retryHandler.retryBundledNodeJsDownload();

    INodeJSEnvironment environment = bundledNode.getBundledEnvironment();

    // download and install all "preinstalled" packages, so they will be available at runtime
    for (String pkg : packagesToInstall)
    {
      // Install always, otherwise npm gets overridden
      _LOGGER.info(Bundle.LBL_Progress_Download(pkg));
      NodeCommands.install(executor, environment, target.getAbsolutePath(), pkg);

      // Update if installed but outdated
      if (NodeCommands.outdated(executor, environment, target.getAbsolutePath(), pkg))
      {
        _LOGGER.info(Bundle.LBL_Progress_Update(pkg));
        NodeCommands.update(executor, environment, target.getAbsolutePath(), pkg);
      }
    }
  }

  /**
   * Determines, if the integrity of pTarget can be checked and the check is OK
   *
   * @param pTarget  target to verify
   * @param pVersion version to check
   * @return true, if OK
   */
  private boolean _isIntegrityOK(@NotNull File pTarget, @NotNull String pVersion)
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
  private void _updateIntegrity(@NotNull File pTarget, @NotNull String pVersion)
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
      BundledNodeJS bundledNode = BundledNodeJS.getInstance();

      int countRetries = 0;
      while (!bundledNode.isBundledEnvironmentAvailable() && countRetries < 3)
      {
        downloadBundledNodeJS();
        countRetries++;
      }
    }
  }
}
