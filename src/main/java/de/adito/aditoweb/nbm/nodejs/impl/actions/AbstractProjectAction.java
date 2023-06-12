package de.adito.aditoweb.nbm.nodejs.impl.actions;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.common.IProjectQuery;
import lombok.NonNull;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.Project;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.actions.NodeAction;

/**
 * Abstract action to execute a command on a specific project
 *
 * @author p.neub, 16.09.2022
 */
abstract class AbstractProjectAction extends NodeAction
{

  @Override
  @NbBundle.Messages("LBL_PerformAction=Executing \"{0}\"")
  protected final void performAction(Node[] pActiveNodes)
  {
    Project project = _findProject(pActiveNodes);
    if (project == null)
      return;
    performAction(project);
  }

  @Override
  protected boolean enable(Node[] pNodes)
  {
    Project project = _findProject(pNodes);
    if (project == null)
      return false;
    return enable(project);
  }

  /**
   * Checks if the action should be enabled
   * default is true, so the action is enabled if a project is found
   * overwrite this method to perform additional checks using the project
   *
   * @param pProject project that can be used to check whether the action should be enabled
   * @return boolean indication if the action should be enabled
   */
  protected boolean enable(@NonNull Project pProject)
  {
    return true;
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }

  /**
   * Performs the action with a valid project
   *
   * @param pProject the project
   */
  public abstract void performAction(@NonNull Project pProject);

  /**
   * Finds a Project in the specified Nodes
   *
   * @param pNodes the nodes
   * @return project or null if no project was found
   */
  @Nullable
  private Project _findProject(Node[] pNodes)
  {
    if (pNodes == null)
      return null;
    for (Node node : pNodes)
    {
      Project project = IProjectQuery.getInstance().findProjects(node.getLookup(), IProjectQuery.ReturnType.MULTIPLE_TO_NULL);
      if (project != null)
        return project;
    }
    return null;
  }

}
