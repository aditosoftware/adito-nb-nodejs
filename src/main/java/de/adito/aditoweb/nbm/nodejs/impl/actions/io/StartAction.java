package de.adito.aditoweb.nbm.nodejs.impl.actions.io;

import de.adito.aditoweb.nbm.vaadinicons.IVaadinIconsProvider;
import de.adito.swing.icon.IconAttributes;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.NonNull;
import org.openide.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Action for re-starting a NodeJS-Script
 *
 * @author s.seemann, 02.03.2022
 */
public class StartAction extends AbstractAction
{
  private final Runnable reRun;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final Disposable disposable; // strong ref

  @NbBundle.Messages("CTL_StartAction=Start Script")
  public StartAction(@NonNull Observable<Optional<CompletableFuture<Integer>>> pObservable,
                     @NonNull Runnable pReRun)
  {
    super(Bundle.CTL_StartAction(), new ImageIcon(Lookup.getDefault().lookup(IVaadinIconsProvider.class).getImage(IVaadinIconsProvider.VaadinIcon.PLAY,
                                                                                                                  new IconAttributes(16f))));
    reRun = pReRun;

    disposable = pObservable.subscribe(pOpt -> SwingUtilities.invokeLater(() -> setEnabled(!pOpt.isPresent() || (pOpt.get().isDone()
        || pOpt.get().isCompletedExceptionally())))
    );
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    reRun.run();
  }
}
