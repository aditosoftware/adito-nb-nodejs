package de.adito.aditoweb.nbm.nodejs.impl;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.openide.util.BaseUtilities;

import java.io.*;
import java.util.concurrent.TimeoutException;

/**
 * @author w.glanzer, 25.10.2021
 */
class NPMCommandExecutorTest
{

  private static final String _MODULE_TO_INSTALL = "big.js";
  private static final String _OLD_MODULE_VERSION = "6.0.3";
  private File target;
  private NPMCommandExecutor npm;
  private MockedStatic<NodeJSInstallation> nodejsMock;

  @BeforeEach
  void beforeEach() throws IOException, TimeoutException, InterruptedException
  {
    target = new File("./target/bundled_nodejs_commands");

    // clean previously used directory
    FileUtils.deleteDirectory(target);

    // mock
    nodejsMock = Mockito.mockStatic(NodeJSInstallation.class);
    nodejsMock.when(NodeJSInstallation::getCurrent)
        .thenReturn(new NodeJSInstallation(target, true));

    // download nodejs
    NodeJSInstaller installer = new NodeJSInstaller();
    installer.downloadBundledNodeJS();
    installer.downloadRequiredGlobalPackages();

    // save
    INodeJSExecutor executor = NodeJSInstallation.getCurrent().getExecutor();
    INodeJSEnvironment environment = NodeJSInstallation.getCurrent().getEnvironment();
    npm = new NPMCommandExecutor(executor, environment, true, target.getAbsolutePath());
  }

  @AfterEach
  void afterEach()
  {
    nodejsMock.close();
  }

  @Test
  void shouldInstallPackage() throws InterruptedException, TimeoutException, IOException
  {
    String packageDir = BaseUtilities.isWindows() ? "node_modules/" : "lib/node_modules/";
    File moduleFolder = new File(target, packageDir + _MODULE_TO_INSTALL);

    // must not be there
    Assertions.assertFalse(moduleFolder.exists());

    // install
    npm.install(_MODULE_TO_INSTALL);

    // it has to be there now
    Assertions.assertTrue(moduleFolder.exists());
    Assertions.assertTrue(moduleFolder.isDirectory());
  }

  @Test
  void shouldUpdatePackage() throws InterruptedException, TimeoutException, IOException
  {
    // install old version
    npm.install(_MODULE_TO_INSTALL + "@" + _OLD_MODULE_VERSION);

    // it has to be outdated, because we installed an old version
    Assertions.assertTrue(npm.outdated(_MODULE_TO_INSTALL));

    // update the package
    npm.update(_MODULE_TO_INSTALL);

    // it has to be the newest version, because we updated it beforehand
    Assertions.assertFalse(npm.outdated(_MODULE_TO_INSTALL));
  }

  @Test
  void shouldReportOldPackageVersion() throws InterruptedException, TimeoutException, IOException
  {
    // install old version
    npm.install(_MODULE_TO_INSTALL + "@" + _OLD_MODULE_VERSION);

    // it has to be outdated, because we installed an old version
    Assertions.assertTrue(npm.outdated(_MODULE_TO_INSTALL));
  }

  @Test
  void shouldNotReportUpToDatePackageVersionAsOutdated() throws InterruptedException, TimeoutException, IOException
  {
    // install current version
    npm.install(_MODULE_TO_INSTALL);

    // it has to be outdated, because we installed an old version
    Assertions.assertFalse(npm.outdated(_MODULE_TO_INSTALL));
  }

  @Test
  void shouldListInstalledPackages() throws InterruptedException, TimeoutException, IOException
  {
    // not installed
    Assertions.assertFalse(npm.list(_MODULE_TO_INSTALL));

    // install
    npm.install(_MODULE_TO_INSTALL);

    // installed
    Assertions.assertTrue(npm.list(_MODULE_TO_INSTALL));
  }

}
