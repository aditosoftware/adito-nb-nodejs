package de.adito.aditoweb.nbm.javascript.impl.options;

import de.adito.aditoweb.nbm.javascript.impl.version.NodeJSVersionFactory;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.INodeJSVersion;
import de.adito.swing.*;
import info.clearthought.layout.TableLayout;
import org.jetbrains.annotations.NotNull;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Panel to display the options
 *
 * @author w.glanzer, 08.03.2021
 */
public class NodeJSOptionsPanel extends JPanel
{

  private final JTextField path = new JTextField();
  private NodeJSOptions options;

  @NbBundle.Messages({
      "LBL_Path=Path to Executable",
      "LBL_Installation=Installation"
  })
  public NodeJSOptionsPanel()
  {
    super(new BorderLayout());

    double fill = TableLayout.FILL;
    double pref = TableLayout.PREFERRED;
    int gap = 5;

    double[] cols = {pref, gap, fill};
    double[] rows = {pref,
                     gap,
                     pref,
                     gap,
                     pref};


    setLayout(new TableLayout(cols, rows));
    TableLayoutUtil tlu = new TableLayoutUtil(this);

    // Group
    tlu.add(0, 0, 2, 0, new LinedDecorator(Bundle.LBL_Installation(), null));

    // Path
    JPanel pathContainer = new JPanel(new BorderLayout(gap, 0));
    pathContainer.add(path, BorderLayout.CENTER);
    pathContainer.add(_createBrowseButton(), BorderLayout.EAST);
    tlu.add(0, 2, new JLabel(Bundle.LBL_Path() + ":"));
    tlu.add(2, 2, pathContainer);

    // Version reader
    tlu.add(2, 4, _createVersionLabel());
  }

  /**
   * Sets the current options to display
   *
   * @param pOptions options to display
   */
  public void setCurrent(@NotNull NodeJSOptions pOptions)
  {
    options = pOptions;
    path.setText(pOptions.getPath());
  }

  /**
   * @return the currently set options, with the user changed values
   */
  @NotNull
  public NodeJSOptions getCurrent()
  {
    return options.toBuilder()
        .path(path.getText())
        .build();
  }

  /**
   * @return creates the browse button to search for a valid nodejs installation
   */
  @NbBundle.Messages({
      "LBL_Browse=Browse...",
      "LBL_ChooserTitle=Locate NodeJS Executable"
  })
  @NotNull
  private JButton _createBrowseButton()
  {
    JButton btn = new JButton(Bundle.LBL_Browse());
    btn.addActionListener(e -> {
      JFileChooser chooser = new JFileChooser(path.getText());
      chooser.setDialogTitle(Bundle.LBL_ChooserTitle());
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      int result = chooser.showOpenDialog(NodeJSOptionsPanel.this);
      if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null)
        path.setText(chooser.getSelectedFile().getAbsolutePath());
    });
    return btn;
  }

  /**
   * @return creates the label with the version of the selected nodejs package
   */
  @NbBundle.Messages({
      "LBL_Version=Version: ",
      "LBL_UnknownVersion=Unknown"
  })
  @NotNull
  private JLabel _createVersionLabel()
  {
    JLabel label = new JLabel();
    label.setEnabled(false);
    Runnable updateLabel = () -> _getNodeJSVersionOfSelectedPath(Bundle.LBL_UnknownVersion())
        .thenAccept(pVersion -> label.setText(Bundle.LBL_Version() + pVersion));
    updateLabel.run();
    path.getDocument().addDocumentListener(new DocumentListener()
    {
      @Override
      public void insertUpdate(DocumentEvent e)
      {
        updateLabel.run();
      }

      @Override
      public void removeUpdate(DocumentEvent e)
      {
        updateLabel.run();
      }

      @Override
      public void changedUpdate(DocumentEvent e)
      {
        updateLabel.run();
      }
    });
    return label;
  }

  /**
   * Tries to read the version of the nodejs package from the selected directory
   *
   * @param pDefault string to return, if read failed
   * @return future with the version
   */
  @NotNull
  private CompletableFuture<String> _getNodeJSVersionOfSelectedPath(@NotNull String pDefault)
  {
    return CompletableFuture.supplyAsync(() -> {
      try
      {
        INodeJSVersion version = NodeJSVersionFactory.create(new File(path.getText()));
        if (version != null)
          return version.getVersion();
      }
      catch (Throwable e)
      {
        // ignore
      }

      return pDefault;
    });
  }

}
