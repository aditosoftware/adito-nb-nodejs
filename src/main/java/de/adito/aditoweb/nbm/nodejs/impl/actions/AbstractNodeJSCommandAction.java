package de.adito.aditoweb.nbm.nodejs.impl.actions;

import com.google.common.base.Suppliers;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.common.IProjectQuery;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import org.jetbrains.annotations.*;
import org.netbeans.api.progress.*;
import org.netbeans.api.project.Project;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;
import org.openide.windows.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.*;

/**
 * Abstract action to execute NodeJS commands
 *
 * @author w.glanzer, 11.05.2021
 */
abstract class AbstractNodeJSCommandAction extends NodeAction
{

  @Override
  @NbBundle.Messages("LBL_PerformAction=Executing \"{0}\"")
  protected final void performAction(Node[] pActiveNodes)
  {
    INodeJSEnvironment env = findCurrentEnvironment(pActiveNodes);
    INodeJSExecutor exec = findCurrentExecutor(pActiveNodes);
    if (env == null || exec == null)
      return;

    // io
    Supplier<InputOutput> ioSupplier = Suppliers.memoize(this::_createIO);

    // progress
    ProgressHandle handle = ProgressHandleFactory.createSystemHandle(Bundle.LBL_PerformAction(getCommandDisplayName()), null);
    handle.start();
    handle.switchToIndeterminate();

    try
    {
      // execute
      performAction(env, exec, ioSupplier).handle((pV, pT) -> {
        handle.finish();
        return null;
      });
    }
    catch (Exception e)
    {
      handle.finish();

      // Print on err and in log
      e.printStackTrace(ioSupplier.get().getErr());
      Logger.getLogger(AbstractNodeJSCommandAction.class.getName()).log(Level.WARNING, getCommandDisplayName(), e);
    }
  }

  @Override
  protected boolean enable(Node[] pNodes)
  {
    return findCurrentEnvironment(pNodes) != null && findCurrentExecutor(pNodes) != null;
  }

  /**
   * Performs the action with a valid environment
   *
   * @param pEnvironment         Environment
   * @param pExecutor            Executor
   * @param pInputOutputSupplier Supplier to provide an input/output, if necessary
   * @return a future determining the state
   */
  @NotNull
  protected abstract CompletableFuture<?> performAction(@NotNull INodeJSEnvironment pEnvironment, @NotNull INodeJSExecutor pExecutor,
                                                        @NotNull Supplier<InputOutput> pInputOutputSupplier) throws Exception;

  /**
   * @return the displayable command, showing in progress
   */
  @NotNull
  protected abstract String getCommandDisplayName();

  /**
   * Returns the current nodejs environment for the given nodes
   *
   * @param pNodes Nodes
   * @return environment
   */
  @Nullable
  protected INodeJSEnvironment findCurrentEnvironment(@Nullable Node[] pNodes)
  {
    if (pNodes == null)
      return null;

    for (Node node : pNodes)
    {
      Project project = IProjectQuery.getInstance().findProjects(node.getLookup(), IProjectQuery.ReturnType.MULTIPLE_TO_NULL);
      if (project != null)
      {
        INodeJSProvider provider = project.getLookup().lookup(INodeJSProvider.class);
        if (provider != null)
          return provider.current().blockingFirst(Optional.empty()).orElse(null);
      }
    }

    return null;
  }

  /**
   * Returns the current nodejs executor for the given nodes
   *
   * @param pNodes Nodes
   * @return executor
   */
  @Nullable
  protected INodeJSExecutor findCurrentExecutor(@Nullable Node[] pNodes)
  {
    if (pNodes == null)
      return null;

    for (Node node : pNodes)
    {
      Project project = IProjectQuery.getInstance().findProjects(node.getLookup(), IProjectQuery.ReturnType.MULTIPLE_TO_NULL);
      if (project != null)
        return INodeJSExecutor.findInstance(project).orElse(null);
    }

    return null;
  }

  /**
   * @return a new IO instance to write to
   */
  @NotNull
  private InputOutput _createIO()
  {
    InputOutput io = IOProvider.get("nodejs_executor").getIO("NodeJS", false);

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

}
