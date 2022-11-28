package de.adito.aditoweb.nbm.nodejs.impl.ls;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.*;
import de.adito.notification.INotificationFacade;
import io.reactivex.rxjava3.disposables.Disposable;
import org.jetbrains.annotations.*;
import org.netbeans.api.editor.mimelookup.*;
import org.netbeans.modules.lsp.client.LanguageServerProviderAccessor;
import org.netbeans.modules.lsp.client.spi.*;
import org.openide.util.*;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.*;

/**
 * @author w.glanzer, 10.03.2021
 */
@MimeRegistrations({
    @MimeRegistration(mimeType = "text/typescript", service = LanguageServerProvider.class),
    @MimeRegistration(mimeType = "text/javascript", service = LanguageServerProvider.class),
})
public class TypeScriptLanguageServerProvider implements LanguageServerProvider
{
  private static final Logger LOGGER = Logger.getLogger(TypeScriptLanguageServerProvider.class.getName());
  private final AtomicReference<Optional<LanguageServerDescription>> currentRef = new AtomicReference<>(null);
  private final _NotificationHandler notificationHandler = new _NotificationHandler();
  private Disposable currentDisposable;

  @Override
  public LanguageServerDescription startServer(@NotNull Lookup pLookup)
  {
    synchronized (currentRef)
    {
      Optional<LanguageServerDescription> current = currentRef.get();

      //noinspection OptionalAssignedToNull
      if (current == null || !_isAlive(current.orElse(null)))  //NOSONAR null is a valid value, even it is not recommended
      {
        current = _startServer(pLookup.lookup(ServerRestarter.class));
        if (!current.isPresent())
          notificationHandler.sendNotification();
        currentRef.set(current);
      }

      return current.orElse(null);
    }
  }

  /**
   * Starts the server for the given project
   *
   * @return the server description
   */
  @NotNull
  private Optional<LanguageServerDescription> _startServer(@Nullable ServerRestarter pServerRestarter)
  {
    // Log Start
    LOGGER.log(Level.INFO, "Starting TypeScript Language Server");

    // restarts
    _handleRestartOnChange(pServerRestarter);

    // execute
    return Optional.of(NodeJSInstallation.getCurrent().getExecutor())
        .map(pExec -> {
          try
          {
            // check if NodeJS is available
            NodeJSInstallation installation = NodeJSInstallation.getCurrent();
            if (!installation.isEnvironmentAvailable())
              return null;

            INodeJSEnvironment bundledEnvironment = installation.getEnvironment();

            try
            {
              INodeJSExecBase lspBinary = new NPMCommandExecutor(pExec, bundledEnvironment, true, null)
                  .binaries().get("typescript-language-server");
              if (lspBinary == null)
                return null;

              String lspBinaryPath = bundledEnvironment.resolveExecBase(lspBinary).getAbsolutePath();
              return pExec.execute(bundledEnvironment, INodeJSExecBase.node(), lspBinaryPath, "--stdio", "--log-level", "4");
            }
            catch (IllegalStateException pE)
            {
              // no valid typescript langauge server binary found
              return null;
            }
          }
          catch (IOException e)
          {
            LOGGER.log(Level.SEVERE, "", e);
            return null;
          }
        })
        .map(pProcess -> LanguageServerDescription.create(pProcess.getInputStream(), pProcess.getOutputStream(), pProcess));
  }

  /**
   * Cares about restarting the Language-Server, if necessary.
   * Not threadsafe!
   *
   * @param pServerRestarter Restarter, NULL if there is no possibility to restart
   */
  private void _handleRestartOnChange(@Nullable ServerRestarter pServerRestarter)
  {
    // remove old disposable
    if (currentDisposable != null && !currentDisposable.isDisposed())
      currentDisposable.dispose();

    // create new, if necessary
    if (pServerRestarter != null)
      currentDisposable = NodeJSInstaller.observeInstallation()
          .skip(1) // ignore initial value, we only want real changes
          .subscribe(pTime -> {
            LOGGER.info("Restarting TypeScript Language Server");

            // Stop Server
            Optional<LanguageServerDescription> currentServer = currentRef.get();
            if (currentServer != null && currentServer.isPresent()) //NOSONAR null is a valid value, even it is not recommended
              _stopServer(currentServer.get());

            // Trigger Restart
            try
            {
              pServerRestarter.restart();
            }
            catch (Exception e)
            {
              LOGGER.log(Level.SEVERE, "", e);
            }
          });
  }

  /**
   * Stops the given language server
   *
   * @param pDescription Description to stop
   */
  private static void _stopServer(@Nullable LanguageServerDescription pDescription)
  {
    if (pDescription == null)
      return;

    try
    {
      if (!_isAlive(pDescription))
        LanguageServerProviderAccessor.getINSTANCE().getProcess(pDescription).destroy();
    }
    catch (Exception e)
    {
      LOGGER.log(Level.WARNING, "Failed to shutdown language server", e);
    }
  }

  /**
   * Returns true, if the language server in the given description is alive
   *
   * @param pDescription Description to check
   * @return true, if it is alive
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private static boolean _isAlive(@Nullable LanguageServerDescription pDescription)
  {
    if (pDescription == null)
      return false;

    try
    {
      Process process = LanguageServerProviderAccessor.getINSTANCE().getProcess(pDescription);
      return process != null && process.isAlive();
    }
    catch (Exception e)
    {
      return false;
    }
  }

  /**
   * Cares about showing notification balloon if lsp is not available
   */
  private static class _NotificationHandler
  {
    private static final long _GAP_BETWEEN_NOTIFICATIONS = 5000;
    private long lastNotify;

    @NbBundle.Messages({
        "Title_NoTypescript=Automatic code completion currently not available",
        "Msg_NoTypescript=Currently automatic code completion not available, NodeJS environment has not been initialized yet"
    })
    public void sendNotification()
    {
      if (System.currentTimeMillis() - lastNotify < _GAP_BETWEEN_NOTIFICATIONS)
        return;

      lastNotify = System.currentTimeMillis();
      INotificationFacade.INSTANCE.notify(Bundle.Title_NoTypescript(), Bundle.Msg_NoTypescript(), false, null);
    }
  }
}
