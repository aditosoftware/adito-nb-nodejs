package de.adito.aditoweb.nbm.nodejs.impl;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.project.IProjectVisibility;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author p.neub, 14.09.2022
 */
@SuppressWarnings("unused") // ServiceProvider
@ServiceProvider(service = ProjectOpenedHook.class, path = "Projects/de-adito-project/StaticLookup")
public class NodeJSProjectOpenedHook extends ProjectOpenedHook
{
  private final Project project;

  @SuppressWarnings("unused") // ServiceProvider
  public NodeJSProjectOpenedHook()
  {
    project = null;
  }

  @SuppressWarnings("unused") // ServiceProvider
  public NodeJSProjectOpenedHook(@NotNull Project pProject)
  {
    project = pProject;
  }

  @Override
  protected void projectOpened()
  {
    if (project == null || !IProjectVisibility.isVisible(project))
      return;
    NPMInstallRequired.forProject(project).notifyIfRequired();
  }

  @Override
  protected void projectClosed()
  {
    if (project == null)
      return;
    NPMInstallRequired.delete(project);
  }
}
