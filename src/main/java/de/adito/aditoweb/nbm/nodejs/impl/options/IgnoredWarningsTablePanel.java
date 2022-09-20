package de.adito.aditoweb.nbm.nodejs.impl.options;

import de.adito.aditoweb.nbm.nodejs.impl.ls.*;
import de.adito.aditoweb.nbm.vaadinicons.IVaadinIconsProvider;
import de.adito.notification.INotificationFacade;
import de.adito.swing.icon.IconAttributes;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author m.kaspera, 01.09.2022
 */
public class IgnoredWarningsTablePanel extends JPanel
{
  private final RemoveIgnoredWarningsAction removeIgnoredWarningsAction;

  public IgnoredWarningsTablePanel(Project pOpenProject)
  {
    JTable table = new JTable(new IgnoredWarningsTableModel(pOpenProject));
    table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    table.getColumnModel().getColumn(1).setMaxWidth(150);
    setLayout(new BorderLayout());
    JScrollPane scrollPane = new JScrollPane(table);
    String removeActionMapKey = "REMOVE_WARNINGS_ACTION";
    table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), removeActionMapKey);
    removeIgnoredWarningsAction = new RemoveIgnoredWarningsAction(pOpenProject, table);
    table.getActionMap().put(removeActionMapKey, removeIgnoredWarningsAction);
    scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
    add(scrollPane, BorderLayout.CENTER);
  }

  public void removeCurrenltySelectedIgnores()
  {
    removeIgnoredWarningsAction.actionPerformed(null);
  }

  private static class IgnoredWarningsTableModel implements TableModel
  {
    private final List<TableModelColumn> tableModelColumns;
    private List<IgnoredWarningsFacade.WarningsItem> warningsItems = List.of();
    private final List<TableModelListener> tableModelListeners = new ArrayList<>();

    public IgnoredWarningsTableModel(Project pOpenProject)
    {
      IgnoredWarningsCache cache = IgnoredWarningsCache.getInstance();
      cache.get(pOpenProject).subscribe(pWarningsItems -> {
        warningsItems = pWarningsItems.stream()
            .sorted(Comparator.comparing(IgnoredWarningsFacade.WarningsItem::getId))
            .collect(Collectors.toList());
        fireModelChanged();
      });
      tableModelColumns = List.of(
          new TableModelColumn.TableColumnBuilder().setName("Description")
              .setClassType(String.class)
              .setIsEditableFn(pInteger -> false)
              .setGetValueFn(pInteger -> warningsItems.get(pInteger).getDescription())
              .build(),
          new TableModelColumn.TableColumnBuilder().setName("ID")
              .setClassType(Integer.class)
              .setIsEditableFn(pInteger -> false)
              .setGetValueFn(pInteger -> warningsItems.get(pInteger).getId())
              .build());
    }

    @Override
    public int getRowCount()
    {
      return warningsItems.size();
    }

    @Override
    public int getColumnCount()
    {
      return tableModelColumns.size();
    }

    @Nls
    @Override
    public String getColumnName(int columnIndex)
    {
      return tableModelColumns.get(columnIndex).getName();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
      return tableModelColumns.get(columnIndex).getClassType();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
      return tableModelColumns.get(columnIndex).isEditable(rowIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
      return tableModelColumns.get(columnIndex).getValue(rowIndex);
    }

    public IgnoredWarningsFacade.WarningsItem getItemAt(int rowIndex)
    {
      return warningsItems.get(rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
      tableModelColumns.get(columnIndex).setValue(aValue, rowIndex);
    }

    @Override
    public void addTableModelListener(TableModelListener l)
    {
      tableModelListeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l)
    {
      tableModelListeners.remove(l);
    }

    private void fireModelChanged()
    {
      for (TableModelListener tableModelListener : tableModelListeners)
      {
        tableModelListener.tableChanged(new TableModelEvent(this));
      }
    }
  }

  private static class RemoveIgnoredWarningsAction extends AbstractAction
  {

    @NotNull
    private final Project project;
    @NotNull
    private final JTable table;

    private RemoveIgnoredWarningsAction(@NotNull Project pProject, @NotNull JTable pTable)
    {
      project = pProject;
      table = pTable;
      Optional.ofNullable(Lookup.getDefault().lookup(IVaadinIconsProvider.class))
          .map(pIVaadinIconsProvider -> pIVaadinIconsProvider.findImage(IVaadinIconsProvider.VaadinIcon.TRASH, new IconAttributes.Builder().create()))
          .map(ImageIcon::new)
          .ifPresent(pImage -> putValue(SMALL_ICON, pImage));
      putValue(SHORT_DESCRIPTION, "Remove selected items from the list of ignored warnings");
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      int[] selectedRows = table.getSelectedRows();
      TableModel model = table.getModel();
      if (model instanceof IgnoredWarningsTableModel)
      {
        IgnoredWarningsTableModel warningsTableModel = (IgnoredWarningsTableModel) model;
        List<IgnoredWarningsFacade.WarningsItem> itemsToRemove = Arrays.stream(selectedRows)
            .mapToObj(warningsTableModel::getItemAt)
            .collect(Collectors.toList());
        try
        {
          IgnoredWarningsFacade.unIgnoreWarnings(project, itemsToRemove);
          FileUtil.toFileObject(IgnoredWarningsCache.getIgnoredWarningsFile(project)).refresh();
        }
        catch (IOException pE)
        {
          INotificationFacade.INSTANCE.error(pE);
        }
      }
    }
  }
}
