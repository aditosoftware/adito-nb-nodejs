package de.adito.aditoweb.nbm.nodejs.impl.runconfig;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.NodeJSScriptExitHook;
import de.adito.aditoweb.nbm.nodejs.impl.actions.io.*;
import de.adito.nbm.runconfig.api.*;
import de.adito.nbm.runconfig.spi.IActiveConfigComponentProvider;
import de.adito.notification.INotificationFacade;
import de.adito.observables.netbeans.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.*;
import lombok.NonNull;
import org.apache.commons.io.output.WriterOutputStream;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.*;
import org.netbeans.core.output2.adito.InputOutputExt;
import org.openide.*;
import org.openide.util.NbBundle;
import org.openide.windows.*;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

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

  public NodeJSScriptRunConfig(@NonNull Project pProject, @NonNull INodeJSEnvironment pEnvironment, @NonNull String pScriptName)
  {
    project = pProject;
    environment = pEnvironment;
    scriptName = pScriptName;
  }

  @NonNull
  @Override
  public Observable<Optional<IRunConfigCategory>> category()
  {
    return Observable.just(Optional.of(new NodeJSScriptRunConfigCategory()));
  }

  @NonNull
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
  public void executeAsnyc(@NonNull ProgressHandle pProgressHandle)
  {
    INodeJSExecutor executor = INodeJSExecutor.findInstance(project).orElse(null);
    if (executor != null)
    {
      Subject<Optional<CompletableFuture<Integer>>> futureObservable = BehaviorSubject.createDefault(Optional.empty());

      AtomicReference<InputOutput> ioRef = new AtomicReference<>();
      StartAction start = new StartAction(futureObservable, () -> run(ioRef.get(), executor, futureObservable));
      StopAction stop = new StopAction(futureObservable);
      ioRef.set(_createIO(start, stop));

      // execute nonblocking, so that other runconfigs can be run in parallel
      run(ioRef.get(), executor, futureObservable);
    }
  }

  @NbBundle.Messages({
      "LBL_ScriptCloseTerminateTitle=NodeJS script is still running...",
      "LBL_ScriptCloseTerminateMessage=Do you want to terminate the NodeJS script?",
      "LBL_ScriptCloseTerminateTerminateBtn=Terminate",
      "LBL_ScriptCloseTerminateDetachBtn=Detach",
  })
  private void run(@NonNull InputOutput pIo, @NonNull INodeJSExecutor pExecutor, @NonNull Subject<Optional<CompletableFuture<Integer>>> pSubject)
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
      String npmScript = Paths.get(environment.getPath().getParent(), "node_modules", "npm", "bin", "npm-cli.js").toString();
      CompletableFuture<Integer> future = pExecutor.executeAsync(environment, INodeJSExecBase.node(), out, err, null, npmScript, "run", scriptName);
      pSubject.onNext(Optional.of(future));

      PropertyChangeListener ioListener = evt -> {
        // also remove, if the script is not stopped
        NodeJSScriptExitHook.remove(future);

        // no cancel option, because the closing of the output window can not be aborted here
        NotifyDescriptor descriptor = new NotifyDescriptor.Confirmation(
            Bundle.LBL_ScriptCloseTerminateMessage(),
            Bundle.LBL_ScriptCloseTerminateTitle());
        descriptor.setOptions(new String[]{
            Bundle.LBL_ScriptCloseTerminateTerminateBtn(),
            Bundle.LBL_ScriptCloseTerminateDetachBtn(),
            });
        Object selected = DialogDisplayer.getDefault().notify(descriptor);
        if (Bundle.LBL_ScriptCloseTerminateTerminateBtn().equals(selected))
          future.cancel(false);
      };
      if (pIo instanceof InputOutputExt)
        ((InputOutputExt) pIo).addPropertyChangeListener(ioListener);

      future.whenComplete((pExit, pEx) -> {
        if (pIo instanceof InputOutputExt)
          ((InputOutputExt) pIo).removePropertyChangeListener(ioListener);

        NodeJSScriptExitHook.remove(future);
        pSubject.onNext(Optional.of(future));
      });
      NodeJSScriptExitHook.add(future);
    }
    catch (IOException pEx)
    {
      INotificationFacade.INSTANCE.error(pEx);
    }
  }


  /**
   * @return a new IO instance to write to
   */
  @NonNull
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
