package de.adito.aditoweb.nbm.javascript.impl;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author w.glanzer, 08.03.2021
 */
@ServiceProvider(service = INodeJSExecutor.class, path = "Projects/de-adito-project/Lookup")
public class NodeJSExecutorImpl implements INodeJSExecutor
{

  private final File workingDir;

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
      throws IOException, InterruptedException
  {
    // Invalid Environment
    if (!pEnv.isValid())
      throw new IOException("Failed to execute command on nodejs, because version is invalid (" + pEnv + ")");

    // Prepare Process
    ArrayList<String> params = new ArrayList<>(Arrays.asList(pParams));
    params.add(0, pEnv.resolveExecBase(pBase).getAbsolutePath());
    Process process = new ProcessBuilder(params)
        .directory(workingDir)
        .start();

    // Execute blocking
    if (pTimeout > -1)
      process.waitFor(pTimeout, TimeUnit.MILLISECONDS);
    else
      process.waitFor();

    // Copy to String
    StringWriter writer = new StringWriter();
    IOUtils.copy(process.getInputStream(), writer, StandardCharsets.UTF_8);

    // trim trailing linebreak
    return writer.toString().trim();
  }

}
