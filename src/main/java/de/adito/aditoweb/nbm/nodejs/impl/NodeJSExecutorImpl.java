package de.adito.aditoweb.nbm.nodejs.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import org.buildobjects.process.ProcBuilder;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author w.glanzer, 08.03.2021
 */
@ServiceProvider(service = INodeJSExecutor.class, path = "Projects/de-adito-project/Lookup")
public class NodeJSExecutorImpl implements INodeJSExecutor
{

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
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    // create and start
    Future<Integer> process = executeAsync(pEnv, pBase, baos, baos, null, pParams);

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
    return new ProcessBuilder(params)
        .directory(workingDir)
        .start();
  }

  @NotNull
  @Override
  public Future<Integer> executeAsync(@NotNull INodeJSEnvironment pEnv, @NotNull INodeJSExecBase pBase,
                                      @NotNull OutputStream pDefaultOut, @Nullable OutputStream pErrorOut, @Nullable InputStream pDefaultIn,
                                      @NotNull String... pParams) throws IOException
  {
    // Invalid Environment
    _checkValid(pEnv);

    // Prepare Process
    ProcBuilder builder = new ProcBuilder(_getCommandPath(pEnv, pBase).getAbsolutePath(), pParams)
        .withWorkingDirectory(workingDir)
        .withOutputStream(pDefaultOut)
        .withErrorStream(pErrorOut == null ? pDefaultOut : pErrorOut)
        .withInputStream(pDefaultIn);

    // execute
    return processExecutor.submit(() -> builder.run().getExitValue());
  }

  /**
   * Checks if the given environment is valid
   *
   * @param pEnv environment to check
   * @throws IOException exception if invalid
   */
  private void _checkValid(@NotNull INodeJSEnvironment pEnv) throws IOException
  {
    if (!pEnv.isValid())
      throw new IOException("Failed to execute command on nodejs, because version is invalid (" + pEnv + ")");
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

}
