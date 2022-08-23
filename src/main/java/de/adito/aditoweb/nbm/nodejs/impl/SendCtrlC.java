package de.adito.aditoweb.nbm.nodejs.impl;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Pattern;

/**
 * @author p.neub, 18.08.2022
 */
public class SendCtrlC
{
  private static final Pattern WRONG_PREFIX_MATCHER = Pattern.compile("/[A-Z]:/.*");
  private static SendCtrlC instance;

  private final String nativePath;

  public SendCtrlC()
  {
    nativePath = getNativePath();
  }

  public void send(long pProcessId) throws IOException
  {
    String pid = String.valueOf(pProcessId);
    if (System.getProperty("os.name").toLowerCase().startsWith("windows"))
      new ProcessBuilder(Paths.get(nativePath, "sendctrlc.x64.exe").toString(), pid).start();
    else
      new ProcessBuilder("kill", "-2", pid).start();
  }

  protected String getNativePath()
  {
    URL pJarFile = SendCtrlC.class.getProtectionDomain().getCodeSource().getLocation();
    String[] split = pJarFile.toExternalForm().split("!");

    String pathToExe = split[0];
    if (pathToExe.startsWith("jar:file:"))
      pathToExe = pathToExe.substring(9);

    if (pathToExe.startsWith("file:"))
      pathToExe = pathToExe.substring(5);

    if (WRONG_PREFIX_MATCHER.matcher(pathToExe).matches())
      pathToExe = pathToExe.substring(1);

    pathToExe = URLDecoder.decode(pathToExe, StandardCharsets.UTF_8);
    Path jarDir = Paths.get(pathToExe).getParent();
    return Paths.get(jarDir.toString(), "/ext/de.adito.aditoweb.nbm.nodejs/native").toString();
  }

  public static SendCtrlC getInstance()
  {
    if (instance == null)
      instance = new SendCtrlC();
    return instance;
  }

  public static void setInstance(SendCtrlC pInstance)
  {
    instance = pInstance;
  }
}
