package de.adito.aditoweb.nbm.nodejs.impl;

import org.openide.*;
import org.openide.modules.OnStop;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Hook that is executed when the designer closes.
 * If NodeJS scripts are still running the user will be warned
 * and has the option to terminate the scripts
 *
 * @author p.neub, 28.09.2022
 */
@OnStop
public class NodeJSScriptExitHook implements Callable<Boolean>
{
  private static final Set<CompletableFuture<Integer>> running = ConcurrentHashMap.newKeySet();

  @NbBundle.Messages({
      "LBL_ScriptExitConfirmTitle={0} NodeJS script(s) is/are still running...",
      "LBL_ScriptExitConfirmMessage=Terminate {0} running NodeJS script(s)?",
      "LBL_ScriptExitConfirmTerminateBtn=Terminate all",
      "LBL_ScriptExitConfirmDetachBtn=Detach all",
      "LBL_ScriptExitConfirmCancelBtn=Cancel exit",
  })
  @Override
  public Boolean call()
  {
    int count = running.size();
    if (count == 0)
      return Boolean.TRUE;
    NotifyDescriptor descriptor = new NotifyDescriptor.Confirmation(
        Bundle.LBL_ScriptExitConfirmMessage(count),
        Bundle.LBL_ScriptExitConfirmTitle(count));
    descriptor.setOptions(new String[]{
        Bundle.LBL_ScriptExitConfirmTerminateBtn(),
        Bundle.LBL_ScriptExitConfirmDetachBtn(),
        Bundle.LBL_ScriptExitConfirmCancelBtn(),
        });
    Object selected = DialogDisplayer.getDefault().notify(descriptor);
    if (NotifyDescriptor.CLOSED_OPTION.equals(selected) || Bundle.LBL_ScriptExitConfirmCancelBtn().equals(selected))
      return Boolean.FALSE;
    if (Bundle.LBL_ScriptExitConfirmTerminateBtn().equals(selected))
      running.forEach(f -> f.cancel(false));
    return Boolean.TRUE;
  }

  /**
   * Adds a running NodeJS process,
   * so that it can be terminated cleanly when the Designer is closed
   *
   * @param pProcessFuture process future
   */
  public static void add(CompletableFuture<Integer> pProcessFuture)
  {
    running.add(pProcessFuture);
  }

  /**
   * Removes the NodeJS process so that the user will no longer be warned about the process
   * this should be called when the process terminates, or the process is detached from the designer
   *
   * @param pProcessFuture process future
   */
  public static void remove(CompletableFuture<Integer> pProcessFuture)
  {
    running.remove(pProcessFuture);
  }

}
