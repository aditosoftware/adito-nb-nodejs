package de.adito.aditoweb.nbm.nodejs.impl.dataobjects;

import de.adito.aditoweb.nbm.nodejs.impl.util.FileAttributeObservable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.*;
import org.jetbrains.annotations.NotNull;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.netbeans.modules.textmate.lexer.api.GrammarRegistration;
import org.openide.filesystems.*;
import org.openide.loaders.*;
import org.openide.nodes.*;
import org.openide.util.*;
import org.openide.windows.TopComponent;

import javax.swing.*;
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
  private static final String _EDITABLE_ATTRIBUTE = "ADITOeditableOnGUI";
  private Node nodeDelegate = null;

  public JavaScriptDataObject(FileObject pf, MultiFileLoader loader) throws IOException
  {
    super(pf, loader);
    registerEditor("text/javascript", true);
  }

  /**
   * Returns an observable containing a flag that describes, if this javascript is editable or not
   *
   * @return true if this file should be editable
   */
  @NotNull
  public Observable<Boolean> editableOnGUI()
  {
    return FileAttributeObservable.create(getPrimaryFile(), _EDITABLE_ATTRIBUTE)
        .map(pOpt -> !pOpt.orElse(true).equals(false));
  }

  @Override
  protected Node createNodeDelegate()
  {
    if (nodeDelegate == null)
      nodeDelegate = new JDitoNodeDelegate(this, JS_EXTENSION);

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
    return new _EditorElement(lkp);
  }

  /**
   * MultiViewEditor-Element-Impl
   */
  private static class _EditorElement extends MultiViewEditorElement
  {
    private final JavaScriptDataObject dataObject;
    private Disposable disposable;

    public _EditorElement(Lookup lookup)
    {
      super(lookup);
      dataObject = getLookup().lookup(JavaScriptDataObject.class);
    }

    @Override
    public void componentOpened()
    {
      super.componentOpened();
      disposable = dataObject.editableOnGUI().subscribe(pEditable -> {
        JEditorPane pane = super.getEditorPane();
        if (pane != null)
          pane.setEditable(pEditable);
      });
    }

    @Override
    public void componentClosed()
    {
      super.componentClosed();
      disposable.dispose();
    }

    @Override
    public JEditorPane getEditorPane()
    {
      JEditorPane pane = super.getEditorPane();
      if (pane != null)
        pane.setEditable(dataObject.editableOnGUI().blockingFirst());
      return pane;
    }
  }
}