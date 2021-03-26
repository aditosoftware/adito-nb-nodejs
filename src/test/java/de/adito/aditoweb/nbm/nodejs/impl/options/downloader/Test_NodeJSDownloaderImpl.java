package de.adito.aditoweb.nbm.nodejs.impl.options.downloader;

import org.junit.jupiter.api.*;
import org.openide.util.Pair;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.logging.Logger;
import java.util.stream.*;

/**
 * @author w.glanzer, 10.03.2021
 */
public class Test_NodeJSDownloaderImpl
{

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
    List<File> validDownloads = new ArrayList<>();
    List<String> invalidDownloads = new ArrayList<>();

    // test
    BiConsumer<String, NodeJSDownloaderImpl.OS_SUFFIX> testDownload = (pVersion, pSuf) -> {
      try
      {
        File bin = downloader.downloadVersion(pVersion, new File("target/nodejsdownload_test/" + pSuf.getSuffix() + "/"), pSuf);
        validDownloads.add(bin);
      }
      catch (Exception e)
      {
        Logger.getLogger(Test_NodeJSDownloaderImpl.class.getName()).warning("Download invalid: " + pVersion);
        invalidDownloads.add(pVersion);
      }
    };

    // execute async
    //noinspection FuseStreamOperations unreadable...
    Set<CompletableFuture<?>> futures = downloader.getAvailableVersions().stream()
        .flatMap(pVersion -> Stream.of(NodeJSDownloaderImpl.OS_SUFFIX.values()).map(pSuf -> Pair.of(pVersion, pSuf)))
        .map(pPair -> CompletableFuture.runAsync(() -> testDownload.accept(pPair.first(), pPair.second())))
        .collect(Collectors.toSet());

    // await all
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

    // check
    Assertions.assertTrue(invalidDownloads.isEmpty());
    Assertions.assertFalse(validDownloads.isEmpty());
    for (File downloadedBin : validDownloads)
    {
      Assertions.assertTrue(downloadedBin.exists());
      Assertions.assertTrue(downloadedBin.isFile());
    }
  }
}
