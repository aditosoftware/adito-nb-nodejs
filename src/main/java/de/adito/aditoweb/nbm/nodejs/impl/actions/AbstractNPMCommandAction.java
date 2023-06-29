package de.adito.aditoweb.nbm.nodejs.impl.actions;

import de.adito.aditoweb.nbm.nodejs.impl.util.NPMCommandUtil;
import lombok.NonNull;
import org.netbeans.api.project.Project;

import java.util.function.Consumer;

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
  protected abstract String @NonNull [] getCommand();

  /**
   * Creates an action that will be executed after the npm command was run.
   *
   * @return the created action
   */
  @NonNull
  protected abstract Consumer<Project> getAfterCommandAction();

  @Override
  public void performAction(@NonNull Project pProject)
  {
    NPMCommandUtil.runCommand(pProject, getAfterCommandAction(), getCommand());
  }

  @Override
  protected boolean enable(@NonNull Project pProject)
  {
    return NPMCommandUtil.findEnvironment(pProject) != null
        && NPMCommandUtil.findExecutor(pProject) != null;
  }

}
