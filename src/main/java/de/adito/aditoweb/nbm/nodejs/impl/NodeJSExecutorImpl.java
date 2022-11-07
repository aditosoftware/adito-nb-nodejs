package de.adito.aditoweb.nbm.nodejs.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.notification.INotificationFacade;
import org.apache.commons.io.output.NullOutputStream;
import org.buildobjects.process.*;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileUtil;
import org.openide.modules.Places;
import org.openide.util.BaseUtilities;
import org.openide.util.lookup.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * @author w.glanzer, 08.03.2021
 */
@ServiceProvider(service = INodeJSExecutor.class, path = "Projects/de-adito-project/StaticLookup")
public class NodeJSExecutorImpl implements INodeJSExecutor
{
  private static final String _PATH_ENVIRONMENT = "PATH";

  private final File workingDir;
  private final ExecutorService processExecutor = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                                                                                    .setDaemon(true)
                                                                                    .setNameFormat("tNodeJSExecutor-%d")
                                                                                    .build());

  /**
   * @param pDirectory Directory to execute commands in
   * @return an executor that is not bound to a project
   */
  @NotNull
  public static INodeJSExecutor getInternalUnboundExecutor(@NotNull File pDirectory)
  {
    return new NodeJSExecutorImpl(pDirectory);
  }

  @SuppressWarnings("unused") // ServiceProvider
  public NodeJSExecutorImpl()
  {
    this(new File("."));
  }

  @SuppressWarnings("unused") // ServiceProvider
  public NodeJSExecutorImpl(@NotNull Project pProject)
  {
    this(FileUtil.toFile(pProject.getProjectDirectory()));
  }

  private NodeJSExecutorImpl(@NotNull File pWorkingDir)
  {
    workingDir = pWorkingDir;
  }

  @NotNull
  @Override
  public String executeSync(@NotNull INodeJSEnvironment pEnv, @NotNull INodeJSExecBase pBase, long pTimeout, @NotNull String... pParams)
      throws IOException, InterruptedException, TimeoutException
  {
    return executeSync(pEnv, pBase, pTimeout, true, pParams);
  }

  @NotNull
  @Override
  public String executeSync(@NotNull INodeJSEnvironment pEnv, @NotNull INodeJSExecBase pBase, long pTimeout, boolean pIncludeStdErr, @NotNull String... pParams)
      throws IOException, InterruptedException, TimeoutException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStream errBaos = pIncludeStdErr ? baos : NullOutputStream.NULL_OUTPUT_STREAM;

    // create and start
    Future<Integer> process = _executeAsync(pEnv, pBase, baos, errBaos, null, false, pParams);

    try
    {
      // wait until finished
      if (pTimeout > -1)
        process.get(pTimeout, TimeUnit.MILLISECONDS);
      else
        process.get();
    }
    catch (ExecutionException e)
    {
      throw new IOException(e);
    }

    // Copy result to string and trim trailing linebreak
    return baos.toString(StandardCharsets.UTF_8).trim();
  }

  @NotNull
  @Override
  public Process execute(@NotNull INodeJSEnvironment pEnv, @NotNull INodeJSExecBase pBase, @NotNull String... pParams) throws IOException
  {
    // Invalid Environment
    _checkValid(pEnv);

    // Prepare Process
    ArrayList<String> params = new ArrayList<>(Arrays.asList(pParams));
    params.add(0, _getCommandPath(pEnv, pBase).getAbsolutePath());

    ProcessBuilder builder = new ProcessBuilder(params)
        .directory(workingDir);

    // redirect error stream to prevent randomly blocking ui...
    // I dont know exactly why this fix works, but .. hey, it works
    File err_log = new File(Places.getUserDirectory(), "var/log/npm_err.log");

    //noinspection ResultOfMethodCallIgnored
    err_log.createNewFile();

    builder = builder.redirectError(err_log);

    return builder.start();
  }

  @NotNull
  @Override
  public CompletableFuture<Integer> executeAsync(@NotNull INodeJSEnvironment pEnv, @NotNull INodeJSExecBase pBase,
                                                 @NotNull OutputStream pDefaultOut, @Nullable OutputStream pErrorOut, @Nullable InputStream pDefaultIn,
                                                 @NotNull String... pParams)
  {
    return _executeAsync(pEnv, pBase, pDefaultOut, pErrorOut, pDefaultIn, true, pParams);
  }

  private CompletableFuture<Integer> _executeAsync(@NotNull INodeJSEnvironment pEnv, @NotNull INodeJSExecBase pBase,
                                                   @NotNull OutputStream pDefaultOut, @Nullable OutputStream pErrorOut, @Nullable InputStream pDefaultIn,
                                                   boolean pFlushDuringExecution, @NotNull String... pParams)
  {
    if (pErrorOut == null)
      pErrorOut = pDefaultOut;

    OutputStream finalDefaultOut;
    OutputStream finalErrorOut;
    // 1. Flushing output
    if (pFlushDuringExecution)
    {
      // flush output every 500 ms
      ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                                                                                          .setDaemon(true)
                                                                                          .setNameFormat("tNodeJsExecutorFlusher-" +
                                                                                                             String.join(" ", pParams) + "-%d")
                                                                                          .build());

      finalDefaultOut = new OutputStreamWrapper(pDefaultOut, scheduler::shutdownNow);
      finalErrorOut = new OutputStreamWrapper(pErrorOut, scheduler::shutdownNow);
      scheduler.scheduleWithFixedDelay(() -> {
        try
        {
          finalDefaultOut.flush();
          finalErrorOut.flush();
        }
        catch (IOException pE)
        {
          INotificationFacade.INSTANCE.error(pE);
        }
      }, 500, 500, TimeUnit.MILLISECONDS);
    }
    else
    {
      finalDefaultOut = pDefaultOut;
      finalErrorOut = pErrorOut;
    }

    // 2. execute
    AtomicReference<Thread> executionThreadRef = new AtomicReference<>(null);
    AtomicReference<Process> processRef = new AtomicReference<>(null);
    AtomicBoolean isRunning = new AtomicBoolean(true);
    CompletableFuture<Integer> executionFuture = CompletableFuture.supplyAsync(() -> {
      executionThreadRef.set(Thread.currentThread());
      // Invalid Environment
      _checkValid(pEnv);

      // Prepare Process
      ProcBuilder builder = new ProcBuilder(_getCommandPath(pEnv, pBase).getAbsolutePath(), pParams);

      // set all environments variables
      for (Map.Entry<String, String> entry : System.getenv().entrySet())
      {
        String key = entry.getKey();
        // modify path with our node environment
        if (key.equalsIgnoreCase(_PATH_ENVIRONMENT))
          builder.withVar(_PATH_ENVIRONMENT, pEnv.getPath().getParent() + _getSeparator() + entry.getValue());
        else
          builder.withVar(entry.getKey(), entry.getValue());
      }

      // add all system properties
      System.getProperties()
          // dots are not allowed as key => replacing with underscore
          .forEach((pKey, pValue) -> builder.withVar(((String) pKey).replaceAll("\\.", "_").toUpperCase(), (String) pValue));

      builder.withWorkingDirectory(workingDir)
          .withOutputStream(finalDefaultOut)
          .withErrorStream(finalErrorOut)
          .withInputStream(pDefaultIn)
          .withNoTimeout()
          .withOnCreationHandler(processRef::set)
          .ignoreExitStatus();

      // log command
      _logCommand(builder, finalDefaultOut);

      ProcResult res = builder.run();
      isRunning.set(false);
      return res.getExitValue();
    }, processExecutor);

    // 3. future which is returned. If this future is cancelled, the thread of the execution future is interrupted, so the execution future finishes
    // normally
    CompletableFuture<Integer> waitingFuture = CompletableFuture.supplyAsync(() -> {
      while (true)
      {
        try
        {
          if (executionFuture.isDone())
            return executionFuture.get();

          //noinspection BusyWait
          Thread.sleep(50);
        }
        catch (Exception e) // NOSONAR thread should not be interrupted
        {
          INotificationFacade.INSTANCE.error(e);
          return Integer.MAX_VALUE; // return max -> we do not know the exit value, because something bad happened during creation time of our process
        }
      }
    }, processExecutor);

    waitingFuture.handle((pExit, pThrowable) -> {
      if (executionThreadRef.get() != null && executionThreadRef.get().isAlive() && isRunning.get())
      {
        try
        {
          if (processRef.get() != null)
            SendCtrlC.getInstance().send(processRef.get().pid());
        }
        catch (IOException ex)
        {
          executionThreadRef.get().interrupt();
        }
      }

      return pExit;
    });

    // 4. Cleaning up future
    CompletableFuture<Integer> cleanUpFuture = executionFuture;
    if (pFlushDuringExecution)
    {
      // Print the exit-code
      cleanUpFuture = executionFuture.handle((pExit, pThrowable) -> {
        try
        {
          String message = "\nProcess finished";
          if (pExit != null)
            message = message + " with exit code " + pExit + "\n";
          finalDefaultOut.write(message.getBytes(StandardCharsets.UTF_8));

          if (pThrowable != null)
            finalErrorOut.write(pThrowable.getMessage().getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException pE)
        {
          INotificationFacade.INSTANCE.error(pE);
        }

        return pExit;
      });
    }

    // 5. shutdown scheduler and close streams
    cleanUpFuture.whenComplete((pExit, pThrowable) -> {
      try
      {
        finalDefaultOut.flush();
        finalDefaultOut.close();
        finalErrorOut.flush();
        finalErrorOut.close();
      }
      catch (Exception pE)
      {
        INotificationFacade.INSTANCE.error(pE);
      }
    });

    return waitingFuture;
  }

  @NotNull
  private String _getSeparator()
  {
    if (BaseUtilities.isWindows())
      return ";";
    return ":";
  }

  /**
   * Checks if the given environment is valid
   *
   * @param pEnv environment to check
   */
  private void _checkValid(@NotNull INodeJSEnvironment pEnv)
  {
    if (!pEnv.isValid())
      throw new IllegalStateException("Failed to execute command on nodejs, because version is invalid (" + pEnv + ")");
  }

  /**
   * Constructs the correct command path
   *
   * @param pEnv  environment
   * @param pBase base
   * @return the path file
   */
  @NotNull
  private File _getCommandPath(@NotNull INodeJSEnvironment pEnv, @NotNull INodeJSExecBase pBase)
  {
    return pBase.isRelativeToWorkingDir() ? new File(workingDir, pBase.getBasePath()) : pEnv.resolveExecBase(pBase);
  }

  /**
   * Logs the command of the given proc builder to the given output stream
   *
   * @param pBuilder Builder
   * @param pOut     Stream to log to
   */
  private void _logCommand(@NotNull ProcBuilder pBuilder, @NotNull OutputStream pOut)
  {
    try
    {
      pOut.write(pBuilder.getCommandLine().getBytes(StandardCharsets.UTF_8));
      pOut.write('\n');
      pOut.flush();
    }
    catch (Exception e)
    {
      // do nothing, just dont log
    }
  }

  /**
   * Delegating OutputStream, which executes a runnable if the stream is closed
   */
  private static class OutputStreamWrapper extends OutputStream
  {
    private final OutputStream delegate;
    private Runnable onClosed;

    public OutputStreamWrapper(@NotNull OutputStream pDelegate, @NotNull Runnable pOnClosed)
    {
      delegate = pDelegate;
      onClosed = pOnClosed;
    }

    @Override
    public void write(@NotNull byte[] b) throws IOException
    {
      delegate.write(b);
    }

    @Override
    public void write(@NotNull byte[] b, int off, int len) throws IOException
    {
      delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException
    {
      delegate.flush();
    }

    @Override
    public void close() throws IOException
    {
      if (onClosed != null)
        onClosed.run();

      onClosed = null;
      flush();
      delegate.close();
    }

    @Override
    public void write(int b) throws IOException
    {
      delegate.write(b);
    }
  }

}
