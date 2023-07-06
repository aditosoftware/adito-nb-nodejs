package de.adito.aditoweb.nbm.nodejs.impl.typescript;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;

import java.util.List;

/**
 * Datatype for the options inside the tsconfig.json
 *
 * @author p.neub, 02.05.2023
 */
@Builder
final class TsConfigJson
{
  /**
   * Set extendsPath to use the options defined in the module/project.
   */
  @SerializedName("extends")
  private String extendsPath;

  /**
   * Files that have been changed and need to be recompiled.
   */
  @SerializedName("files")
  private List<String> files;

  /**
   * Overwrite include to reset the include paths, we only want specific files.
   */
  @SerializedName("include")
  private List<String> include;

  /**
   * Overwrite exclude to reset the exclude paths, we do want all specified files.
   */
  @SerializedName("exclude")
  private List<String> exclude;

  /**
   * Predefine common compiler options so projects/modules don't get it wrong.
   */
  @SerializedName("compilerOptions")
  private CompilerOptions compilerOptions;

  @Builder
  static final class CompilerOptions
  {
    /**
     * We do want to emit .js files, some projects/modules set this to true, since they want tsc to only typescript.
     */
    @SerializedName("noEmit")
    private boolean noEmit;

    /**
     * Don't check js, since they contain to many errors.
     */
    @SerializedName("checkJs")
    private boolean checkJs;

    /**
     * Always generate source maps.
     */
    @SerializedName("sourceMap")
    private boolean sourceMap;
  }
}
