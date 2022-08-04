package de.adito.aditoweb.nbm.nodejs.impl.util;

import de.adito.util.reactive.*;
import io.reactivex.rxjava3.core.Observable;
import org.jetbrains.annotations.NotNull;
import org.openide.filesystems.*;

import java.util.Optional;

/**
 * @author w.glanzer, 04.08.2022
 */
public class FileAttributeObservable extends AbstractListenerObservable<FileChangeListener, FileObject, Optional<Object>>
{

  private final String attribute;

  @NotNull
  public static Observable<Optional<Object>> create(@NotNull FileObject pFileObject, @NotNull String pAttributeName)
  {
    return Observables.create(new FileAttributeObservable(pFileObject, pAttributeName),
                              () -> Optional.ofNullable(pFileObject.getAttribute(pAttributeName)));
  }

  private FileAttributeObservable(@NotNull FileObject pListenableValue, @NotNull String pAttribute)
  {
    super(pListenableValue);
    attribute = pAttribute;
  }

  @NotNull
  @Override
  protected FileChangeListener registerListener(@NotNull FileObject pFileObject, @NotNull IFireable<Optional<Object>> pFireable)
  {
    FileChangeListener fcl = new FileChangeAdapter()
    {
      @Override
      public void fileAttributeChanged(FileAttributeEvent fe)
      {
        if (attribute.equals(fe.getName()))
          pFireable.fireValueChanged(Optional.ofNullable(fe.getFile().getAttribute(attribute)));
      }
    };

    pFileObject.addFileChangeListener(fcl);
    return fcl;
  }

  @Override
  protected void removeListener(@NotNull FileObject pFileObject, @NotNull FileChangeListener pFileChangeListener)
  {
    pFileObject.removeFileChangeListener(pFileChangeListener);
  }

}
