package de.adito.aditoweb.nbm.nodejs.impl.version;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.NodeJSExecutorImpl;
import lombok.ToString;
import org.jetbrains.annotations.*;
import org.openide.util.BaseUtilities;

import java.io.File;

/**
 * @author w.glanzer, 08.03.2021
 */
public class NodeJSEnvironmentFactory
{

  /**
   * Creates an environment from a nodejs binary
   *
   * @param pBinary Binary of the installation
   * @return the (valid) env
   */
  @Nullable
  public static INodeJSEnvironment create(@NotNull File pBinary)
  {
    _BinaryEnvironment version = new _BinaryEnvironment(pBinary);
    if (version.isValid())
      return version;
    return null;
  }

  /**
   * Version of nodeJS
   */
  @ToString(doNotUseGetters = true)
  private static class _BinaryEnvironment implements INodeJSEnvironment
  {
    private final File nodejsBinary;

    public _BinaryEnvironment(@NotNull File pNodejsBinary)
    {
      nodejsBinary = pNodejsBinary;
    }

    @NotNull
    @Override
    public File getPath()
    {
      return nodejsBinary;
    }

    @NotNull
    @Override
    public File resolveExecBase(@NotNull INodeJSExecBase pBase)
    {
      // try with casual base path
      File executable = new File(nodejsBinary.getParentFile(), pBase.getBasePath());
      if (executable.exists())
        return executable;

      // not found - maybe add .exe on windows?
      if (BaseUtilities.isWindows())
      {
        executable = new File(nodejsBinary.getParentFile(), pBase.getBasePath() + ".exe");
        if (executable.exists())
          return executable;
      }

      // not found
      throw new RuntimeException("Unable to determine absolute path of execution base (" + pBase.getBasePath() + ", " +
                                     nodejsBinary.getParentFile().getAbsolutePath() + ")");
    }

    @NotNull
    @Override
    public String getVersion()
    {
      _ensureValid();

      try
      {
        return _readVersion();
      }
      catch (Exception e)
      {
        throw new RuntimeException("Failed to retrieve version from nodejs package (" + getPath() + ")", e);
      }
    }

    /**
     * @return true, if this version is valid and can be used
     */
    @Override
    public boolean isValid()
    {
      try
      {
        return !_readVersion().isEmpty();
      }
      catch (Exception e)
      {
        return false;
      }
    }

    /**
     * Ensures, that this version is valid
     */
    private void _ensureValid()
    {
      if (!isValid())
        throw new IllegalArgumentException("NodeJSVersion is not valid (" + getPath() + ")");
    }

    /**
     * Extracts the version from this package
     *
     * @return the version
     */
    @NotNull
    private String _readVersion() throws Exception
    {
      return NodeJSExecutorImpl.getInternalUnboundExecutor(new File(".")).executeSync(new INodeJSEnvironment()
      {
        @NotNull
        @Override
        public File getPath()
        {
          return _BinaryEnvironment.this.getPath();
        }

        @NotNull
        @Override
        public File resolveExecBase(@NotNull INodeJSExecBase pBase)
        {
          return _BinaryEnvironment.this.getPath();
        }

        @NotNull
        @Override
        public String getVersion()
        {
          return "invalid";
        }

        @Override
        public boolean isValid()
        {
          return true;
        }
      }, INodeJSExecBase.node(), 2000, "--version");
    }
  }

}
