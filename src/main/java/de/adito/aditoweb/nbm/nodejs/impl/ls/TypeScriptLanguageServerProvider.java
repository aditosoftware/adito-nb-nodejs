package de.adito.aditoweb.nbm.nodejs.impl.ls;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.*;
import de.adito.notification.INotificationFacade;
import de.adito.observables.netbeans.FileFullObservable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.jetbrains.annotations.*;
import org.netbeans.api.editor.mimelookup.*;
import org.netbeans.api.io.InputOutput;
import org.netbeans.api.project.Project;
import org.netbeans.modules.lsp.client.LanguageServerProviderAccessor;
import org.netbeans.modules.lsp.client.spi.*;
import org.openide.util.*;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
  private static final long STARTUP_DELAY = 10000;
  private static final RequestProcessor WORKER = new RequestProcessor(TypeScriptLanguageServerProvider.class.getName(), Integer.MAX_VALUE, false, false);
  private static final Logger LOGGER = Logger.getLogger(TypeScriptLanguageServerProvider.class.getName());
  private final AtomicReference<Optional<LanguageServerDescription>> currentRef = new AtomicReference<>(null);
  private final _NotificationHandler notificationHandler = new _NotificationHandler();
  private Disposable currentDisposable;

  @Override
  public LanguageServerDescription startServer(@NotNull Lookup pLookup)
  {
    Project prj = pLookup.lookup(Project.class);
    if (prj == null)
      return null;

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

    // Reset IO, because of new server
    InputOutput io = InputOutput.get("TypeScript Language Server", false);
    io.reset();

    // restarts
    _handleRestartOnChange(pServerRestarter);

    // execute
    return Optional.of(BundledNodeJS.getInstance().getBundledExecutor())
        .map(pExec -> {
          try
          {
            // check if NodeJS is available
            BundledNodeJS bundled = BundledNodeJS.getInstance();
            if (!bundled.isBundledEnvironmentAvailable())
              return null;

            INodeJSEnvironment bundledEnvironment = bundled.getBundledEnvironment();
            String pathLSP = "node_modules/" + IBundledPackages.TYPESCRIPT_LANGUAGE_SERVER + "/lib/cli.js";

            try
            {
              // check if Typescript LSP is available
              bundledEnvironment.resolveExecBase(INodeJSExecBase.module(IBundledPackages.TYPESCRIPT_LANGUAGE_SERVER, "lib/cli.js"));
            }
            catch (IllegalStateException e)
            {
              // no exec base found
              return null;
            }

            return pExec.execute(bundledEnvironment, INodeJSExecBase.node(), pathLSP, "--stdio", "--log-level", "4");
          }
          catch (IOException e)
          {
            LOGGER.log(Level.SEVERE, "", e);
            return null;
          }
        })
        .map(pProcess -> {
          WORKER.post(new _InputOutputWatcher(pProcess, io));
          return LanguageServerDescription.create(pProcess.getInputStream(), pProcess.getOutputStream(), pProcess);
        });
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
      currentDisposable = FileFullObservable.create(BundledNodeJS.getInstance().getBundledNodeJSContainer())
          .skip(1) // we only want changes, not the current one
          .throttleLast(2, TimeUnit.SECONDS)
          .subscribe(pEnvOpt -> {
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

  /**
   * Cares about showing and printing errors in InputOutput
   */
  private static class _InputOutputWatcher implements Runnable
  {
    private final Process process;
    private final InputOutput io;

    public _InputOutputWatcher(@NotNull Process pProcess, @NotNull InputOutput pIo)
    {
      process = pProcess;
      io = pIo;
    }

    @Override
    public void run()
    {
      long start = System.currentTimeMillis();
      try (Reader r = new InputStreamReader(process.getErrorStream()))
      {
        int read;
        while ((read = r.read()) != (-1))
          io.getErr().write("" + (char) read);
      }
      catch (IOException e)
      {
        LOGGER.log(Level.SEVERE, "", e);
      }
      finally
      {
        _closeSilently(io.getErr());
        _closeSilently(io.getOut());
        _closeSilently(io.getIn());
      }

      // show input/output if it was killed too early
      long end = System.currentTimeMillis();
      if (!process.isAlive() && (process.exitValue() != 0 || (end - start) < STARTUP_DELAY))
        io.show();
    }

    /**
     * Closes the closeable silently and only loggs errors
     *
     * @param pCloseable Object to close
     */
    private void _closeSilently(@NotNull Closeable pCloseable)
    {
      try
      {
        pCloseable.close();
      }
      catch (IOException e)
      {
        LOGGER.log(Level.WARNING, "", e);
      }
    }
  }
}
