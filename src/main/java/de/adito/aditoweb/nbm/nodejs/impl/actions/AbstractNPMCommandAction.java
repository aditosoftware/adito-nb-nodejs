package de.adito.aditoweb.nbm.nodejs.impl.actions;

import de.adito.aditoweb.nbm.nodejs.impl.util.NPMCommandUtil;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.Project;

/**
 * Base class for NPM command actions
 * Child classes only need to provide the npm command via getCommand()
 * running the command and validating if a valid environment/executor
 * is available is handled by this class
 *
 * @author p.neub, 16.09.2022
 */
public abstract class AbstractNPMCommandAction extends AbstractProjectAction
{

  /**
   * Returns the npm command that shall be executed
   * once the action is ran
   *
   * @return the npm command
   */
  @NotNull
  protected abstract String[] getCommand();

  @Override
  protected void performAction(@NotNull Project pProject)
  {
    NPMCommandUtil.runCommand(pProject, getCommand());
  }

  @Override
  protected boolean enable(@NotNull Project pProject)
  {
    return NPMCommandUtil.findEnvironment(pProject) != null
        && NPMCommandUtil.findExecutor(pProject) != null;
  }

}