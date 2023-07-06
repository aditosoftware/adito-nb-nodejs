package de.adito.aditoweb.nbm.nodejs.impl.typescript;

import com.google.gson.*;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.javascript.node.*;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.transpiler.ILinkedFileCompiler;
import de.adito.aditoweb.nbm.nodejs.impl.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.openide.util.RequestProcessor;
import org.openide.util.io.*;
import org.openide.util.lookup.ServiceProvider;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ILinkedFileCompiler} for TypeScript support
 *
 * @author p.neub, 26.04.2023
 */
@ServiceProvider(service = ILinkedFileCompiler.class)
public final class TypeScriptLinkedFileCompiler implements ILinkedFileCompiler
{
  @Override
  public String getName()
  {
    return "TypeScript";
  }

  @NotNull
  @Override
  public String getSourceExtension()
  {
    return "ts";
  }

  @NotNull
  @Override
  public String getTargetExtension()
  {
    return "js";
  }

  @NotNull
  @Override
  public CompletableFuture<Void> compileFiles(@NotNull Path pRoot, @NotNull List<Path> pFiles)
  {
    return CompletableFuture.supplyAsync(() -> createConfig(pRoot, pFiles), RequestProcessor.getDefault())
        .thenApplyAsync(this::runCompiler)
        .thenAcceptAsync(this::cleanup);
  }

  /**
   * Creates a temporary tsconfig file.
   *
   * @param pRoot  root of the project/module
   * @param pFiles files that sohuld be included in the compilation
   * @return path to the tsconfig file
   */
  @NotNull
  private Path createConfig(@NotNull Path pRoot, @NotNull List<Path> pFiles)
  {
    Path tsconfig;
    try
    {
      Path extendsTsConfig = pRoot.resolve("tsconfig.json");

      TsConfigJson.CompilerOptions compilerOptions = TsConfigJson.CompilerOptions.builder()
          .noEmit(false)
          .checkJs(false)
          .sourceMap(true)
          .build();

      TsConfigJson tsConfigJson = TsConfigJson.builder()
          .extendsPath(Files.exists(extendsTsConfig) ? extendsTsConfig.toString() : null)
          .files(pFiles.stream().map(Path::toString).collect(Collectors.toList()))
          .include(List.of())
          .exclude(List.of())
          .compilerOptions(compilerOptions)
          .build();


      tsconfig = Files.createTempFile("tsconfig.jdito", ".json");
      try (final FileWriter writer = new FileWriter(tsconfig.toFile()))
      {
        new Gson().toJson(tsConfigJson, TsConfigJson.class, writer);
      }
    }
    catch (IOException pE)
    {
      throw new CompilationException(pE);
    }

    return tsconfig;
  }

  /**
   * Runs the TypeScript-Compiler.
   *
   * @param pTsConfig tsconfig that should be used as an input to tsc
   * @return future that resolves once the compiler has finished executing
   */
  @NotNull
  private CompilationResult runCompiler(@NotNull Path pTsConfig)
  {
    INodeJSExecutor executor = NodeJSInstallation.getCurrent().getExecutor();
    INodeJSEnvironment bundledEnvironment = NodeJSInstallation.getCurrent().getEnvironment();
    INodeJSExecBase tscBinary = new NPMCommandExecutor(executor, bundledEnvironment, true, null).binaries().get("tsc");
    String tscBinaryPath = bundledEnvironment.resolveExecBase(tscBinary).getAbsolutePath();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try
    {
      return new CompilationResult(
          pTsConfig, baos,
          executor.executeAsync(
              bundledEnvironment,
              INodeJSExecBase.node(),
              baos, baos, new NullInputStream(),
              tscBinaryPath, "--build", pTsConfig.toString()).join()
      );
    }
    catch (IOException pE)
    {
      throw new CompilationException(pE);
    }
  }

  /**
   * Cleans up the temporary tsconfig file and handles compiler errors
   *
   * @param pResult result of the compilation
   */
  private void cleanup(@NotNull CompilationResult pResult)
  {
    // Don't delete the temporary tsconfig, if an error occurred. This is intentional.
    if (pResult.getExitCode() != 0)
      throw new CompilationException(pResult.getBaos().toString(StandardCharsets.UTF_8));

    try
    {
      Files.delete(pResult.getTsconfig());
    }
    catch (IOException pE)
    {
      throw new CompilationException(pE);
    }
  }

  /**
   * Represents the result of a compilation
   */
  @AllArgsConstructor
  @Getter
  private static class CompilationResult
  {
    private final Path tsconfig;
    private final ByteArrayOutputStream baos;
    private final int exitCode;
  }
}
