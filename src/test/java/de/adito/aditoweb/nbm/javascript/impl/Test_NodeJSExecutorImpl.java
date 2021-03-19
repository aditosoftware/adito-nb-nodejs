package de.adito.aditoweb.nbm.javascript.impl;

import de.adito.aditoweb.nbm.javascript.impl.options.downloader.INodeJSDownloader;
import de.adito.aditoweb.nbm.javascript.impl.version.NodeJSEnvironmentFactory;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import org.junit.jupiter.api.*;

import java.io.File;

/**
 * @author w.glanzer, 19.03.2021
 */
public class Test_NodeJSExecutorImpl
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
  void test_executeSimple() throws Exception
  {
    Assertions.assertEquals("v15.12.0", executor.executeSync(env, INodeJSExecBase.node(), "--version", 2000));
  }

  @Test
  void test_resolvBase()
  {
    Assertions.assertTrue(env.resolveExecBase(INodeJSExecBase.node()).exists());
    Assertions.assertTrue(env.resolveExecBase(INodeJSExecBase.packageManager()).exists());
  }

}
