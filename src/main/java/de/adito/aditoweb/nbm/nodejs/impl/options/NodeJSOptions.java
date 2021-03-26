package de.adito.aditoweb.nbm.nodejs.impl.options;

import de.adito.observables.netbeans.PreferencesObservable;
import io.reactivex.rxjava3.core.Observable;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.openide.util.NbPreferences;

import java.util.prefs.Preferences;

/**
 * @author w.glanzer, 08.03.2021
 */
@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
public class NodeJSOptions
{

  private static final Preferences PREFS = NbPreferences.forModule(NodeJSOptions.class);

  /**
   * Path to a valid NodeJS environment (binary)
   */
  private String path;

  /**
   * Reads a new instance from preferences
   *
   * @return options, freshly generated from preferences
   */
  @NotNull
  public static NodeJSOptions getInstance()
  {
    return NodeJSOptions.builder()
        .path(PREFS.get("path", null))
        .build();
  }

  /**
   * Creates a new observable that fires, if options change
   *
   * @return Observable with options
   */
  @NotNull
  public static Observable<NodeJSOptions> observe()
  {
    return PreferencesObservable.create(PREFS)
        .map(pPrefs -> getInstance());
  }

  /**
   * Updates the options in preferences with the values given
   *
   * @param pOptions Values to set in preferences
   */
  public static void update(@NotNull NodeJSOptions pOptions)
  {
    PREFS.put("path", pOptions.getPath());
  }

}
