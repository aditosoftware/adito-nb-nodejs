package de.adito.aditoweb.nbm.javascript.impl.version;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import lombok.ToString;
import org.jetbrains.annotations.*;

import java.io.File;

/**
 * @author w.glanzer, 08.03.2021
 */
public class NodeJSVersionFactory
{

  /**
   * Creates a version from a nodejs binary
   *
   * @param pBinary Binary of the installation
   * @return the (valid) version
   */
  @Nullable
  public static INodeJSVersion create(@NotNull File pBinary)
  {
    _BinaryVersion version = new _BinaryVersion(pBinary);
    if (version.isValid())
      return version;
    return null;
  }

  /**
   * Version of nodeJS
   */
  @ToString(doNotUseGetters = true)
  private static class _BinaryVersion implements INodeJSVersion
  {
    private final File nodejsBinary;

    public _BinaryVersion(@NotNull File pNodejsBinary)
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
      return INodeJSExecutor.getInstance().executeSync(new INodeJSVersion()
      {
        @NotNull
        @Override
        public File getPath()
        {
          return _BinaryVersion.this.getPath();
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
      }, "--version", 2000);
    }
  }

}
