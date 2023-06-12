package de.adito.aditoweb.nbm.nodejs.impl.options.downloader;

import lombok.NonNull;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.List;

/**
 * Automatically download NodeJS versions
 *
 * @author w.glanzer, 10.03.2021
 */
public interface INodeJSDownloader
{

  @NonNull
  static INodeJSDownloader getInstance()
  {
    return NodeJSDownloaderImpl.INSTANCE;
  }

  /**
   * Retrieves all available versions
   *
   * @return versions, sorted descending (in correct "versioning" order)
   */
  @NonNull
  List<String> getAvailableVersions() throws IOException;

  /**
   * Detects the appropriate binary to use in nodejs installation
   *
   * @param pInstallation installation folder of nodejs
   * @return the binary file or null if not found
   */
  @Nullable
  File findNodeExecutableInInstallation(@NonNull File pInstallation);

  /**
   * Detects the node installation folder from a binary
   *
   * @param pBinary binary
   * @return the installation root
   */
  @Nullable
  File findInstallationFromNodeExecutable(@NonNull File pBinary);

  /**
   * Downloads a nodejs version to pTarget.
   *
   * @param pVersion Version to download (has to be a version from getAvailableVersions())
   * @param pTarget  Target Folder
   * @return the binary nodejs target
   */
  @NonNull
  File downloadVersion(@NonNull String pVersion, @NonNull File pTarget) throws IOException;

}
