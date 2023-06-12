package de.adito.aditoweb.nbm.nodejs.impl.util;

import de.adito.util.reactive.*;
import io.reactivex.rxjava3.core.Observable;
import lombok.NonNull;
import org.openide.filesystems.*;

import java.util.Optional;

/**
 * @author w.glanzer, 04.08.2022
 */
public class FileAttributeObservable extends AbstractListenerObservable<FileChangeListener, FileObject, Optional<Object>>
{

  private final String attribute;

  @NonNull
  public static Observable<Optional<Object>> create(@NonNull FileObject pFileObject, @NonNull String pAttributeName)
  {
    return Observables.create(new FileAttributeObservable(pFileObject, pAttributeName),
                              () -> Optional.ofNullable(pFileObject.getAttribute(pAttributeName)));
  }

  private FileAttributeObservable(@NonNull FileObject pListenableValue, @NonNull String pAttribute)
  {
    super(pListenableValue);
    attribute = pAttribute;
  }

  @NonNull
  @Override
  protected FileChangeListener registerListener(@NonNull FileObject pFileObject, @NonNull IFireable<Optional<Object>> pFireable)
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
  protected void removeListener(@NonNull FileObject pFileObject, @NonNull FileChangeListener pFileChangeListener)
  {
    pFileObject.removeFileChangeListener(pFileChangeListener);
  }

}
