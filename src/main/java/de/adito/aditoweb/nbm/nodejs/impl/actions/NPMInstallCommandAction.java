package de.adito.aditoweb.nbm.nodejs.impl.actions;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import org.jetbrains.annotations.NotNull;
import org.openide.awt.*;
import org.openide.util.NbBundle;

import java.io.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author w.glanzer, 11.05.2021
 */
@NbBundle.Messages("CTL_NPMInstallAction=npm install")
@ActionID(category = "NodeJS", id = "de.adito.aditoweb.nbm.nodejs.impl.actions.NPMInstallAction")
@ActionRegistration(displayName = "#CTL_NPMInstallAction", lazy = false)
@ActionReference(path = "Plugins/NodeJS/Actions", position = 0)
public class NPMInstallCommandAction extends AbstractNodeJSCommandAction
{

  @NotNull
  @Override
  protected CompletableFuture<?> performAction(@NotNull INodeJSEnvironment pEnvironment, @NotNull INodeJSExecutor pExecutor,
                                               @NotNull OutputStream pOut, @NotNull OutputStream pErr) throws IOException
  {
    return pExecutor.executeAsync(pEnvironment, INodeJSExecBase.packageManager(), pOut, pErr, null, "install");
  }

  @NotNull
  @Override
  protected String getCommandDisplayName()
  {
    return "npm install";
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_NPMInstallAction();
  }

}
