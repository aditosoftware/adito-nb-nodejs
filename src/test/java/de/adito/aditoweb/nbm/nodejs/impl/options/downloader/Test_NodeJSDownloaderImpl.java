package de.adito.aditoweb.nbm.nodejs.impl.options.downloader;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.openide.util.Pair;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.*;

/**
 * @author w.glanzer, 10.03.2021
 */
class Test_NodeJSDownloaderImpl
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
    List<String> validDownloads = new ArrayList<>();
    List<String> invalidDownloads = new ArrayList<>();

    // test
    BiConsumer<String, NodeJSDownloaderImpl.OS_SUFFIX> testDownload = (pVersion, pSuf) -> {
      File target = new File("target/nodejsdownload_test/" + pSuf.getSuffix() + "/");

      try
      {
        File bin = downloader.downloadVersion(pVersion, target, pSuf);
        if (bin.exists() && bin.isFile())
        {
          Logger.getLogger(Test_NodeJSDownloaderImpl.class.getName()).info("Download valid, binary exists and is valid: " + pVersion);
          validDownloads.add(pVersion);
        }
        else
        {
          Logger.getLogger(Test_NodeJSDownloaderImpl.class.getName()).warning("Download invalid, because binary does not exist or is invalid: " + pVersion);
          invalidDownloads.add(pVersion);
        }
      }
      catch (Exception e)
      {
        Logger.getLogger(Test_NodeJSDownloaderImpl.class.getName()).log(Level.WARNING, "Download invalid, because of an exception: " + pVersion, e);
        invalidDownloads.add(pVersion);
      }

      // Delete downloaded directory
      if (target.exists())
        FileUtils.deleteQuietly(target);
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
  }
}
