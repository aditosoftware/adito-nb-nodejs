package de.adito.aditoweb.nbm.nodejs.impl;

import de.adito.aditoweb.nbm.nodejs.impl.util.NPMCommandUtil;
import de.adito.notification.INotificationFacade;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.*;
import org.openide.filesystems.*;
import org.openide.util.NbBundle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class that manages "npm install necessary" notifications
 * and keeps trank of this like the last time the notification was shown
 *
 * @author p.neub, 14.09.2022
 */
public final class NPMInstallRequired extends FileChangeAdapter
{
  private static final Map<Project, NPMInstallRequired> instances = new ConcurrentHashMap<>();

  private final Project project;
  private long lastNotification = 0;

  private NPMInstallRequired(@NotNull Project pProject)
  {
    project = pProject;
    project.getProjectDirectory().addFileChangeListener(this);
  }

  /**
   * Displays a "npm install necessary" notification
   * for the associated project if necessary
   */
  public void notifyIfRequired()
  {
    if (_isRequired())
      _showNotification();
  }

  private boolean _isRequired()
  {
    FileObject packageJsonFo = project.getProjectDirectory().getFileObject("package.json");
    if (packageJsonFo == null)
      return false;
    FileObject packageLockFo = project.getProjectDirectory()
        .getFileObject("node_modules", false)
        .getFileObject(".package-lock.json");
    long nodeModulesLastModified = packageLockFo == null ? 0 : FileUtil.toFile(packageLockFo).lastModified();
    return nodeModulesLastModified < FileUtil.toFile(packageJsonFo).lastModified();
  }

  @NbBundle.Messages({
      "LBL_DependenciesHaveChanged=Dependencies of project ({0}) have changed",
      "LBL_RunNpmInstall=run: npm install",
  })
  private void _showNotification()
  {
    // 30s verzögerung
    if (lastNotification != 0 && System.currentTimeMillis() - lastNotification < 30000)
      return;
    lastNotification = System.currentTimeMillis();
    ProjectInformation info = ProjectUtils.getInformation(project);
    INotificationFacade.INSTANCE.notify(
        Bundle.LBL_DependenciesHaveChanged(info.getDisplayName()),
        Bundle.LBL_RunNpmInstall(),
        false,
        e -> NPMCommandUtil.runCommand(project, "install"));
  }

  /**
   * Returns a NpmInstallRequired instance for the specified project
   *
   * @param pProject the project
   * @return the NpmInstallRequired instance
   */
  @NotNull
  public static NPMInstallRequired forProject(@NotNull Project pProject)
  {
    return instances.computeIfAbsent(pProject, NPMInstallRequired::new);
  }

  /**
   * Deletes/Cleans up the NpmInstallRequired instance
   * associated with the specified project
   *
   * @param pProject the project
   */
  public static void delete(@NotNull Project pProject)
  {
    NPMInstallRequired instance = instances.remove(pProject);
    if (instance != null)
      instance.project.getProjectDirectory().removeFileChangeListener(instance);
  }

  @Override
  public void fileChanged(FileEvent fe)
  {
    if ("package.json".equals(fe.getFile().getNameExt()))
      _showNotification();
  }
}
