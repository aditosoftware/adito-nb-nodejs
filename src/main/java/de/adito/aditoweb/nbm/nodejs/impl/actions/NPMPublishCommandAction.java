package de.adito.aditoweb.nbm.nodejs.impl.actions;

import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.Project;
import org.openide.awt.*;
import org.openide.util.NbBundle;

import java.util.function.Consumer;

/**
 * @author w.glanzer, 22.04.2022
 */
@NbBundle.Messages("CTL_NPMPublishAction=npm publish")
@ActionID(category = "NodeJS", id = "de.adito.aditoweb.nbm.nodejs.impl.actions.NPMPublishCommandAction")
@ActionRegistration(displayName = "#CTL_NPMPublishAction", lazy = false)
@ActionReference(path = "Plugins/NodeJS/Actions", position = 200, separatorBefore = 199)
public class NPMPublishCommandAction extends AbstractNPMCommandAction
{

  @NotNull
  @Override
  protected String[] getCommand()
  {
    return new String[]{"publish"};
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
    return Bundle.CTL_NPMPublishAction();
  }

}
