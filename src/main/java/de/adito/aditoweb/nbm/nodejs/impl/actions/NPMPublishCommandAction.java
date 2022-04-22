package de.adito.aditoweb.nbm.nodejs.impl.actions;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import org.jetbrains.annotations.NotNull;
import org.openide.awt.*;
import org.openide.util.NbBundle;

import java.io.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author w.glanzer, 22.04.2022
 */
@NbBundle.Messages("CTL_NPMPublishAction=npm publish")
@ActionID(category = "NodeJS", id = "de.adito.aditoweb.nbm.nodejs.impl.actions.NPMPublishCommandAction")
@ActionRegistration(displayName = "#CTL_NPMPublishAction", lazy = false)
@ActionReference(path = "Plugins/NodeJS/Actions", position = 200, separatorBefore = 199)
public class NPMPublishCommandAction extends AbstractNodeJSCommandAction
{

  @NotNull
  @Override
  protected CompletableFuture<?> performAction(@NotNull INodeJSEnvironment pEnvironment, @NotNull INodeJSExecutor pExecutor,
                                               @NotNull OutputStream pOut, @NotNull OutputStream pErr) throws IOException
  {
    return pExecutor.executeAsync(pEnvironment, INodeJSExecBase.packageManager(), pOut, pErr, null, "publish");
  }

  @NotNull
  @Override
  protected String getCommandDisplayName()
  {
    return "npm publish";
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_NPMPublishAction();
  }

}
