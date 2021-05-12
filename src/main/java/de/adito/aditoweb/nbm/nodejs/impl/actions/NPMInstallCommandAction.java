package de.adito.aditoweb.nbm.nodejs.impl.actions;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import org.apache.commons.io.output.WriterOutputStream;
import org.jetbrains.annotations.NotNull;
import org.openide.awt.*;
import org.openide.util.*;
import org.openide.windows.InputOutput;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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
                                               @NotNull Supplier<InputOutput> pInputOutputSupplier) throws IOException
  {
    OutputStream out = new WriterOutputStream(pInputOutputSupplier.get().getOut(), StandardCharsets.UTF_8); //NOSONAR will be closed in future
    OutputStream err = new WriterOutputStream(pInputOutputSupplier.get().getErr(), StandardCharsets.UTF_8); //NOSONAR will be closed in future
    return pExecutor.executeAsync(pEnvironment, INodeJSExecBase.packageManager(), out, err, null, "install")
        .handle((pExitCode, pEx) -> {
          try
          {
            out.flush();
            out.close();
            err.flush();
            err.close();
          }
          catch (Exception ex)
          {
            // do nothing
          }

          return pExitCode;
        });
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

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }

}
