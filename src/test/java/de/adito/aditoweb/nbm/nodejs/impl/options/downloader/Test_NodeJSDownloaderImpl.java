package de.adito.aditoweb.nbm.nodejs.impl.options.downloader;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.openide.util.Pair;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.*;

/**
 * @author w.glanzer, 10.03.2021
 */
class Test_NodeJSDownloaderImpl
{

  private static final Logger _LOGGER = Logger.getLogger(Test_NodeJSDownloaderImpl.class.getName());
  private NodeJSDownloaderImpl downloader;

  @BeforeEach
  void setUp()
  {
    downloader = new NodeJSDownloaderImpl();
  }

  @Test
  void test_getVersions() throws Exception
  {
    Assertions.assertFalse(downloader.getAvailableVersions().isEmpty());
  }

  @Test
  void test_validDownloadURL() throws Exception
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
        Logger.getLogger(Test_NodeJSDownloaderImpl.class.getName()).warning("URL invalid: " + pURL);
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
    Assertions.assertFalse(validURLs.isEmpty());
    Assertions.assertTrue(invalidURLs.isEmpty());
  }

  @Test
  void test_download() throws Exception
  {
    List<String> validDownloads = new ArrayList<>();
    List<String> invalidDownloads = new ArrayList<>();
    ExecutorService executor = Executors.newFixedThreadPool(8);

    // test
    BiConsumer<String, NodeJSDownloaderImpl.OS_SUFFIX> testDownload = (pVersion, pSuf) -> {
      File target = new File("target/nodejsdownload_test/" + pSuf.getSuffix() + "/");
      File binary = null;

      try
      {
        binary = downloader.downloadVersion(pVersion, target, pSuf);
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
        File fileToDelete = pSuf == NodeJSDownloaderImpl.OS_SUFFIX.WINDOWS_X64 ? binary.getParentFile() : binary.getParentFile().getParentFile();
        _LOGGER.info("Deleting previously used directory: " + fileToDelete.getAbsolutePath());
        FileUtils.deleteQuietly(fileToDelete);
      }
    };

    // execute async
    downloader.getAvailableVersions().stream()
        .flatMap(pVersion -> Stream.of(NodeJSDownloaderImpl.OS_SUFFIX.values()).map(pSuf -> Pair.of(pVersion, pSuf)))
        .forEach(pPair -> CompletableFuture.runAsync(() -> testDownload.accept(pPair.first(), pPair.second()), executor));

    // await all
    executor.shutdown();

    //noinspection ResultOfMethodCallIgnored we do not need this
    executor.awaitTermination(30, TimeUnit.MINUTES);

    // check
    Assertions.assertTrue(invalidDownloads.isEmpty());
    Assertions.assertFalse(validDownloads.isEmpty());
  }
}
