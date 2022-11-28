package de.adito.aditoweb.nbm.nodejs.impl;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nodejs.impl.parser.PackageParser;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Helper for executing certain npm commands
 *
 * @author p.neub, 25.11.2022
 */
@AllArgsConstructor
public class NPMCommandExecutor
{

  private final INodeJSExecutor executor;
  private final INodeJSEnvironment environment;
  private final boolean isGlobal;
  private final String prefix;

  /**
   * Executes the "npm install" command and installs all given packages.
   *
   * @param pPackages packages that should be installed
   */
  public void install(@NotNull String... pPackages) throws IOException, InterruptedException, TimeoutException
  {
    String[] arguments = createArguments("install", pPackages);
    executor.executeSync(environment, INodeJSExecBase.packageManager(), -1, arguments);
  }

  /**
   * Executes the "npm update" command and updates all given packages.
   *
   * @param pPackages packages that should be updated
   */
  public void update(@NotNull String... pPackages) throws IOException, InterruptedException, TimeoutException
  {
    String[] arguments = createArguments("update", pPackages);
    executor.executeSync(environment, INodeJSExecBase.packageManager(), -1, arguments);
  }

  /**
   * Executes the "npm outdated" command and determines, if one of the given packages is outdated.
   *
   * @param pPackages packages that should be checked
   */
  public boolean outdated(@NotNull String... pPackages) throws IOException, InterruptedException, TimeoutException
  {
    String[] arguments = createArguments("outdated", pPackages);
    String result = executor.executeSync(environment, INodeJSExecBase.packageManager(), -1, false, arguments);
    return Arrays.stream(result.split("\n"))
        //the first line is the command line, that should be skipped
        .skip(1)
        .count() == pPackages.length;
  }

  /**
   * Executes the "npm list" command and determines, if all the given packages are installed.
   * It does not handle outdated packages - it just verifies, if it is installed.
   *
   * @param pPackages packages that should be checked
   */
  public boolean list(@NotNull String... pPackages) throws IOException, InterruptedException, TimeoutException
  {
    String[] arguments = createArguments("list", pPackages);
    String result = executor.executeSync(environment, INodeJSExecBase.packageManager(), -1, false, arguments);
    return Arrays.stream(result.split("\n"))
        // the first line is the command line, that should be skipped
        .skip(1)
        .count() == pPackages.length;
  }

  public Map<String, INodeJSExecBase> binaries()
  {
    try
    {
      String[] arguments = createArguments("list", "-j", "-l");
      String stdout = executor.executeSync(environment, INodeJSExecBase.packageManager(), 8000, false, arguments);
      return PackageParser.parseBinaries(stdout.substring(stdout.indexOf('\n')));
    }
    catch (IOException | InterruptedException | TimeoutException pE)
    {
      return Map.of();
    }
  }

  /**
   * Helper method to build the arguments needed for a npm command.
   * It automatically adds the global flag and the prefix argument if required.
   *
   * @param command    the npm subcommand that should be used in the generated arguments
   * @param pArguments additional arguments that should be passed to the npm command
   * @return the arguments as array
   */
  private String[] createArguments(@NotNull String command, @NotNull String... pArguments)
  {
    List<String> arguments = new ArrayList<>();
    arguments.add(command);
    arguments.add("-p");
    if (isGlobal)
      arguments.add("-g");
    if (prefix != null)
    {
      arguments.add("--prefix");
      arguments.add(prefix);
    }
    arguments.addAll(List.of(pArguments));
    return arguments.toArray(new String[0]);
  }

}
