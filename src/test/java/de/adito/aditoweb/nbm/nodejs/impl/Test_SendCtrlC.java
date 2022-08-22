package de.adito.aditoweb.nbm.nodejs.impl;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.options.downloader.INodeJSDownloader;
import de.adito.aditoweb.nbm.nodejs.impl.version.NodeJSEnvironmentFactory;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.concurrent.Future;

/**
 * @author p.neub, 22.08.2022
 */
public class Test_SendCtrlC
{

  private INodeJSEnvironment env;
  private NodeJSExecutorImpl executor;

  @BeforeEach
  void setUp() throws Exception
  {
    // set custom instance of SendCtrlC that returns a path directly to the source resources for testing
    SendCtrlC.setInstance(new SendCtrlC()
    {
      @Override
      protected String getNativePath()
      {
        return "./src/main/resources/de/adito/aditoweb/nbm/nodejs/impl/native";
      }
    });

    // download nodejs to ./target/bundled_nodejs_sendctrlc
    File target = new File("./target/bundled_nodejs_sendctrlc");
    target.deleteOnExit();
    env = NodeJSEnvironmentFactory.create(INodeJSDownloader.getInstance().downloadVersion("v15.12.0", target));
    executor = new NodeJSExecutorImpl();
  }

  @Test
  void test_exitProcess() throws Exception
  {
    // create nodejs process, that console.logs its own pid and setIntervals infinitely, so the process doesn't exit on itself
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    String code = "setInterval(() => undefined, 1000); console.log(process.pid);";
    Future<Integer> future = executor.executeAsync(env, INodeJSExecBase.node(), outputStream, null, null, "-e", code);

    // sleep so nodejs can start
    Thread.sleep(5000);

    // terminate the process
    future.cancel(false);

    // wait, so the process has time to terminate
    Thread.sleep(5000);

    // get the console.logged pid from the process output
    String pid = outputStream.toString().split("\n")[1];

    // start nodejs script, that invokes process.kill on the previous nodejs process
    // the second argument '0' is a platform independent way to check if a process exists
    // if the process exists we continue and print "running", otherwise process.kill will throw an error, and we print "stopped"
    String validationCode = "try { process.kill('" + pid + "', 0); console.log('running'); } catch(ex) { console.log('stopped'); }";
    String validation = executor.executeSync(env, INodeJSExecBase.node(), -1, "-e", validationCode);

    // validate that the process is stopped by checking if the output is "stopped"
    Assertions.assertEquals("stopped", validation.split("\n")[1]);
  }

  @Test
  void test_sigintHandler() throws Exception
  {
    // create nodejs process that setIntervals infinitely, so the process doesn't exit on itself
    // console.logs "start" at startup
    // and console.logs "stop" if it gets terminated using sigint
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    String code = "setInterval(() => undefined, 1000); console.log('start'); process.on('SIGINT', () => { console.log('stop'); process.exit(); } );";
    Future<Integer> future = executor.executeAsync(env, INodeJSExecBase.node(), outputStream, null, null, "-e", code);

    // sleep so nodejs can start
    Thread.sleep(5000);

    // terminate the process
    future.cancel(false);

    // wait until the process is terminated
    Thread.sleep(5000);

    // check the output
    String[] lines = outputStream.toString().split("\n");
    Assertions.assertEquals("start", lines[1]);
    Assertions.assertEquals("stop", lines[2]);
  }
}
