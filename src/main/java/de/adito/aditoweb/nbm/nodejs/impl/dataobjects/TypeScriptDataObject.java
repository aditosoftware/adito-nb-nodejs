package de.adito.aditoweb.nbm.nodejs.impl.dataobjects;

import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.netbeans.modules.textmate.lexer.api.GrammarRegistration;
import org.openide.filesystems.*;
import org.openide.loaders.*;
import org.openide.util.*;
import org.openide.windows.TopComponent;

import java.io.IOException;

/**
 * DataObject for TypeScript
 *
 * @author w.glanzer, 10.03.21
 */
@NbBundle.Messages("LBL_TypeScriptDataObject_LOADER=TypeScript")
@MIMEResolver.ExtensionRegistration(displayName = "#LBL_TypeScriptDataObject_LOADER", mimeType = "text/typescript", extension = "ts", position = 193)
@DataObject.Registration(mimeType = "text/typescript", displayName = "#LBL_TypeScriptDataObject_LOADER", position = 300,
    iconBase = "de/adito/aditoweb/nbm/nodejs/impl/dataobjects/typescript.png")
@GrammarRegistration(mimeType = "text/typescript", grammar = "TypeScript.tmLanguage.json")
public class TypeScriptDataObject extends MultiDataObject
{

  public TypeScriptDataObject(FileObject pf, MultiFileLoader loader) throws IOException
  {
    super(pf, loader);
    registerEditor("text/typescript", true);
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