package de.adito.aditoweb.nbm.nodejs.impl.ls;

import com.google.common.cache.*;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import io.reactivex.rxjava3.disposables.Disposable;
import org.jetbrains.annotations.*;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.io.InputOutput;
import org.netbeans.api.project.*;
import org.netbeans.modules.lsp.client.LanguageServerProviderAccessor;
import org.netbeans.modules.lsp.client.spi.*;
import org.openide.util.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * @author w.glanzer, 10.03.2021
 */
@MimeRegistration(mimeType = "text/typescript", service = LanguageServerProvider.class)
public class TypeScriptLanguageServerProvider implements LanguageServerProvider
{
  public static final String NEEDED_MODULE = "typescript-language-server";
  private static final long STARTUP_DELAY = 10000;
  private static final RequestProcessor WORKER = new RequestProcessor(TypeScriptLanguageServerProvider.class.getName(), Integer.MAX_VALUE, false, false);
  private static final Logger LOGGER = Logger.getLogger(TypeScriptLanguageServerProvider.class.getName());
  private static final Map<String, Disposable> PROJECT_DISPOSABLES_MAP = new HashMap<>();
  private static final Cache<String, Optional<LanguageServerDescription>> PROJECT_LSP_CACHE = CacheBuilder.newBuilder()
      .expireAfterAccess(15, TimeUnit.MINUTES)
      .removalListener((RemovalListener<String, Optional<LanguageServerDescription>>) pRemoval -> {
        Optional<LanguageServerDescription> value = pRemoval.getValue();

        //noinspection OptionalAssignedToNull
        _stopServer(value == null ? null : value.orElse(null)); //NOSONAR null is a valid value
      })
      .build();

  @Override
  public LanguageServerDescription startServer(@NotNull Lookup pLookup)
  {
    Project prj = pLookup.lookup(Project.class);
    if (prj == null)
      return null;

    try
    {
      String projectPath = prj.getProjectDirectory().getPath();
      Optional<LanguageServerDescription> current = PROJECT_LSP_CACHE.get(projectPath, () -> _startServer(prj, pLookup.lookup(ServerRestarter.class)));
      if (!_isAlive(current.orElse(null)))
      {
        PROJECT_LSP_CACHE.invalidate(projectPath);
        current = PROJECT_LSP_CACHE.get(projectPath, () -> _startServer(prj, pLookup.lookup(ServerRestarter.class)));
      }

      return current.orElse(null);
    }
    catch (ExecutionException e)
    {
      return null;
    }
  }

  /**
   * Starts the server for the given project
   *
   * @param pProject project
   * @return the server description
   */
  @NotNull
  private Optional<LanguageServerDescription> _startServer(@NotNull Project pProject, @Nullable ServerRestarter pServerRestarter)
  {
    // Log Start
    String projectPath = pProject.getProjectDirectory().getPath();
    LOGGER.log(Level.INFO, "Starting TypeScript Language Server for project {0}", projectPath);

    // Reset IO, because of new server
    InputOutput io = InputOutput.get("TypeScript Language Server for project " + ProjectUtils.getInformation(pProject).getDisplayName(), false);
    io.reset();

    // restarts
    _handleRestartOnChange(pProject, pServerRestarter);

    // nodejs provider for environment
    INodeJSProvider nodeJSProvider = pProject.getLookup().lookup(INodeJSProvider.class);
    if (nodeJSProvider == null)
      return Optional.empty();

    // retrieve environment with blocking first, because we restart the server anyway, if changed
    INodeJSEnvironment env = nodeJSProvider.current().blockingFirst().orElse(null);
    if (env == null)
      return Optional.empty();

    // execute
    return INodeJSExecutor.findInstance(pProject)
        .map(pExec -> {
          try
          {
            return pExec.execute(env, INodeJSExecBase.module(NEEDED_MODULE, "lib/cli.js"), "--stdio");
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
   * Cares about restarting the Language-Server, if necessary
   *
   * @param pProject         Project to handle
   * @param pServerRestarter Restarter, NULL if there is no possibility to restart
   */
  private void _handleRestartOnChange(@NotNull Project pProject, @Nullable ServerRestarter pServerRestarter)
  {
    String projectPath = pProject.getProjectDirectory().getPath();

    // remove old disposable
    Disposable oldDisposable = PROJECT_DISPOSABLES_MAP.remove(projectPath);
    if (oldDisposable != null && !oldDisposable.isDisposed())
      oldDisposable.dispose();

    // create new, if necessary
    if (pServerRestarter != null)
    {
      INodeJSProvider provider = pProject.getLookup().lookup(INodeJSProvider.class);
      if (provider != null)
        PROJECT_DISPOSABLES_MAP.put(projectPath, provider
            .current()
            .skip(1) // we only want changes, not the current one
            .distinctUntilChanged()
            .subscribe(pEnvOpt -> {
              LOGGER.info("Restarting TypeScript Language Server for project " + projectPath + " because environment changed");

              // Stop Server
              Optional<LanguageServerDescription> currentServer = PROJECT_LSP_CACHE.getIfPresent(projectPath);
              if (currentServer != null && currentServer.isPresent()) //NOSONAR null is a valid value, even it is not recommended
                _stopServer(currentServer.get());

              // Trigger Restart
              pServerRestarter.restart();
            }));
    }
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
