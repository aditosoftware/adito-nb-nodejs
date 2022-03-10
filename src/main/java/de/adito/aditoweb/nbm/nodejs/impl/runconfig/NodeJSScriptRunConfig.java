package de.adito.aditoweb.nbm.nodejs.impl.runconfig;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.actions.io.*;
import de.adito.nbm.runconfig.api.*;
import de.adito.nbm.runconfig.spi.IActiveConfigComponentProvider;
import de.adito.notification.INotificationFacade;
import de.adito.observables.netbeans.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.*;
import org.apache.commons.io.output.WriterOutputStream;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.*;
import org.openide.windows.*;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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
    return OpenProjectsObservable.create()
        .switchMap(pProjects -> {
          if (pProjects.size() > 1)
            return ProjectObservable.createInfos(project)
                .map(ProjectInformation::getDisplayName)
                .map(pName -> " (" + pName + ")");
          return Observable.just("");
        })
        .map(pProjectName -> {
          String text = scriptName;
          if (pProjectName.trim().isEmpty())
            return text;
          return text + IActiveConfigComponentProvider.DISPLAY_NAME_SEPARATOR + pProjectName;
        });
  }

  @Override
  public void executeAsnyc(@NotNull ProgressHandle pProgressHandle)
  {
    INodeJSExecutor executor = INodeJSExecutor.findInstance(project).orElse(null);
    if (executor != null)
    {
      Subject<Optional<CompletableFuture<Integer>>> futureObservable = BehaviorSubject.createDefault(Optional.empty());

      Action[] actions = new Action[2];
      InputOutput io = _createIO(actions);

      actions[0] = new StartAction(futureObservable, () -> run(io, executor, futureObservable));
      actions[1] = new StopAction(futureObservable);

      // execute nonblocking, so that other runconfigs can be run in parallel
      run(io, executor, futureObservable);
    }
  }

  private void run(@NotNull InputOutput pIo, @NotNull INodeJSExecutor pExecutor, @NotNull Subject<Optional<CompletableFuture<Integer>>> pSubject)
  {
    try
    {
      pIo.getOut().reset();
      pIo.getErr().reset();
      pIo.select();
    }
    catch (IOException pE)
    {
      INotificationFacade.INSTANCE.error(pE);
    }

    OutputStream out = new WriterOutputStream(pIo.getOut(), StandardCharsets.UTF_8); //NOSONAR will be closed in future
    OutputStream err = new WriterOutputStream(pIo.getErr(), StandardCharsets.UTF_8); //NOSONAR will be closed in future

    try
    {
      CompletableFuture<Integer> future = pExecutor.executeAsync(environment, INodeJSExecBase.packageManager(), out, err, null, "run", scriptName);
      pSubject.onNext(Optional.of(future));
      future.whenComplete((pExit, pEx) -> pSubject.onNext(Optional.of(future)));
    }
    catch (IOException pEx)
    {
      INotificationFacade.INSTANCE.error(pEx);
    }
  }


  /**
   * @return a new IO instance to write to
   */
  @NotNull
  private InputOutput _createIO(Action... pActions)
  {
    InputOutput io = IOProvider.get("nodejs_runconfig_executor").getIO("NodeJS Script: " + scriptName, true,
                                                                       pActions, null);

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
