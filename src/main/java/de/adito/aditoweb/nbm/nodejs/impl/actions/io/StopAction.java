package de.adito.aditoweb.nbm.nodejs.impl.actions.io;

import de.adito.aditoweb.nbm.vaadinicons.IVaadinIconsProvider;
import de.adito.swing.icon.IconAttributes;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.jetbrains.annotations.NotNull;
import org.openide.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Action for stopping a running NodeJS-Script
 *
 * @author s.seemann, 02.03.2022
 */
public class StopAction extends AbstractAction
{
  private final Observable<Optional<CompletableFuture<Integer>>> observable;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final Disposable disposable; // strong ref

  @NbBundle.Messages("CTL_StopAction=Stop Script")
  public StopAction(@NotNull Observable<Optional<CompletableFuture<Integer>>> pObservable)
  {
    super(Bundle.CTL_StopAction(), new ImageIcon(Lookup.getDefault().lookup(IVaadinIconsProvider.class).getImage(IVaadinIconsProvider.VaadinIcon.STOP,
                                                                                                                 new IconAttributes(16f))));
    observable = pObservable;

    disposable = observable.subscribe(pOpt -> SwingUtilities.invokeLater(() -> setEnabled(pOpt.isPresent()
                                                                                              && !pOpt.get().isDone()
                                                                                              && !pOpt.get().isCompletedExceptionally())));
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    Optional<CompletableFuture<Integer>> futureOpt = observable.blockingFirst(Optional.empty());
    futureOpt.ifPresent(pFuture -> pFuture.cancel(false));
  }
}
