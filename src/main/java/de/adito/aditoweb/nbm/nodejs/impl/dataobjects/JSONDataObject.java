package de.adito.aditoweb.nbm.nodejs.impl.dataobjects;

import org.jetbrains.annotations.NotNull;
import org.netbeans.modules.textmate.lexer.api.GrammarRegistration;
import org.openide.actions.ToolsAction;
import org.openide.filesystems.*;
import org.openide.loaders.*;
import org.openide.nodes.*;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.*;

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

  @Override
  protected Node createNodeDelegate()
  {
    return new _JSONNode(super.createNodeDelegate());
  }

  /**
   * FilterNode-Impl
   */
  private class _JSONNode extends FilterNode
  {
    public _JSONNode(@NotNull Node pOriginal)
    {
      super(pOriginal);
    }

    @Override
    public Action[] getActions(boolean context)
    {
      // only create nodejs action, if really necessary
      if (!getPrimaryFile().getNameExt().equals("package.json"))
        return super.getActions(context);

      List<Action> actions = new ArrayList<>(Arrays.asList(super.getActions(context)));
      _NodeJSContainerAction containerAction = new _NodeJSContainerAction();

      // Place above "Tools" action ... there is no other way to find a possible (and better) position.
      actions.stream()
          .filter(pAction -> pAction instanceof ToolsAction)
          .map(actions::indexOf)
          .findFirst()
          .ifPresentOrElse(pToolsActionIdx -> actions.add(pToolsActionIdx - 1, containerAction),
                           () -> actions.add(Math.max(0, actions.size() - 2), containerAction));

      return actions.toArray(new Action[0]);
    }
  }

  /**
   * Action-Impl
   */
  @NbBundle.Messages("CTL_NodeJSAction=NodeJS")
  private static class _NodeJSContainerAction extends AbstractAction implements Presenter.Popup
  {

    public _NodeJSContainerAction()
    {
      super(Bundle.CTL_NodeJSAction());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      throw new RuntimeException("Not implemented");
    }

    @Override
    public JMenuItem getPopupPresenter()
    {
      JMenu main = new JMenu(Bundle.CTL_NodeJSAction());
      Utilities.actionsForPath("Plugins/NodeJS/Actions").forEach(pAction -> {
        if (pAction != null)
          main.add(pAction);
        else
          main.addSeparator();
      });
      return main;
    }

  }
}
