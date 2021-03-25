package de.adito.aditoweb.nbm.javascript.impl.ls;

import com.google.common.cache.*;
import org.jetbrains.annotations.*;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.io.InputOutput;
import org.netbeans.api.project.*;
import org.netbeans.modules.lsp.client.LanguageServerProviderAccessor;
import org.netbeans.modules.lsp.client.spi.LanguageServerProvider;
import org.openide.util.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * @author w.glanzer, 10.03.2021
 */
@MimeRegistration(mimeType = "text/typescript", service = LanguageServerProvider.class)
public class TypeScriptLanguageServerProvider implements LanguageServerProvider
{
  private static final long STARTUP_DELAY = 10000;
  private static final RequestProcessor WORKER = new RequestProcessor(TypeScriptLanguageServerProvider.class.getName(), Integer.MAX_VALUE, false, false);
  private static final Logger LOGGER = Logger.getLogger(TypeScriptLanguageServerProvider.class.getName());
  private static final Cache<String, LanguageServerDescription> PROJECT_LSP_CACHE = CacheBuilder.newBuilder()
      .expireAfterAccess(15, TimeUnit.MINUTES)
      .removalListener((RemovalListener<String, LanguageServerDescription>) pRemoval -> _stopServer(pRemoval.getValue()))
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
      LanguageServerDescription current = PROJECT_LSP_CACHE.get(projectPath, () -> _startServer(prj));
      if (!_isAlive(current))
      {
        PROJECT_LSP_CACHE.invalidate(projectPath);
        current = PROJECT_LSP_CACHE.get(projectPath, () -> _startServer(prj));
      }

      return current;
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
  @Nullable
  private LanguageServerDescription _startServer(@NotNull Project pProject)
  {
    // Log Start
    String projectPath = pProject.getProjectDirectory().getPath();
    LOGGER.warning("Starting TypeScript Language Server for project " + projectPath);

    try
    {
      // Start
      InputOutput io = InputOutput.get("TypeScript Language Server for project " + ProjectUtils.getInformation(pProject).getDisplayName(), false);
      io.reset();
      Process process = new ProcessBuilder(projectPath + "/node_modules/typescript-language-server/lib/cli.js", "--stdio").start(); //todo should be in executor
      WORKER.post(new _InputOutputWatcher(process, io));
      return LanguageServerDescription.create(process.getInputStream(), process.getOutputStream(), process);
    }
    catch (Throwable t)
    {
      LOGGER.log(Level.SEVERE, "", t);
      return null;
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
