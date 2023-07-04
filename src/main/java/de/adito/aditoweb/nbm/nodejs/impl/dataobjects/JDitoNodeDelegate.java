package de.adito.aditoweb.nbm.nodejs.impl.dataobjects;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.IJSNodeNameProvider;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.nodes.INodeProvider;
import de.adito.observables.netbeans.OpenProjectsObservable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.*;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.*;
import org.openide.filesystems.FileObject;
import org.openide.loaders.*;
import org.openide.nodes.*;
import org.openide.util.Lookup;

import java.beans.PropertyChangeEvent;
import java.util.*;

/**
 * Node that handles the displayed name for the node in such a way that an sensible name is displayed for the ADITO project structure
 *
 * @author p.neub, 03.05.2023
 */
class JDitoNodeDelegate extends DataNode implements Disposable
{
  @NotNull
  private final DataObject dataObject;
  @NotNull
  private final String jditoExtension;
  @NotNull
  private final CompositeDisposable disposable = new CompositeDisposable();
  @NotNull
  private String displayName;

  private Node node;
  private NodeListener listener;

  public JDitoNodeDelegate(@NotNull DataObject pDataObject, @NotNull String pJDitoExtension)
  {
    super(pDataObject, Children.LEAF);
    dataObject = pDataObject;
    jditoExtension = pJDitoExtension;
    displayName = dataObject.getPrimaryFile().getNameExt();
    Project owner = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
    disposable.add(OpenProjectsObservable.create()
                       .map(pProjects -> {
                         if (pProjects.contains(owner))
                           return IJSNodeNameProvider.findInstance(owner);
                         return Optional.<IJSNodeNameProvider>empty();
                       })
                       .switchMap(pOpt -> pOpt.map(pNodeNameProvider -> pNodeNameProvider.getDisplayName(dataObject))
                           .orElse(Observable.empty()))
                       .subscribe(pNameOpt -> pNameOpt.ifPresent(this::updateDisplayName)));
  }

  private void updateDisplayName(String pName)
  {
    String old = displayName;
    displayName = pName + "." + jditoExtension;
    setDisplayName(displayName);
    fireDisplayNameChange(old, displayName);
  }

  @Override
  @NotNull
  public String getDisplayName()
  {
    return displayName;
  }

  @Override
  public void dispose()
  {
    if (node != null && listener != null)
    {
      node.removeNodeListener(listener);
      node = null;
      listener = null;
    }
    disposable.dispose();
  }

  @Override
  public boolean isDisposed()
  {
    return disposable.isDisposed();
  }

  @Override
  public Node.PropertySet[] getPropertySets()
  {
    INodeProvider provider = Lookup.getDefault().lookup(INodeProvider.class);
    FileObject fo = getLookup().lookup(FileObject.class);
    if (provider != null && fo != null)
    {
      if (node == null)
      {
        node = provider.findNodeFromLinkedFo(fo);

        // Add listener because property sets of the origin node can be changed
        listener = new NodeAdapter()
        {
          @Override
          public void propertyChange(PropertyChangeEvent ev)
          {
            if (Objects.equals(ev.getPropertyName(), Node.PROP_PROPERTY_SETS))
              firePropertySetsChange(null, null);
          }
        };

        if (node != null)
          node.addNodeListener(listener);
      }

      if (node != null)
        return node.getPropertySets();
    }

    return super.getPropertySets();
  }
}
