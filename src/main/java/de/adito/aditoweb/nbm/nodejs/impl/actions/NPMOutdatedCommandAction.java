package de.adito.aditoweb.nbm.nodejs.impl.actions;

import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.Project;
import org.openide.awt.*;
import org.openide.util.NbBundle;

import java.util.function.Consumer;

/**
 * @author w.glanzer, 22.04.2022
 */
@NbBundle.Messages("CTL_NPMOutdatedAction=npm outdated")
@ActionID(category = "NodeJS", id = "de.adito.aditoweb.nbm.nodejs.impl.actions.NPMOutdatedCommandAction")
@ActionRegistration(displayName = "#CTL_NPMOutdatedAction", lazy = false)
@ActionReference(path = "Plugins/NodeJS/Actions", position = 150, separatorBefore = 149)
public class NPMOutdatedCommandAction extends AbstractNPMCommandAction
{

  @NotNull
  @Override
  protected String[] getCommand()
  {
    return new String[]{"outdated"};
  }

  @NotNull
  @Override
  protected Consumer<Project> getAfterCommandAction()
  {
    return pProject -> {
    };
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_NPMOutdatedAction();
  }

}
