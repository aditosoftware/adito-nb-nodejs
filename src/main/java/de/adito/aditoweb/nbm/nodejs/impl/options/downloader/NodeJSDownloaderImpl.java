package de.adito.aditoweb.nbm.nodejs.impl.options.downloader;

import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.buildobjects.process.ProcBuilder;
import org.jetbrains.annotations.*;
import org.openide.util.BaseUtilities;
import org.rauschig.jarchivelib.*;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * @author w.glanzer, 10.03.2021
 */
public class NodeJSDownloaderImpl implements INodeJSDownloader
{

  protected static final NodeJSDownloaderImpl INSTANCE = new NodeJSDownloaderImpl();
  private static final String _NODEJS_URL = "https://nodejs.org/dist/";
  private static final Pattern _VERSION_REGEX = Pattern.compile("<a href=\"([^\"]*)\">");
  private static final Set<String> _IGNORED_VERSIONS = Set.of("v6.", "v5.", "v4.", "v0.");

  @NotNull
  @Override
  public List<String> getAvailableVersions() throws IOException
  {
    try
    {
      Set<String> result = new HashSet<>();
      String versions = IOUtils.toString(URI.create(_NODEJS_URL), StandardCharsets.UTF_8);
      Matcher matcher = _VERSION_REGEX.matcher(versions);
      int position = 0;
      while (matcher.find(position))
      {
        String name = matcher.group(1);
        name = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;

        // only list real versions
        if (name.startsWith("v"))
          result.add(name);

        position = matcher.end();
      }

      // sort
      return result.stream()
          .filter(pVersion -> !_IGNORED_VERSIONS.contains(pVersion.substring(0, 3)))
          .sorted(VersionNumberComparator.getInstance().reversed())
          .collect(Collectors.toList());
    }
    catch (Exception e)
    {
      throw new IOException("Failed to retrieve sources from nodejs server", e);
    }
  }

  @Nullable
  @Override
  public File findNodeExecutableInInstallation(@NotNull File pInstallation)
  {
    return _findBinary(pInstallation, OS_SUFFIX.getCurrent());
  }

  @Nullable
  @Override
  public File findInstallationFromNodeExecutable(@NotNull File pBinary)
  {
    return _findInstallationRoot(pBinary, OS_SUFFIX.getCurrent());
  }

  @NotNull
  @Override
  public File downloadVersion(@NotNull String pVersion, @NotNull File pTarget) throws IOException
  {
    return _downloadVersion(pVersion, pTarget, OS_SUFFIX.getCurrent());
  }

  /**
   * Constructs a download url to download the appropriate version
   *
   * @param pVersion Version (from getAvailableVersions())
   * @param pOS      OS-Type to download
   * @return the URL
   */
  @NotNull
  protected String getDownloadURL(@NotNull String pVersion, @NotNull OS_SUFFIX pOS)
  {
    return _NODEJS_URL + pVersion + "/node-" + pVersion + "-" + pOS.getSuffix() + pOS.getFileEnding();
  }

  /**
   * Downloads a nodejs version to pTarget.
   *
   * @param pVersion Version to download (has to be a version from getAvailableVersions())
   * @param pTarget  Target Folder
   * @param pOsType  OS-Type
   * @return the binary nodejs target
   */
  @NotNull
  private File _downloadVersion(@NotNull String pVersion, @NotNull File pTarget, @NotNull OS_SUFFIX pOsType) throws IOException
  {
    File downloadedFile = new File(pTarget, pVersion + pOsType.getFileEnding());

    //noinspection ResultOfMethodCallIgnored
    downloadedFile.getParentFile().mkdirs();

    // Download
    try (FileOutputStream fos = new FileOutputStream(downloadedFile);
         InputStream is = URI.create(getDownloadURL(pVersion, pOsType)).toURL().openStream())
    {
      IOUtils.copy(is, fos);
    }

    if (!downloadedFile.exists() || !downloadedFile.canRead())
      throw new IOException("Failed to extract downloaded archive, because it does not exist (" + downloadedFile + ")");

    // Extract
    File extractedFolder = _extractWithLinks(downloadedFile, pTarget);

    // Delete Download
    Files.delete(downloadedFile.toPath());

    // find nodeJS binary
    File binary = _findBinary(extractedFolder, pOsType);
    if (binary == null || !binary.exists() || !binary.canRead())
      throw new IOException("Downloaded nodejs did not contain a valid nodejs binary (" + extractedFolder + ")");

    return binary;
  }

  /**
   * Extracts the given file to pTarget and respects links in the downloaded zip
   *
   * @param pFile   File to extract
   * @param pTarget Target folder
   * @return the extracted folder
   */
  @NotNull
  private File _extractWithLinks(@NotNull File pFile, @NotNull File pTarget) throws IOException
  {
    Archiver factory = ArchiverFactory.createArchiver(pFile);

    //noinspection ResultOfMethodCallIgnored
    pTarget.mkdirs();

    try
    {
      // try "tar" command on linux and mac, because of symlinks
      new ProcBuilder("tar", "-xzf", pFile.getAbsolutePath())
          .withWorkingDirectory(pTarget)
          .withNoTimeout()
          .run();
    }
    catch (Exception e)
    {
      // if an exception happens, then use jarchivelib - we should now be on windows (and windows does not have something like symlinks)
      factory.extract(pFile, pTarget);
    }

    // Get first extracted folder, because the node zip contains a subfolder..
    File extractedFolder;
    try (ArchiveStream stream = factory.stream(pFile))
    {
      extractedFolder = new File(pTarget, stream.getNextEntry().getName());
    }

    return extractedFolder;
  }

  /**
   * Detects the appropriate binary to use in nodejs installation
   *
   * @param pNodeJSInstallation installation folder of nodejs
   * @return the binary file or null if not found
   */
  @Nullable
  private File _findBinary(@NotNull File pNodeJSInstallation, @NotNull OS_SUFFIX pOSType)
  {
    File file = pOSType.getBinaryExtractor().apply(pNodeJSInstallation);
    if (file.exists() && file.canRead())
      return file;
    return null;
  }

  /**
   * Detects the appropriate installation root of the given nodejs binary
   *
   * @param pBinary binary
   * @return the installation root or null if not found
   */
  @Nullable
  private File _findInstallationRoot(@NotNull File pBinary, @NotNull OS_SUFFIX pOSType)
  {
    File file = pOSType.getInstallationExtractor().apply(pBinary);
    if (file.exists() && file.canRead() && file.isDirectory())
      return file;
    return null;
  }

  /**
   * The suffix for OS specific download
   */
  @Getter
  protected enum OS_SUFFIX
  {
    WINDOWS_X64("win-x64", "Windows (64 bit)", ".zip", pDir -> new File(pDir, "node.exe"), File::getParentFile),
    LINUX_X64("linux-x64", "Linux (64 bit)", ".tar.gz", pDir -> new File(new File(pDir, "bin"), "node"), pBin -> pBin.getParentFile().getParentFile()),
    MAC("darwin-x64", "MacOS", ".tar.gz", pDir -> new File(new File(pDir, "bin"), "node"), pBin -> pBin.getParentFile().getParentFile());

    private final String suffix;
    private final String displayName;
    private final String fileEnding;
    private final UnaryOperator<File> binaryExtractor;
    private final UnaryOperator<File> installationExtractor;

    OS_SUFFIX(@NotNull String pSuffix, @NotNull String pDisplayName, @NotNull String pFileEnding,
              @NotNull UnaryOperator<File> pBinaryExtractor, @NotNull UnaryOperator<File> pInstallationExtractor)
    {
      suffix = pSuffix;
      displayName = pDisplayName;
      fileEnding = pFileEnding;
      binaryExtractor = pBinaryExtractor;
      installationExtractor = pInstallationExtractor;
    }

    /**
     * @return the type of the current OS
     */
    @NotNull
    public static OS_SUFFIX getCurrent()
    {
      if (BaseUtilities.isMac())
        return MAC;
      else if (BaseUtilities.isUnix())
        return LINUX_X64;
      else if (BaseUtilities.isWindows())
        return WINDOWS_X64;

      // should not happen..
      throw new IllegalStateException("Failed to retrieve os system type");
    }
  }

}
