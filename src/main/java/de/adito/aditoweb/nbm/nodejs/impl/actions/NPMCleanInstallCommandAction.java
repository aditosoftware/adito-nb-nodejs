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
@NbBundle.Messages("CTL_NPMCleanInstallAction=npm clean-install")
@ActionID(category = "NodeJS", id = "de.adito.aditoweb.nbm.nodejs.impl.actions.NPMCleanInstallCommandAction")
@ActionRegistration(displayName = "#CTL_NPMCleanInstallAction", lazy = false)
@ActionReference(path = "Plugins/NodeJS/Actions", position = 100)
public class NPMCleanInstallCommandAction extends AbstractNodeJSCommandAction
{

  @NotNull
  @Override
  protected CompletableFuture<?> performAction(@NotNull INodeJSEnvironment pEnvironment, @NotNull INodeJSExecutor pExecutor,
                                               @NotNull OutputStream pOut, @NotNull OutputStream pErr) throws IOException
  {
    return pExecutor.executeAsync(pEnvironment, INodeJSExecBase.packageManager(), pOut, pErr, null, "ci");
  }

  @NotNull
  @Override
  protected String getCommandDisplayName()
  {
    return "npm clean-install";
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_NPMCleanInstallAction();
  }

}
