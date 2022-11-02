package de.adito.aditoweb.nbm.nodejs.impl.runconfig;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.parser.PackageParser;
import de.adito.nbm.runconfig.api.*;
import de.adito.observables.netbeans.*;
import io.reactivex.rxjava3.core.Observable;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.util.lookup.ServiceProvider;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author w.glanzer, 12.05.2021
 */
@ServiceProvider(service = ISystemRunConfigProvider.class)
public class NodeJSScriptsRunConfigProvider implements ISystemRunConfigProvider
{

  private final Project project;

  @SuppressWarnings("unused")
  public NodeJSScriptsRunConfigProvider()
  {
    project = null;
  }

  public NodeJSScriptsRunConfigProvider(@NotNull Project pProject)
  {
    project = pProject;
  }

  @NotNull
  @Override
  public Observable<List<IRunConfig>> runConfigurations(List<ISystemInfo> pList)
  {
    if (project == null || !Arrays.stream(OpenProjects.getDefault().getOpenProjects()).filter(p -> p.equals(project)).findFirst().isPresent())
      return Observable.just(List.of());
    return _createRunConfigsForProject(project);
  }

  @Override
  public ISystemRunConfigProvider getInstance(Project pProject)
  {
    return pProject == null ? this : new NodeJSScriptsRunConfigProvider(pProject);
  }

  /**
   * Creates all necessary runconfigs for a given project
   *
   * @param pProject Project
   * @return Observable with runconfigs
   */
  @NotNull
  private Observable<List<IRunConfig>> _createRunConfigsForProject(@NotNull Project pProject)
  {
    return LookupResultObservable.create(pProject.getLookup(), INodeJSProvider.class)
        .map(pProviders -> pProviders.stream().findFirst())
        .switchMap(pProvOpt -> pProvOpt
            .map(INodeJSProvider::current)
            .orElseGet(() -> Observable.just(Optional.empty())))
        .switchMap(pEnvOpt -> pEnvOpt
            .map(pEnv -> _createRunConfigsForProject(pProject, pEnv))
            .orElse(Observable.just(List.of())));
  }

  /**
   * Creates all necessary runconfigs for a given project with a already observed environment
   *
   * @param pProject     Project
   * @param pEnvironment Environment, already observed
   * @return Observable with runconfigs
   */
  @NotNull
  private Observable<List<IRunConfig>> _createRunConfigsForProject(@NotNull Project pProject, @NotNull INodeJSEnvironment pEnvironment)
  {
    return FileObservable.createForPlainFile(new File(pProject.getProjectDirectory().getPath(), "package.json"))
        .map(pFileOpt -> pFileOpt
            .map(PackageParser::parseScripts)
            .orElse(Map.of()))
        .map(pScripts -> pScripts.keySet().stream()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .map(pScript -> new NodeJSScriptRunConfig(pProject, pEnvironment, pScript))
            .collect(Collectors.<IRunConfig>toList()))
        .distinctUntilChanged();
  }

}
