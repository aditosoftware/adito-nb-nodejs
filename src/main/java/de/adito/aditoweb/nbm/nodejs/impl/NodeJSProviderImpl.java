package de.adito.aditoweb.nbm.nodejs.impl;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.options.NodeJSOptions;
import de.adito.aditoweb.nbm.nodejs.impl.options.downloader.INodeJSDownloader;
import de.adito.aditoweb.nbm.nodejs.impl.version.NodeJSEnvironmentFactory;
import de.adito.observables.netbeans.FileObservable;
import io.reactivex.rxjava3.core.Observable;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.Project;
import org.openide.util.lookup.*;

import java.io.File;
import java.util.Optional;

/**
 * @author w.glanzer, 05.03.2021
 */
@ServiceProviders({
    @ServiceProvider(service = INodeJSProvider.class, path = "Projects/de-adito-project/Lookup"), //backwards compatibility to 2022.0.0
    @ServiceProvider(service = INodeJSProvider.class, path = "Projects/de-adito-project/StaticLookup"),
})
public class NodeJSProviderImpl implements INodeJSProvider
{
  private final Project project;

  @SuppressWarnings("unused") // ServiceProvider
  public NodeJSProviderImpl()
  {
    project = null;
  }

  @SuppressWarnings("unused") // ServiceProvider
  public NodeJSProviderImpl(@NotNull Project pProject)
  {
    project = pProject;
  }

  @NotNull
  @Override
  public Observable<Optional<INodeJSEnvironment>> current()
  {
    return Observable.combineLatest(_observeNodeJSVersion(), _observePackageJson(),
                                    (pNodeJSOpt, pPackageJsonOpt) -> {
                                      if (pNodeJSOpt.isPresent() && pPackageJsonOpt.isPresent())
                                        return Optional.ofNullable(NodeJSEnvironmentFactory.create(pNodeJSOpt.get()));
                                      return Optional.empty();
                                    });
  }

  /**
   * @return Observable that contains the current nodejs version specified in options (or bundled, if specified is invalid)
   */
  @NotNull
  private Observable<Optional<File>> _observeNodeJSVersion()
  {
    return NodeJSOptions.observe()
        // extract path from options
        .map(pOptions -> Optional.ofNullable(pOptions.getPath()))
        .distinctUntilChanged()

        // observe if file exists
        .switchMap(pPathOpt -> pPathOpt
            .map(File::new)
            .map(FileObservable::createForPlainFile)
            .orElseGet(() -> Observable.just(Optional.empty())))

        // if specified version is invalid -> use bundled
        .switchMap(pOptionsOpt -> pOptionsOpt
            .filter(File::exists)
            .map(pFile -> Observable.just(Optional.of(pFile)))
            .orElseGet(() -> BundledNodeJS.getInstance().observeBundledEnvironment()
                .map(pL -> {
                  BundledNodeJS node = BundledNodeJS.getInstance();
                  if (node.isBundledEnvironmentAvailable())
                    return Optional.ofNullable(INodeJSDownloader.getInstance().findNodeExecutableInInstallation(node.getBundledNodeJSContainer()));
                  return Optional.empty();
                })));
  }

  /**
   * @return Observable that contains the current package.json
   */
  @NotNull
  private Observable<Optional<File>> _observePackageJson()
  {
    assert project != null;
    return FileObservable.createForPlainFile(new File(project.getProjectDirectory().getPath(), "package.json"));
  }

}
