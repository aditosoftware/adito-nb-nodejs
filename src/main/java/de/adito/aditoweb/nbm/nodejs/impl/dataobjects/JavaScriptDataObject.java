package de.adito.aditoweb.nbm.nodejs.impl.dataobjects;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.IJSNodeNameProvider;
import io.reactivex.rxjava3.disposables.*;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.*;
import org.netbeans.modules.textmate.lexer.api.GrammarRegistration;
import org.openide.filesystems.*;
import org.openide.loaders.*;
import org.openide.nodes.*;
import org.openide.util.NbBundle;

import java.io.IOException;

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
    registerEditor("text/javascript", false);
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

    public JSNodeDelegate(@NotNull DataObject pDataObject)
    {
      super(pDataObject, Children.LEAF);
      dataObject = pDataObject;
      displayName = dataObject.getPrimaryFile().getNameExt();
      Project owner = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
      IJSNodeNameProvider.findInstance(owner)
          .ifPresent(pNodeNameProvider -> disposable.add(pNodeNameProvider.getDisplayName(dataObject)
                                                             .subscribe(pNameOpt -> pNameOpt
                                                                 .ifPresent(this::updateDisplayName))));
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
      disposable.dispose();
    }

    @Override
    public boolean isDisposed()
    {
      return disposable.isDisposed();
    }
  }
}