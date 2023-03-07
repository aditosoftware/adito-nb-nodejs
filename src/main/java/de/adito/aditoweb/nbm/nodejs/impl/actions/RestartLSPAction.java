package de.adito.aditoweb.nbm.nodejs.impl.actions;

import de.adito.aditoweb.nbm.nodejs.impl.ls.TypeScriptLanguageServerProvider;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.editor.BaseAction;
import org.netbeans.modules.lsp.client.LSPBindings;
import org.netbeans.modules.lsp.client.spi.ServerRestarter;
import org.openide.awt.*;

import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;

/**
 * Action for restarting the LSP in javascript and typescript files.
 *
 * @author r.hartinger, 07.03.2023
 */
@ActionReferences({
    @ActionReference(path = "Editors/text/typescript/Toolbars/Default", position = 20220),
    @ActionReference(path = "Editors/text/javascript/Toolbars/Default", position = 20220)
})
@ActionID(category = "adito/editor/toolbar", id = "de.adito.aditoweb.nbm.nodejs.impl.actions.RestartLSP")
@ActionRegistration(displayName = "Restart LSP",
    iconBase = "de/adito/aditoweb/nbm/nodejs/impl/actions/restart.svg", lazy = false)
public class RestartLSPAction extends BaseAction
{

  /**
   * Creates a new instance.
   */
  public RestartLSPAction()
  {
    super("Restart LSP");
    putValue(BaseAction.ICON_RESOURCE_PROPERTY, "de/adito/aditoweb/nbm/nodejs/impl/actions/restart.svg");
  }

  @Override
  public void actionPerformed(ActionEvent evt, JTextComponent target)
  {
    String mimeType = (String) target.getDocument().getProperty("mimeType");
    if (mimeType != null)
    {
      TypeScriptLanguageServerProvider typeScriptLanguageServerProvider = MimeLookup.getLookup(mimeType).lookup(TypeScriptLanguageServerProvider.class);
      if (typeScriptLanguageServerProvider != null)
      {
        ServerRestarter serverRestarter = typeScriptLanguageServerProvider.getServerRestarter();
        if (serverRestarter != null)
        {
          synchronized (LSPBindings.class)
          {
            typeScriptLanguageServerProvider.stopServer(serverRestarter);
          }
        }
      }
    }
  }

}
