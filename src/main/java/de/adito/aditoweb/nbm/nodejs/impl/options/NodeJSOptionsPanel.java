package de.adito.aditoweb.nbm.nodejs.impl.options;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.INodeJSEnvironment;
import de.adito.aditoweb.nbm.nodejs.impl.*;
import de.adito.aditoweb.nbm.nodejs.impl.options.downloader.INodeJSDownloader;
import de.adito.aditoweb.nbm.nodejs.impl.version.NodeJSEnvironmentFactory;
import de.adito.swing.*;
import info.clearthought.layout.*;
import lombok.NonNull;
import org.jetbrains.annotations.*;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.*;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

/**
 * Panel to display the options
 *
 * @author w.glanzer, 08.03.2021
 */
public class NodeJSOptionsPanel extends JPanel implements Scrollable
{
  private static final String _DEFAULT_PATH = System.getProperty("user.home") + "/.nodejs-versions";

  private final PathSelectionPanel path;
  private NodeJSOptions options; //NOSONAR it's just a swing panel

  @NbBundle.Messages({
      "LBL_Path=Path to Executable",
      "LBL_Installation=Installation",
      "LBL_Chooser_LocateNodeJS=Locate NodeJS Executable",
      "LBL_IgnoredWarnings=Ignored Warnings",
      "LBL_BundledInstallation=<use bundled installation>"
  })
  public NodeJSOptionsPanel()
  {
    super(new BorderLayout());

    double fill = TableLayoutConstants.FILL;
    double pref = TableLayoutConstants.PREFERRED;
    int gap = 5;
    int categoryGap = 20;

    double[] cols = {pref, gap, fill};
    double[] rows = {pref,
                     gap,
                     pref,
                     gap,
                     pref,
                     categoryGap,
                     pref,
                     gap,
                     fill,
                     gap};


    setLayout(new TableLayout(cols, rows));
    TableLayoutUtil tlu = new TableLayoutUtil(this);

    // Group
    tlu.add(0, 0, 2, 0, new LinedDecorator(Bundle.LBL_Installation(), null));

    // Path
    path = new PathSelectionPanel(Bundle.LBL_Chooser_LocateNodeJS(), JFileChooser.FILES_ONLY, _getInstalledNodeJSVersions(), _createDownloadButton());
    tlu.add(0, 2, new JLabel(Bundle.LBL_Path() + ":"));
    tlu.add(2, 2, path);

    // Version reader
    tlu.add(2, 4, _createVersionLabel());
    tlu.add(0, 6, 2, 6, new LinedDecorator(Bundle.LBL_IgnoredWarnings(), null));
    tlu.add(0, 8, 2, 8, new IgnoredWarningsPanel());
  }

  @Override
  public Dimension getPreferredScrollableViewportSize()
  {
    return getPreferredSize();
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
  {
    return 16;
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
  {
    return 16;
  }

  @Override
  public boolean getScrollableTracksViewportWidth()
  {
    return true;
  }

  @Override
  public boolean getScrollableTracksViewportHeight()
  {
    return true;
  }

  /**
   * @return the currently set options, with the user changed values
   */
  @NonNull
  public NodeJSOptions getCurrent()
  {
    String selectedPath = path.getValue();
    return options.toBuilder()
        .path(selectedPath.equals(Bundle.LBL_BundledInstallation()) ? null : selectedPath)
        .build();
  }

  /**
   * Sets the current options to display
   *
   * @param pOptions options to display
   */
  public void setCurrent(@NonNull NodeJSOptions pOptions)
  {
    options = pOptions;
    String optionsPath = pOptions.getPath();
    path.setValue(optionsPath != null ? optionsPath : Bundle.LBL_BundledInstallation());
  }

  /**
   * @return all available nodejs installations
   */
  @NonNull
  private List<String> _getInstalledNodeJSVersions()
  {
    List<String> result = new ArrayList<>();

    // add bundled
    result.add(Bundle.LBL_BundledInstallation());

    // installed in default downloader path
    File folder = new File(_DEFAULT_PATH);
    if (folder.exists() && folder.isDirectory())
    {
      File[] children = folder.listFiles();
      if (children != null)
        Arrays.stream(children)
            .map(pChild -> INodeJSDownloader.getInstance().findNodeExecutableInInstallation(pChild))
            .filter(Objects::nonNull)
            .map(File::getAbsolutePath)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .forEachOrdered(result::add);
    }

    return result;
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
  @NonNull
  private JButton _createDownloadButton()
  {
    JButton btn = new JButton(Bundle.LBL_Download());
    btn.addActionListener(e -> {
      try
      {
        INodeJSDownloader downloader = INodeJSDownloader.getInstance();
        _DownloadPanel container = new _DownloadPanel(downloader.getAvailableVersions());

        // validity check
        BooleanSupplier isValid = () -> container.getVersion() != null && container.getTarget() != null;

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
                path.setEntries(_getInstalledNodeJSVersions()); //refresh
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
                                                           new Object[]{downloadBtn, NotifyDescriptor.CANCEL_OPTION},
                                                           downloadBtn, DialogDescriptor.DEFAULT_ALIGN, null,
                                                           onAction);

        // configure descriptor
        descriptor.setClosingOptions(new Object[]{downloadBtn, NotifyDescriptor.CANCEL_OPTION});

        // set valid, if changed
        downloadBtn.setEnabled(isValid.getAsBoolean());
        container.addChangeListener(a -> downloadBtn.setEnabled(isValid.getAsBoolean()));

        Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.setMinimumSize(new Dimension(450, 150));
        dialog.setPreferredSize(getMinimumSize());
        dialog.setVisible(true);
      }
      catch (Exception ex)
      {
        throw new IllegalStateException("Failed to download nodejs binary", ex);
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
  @NonNull
  private JLabel _createVersionLabel()
  {
    JLabel label = new JLabel();
    label.setEnabled(false);
    Runnable updateLabel = () -> _getNodeJSVersionOfSelectedPath(Bundle.LBL_UnknownVersion())
        .thenAccept(pVersion -> label.setText(Bundle.LBL_Version() + pVersion));
    updateLabel.run();
    path.addDocumentListener(new DocumentListener()
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
  @NonNull
  private CompletableFuture<String> _getNodeJSVersionOfSelectedPath(@NonNull String pDefault)
  {
    return CompletableFuture.supplyAsync(() -> {
      try
      {
        String value = path.getValue();
        if (value.equals(Bundle.LBL_BundledInstallation()))
          return NodeJSInstaller.DEFAULT_VERSION;

        INodeJSEnvironment env = NodeJSEnvironmentFactory.create(new File(value));
        if (env != null)
          return env.getVersion();
      }
      catch (Exception e)
      {
        // ignore
      }

      return pDefault;
    });
  }

  /**
   * Panel for Download Dialog
   */
  private static class _DownloadPanel extends JPanel
  {
    private final JComboBox<String> versions;
    private final PathSelectionPanel path;

    @NbBundle.Messages({
        "LBL_TargetPath=Target Path: ",
        "LBL_Chooser_Download=Target Folder for Downloaded Version"
    })
    public _DownloadPanel(@NonNull List<String> pAvailableVersions)
    {
      setBorder(new EmptyBorder(5, 5, 5, 5));

      double fill = TableLayoutConstants.FILL;
      double pref = TableLayoutConstants.PREFERRED;
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
      path = new PathSelectionPanel(Bundle.LBL_Chooser_Download(), JFileChooser.DIRECTORIES_ONLY);
      path.setValue(_DEFAULT_PATH);
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

    public void addChangeListener(@NonNull ChangeListener pOnChange)
    {
      path.addDocumentListener(new DocumentListener()
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

    /**
     * Checks, if the given path is a valid file path
     *
     * @param pPath Path to check
     * @return true, if valid
     */
    private static boolean _isValidFilePath(@NonNull String pPath)
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
  }

}
