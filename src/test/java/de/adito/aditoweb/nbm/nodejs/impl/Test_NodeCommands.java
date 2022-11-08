package de.adito.aditoweb.nbm.nodejs.impl;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.io.*;
import java.util.concurrent.TimeoutException;

/**
 * @author w.glanzer, 25.10.2021
 */
class Test_NodeCommands
{

  private static final String _MODULE_TO_INSTALL = "big.js";
  private static final String _OLD_MODULE_VERSION = "6.0.3";
  private File target;
  private INodeJSExecutor executor;
  private INodeJSEnvironment environment;
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
    installer.downloadOrUpdateBundledTypeScript();

    // save
    executor = NodeJSInstallation.getCurrent().getExecutor();
    environment = NodeJSInstallation.getCurrent().getEnvironment();
  }

  @AfterEach
  void afterEach()
  {
    nodejsMock.close();
  }

  @Test
  void test_install() throws InterruptedException, TimeoutException, IOException
  {
    File moduleFolder = new File(target, "node_modules/" + _MODULE_TO_INSTALL);

    // must not be there
    Assertions.assertFalse(moduleFolder.exists());

    // install
    NodeCommands.install(executor, environment, target.getAbsolutePath(), _MODULE_TO_INSTALL);

    // it has to be there now
    Assertions.assertTrue(moduleFolder.exists());
    Assertions.assertTrue(moduleFolder.isDirectory());
  }

  @Test
  void test_update() throws InterruptedException, TimeoutException, IOException
  {
    // install old version
    NodeCommands.install(executor, environment, target.getAbsolutePath(), _MODULE_TO_INSTALL + "@" + _OLD_MODULE_VERSION);

    // it has to be outdated, because we installed an old version
    Assertions.assertTrue(NodeCommands.outdated(executor, environment, target.getAbsolutePath(), _MODULE_TO_INSTALL));

    // update the package
    NodeCommands.update(executor, environment, target.getAbsolutePath(), _MODULE_TO_INSTALL);

    // it has to be the newest version, because we updated it beforehand
    Assertions.assertFalse(NodeCommands.outdated(executor, environment, target.getAbsolutePath(), _MODULE_TO_INSTALL));
  }

  @Test
  void test_outdated() throws InterruptedException, TimeoutException, IOException
  {
    // install old version
    NodeCommands.install(executor, environment, target.getAbsolutePath(), _MODULE_TO_INSTALL + "@" + _OLD_MODULE_VERSION);

    // it has to be outdated, because we installed an old version
    Assertions.assertTrue(NodeCommands.outdated(executor, environment, target.getAbsolutePath(), _MODULE_TO_INSTALL));
  }

  @Test
  void test_outdated_notOutdated() throws InterruptedException, TimeoutException, IOException
  {
    // install current version
    NodeCommands.install(executor, environment, target.getAbsolutePath(), _MODULE_TO_INSTALL);

    // it has to be outdated, because we installed an old version
    Assertions.assertFalse(NodeCommands.outdated(executor, environment, target.getAbsolutePath(), _MODULE_TO_INSTALL));
  }

  @Test
  void test_list() throws InterruptedException, TimeoutException, IOException
  {
    // not installed
    Assertions.assertFalse(NodeCommands.list(executor, environment, target.getAbsolutePath(), _MODULE_TO_INSTALL));

    // install
    NodeCommands.install(executor, environment, target.getAbsolutePath(), _MODULE_TO_INSTALL);

    // installed
    Assertions.assertTrue(NodeCommands.list(executor, environment, target.getAbsolutePath(), _MODULE_TO_INSTALL));
  }

}
