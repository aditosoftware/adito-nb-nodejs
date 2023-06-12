package de.adito.aditoweb.nbm.nodejs.impl.runconfig;

import de.adito.nbm.runconfig.api.IRunConfigCategory;
import io.reactivex.rxjava3.core.Observable;
import lombok.NonNull;
import org.openide.util.*;

import java.awt.*;
import java.util.Optional;

/**
 * Category for NodeJSScriptRunConfig
 *
 * @author w.glanzer, 12.05.2021
 * @see NodeJSScriptRunConfig
 */
class NodeJSScriptRunConfigCategory implements IRunConfigCategory
{

  private static final Image _ICON = ImageUtilities.loadImage("de/adito/aditoweb/nbm/nodejs/impl/runconfig/nodejs16.png"); //NOI18N

  @NonNull
  @Override
  public String getName()
  {
    return "de-adito-aditoweb-nbm-nodejs-impl-runconfig-NodeJSScriptRunConfigCategory";
  }

  @NonNull
  @Override
  @NbBundle.Messages("LBL_RunConfigCategoryTitle=Scripts")
  public Observable<String> title()
  {
    return Observable.just(Bundle.LBL_RunConfigCategoryTitle());
  }

  @NonNull
  @Override
  public Observable<Optional<Image>> icon()
  {
    return Observable.just(Optional.of(_ICON));
  }

}
