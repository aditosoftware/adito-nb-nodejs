package de.adito.aditoweb.nbm.nodejs.impl.options;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.project.IProjectVisibility;
import de.adito.aditoweb.nbm.vaadinicons.IVaadinIconsProvider;
import de.adito.swing.icon.IconAttributes;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.util.Lookup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

/**
 * @author m.kaspera, 02.09.2022
 */
public class IgnoredWarningsPanel extends JPanel
{

  public IgnoredWarningsPanel()
  {
    super(new BorderLayout());
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setBorder(new EmptyBorder(0, 0, 0, 0));
    Project[] openProjects = OpenProjects.getDefault().getOpenProjects();
    for (Project openProject : openProjects)
    {
      if (Optional.ofNullable(openProject.getLookup().lookup(IProjectVisibility.class)).map(IProjectVisibility::isVisible).orElse(false))
        tabbedPane.add(openProject.getProjectDirectory().getName(), new IgnoredWarningsTablePanel(openProject));
    }
    JToolBar toolBar = new JToolBar(SwingConstants.HORIZONTAL);
    toolBar.setBorder(new EmptyBorder(0, 0, 0, 0));
    toolBar.setAlignmentX(SwingConstants.RIGHT);
    toolBar.setFloatable(false);
    toolBar.add(Box.createGlue());
    toolBar.add(new RemoveSelectedIgnoresAction(tabbedPane));
    add(toolBar, BorderLayout.NORTH);
    JPanel tabbedPaneContainer = new JPanel(new BorderLayout());
    tabbedPaneContainer.add(tabbedPane, BorderLayout.CENTER);
    add(tabbedPaneContainer, BorderLayout.CENTER);
  }

  private static class RemoveSelectedIgnoresAction extends AbstractAction {

    private final JTabbedPane tabbedPane;

    public RemoveSelectedIgnoresAction(@NotNull JTabbedPane pTabbedPane)
    {
      tabbedPane = pTabbedPane;
      Optional.ofNullable(Lookup.getDefault().lookup(IVaadinIconsProvider.class))
          .map(pIVaadinIconsProvider -> pIVaadinIconsProvider.findImage(IVaadinIconsProvider.VaadinIcon.TRASH, new IconAttributes.Builder().create()))
          .map(ImageIcon::new)
          .ifPresent(pImage -> putValue(SMALL_ICON, pImage));
      putValue(SHORT_DESCRIPTION, "Remove selected items from the list of ignored warnings");
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      Optional.ofNullable(tabbedPane.getSelectedComponent())
          .filter(pComp -> pComp instanceof IgnoredWarningsTablePanel)
          .map(IgnoredWarningsTablePanel.class::cast).ifPresent(IgnoredWarningsTablePanel::removeCurrenltySelectedIgnores);
    }
  }
}
