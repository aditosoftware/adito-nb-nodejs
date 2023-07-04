package de.adito.aditoweb.nbm.nodejs.impl.typescript;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Datatype for the options inside the tsconfig.json
 *
 * @author p.neub, 02.05.2023
 */
final class TsConfigJson
{
  @SerializedName("extends")
  public String extendsPath;

  @SerializedName("files")
  public List<String> files;

  @SerializedName("include")
  public List<String> include;

  @SerializedName("exclude")
  public List<String> exclude;

  @SerializedName("compilerOptions")
  public CompilerOptions compilerOptions;

  static final class CompilerOptions
  {
    @SerializedName("noEmit")
    public boolean noEmit;

    @SerializedName("checkJs")
    public boolean checkJs;

    @SerializedName("sourceMap")
    public boolean sourceMap;
  }
}
