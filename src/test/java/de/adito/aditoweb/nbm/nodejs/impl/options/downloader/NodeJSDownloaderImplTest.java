package de.adito.aditoweb.nbm.nodejs.impl.options.downloader;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.openide.util.Pair;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.*;
import java.util.stream.*;

/**
 * @author w.glanzer, 10.03.2021
 */
class NodeJSDownloaderImplTest
{

  private static final Logger _LOGGER = Logger.getLogger(NodeJSDownloaderImplTest.class.getName());
  private NodeJSDownloaderImpl downloader;

  @BeforeEach
  void setUp()
  {
    downloader = new NodeJSDownloaderImpl();
  }

  @Test
  void shouldHaveVersionsAvailable() throws Exception
  {
    Assertions.assertFalse(downloader.getAvailableVersions().isEmpty());
  }

  @Test
  void shouldBeValidDownloadUrls() throws Exception
  {
    List<String> invalidURLs = new ArrayList<>();
    List<String> validURLs = new ArrayList<>();

    // test
    Consumer<String> testUrl = pURL -> {
      // noinspection unused
      try (InputStream is = URI.create(pURL).toURL().openStream())
      {
        validURLs.add(pURL);
      }
      catch (Exception e)
      {
        Logger.getLogger(NodeJSDownloaderImplTest.class.getName()).warning("URL invalid: " + pURL);
        invalidURLs.add(pURL);
      }
    };

    // execute async
    //noinspection FuseStreamOperations unreadable...
    Set<CompletableFuture<?>> futures = downloader.getAvailableVersions().stream()
        .flatMap(pVersion -> Stream.of(NodeJSDownloaderImpl.OS_SUFFIX.values()).map(pSuf -> Pair.of(pVersion, pSuf)))
        .map(pPair -> downloader.getDownloadURL(pPair.first(), pPair.second()))
        .map(pURL -> CompletableFuture.runAsync(() -> testUrl.accept(pURL)))
        .collect(Collectors.toSet());

    // await all
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

    // check
    Assertions.assertFalse(validURLs.isEmpty(), invalidURLs::toString);
    Assertions.assertTrue(invalidURLs.isEmpty(), invalidURLs::toString);
  }

  @Test
  void shouldDownloadSuccessfully() throws Exception
  {
    List<String> validDownloads = new ArrayList<>();
    List<String> invalidDownloads = new ArrayList<>();
    ExecutorService executor = Executors.newSingleThreadExecutor();

    // test
    Consumer<String> testDownload = (pVersion) -> {
      File target = new File("target/nodejsdownload_test/");
      File binary = null;

      try
      {
        _LOGGER.info("Downloading version " + pVersion + " into " + target.getAbsolutePath());
        binary = downloader.downloadVersion(pVersion, target);
        if (binary.exists() && binary.isFile())
        {
          _LOGGER.info("Download valid, binary exists and is valid: " + pVersion);
          validDownloads.add(pVersion);
        }
        else
        {
          _LOGGER.warning("Download invalid, because binary does not exist or is invalid: " + pVersion);
          invalidDownloads.add(pVersion);
        }
      }
      catch (Exception e)
      {
        _LOGGER.log(Level.WARNING, "Download invalid, because of an exception: " + pVersion, e);
        invalidDownloads.add(pVersion);
      }

      // Delete downloaded directory
      if (binary != null && binary.exists())
      {
        File fileToDelete = INodeJSDownloader.getInstance().findInstallationFromNodeExecutable(binary);
        Assertions.assertNotNull(fileToDelete);

        _LOGGER.info("Deleting previously used directory: " + fileToDelete.getAbsolutePath());
        FileUtils.deleteQuietly(fileToDelete);
      }
    };

    // execute async
    List<String> availableVersions = downloader.getAvailableVersions();
    availableVersions.forEach(pVersion -> CompletableFuture.runAsync(() -> testDownload.accept(pVersion), executor));

    // await all
    executor.shutdown();

    //noinspection ResultOfMethodCallIgnored we do not need this
    executor.awaitTermination(30, TimeUnit.MINUTES);

    // check
    Assertions.assertTrue(invalidDownloads.isEmpty(), invalidDownloads::toString);
    Assertions.assertTrue(validDownloads.containsAll(availableVersions), invalidDownloads::toString);
  }
}
