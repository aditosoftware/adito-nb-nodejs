package de.adito.aditoweb.nbm.nodejs.impl.dataobjects;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.IJSNodeNameProvider;
import de.adito.observables.netbeans.OpenProjectsObservable;
import io.reactivex.rxjava3.core.Observable;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.nodes.INodeProvider;
import io.reactivex.rxjava3.disposables.*;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.*;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.netbeans.modules.textmate.lexer.api.GrammarRegistration;
import org.openide.filesystems.*;
import org.openide.loaders.*;
import org.openide.nodes.*;
import org.openide.util.*;
import org.openide.windows.TopComponent;

import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.util.Optional;
import java.util.Objects;

import static de.adito.aditoweb.nbm.nodejs.impl.dataobjects.JavaScriptDataObject.JS_EXTENSION;

/**
 * DataObject for JavaScript
 *
 * @author w.glanzer, 19.08.21
 */
@NbBundle.Messages("LBL_JavaScriptDataObject_LOADER=JavaScript")
@MIMEResolver.ExtensionRegistration(displayName = "#LBL_JavaScriptDataObject_LOADER", mimeType = "text/javascript", extension = JS_EXTENSION, position = 0)
@DataObject.Registration(mimeType = "text/javascript", displayName = "#LBL_JavaScriptDataObject_LOADER", position = 300,
    iconBase = "de/adito/aditoweb/nbm/nodejs/impl/dataobjects/javascript.png")
@GrammarRegistration(mimeType = "text/javascript", grammar = "JavaScript.tmLanguage.json")
public class JavaScriptDataObject extends MultiDataObject
{

  public static final String JS_EXTENSION = "js";
  private Node nodeDelegate = null;

  public JavaScriptDataObject(FileObject pf, MultiFileLoader loader) throws IOException
  {
    super(pf, loader);
    registerEditor("text/javascript", true);
  }

  @Override
  protected Node createNodeDelegate()
  {
    if (nodeDelegate == null)
      nodeDelegate = new JSNodeDelegate(this);

    return nodeDelegate;
  }

  @Override
  protected int associateLookup()
  {
    return 1;
  }

  @MultiViewElement.Registration(
      displayName = "#LBL_JAVASCRIPT_EDITOR",
      mimeType = "text/javascript",
      persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
      preferredID = "JavaScript",
      position = 1000
  )
  @NbBundle.Messages("LBL_JAVASCRIPT_EDITOR=Source")
  public static MultiViewEditorElement createEditor(Lookup lkp)
  {
    return new MultiViewEditorElement(lkp);
  }

  /**
   * Node that handles the displayed name for the node in such a way that an sensible name is displayed for the ADITO project structure
   */
  private static class JSNodeDelegate extends DataNode implements Disposable
  {
    @NotNull
    private final DataObject dataObject;
    @NotNull
    private final CompositeDisposable disposable = new CompositeDisposable();
    @NotNull
    private String displayName;

    private Node node;
    private NodeListener listener;

    public JSNodeDelegate(@NotNull DataObject pDataObject)
    {
      super(pDataObject, Children.LEAF);
      dataObject = pDataObject;
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
      displayName = pName + "." + JS_EXTENSION;
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
    public PropertySet[] getPropertySets()
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
}