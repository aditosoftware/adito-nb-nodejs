package de.adito.aditoweb.nbm.nodejs.impl.actions;

import de.adito.aditoweb.nbm.nodejs.impl.document.DocumentUtil;
import org.netbeans.api.editor.document.*;
import org.netbeans.editor.BaseAction;
import org.openide.awt.*;
import org.openide.util.*;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * @author m.kaspera, 26.04.2022
 */
@ActionReferences({
    @ActionReference(path = "Editors/text/typescript/Toolbars/Default", position = 20200),
    @ActionReference(path = "Editors/text/javascript/Toolbars/Default", position = 20200)
})
@ActionID(category = "adito/editor/toolbar", id = "de.adito.aditoweb.nbm.nodejs.impl.actions.UnCommentAction")
@ActionRegistration(displayName = "Uncomment",
    iconBase = "de/adito/aditoweb/nbm/nodejs/impl/actions/uncomment.svg", lazy = false)
public class UnCommentAction extends BaseAction
{

  public UnCommentAction()
  {
    super("Uncomment");
    putValue(BaseAction.ICON_RESOURCE_PROPERTY, "de/adito/aditoweb/nbm/nodejs/impl/actions/uncomment.svg");
  }

  @Override
  public void actionPerformed(ActionEvent evt, JTextComponent target)
  {
    if (target != null)
    {
      if (!target.isEditable() || !target.isEnabled())
      {
        target.getToolkit().beep();
        return;
      }
      AtomicLockDocument doc = LineDocumentUtils.as(target.getDocument(), AtomicLockDocument.class);
      final LineDocument ld = LineDocumentUtils.as(target.getDocument(), LineDocument.class);
      if (doc == null || ld == null)
      {
        target.getToolkit().beep();
        return;
      }
      doc.runAtomic(() -> {
        try
        {
          List<Integer> selectedLineIndizes = DocumentUtil.getSelectedLineOffsets(target, ld);
          for (Integer lineIndex : selectedLineIndizes)
          {
            int startIndex = DocumentUtil.getLineFirstNonWhiteSpaceForLine(ld, lineIndex);
            if (target.getDocument().getText(startIndex, 2).equals("//"))
            {
              target.getDocument().remove(startIndex, 2);
            }
          }
        }
        catch (BadLocationException e)
        {
          target.getToolkit().beep();
        }
      });
    }
  }
}
