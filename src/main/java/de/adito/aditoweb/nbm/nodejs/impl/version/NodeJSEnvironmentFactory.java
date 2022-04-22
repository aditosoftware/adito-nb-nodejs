package de.adito.aditoweb.nbm.nodejs.impl.version;

import com.google.common.base.Strings;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.NodeJSExecutorImpl;
import lombok.ToString;
import org.jetbrains.annotations.*;
import org.openide.util.BaseUtilities;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

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

  private NodeJSEnvironmentFactory()
  {
  }

  /**
   * Version of nodeJS
   */
  @ToString(doNotUseGetters = true)
  private static class _BinaryEnvironment implements INodeJSEnvironment
  {
    private final File nodejsBinary;
    private Boolean valid = null;
    private Long validCheckBinaryLastModified = null;

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
      String extension = "";
      if (BaseUtilities.isWindows())
        extension = pBase.getWindowsExt();
      else if (BaseUtilities.isUnix())
        extension = pBase.getLinuxExt();
      else if (BaseUtilities.isMac())
        extension = pBase.getMacExt();

      File parent = nodejsBinary.getParentFile();
      String child = pBase.getBasePath() + (Strings.isNullOrEmpty(extension) ? "" : "." + extension);

      // first take a look in the direct parent - mostly correct under Windows
      File executable = new File(parent, child);
      if (executable.exists())
        return executable;

      // second take a look in the grandparents directory - mostly correct under Unix / Mac
      executable = new File(parent.getParentFile(), child);
      if (executable.exists())
        return executable;

      // not found
      throw new IllegalStateException("Unable to determine absolute path of execution base (" + pBase.getBasePath() + ", " +
                                          parent.getAbsolutePath() + ")");
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
        throw new IllegalStateException("Failed to retrieve version from nodejs package (" + getPath() + ")", e);
      }
    }

    /**
     * @return true, if this version is valid and can be used
     */
    @Override
    public boolean isValid()
    {
      if (valid == Boolean.TRUE && Objects.equals(validCheckBinaryLastModified, nodejsBinary.lastModified()))
        return true;

      try
      {
        valid = !_readVersion().isEmpty();
        validCheckBinaryLastModified = nodejsBinary.lastModified();
        return valid;
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
    private String _readVersion() throws IOException, InterruptedException, TimeoutException
    {
      String result = NodeJSExecutorImpl.getInternalUnboundExecutor(new File(".")).executeSync(new INodeJSEnvironment()
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

      // only read last line, because this line contains the version
      String[] lines = result.split("\n");
      if (lines.length < 2)
        return "";
      return lines[lines.length - 1];
    }
  }

}
