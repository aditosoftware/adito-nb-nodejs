package de.adito.aditoweb.nbm.nodejs.impl;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.options.downloader.INodeJSDownloader;
import de.adito.aditoweb.nbm.nodejs.impl.version.NodeJSEnvironmentFactory;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.stream.Stream;

/**
 * @author w.glanzer, 19.03.2021
 */
class NodeJSExecutorImplTest
{

  private INodeJSEnvironment env;
  private NodeJSExecutorImpl executor;

  @BeforeEach
  void setUp() throws Exception
  {
    File target = new File("target/executor_test_version");
    target.deleteOnExit();
    env = NodeJSEnvironmentFactory.create(INodeJSDownloader.getInstance().downloadVersion("v15.12.0", target));
    executor = new NodeJSExecutorImpl();
  }

  @Test
  void shouldExecuteNodeVersion() throws Exception
  {
    Assertions.assertEquals("v15.12.0", Stream.of(executor.executeSync(env, INodeJSExecBase.node(), 30000, "--version").split("\n"))
        .skip(1)
        .findFirst()
        .orElse(null));
  }

  @Test
  void shouldExecuteNpmVersion() throws Exception
  {
    Assertions.assertEquals("7.6.3", Stream.of(executor.executeSync(env, INodeJSExecBase.packageManager(), 30000, "--version").split("\n"))
        .skip(1)
        .findFirst()
        .orElse(null));
  }

  @Test
  void shouldResolveNodeAndNpmBinaryPaths()
  {
    Assertions.assertTrue(env.resolveExecBase(INodeJSExecBase.node()).exists());
    Assertions.assertTrue(env.resolveExecBase(INodeJSExecBase.packageManager()).exists());
  }

}
