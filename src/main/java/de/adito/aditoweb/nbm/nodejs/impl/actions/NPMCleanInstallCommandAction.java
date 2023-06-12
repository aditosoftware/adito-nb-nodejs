package de.adito.aditoweb.nbm.nodejs.impl.actions;

import lombok.NonNull;
import org.openide.awt.*;
import org.openide.util.NbBundle;

/**
 * @author w.glanzer, 22.04.2022
 */
@NbBundle.Messages("CTL_NPMCleanInstallAction=npm clean-install")
@ActionID(category = "NodeJS", id = "de.adito.aditoweb.nbm.nodejs.impl.actions.NPMCleanInstallCommandAction")
@ActionRegistration(displayName = "#CTL_NPMCleanInstallAction", lazy = false)
@ActionReference(path = "Plugins/NodeJS/Actions", position = 100)
public class NPMCleanInstallCommandAction extends AbstractNPMInstallCommand
{

  @NonNull
  @Override
  protected String[] getCommand()
  {
    return new String[]{"clean-install"};
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_NPMCleanInstallAction();
  }

}
