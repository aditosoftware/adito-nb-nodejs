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

  /**
   * Returns the arguments for the {@link NodeJSInstallerTest#shouldDisableSymlinksDependingOnTheNodeJsVersion} test.
   *
   * @return stream of arguments
   */
  @NotNull
  static Stream<Arguments> shouldDisableSymlinksDependingOnTheNodeJsVersion()
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
   *
   * @param pSymlinkProperty        value of the symlink property, if null the property gets cleared
   * @param pIsEnvironmentAvailable whether the nodejs environment will be available
   * @param pNodeVersion            the mocked node js version, since doesNodeCreateSymlinksOnNpmInstall is tested separately
   *                                doesNodeCreateSymlinksOnNpmInstall(pNodeVersion) will be used to check,
   *                                whether the system property should be set or not
   */
  @ParameterizedTest
  @MethodSource
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
   * Returns the arguments for the {@link NodeJSInstallerTest#shouldCheckIfNodeJsCreatesSymlinksOnNpmInstall} test.
   *
   * @return stream of arguments
   */
  static Stream<Arguments> shouldCheckIfNodeJsCreatesSymlinksOnNpmInstall()
  {
    return Stream.of(
        Arguments.of("v16.1.2", true),
        Arguments.of("v17.167867867.3542", true),
        Arguments.of("v18.12345.56789", false),
        Arguments.of("v123456789.12345.12345", false)
    );
  }

  /**
   * Checks whether doesNodeCreateSymlinksOnNpmInstall parses and checks the version correctly.
   *
   * @param pVersion            the nodejs version as string (output from node --version)
   * @param pDoesCreateSymlinks whether the nodejs version creates symlinks on npm install or not
   */
  @ParameterizedTest
  @MethodSource
  void shouldCheckIfNodeJsCreatesSymlinksOnNpmInstall(@NotNull String pVersion, boolean pDoesCreateSymlinks)
  {
    Assertions.assertEquals(pDoesCreateSymlinks, NodeJSInstaller.doesNodeCreateSymlinksOnNpmInstall(pVersion));
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
