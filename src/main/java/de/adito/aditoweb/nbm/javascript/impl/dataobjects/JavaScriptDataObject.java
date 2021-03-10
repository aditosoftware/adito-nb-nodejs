package de.adito.aditoweb.nbm.javascript.impl.dataobjects;

import org.netbeans.modules.textmate.lexer.api.GrammarRegistration;
import org.openide.filesystems.*;
import org.openide.loaders.*;
import org.openide.util.NbBundle;

import java.io.IOException;

/**
 * DataObject for JavaScript
 *
 * @author w.glanzer, 10.03.21
 */
@NbBundle.Messages("LBL_JavaScriptDataObject_LOADER=JavaScript")
@MIMEResolver.ExtensionRegistration(displayName = "#LBL_JavaScriptDataObject_LOADER", mimeType = "text/javascript", extension = "js", position = 192)
@DataObject.Registration(mimeType = "text/javascript", displayName = "#LBL_JavaScriptDataObject_LOADER", position = 300)
@GrammarRegistration(mimeType = "text/javascript", grammar = "TypeScript.tmLanguage.json")
public class JavaScriptDataObject extends MultiDataObject
{

  public JavaScriptDataObject(FileObject pf, MultiFileLoader loader) throws IOException
  {
    super(pf, loader);
    registerEditor("text/javascript", false);
  }

  @Override
  protected int associateLookup()
  {
    return 1;
  }

}