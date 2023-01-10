package de.adito.aditoweb.nbm.nodejs.impl.util;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.DesignerBusUtils;
import lombok.*;
import org.apache.commons.io.output.WriterOutputStream;
import org.jetbrains.annotations.*;
import org.netbeans.api.progress.*;
import org.netbeans.api.project.Project;
import org.openide.util.NbBundle;
import org.openide.windows.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.*;

/**
 * @author p.neub, 16.09.2022
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NPMCommandUtil
{

  /**
   * Utility function for running a npm command on a specific project
   *
   * @param pProject          the project
   * @param pastCommandAction the command that should be executed after the node command was run
   * @param pParams           the npm parameters
   */
  @NbBundle.Messages("LBL_PerformAction=Executing \"{0}\"")
  public static void runCommand(@NotNull Project pProject, @NotNull Consumer<Project> pastCommandAction, @NotNull String... pParams)
  {
    INodeJSEnvironment env = findEnvironment(pProject);
    INodeJSExecutor executor = findExecutor(pProject);
    if (env == null || executor == null)
      return;

    String display = "npm " + String.join(" ", pParams);
    ProgressHandle handle = ProgressHandleFactory.createSystemHandle(Bundle.LBL_PerformAction(display), null);
    handle.start();
    handle.switchToIndeterminate();

    InputOutput io = IOProvider.get("nodejs_executor").getIO("NodeJS", false);
    try
    {
      io.getOut().reset();
      io.getErr().reset();
      io.getIn().reset();
    }
    catch (Exception ex)
    {
      // do nothing
    }
    io.select();

    OutputStream out = new WriterOutputStream(io.getOut(), StandardCharsets.UTF_8);
    OutputStream err = new WriterOutputStream(io.getErr(), StandardCharsets.UTF_8);

    try
    {
      executor.executeAsync(env, INodeJSExecBase.packageManager(), out, err, null, pParams)
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

            // do some action after the node command
            // this action can not be done in the designer and the module change, because it needs the project
            //  will not be there on startup, so it will throw some errors
            pastCommandAction.accept(pProject);

            DesignerBusUtils.fireModuleChange();
            return pExitCode;
          }).handle((pV, pT) -> {
            handle.finish();
            return null;
          });
    }
    catch (IOException e)
    {
      handle.finish();

      // Print on err and in log
      e.printStackTrace(io.getErr());
      Logger.getLogger(NPMCommandUtil.class.getName()).log(Level.WARNING, display, e);
    }
  }

  /**
   * Finds a nodejs environment for the specified project
   *
   * @param pProject the project
   * @return the environment or null if not found
   */
  @Nullable
  public static INodeJSEnvironment findEnvironment(@NotNull Project pProject)
  {
    INodeJSProvider provider = INodeJSProvider.findInstance(pProject).orElse(null);
    if (provider == null)
      return null;
    return provider.current().blockingFirst(Optional.empty()).orElse(null);
  }

  /**
   * Finds a nodejs executor for the specified project
   *
   * @param pProject the project
   * @return the executor or null if not found
   */
  @Nullable
  public static INodeJSExecutor findExecutor(@NotNull Project pProject)
  {
    return INodeJSExecutor.findInstance(pProject).orElse(null);
  }

}
