package de.adito.aditoweb.nbm.javascript.impl.options;

import de.adito.aditoweb.nbm.javascript.impl.options.downloader.INodeJSDownloader;
import de.adito.aditoweb.nbm.javascript.impl.version.NodeJSVersionFactory;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.INodeJSVersion;
import de.adito.swing.*;
import info.clearthought.layout.TableLayout;
import org.jetbrains.annotations.*;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.*;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

/**
 * Panel to display the options
 *
 * @author w.glanzer, 08.03.2021
 */
public class NodeJSOptionsPanel extends JPanel
{

  private final _PathSelection path;
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
    path = new _PathSelection(_createDownloadButton());
    tlu.add(0, 2, new JLabel(Bundle.LBL_Path() + ":"));
    tlu.add(2, 2, path);

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
    path.setValue(pOptions.getPath());
  }

  /**
   * @return the currently set options, with the user changed values
   */
  @NotNull
  public NodeJSOptions getCurrent()
  {
    return options.toBuilder()
        .path(path.getValue())
        .build();
  }

  /**
   * @return creates the button to download a nodejs version
   */
  @NbBundle.Messages({
      "LBL_Download=Download...",
      "LBL_DownloadTitle=Select Version to Download",
      "LBL_DownloadBtn=Download",
      "LBL_DownloadingVersion=Downloading NodeJS ({0}, {1})..."
  })
  @NotNull
  private JButton _createDownloadButton()
  {
    JButton btn = new JButton(Bundle.LBL_Download());
    btn.addActionListener(e -> {
      try
      {
        INodeJSDownloader downloader = INodeJSDownloader.getInstance();
        _DownloadPanel container = new _DownloadPanel(downloader.getAvailableVersions());

        // validity check
        Supplier<Boolean> isValid = () -> container.getVersion() != null && container.getTarget() != null;

        // action listener
        ActionListener onAction = action -> {
          if (Objects.equals(action.getActionCommand(), Bundle.LBL_DownloadBtn()) &&
              container.getVersion() != null && container.getTarget() != null)
          {
            // Async
            BaseProgressUtils.showProgressDialogAndRun(() -> {
              try
              {
                File bin = downloader.downloadVersion(container.getVersion(), container.getTarget());
                path.setValue(bin.getAbsolutePath());
              }
              catch (Exception ex2)
              {
                throw new RuntimeException("Failed to download nodejs binary", ex2);
              }
            }, Bundle.LBL_DownloadingVersion(container.getVersion(), container.getTarget().getAbsolutePath()));
          }
        };

        // create dialog
        JButton downloadBtn = new JButton(Bundle.LBL_DownloadBtn());
        DialogDescriptor descriptor = new DialogDescriptor(container, Bundle.LBL_DownloadTitle(), true,
                                                           new Object[]{downloadBtn, DialogDescriptor.CANCEL_OPTION},
                                                           downloadBtn, DialogDescriptor.DEFAULT_ALIGN, null,
                                                           onAction);

        // configure descriptor
        descriptor.setClosingOptions(new Object[]{downloadBtn, DialogDescriptor.CANCEL_OPTION});

        // set valid, if changed
        downloadBtn.setEnabled(isValid.get());
        container.addChangeListener(a -> downloadBtn.setEnabled(isValid.get()));

        Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.setMinimumSize(new Dimension(450, 150));
        dialog.setPreferredSize(getMinimumSize());
        dialog.setVisible(true);
      }
      catch (Exception ex)
      {
        throw new RuntimeException("Failed to download nodejs binary", ex);
      }
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
    path.path.getDocument().addDocumentListener(new DocumentListener()
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
        INodeJSVersion version = NodeJSVersionFactory.create(new File(path.getValue()));
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

  /**
   * Checks, if the given path is a valid file path
   *
   * @param pPath Path to check
   * @return true, if valid
   */
  private static boolean _isValidFilePath(@NotNull String pPath)
  {
    try
    {
      //noinspection ResultOfMethodCallIgnored
      new File(pPath).getCanonicalPath();
      return true;
    }
    catch (Exception e)
    {
      return false;
    }
  }

  /**
   * Component to select a path
   */
  private static class _PathSelection extends JPanel
  {
    private final JTextField path = new JTextField();

    public _PathSelection(JButton... pAdditionalButtons)
    {
      super(new BorderLayout(5, 0));
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
      panel.add(_createBrowseButton(this, path::getText, path::setText));
      for (JButton btn : pAdditionalButtons)
        panel.add(btn);
      add(path, BorderLayout.CENTER);
      add(panel, BorderLayout.EAST);
    }

    public void setValue(@NotNull String pValue)
    {
      path.setText(pValue);
    }

    @NotNull
    public String getValue()
    {
      return path.getText();
    }

    /**
     * @return creates the browse button to search for a valid nodejs installation
     */
    @NbBundle.Messages({
        "LBL_Browse=Browse...",
        "LBL_ChooserTitle=Locate NodeJS Executable"
    })
    @NotNull
    private static JButton _createBrowseButton(@NotNull JComponent pParent, @NotNull Supplier<String> pDefaultPath, @NotNull Consumer<String> pOnFileSelected)
    {
      JButton btn = new JButton(Bundle.LBL_Browse());
      btn.addActionListener(e -> {
        JFileChooser chooser = new JFileChooser(pDefaultPath.get());
        chooser.setDialogTitle(Bundle.LBL_ChooserTitle());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = chooser.showOpenDialog(pParent);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null)
          pOnFileSelected.accept(chooser.getSelectedFile().getAbsolutePath());
      });
      return btn;
    }
  }

  /**
   * Panel for Download Dialog
   */
  private static class _DownloadPanel extends JPanel
  {
    private static final String _DEFAULT_PATH = System.getProperty("user.home") + "/.nodejs-versions";
    private final JComboBox<String> versions;
    private final _PathSelection path;

    @NbBundle.Messages({
        "LBL_TargetPath=Target Path: "
    })
    public _DownloadPanel(@NotNull List<String> pAvailableVersions)
    {
      setBorder(new EmptyBorder(5, 5, 5, 5));

      double fill = TableLayout.FILL;
      double pref = TableLayout.PREFERRED;
      int gap = 5;

      double[] cols = {pref, gap, pref, fill};
      double[] rows = {pref,
                       gap,
                       pref};

      setLayout(new TableLayout(cols, rows));
      TableLayoutUtil tlu = new TableLayoutUtil(this);

      // version
      versions = new JComboBox<>(new DefaultComboBoxModel<>(pAvailableVersions.toArray(new String[0])));
      versions.setSelectedIndex(0); //select "latest"
      versions.setEditable(false);
      tlu.add(0, 0, new JLabel(Bundle.LBL_Version()));
      tlu.add(2, 0, versions);

      // path
      path = new _PathSelection();
      path.path.setText(_DEFAULT_PATH);
      tlu.add(0, 2, new JLabel(Bundle.LBL_TargetPath()));
      tlu.add(2, 2, 3, 2, path);
    }

    @Nullable
    public String getVersion()
    {
      return (String) versions.getSelectedItem();
    }

    @Nullable
    public File getTarget()
    {
      if (path.getValue().trim().isEmpty() || !_isValidFilePath(path.getValue()) || !new File(path.getValue()).isAbsolute())
        return null;
      return new File(path.getValue());
    }

    public void addChangeListener(@NotNull ChangeListener pOnChange)
    {
      path.path.getDocument().addDocumentListener(new DocumentListener()
      {
        @Override
        public void insertUpdate(DocumentEvent e)
        {
          pOnChange.stateChanged(new ChangeEvent(path));
        }

        @Override
        public void removeUpdate(DocumentEvent e)
        {
          pOnChange.stateChanged(new ChangeEvent(path));
        }

        @Override
        public void changedUpdate(DocumentEvent e)
        {
          pOnChange.stateChanged(new ChangeEvent(path));
        }
      });

      versions.addItemListener(e -> pOnChange.stateChanged(new ChangeEvent(versions)));
    }
  }

}
