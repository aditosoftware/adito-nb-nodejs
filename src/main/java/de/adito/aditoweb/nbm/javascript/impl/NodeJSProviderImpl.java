package de.adito.aditoweb.nbm.javascript.impl;

import de.adito.aditoweb.nbm.javascript.impl.options.NodeJSOptions;
import de.adito.aditoweb.nbm.javascript.impl.version.NodeJSEnvironmentFactory;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.observables.netbeans.FileObservable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.*;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.Project;
import org.openide.util.lookup.ServiceProvider;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author w.glanzer, 05.03.2021
 */
@ServiceProvider(service = INodeJSProvider.class, path = "Projects/de-adito-project/Lookup")
public class NodeJSProviderImpl implements INodeJSProvider, Disposable
{
  private final Project project;
  private final CompositeDisposable compositeDisposable = new CompositeDisposable();

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
    return Observable.combineLatest(_observeNodeJSVersion(), _observePackageJson(), _observeTSConfig(),
                                    (pNodeJSOpt, pPackageJsonOpt, pTSConfigOpt) -> {
                                      if (pNodeJSOpt.isPresent() && pPackageJsonOpt.isPresent())
                                        return Optional.ofNullable(NodeJSEnvironmentFactory.create(pNodeJSOpt.get()));
                                      return Optional.<INodeJSEnvironment>empty();
                                    })

        // we will throttle, so that the listening server wont trigger too often
        .throttleLatest(500, TimeUnit.MILLISECONDS);
  }

  @Override
  public void dispose()
  {
    compositeDisposable.dispose();
  }

  @Override
  public boolean isDisposed()
  {
    return compositeDisposable.isDisposed();
  }

  /**
   * @return Observable that contains the current nodejs version specified in options
   */
  @NotNull
  private Observable<Optional<File>> _observeNodeJSVersion()
  {
    return NodeJSOptions.observe()
        // extract path from options
        .map(pOptions -> Optional.ofNullable(pOptions.getPath()))
        .distinctUntilChanged()

        // check if file exists
        .map(pPathOpt -> pPathOpt
            .map(File::new)
            .filter(File::exists));
  }

  /**
   * @return Observable that contains the current package.json
   */
  @NotNull
  private Observable<Optional<File>> _observePackageJson()
  {
    assert project != null;
    return FileObservable.create(new File(project.getProjectDirectory().getPath(), "package.json"));
  }

  /**
   * @return Observable that contains the current tsconfig.json
   */
  @NotNull
  private Observable<Optional<File>> _observeTSConfig()
  {
    assert project != null;
    return FileObservable.create(new File(project.getProjectDirectory().getPath(), "tsconfig.json"));
  }

}
