package de.adito.aditoweb.nbm.nodejs.impl.options.downloader;

import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.*;
import org.openide.util.BaseUtilities;
import org.rauschig.jarchivelib.*;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
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

  @NotNull
  @Override
  public File downloadVersion(@NotNull String pVersion, @NotNull File pTarget) throws IOException
  {
    return downloadVersion(pVersion, pTarget, OS_SUFFIX.getCurrent());
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
  protected File downloadVersion(@NotNull String pVersion, @NotNull File pTarget, @NotNull OS_SUFFIX pOsType) throws IOException
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
    Archiver factory = ArchiverFactory.createArchiver(downloadedFile);
    factory.extract(downloadedFile, pTarget);

    // Get Folder
    File extractedFolder;
    try (ArchiveStream stream = factory.stream(downloadedFile))
    {
      extractedFolder = new File(pTarget, stream.getNextEntry().getName());
    }

    // Delete Download
    //noinspection ResultOfMethodCallIgnored
    downloadedFile.delete();

    // find nodeJS binary
    File binary = _findBinary(extractedFolder, pOsType);
    if (binary == null || !binary.exists() || !binary.canRead())
      throw new IOException("Downloaded nodejs did not contain a valid nodejs binary (" + extractedFolder + ")");

    return binary;
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
   * The suffix for OS specific download
   */
  @Getter
  protected enum OS_SUFFIX
  {
    WINDOWS_X64("win-x64", "Windows (64 bit)", ".zip", pDir -> new File(pDir, "node.exe")),
    LINUX_X64("linux-x64", "Linux (64 bit)", ".tar.gz", pDir -> new File(new File(pDir, "bin"), "node")),
    MAC("darwin-x64", "MacOS", ".tar.gz", pDir -> new File(new File(pDir, "bin"), "node"));

    private final String suffix;
    private final String displayName;
    private final String fileEnding;
    private final Function<File, File> binaryExtractor;

    OS_SUFFIX(@NotNull String pSuffix, @NotNull String pDisplayName, @NotNull String pFileEnding, @NotNull Function<File, File> pBinaryExtractor)
    {
      suffix = pSuffix;
      displayName = pDisplayName;
      fileEnding = pFileEnding;
      binaryExtractor = pBinaryExtractor;
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
      throw new RuntimeException("Failed to retrieve os system type");
    }
  }

}
