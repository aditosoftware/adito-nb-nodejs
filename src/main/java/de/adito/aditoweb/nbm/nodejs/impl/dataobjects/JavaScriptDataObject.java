package de.adito.aditoweb.nbm.nodejs.impl.dataobjects;

import org.netbeans.modules.textmate.lexer.api.GrammarRegistration;
import org.openide.filesystems.*;
import org.openide.loaders.*;
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