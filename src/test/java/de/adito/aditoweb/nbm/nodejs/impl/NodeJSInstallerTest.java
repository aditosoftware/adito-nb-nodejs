package de.adito.aditoweb.nbm.nodejs.impl;

import de.adito.aditoweb.nbm.nodejs.impl.options.downloader.INodeJSDownloader;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.io.*;

/**
 * @author w.glanzer, 11.05.2021
 */
class NodeJSInstallerTest
{

  private static File target;
  private static MockedStatic<NodeJSInstallation> nodejsMock;
  private NodeJSInstaller installer;

  @BeforeAll
  static void beforeAll() throws IOException
  {
    target = new File("./target/bundled_nodejs");

    // clean previously used directory
    FileUtils.deleteDirectory(target);

    // mock
    nodejsMock = Mockito.mockStatic(NodeJSInstallation.class);
    nodejsMock.when(NodeJSInstallation::getCurrent)
        .thenReturn(new NodeJSInstallation(target, true));
  }

  @AfterAll
  static void afterAll()
  {
    nodejsMock.close();
  }

  @BeforeEach
  void setUp()
  {
    installer = new NodeJSInstaller();
  }

  @Test
  void shouldDownloadBundledNodeJsAndResolveNodeExecutable() throws Exception
  {
    installer.downloadBundledNodeJS();

    Assertions.assertTrue(target.exists());
    Assertions.assertTrue(target.isDirectory());
    Assertions.assertNotNull(INodeJSDownloader.getInstance().findNodeExecutableInInstallation(target));
  }

  @Test
  void shouldDownloadBundledNodeJsAndInstallTypeScriptLanguageServer() throws Exception
  {
    installer.downloadBundledNodeJS();
    installer.downloadOrUpdateBundledTypeScript();

    File module = new File(target, "node_modules/" + IBundledPackages.TYPESCRIPT_LANGUAGE_SERVER_NAME);
    Assertions.assertTrue(module.exists());
    Assertions.assertTrue(module.isDirectory());
  }

}