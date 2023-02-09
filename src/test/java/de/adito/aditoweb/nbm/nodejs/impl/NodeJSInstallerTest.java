package de.adito.aditoweb.nbm.nodejs.impl;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.INodeJSEnvironment;
import de.adito.aditoweb.nbm.nodejs.impl.options.downloader.INodeJSDownloader;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.openide.util.BaseUtilities;

import java.io.*;
import java.util.stream.Stream;

/**
 * @author w.glanzer, 11.05.2021
 */
class NodeJSInstallerTest
{

  private static File target;
  private NodeJSInstaller installer;

  @BeforeAll
  static void beforeAll() throws IOException
  {
    target = new File("./target/bundled_nodejs");

    // clean previously used directory
    FileUtils.deleteDirectory(target);
  }

  @BeforeEach
  void setUp()
  {
    installer = new NodeJSInstaller();
  }

  @NotNull
  static Stream<Arguments> shouldDisableSymlinksDependingOnTheNodeJsVersionSource()
  {
    return Stream.of(
        Arguments.of(null, false, "v1.0.0"),
        Arguments.of(null, true, "v1.0.0"),
        Arguments.of(null, true, "v20.0.0"),
        Arguments.of(Boolean.TRUE.toString(), false, "v20.0.0"),
        Arguments.of(Boolean.FALSE.toString(), false, "v20.0.0")
    );
  }

  /**
   * Checks that symlinks are disabled if the nodejs version does not create symlinks on npm install.
   */
  @ParameterizedTest
  @MethodSource("shouldDisableSymlinksDependingOnTheNodeJsVersionSource")
  void shouldDisableSymlinksDependingOnTheNodeJsVersion(@Nullable String pSymlinkProperty, boolean pIsEnvironmentAvailable, @NotNull String pNodeVersion)
  {
    if (pSymlinkProperty == null)
      System.clearProperty(NodeJSInstaller.IS_INCLUDE_SYMLINKS_PROPERTY);
    else
      System.setProperty(NodeJSInstaller.IS_INCLUDE_SYMLINKS_PROPERTY, pSymlinkProperty);

    try (MockedStatic<NodeJSInstallation> installationStaticMock = Mockito.mockStatic(NodeJSInstallation.class))
    {
      INodeJSEnvironment environmentMock = Mockito.mock(INodeJSEnvironment.class);
      Mockito.when(environmentMock.getVersion()).thenReturn(pNodeVersion);

      NodeJSInstallation installationMock = Mockito.mock(NodeJSInstallation.class);
      Mockito.when(installationMock.isEnvironmentAvailable()).thenReturn(pIsEnvironmentAvailable);
      Mockito.when(installationMock.getEnvironment()).thenReturn(environmentMock);

      installationStaticMock.when(NodeJSInstallation::getCurrent).thenReturn(installationMock);
      installer.disableSymlinksIfNodeMeetsTheRequiredVersion();

      if (pSymlinkProperty != null)
        installationStaticMock.verifyNoInteractions();
      else
      {
        Mockito.verify(installationMock).isEnvironmentAvailable();
        if (!pIsEnvironmentAvailable)
          Mockito.verify(installationMock, Mockito.never()).getEnvironment();
        else
        {
          Mockito.verify(installationMock).getEnvironment();
          if (!NodeJSInstaller.doesNodeCreateSymlinksOnNpmInstall(pNodeVersion))
            Assertions.assertEquals(Boolean.FALSE.toString(), System.getProperty(NodeJSInstaller.IS_INCLUDE_SYMLINKS_PROPERTY));
        }
      }
    }
  }

  /**
   * Checks whether doesNodeCreateSymlinksOnNpmInstall parses and checks the version correctly.
   */
  @Test
  void shouldCheckIfNodeJsCreatesSymlinksOnNpmInstall()
  {
    Assertions.assertTrue(NodeJSInstaller.doesNodeCreateSymlinksOnNpmInstall("v16.1.2"));
    Assertions.assertTrue(NodeJSInstaller.doesNodeCreateSymlinksOnNpmInstall("v17.167867867.3542"));
    Assertions.assertFalse(NodeJSInstaller.doesNodeCreateSymlinksOnNpmInstall("v18.12345.56789"));
    Assertions.assertFalse(NodeJSInstaller.doesNodeCreateSymlinksOnNpmInstall("v123456789.12345.12345"));
  }

  @Test
  void shouldDownloadBundledNodeJsAndResolveNodeExecutable() throws Exception
  {
    try (MockedStatic<NodeJSInstallation> installationMockedStatic = Mockito.mockStatic(NodeJSInstallation.class))
    {
      installationMockedStatic.when(NodeJSInstallation::getCurrent).thenReturn(new NodeJSInstallation(target, true));

      installer.downloadBundledNodeJS();

      Assertions.assertTrue(target.exists());
      Assertions.assertTrue(target.isDirectory());
      Assertions.assertNotNull(INodeJSDownloader.getInstance().findNodeExecutableInInstallation(target));
    }
  }

  @Test
  void shouldDownloadBundledNodeJsAndInstallGlobalPackages() throws Exception
  {
    try (MockedStatic<NodeJSInstallation> installationMockedStatic = Mockito.mockStatic(NodeJSInstallation.class))
    {
      installationMockedStatic.when(NodeJSInstallation::getCurrent).thenReturn(new NodeJSInstallation(target, true));

      installer.downloadBundledNodeJS();
      installer.downloadRequiredGlobalPackages();

      for (String packageSpec : NodeJSInstaller.getRequiredGlobalPackages())
      {
        int versionIndex = packageSpec.indexOf("@");
        String packageName = versionIndex < 0 ? packageSpec : packageSpec.substring(0, versionIndex);
        String packageDir = BaseUtilities.isWindows() ? "node_modules/" : "lib/node_modules/";
        File module = new File(target, packageDir + packageName);
        Assertions.assertTrue(module.exists());
        Assertions.assertTrue(module.isDirectory());
      }
    }
  }

}
