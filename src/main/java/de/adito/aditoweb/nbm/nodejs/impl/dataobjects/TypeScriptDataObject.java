package de.adito.aditoweb.nbm.nodejs.impl.dataobjects;

import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.netbeans.modules.textmate.lexer.api.GrammarRegistration;
import org.openide.filesystems.*;
import org.openide.loaders.*;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.windows.TopComponent;

import java.io.IOException;

import static de.adito.aditoweb.nbm.nodejs.impl.dataobjects.TypeScriptDataObject.TS_EXTENSION;

/**
 * DataObject for TypeScript
 *
 * @author w.glanzer, 10.03.21
 */
@NbBundle.Messages("LBL_TypeScriptDataObject_LOADER=TypeScript")
@MIMEResolver.ExtensionRegistration(displayName = "#LBL_TypeScriptDataObject_LOADER", mimeType = "text/typescript", extension = TS_EXTENSION, position = 193)
@DataObject.Registration(mimeType = "text/typescript", displayName = "#LBL_TypeScriptDataObject_LOADER", position = 300,
    iconBase = "de/adito/aditoweb/nbm/nodejs/impl/dataobjects/typescript.png")
@GrammarRegistration(mimeType = "text/typescript", grammar = "TypeScript.tmLanguage.json")
public class TypeScriptDataObject extends MultiDataObject
{
  public static final String TS_EXTENSION = "ts";
  private Node nodeDelegate = null;

  public TypeScriptDataObject(FileObject pf, MultiFileLoader loader) throws IOException
  {
    super(pf, loader);
    registerEditor("text/typescript", true);
  }

  @Override
  protected Node createNodeDelegate()
  {
    if (nodeDelegate == null)
      nodeDelegate = new JDitoNodeDelegate(this, TS_EXTENSION);

    return nodeDelegate;
  }

  @Override
  protected int associateLookup()
  {
    return 1;
  }

  @MultiViewElement.Registration(
      displayName = "#LBL_TYPESCRIPT_EDITOR",
      mimeType = "text/typescript",
      persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
      preferredID = "TypeScript",
      position = 1000
  )
  @NbBundle.Messages("LBL_TYPESCRIPT_EDITOR=Source")
  public static MultiViewEditorElement createEditor(Lookup lkp)
  {
    return new MultiViewEditorElement(lkp);
  }
}