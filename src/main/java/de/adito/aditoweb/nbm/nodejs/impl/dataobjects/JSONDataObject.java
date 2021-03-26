package de.adito.aditoweb.nbm.nodejs.impl.dataobjects;

import org.netbeans.modules.textmate.lexer.api.GrammarRegistration;
import org.openide.filesystems.*;
import org.openide.loaders.*;
import org.openide.util.NbBundle;

import java.io.IOException;

/**
 * DataObject for JSON
 *
 * @author w.glanzer, 10.03.2021
 */
@NbBundle.Messages("LBL_JSONDataObject_LOADER=JSON")
@MIMEResolver.ExtensionRegistration(displayName = "#LBL_JSONDataObject_LOADER", mimeType = "text/json", extension = "json")
@DataObject.Registration(mimeType = "text/json", displayName = "#LBL_JSONDataObject_LOADER", position = 300,
    iconBase = "de/adito/aditoweb/nbm/nodejs/impl/dataobjects/json.png")
@GrammarRegistration(mimeType = "text/json", grammar = "JSON.tmLanguage.json")
public class JSONDataObject extends MultiDataObject
{

  public JSONDataObject(FileObject pf, MultiFileLoader loader) throws IOException
  {
    super(pf, loader);
    registerEditor("text/json", false);
  }

  @Override
  protected int associateLookup()
  {
    return 1;
  }

}
