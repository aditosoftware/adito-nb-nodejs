package de.adito.aditoweb.nbm.nodejs.impl.runconfig;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.nbm.runconfig.api.*;
import io.reactivex.rxjava3.core.Observable;
import org.apache.commons.io.output.WriterOutputStream;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.openide.windows.*;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * RunConfig to execute a single nodejs script
 *
 * @author w.glanzer, 12.05.2021
 */
class NodeJSScriptRunConfig implements IRunConfig
{

  private final Project project;
  private final INodeJSEnvironment environment;
  private final String scriptName;

  public NodeJSScriptRunConfig(@NotNull Project pProject, @NotNull INodeJSEnvironment pEnvironment, @NotNull String pScriptName)
  {
    project = pProject;
    environment = pEnvironment;
    scriptName = pScriptName;
  }

  @NotNull
  @Override
  public Observable<Optional<IRunConfigCategory>> category()
  {
    return Observable.just(Optional.of(new NodeJSScriptRunConfigCategory()));
  }

  @NotNull
  @Override
  public Observable<String> displayName()
  {
    return Observable.just(scriptName);
  }

  @Override
  public void executeAsnyc(@NotNull ProgressHandle pProgressHandle) throws Exception
  {
    INodeJSExecutor executor = INodeJSExecutor.findInstance(project).orElse(null);
    if (executor != null)
    {
      InputOutput io = _createIO();
      OutputStream out = new WriterOutputStream(io.getOut(), StandardCharsets.UTF_8); //NOSONAR will be closed in future
      OutputStream err = new WriterOutputStream(io.getErr(), StandardCharsets.UTF_8); //NOSONAR will be closed in future

      // execute nonblocking, so that other runconfigs can be run in parallel
      executor.executeAsync(environment, INodeJSExecBase.packageManager(), out, err, null, "run", scriptName)
          .handle((pExitCode, pEx) -> {
            try
            {
              out.flush();
              out.close();
              err.flush();
              err.close();
            }
            catch (Exception ex)
            {
              // do nothing
            }

            return pExitCode;
          });
    }
  }


  /**
   * @return a new IO instance to write to
   */
  @NotNull
  private InputOutput _createIO()
  {
    InputOutput io = IOProvider.get("nodejs_runconfig_executor").getIO("NodeJS Script: " + scriptName, false);

    try
    {
      io.getOut().reset();
      io.getErr().reset();
      io.getIn().reset();
    }
    catch (Exception e)
    {
      // do nothing
    }

    io.select();
    return io;
  }

  @Override
  public boolean equals(Object pO)
  {
    if (this == pO) return true;
    if (pO == null || getClass() != pO.getClass()) return false;
    NodeJSScriptRunConfig that = (NodeJSScriptRunConfig) pO;
    return Objects.equals(project, that.project) &&
        Objects.equals(environment, that.environment) &&
        Objects.equals(scriptName, that.scriptName);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(project, environment, scriptName);
  }

}
