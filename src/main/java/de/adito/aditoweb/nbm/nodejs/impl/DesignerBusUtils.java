package de.adito.aditoweb.nbm.nodejs.impl;

import com.google.common.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.openide.util.Lookup;

/**
 * @author w.glanzer, 08.04.2022
 */
public class DesignerBusUtils
{

  private static final String _PLUGIN_ID = "PLUGIN_NODEJS";

  /**
   * Fires the event that signalizes, that a modules definition
   * possibly has been changed (or some of its children)
   */
  public static void fireModuleChange()
  {
    fireEvent("MODULE_CHANGE");
  }

  /**
   * Fires the given event in the common designer bus, if possible
   *
   * @param pEvent event to fire
   */
  @SuppressWarnings("UnstableApiUsage")
  public static void fireEvent(@NotNull String pEvent)
  {
    EventBus bus = Lookup.getDefault().lookup(EventBus.class);
    if (bus != null)
      bus.post(_PLUGIN_ID + "_" + pEvent);
  }

}
