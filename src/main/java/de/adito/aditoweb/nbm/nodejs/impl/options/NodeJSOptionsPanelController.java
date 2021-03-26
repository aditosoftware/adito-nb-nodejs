package de.adito.aditoweb.nbm.nodejs.impl.options;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.*;

import javax.swing.*;
import java.beans.PropertyChangeListener;

/**
 * @author w.glanzer, 08.03.2021
 */
@NbBundle.Messages("LBL_NodeJSOptionsPanelController_Title=NodeJS")
@OptionsPanelController.SubRegistration(id = "nodejs", displayName = "#LBL_NodeJSOptionsPanelController_Title", location = "Adito", position = 150,
    keywords = "nodejs node js javascript adito", keywordsCategory = "Adito")
public class NodeJSOptionsPanelController extends OptionsPanelController
{

  private final NodeJSOptionsPanel panel = new NodeJSOptionsPanel();

  @Override
  public void update()
  {
    panel.setCurrent(NodeJSOptions.getInstance());
  }

  @Override
  public void applyChanges()
  {
    NodeJSOptions.update(panel.getCurrent());
  }

  @Override
  public void cancel()
  {
    update();
  }

  @Override
  public boolean isValid()
  {
    return true;
  }

  @Override
  public boolean isChanged()
  {
    return !NodeJSOptions.getInstance().equals(panel.getCurrent());
  }

  @Override
  public JComponent getComponent(Lookup pLookup)
  {
    return panel;
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener pPropertyChangeListener)
  {
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener pPropertyChangeListener)
  {
  }

}
