package de.adito.aditoweb.nbm.nodejs.impl.actions;

import org.jetbrains.annotations.NotNull;
import org.openide.awt.*;
import org.openide.util.NbBundle;

/**
 * @author w.glanzer, 11.05.2021
 */
@NbBundle.Messages("CTL_NPMInstallAction=npm install")
@ActionID(category = "NodeJS", id = "de.adito.aditoweb.nbm.nodejs.impl.actions.NPMInstallAction")
@ActionRegistration(displayName = "#CTL_NPMInstallAction", lazy = false)
@ActionReference(path = "Plugins/NodeJS/Actions", position = 0)
public class NPMInstallCommandAction extends AbstractNPMInstallCommand
{

  @NotNull
  @Override
  protected String[] getCommand()
  {
    return new String[]{"install"};
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_NPMInstallAction();
  }

}
