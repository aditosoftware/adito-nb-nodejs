package de.adito.aditoweb.nbm.javascript.impl;

import de.adito.aditoweb.nbm.javascript.impl.options.NodeJSOptions;
import de.adito.aditoweb.nbm.javascript.impl.version.NodeJSEnvironmentFactory;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.Project;
import org.openide.util.lookup.ServiceProvider;

import java.io.File;
import java.util.Optional;

/**
 * @author w.glanzer, 05.03.2021
 */
@ServiceProvider(service = INodeJSProvider.class, path = "Projects/de-adito-project/Lookup")
public class NodeJSProviderImpl implements INodeJSProvider, Disposable
{

  private final Project project;
  private final CompositeDisposable compositeDisposable = new CompositeDisposable();
  private final ReplaySubject<Optional<INodeJSEnvironment>> current = ReplaySubject.create();
  private final Observable<Optional<INodeJSEnvironment>> current_observe = current
      .replay(1)
      .autoConnect(1, compositeDisposable::add);

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
    //todo do something useful
    return Observable.just(Optional.ofNullable(NodeJSEnvironmentFactory.create(new File(NodeJSOptions.getInstance().getPath()))));
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

}
