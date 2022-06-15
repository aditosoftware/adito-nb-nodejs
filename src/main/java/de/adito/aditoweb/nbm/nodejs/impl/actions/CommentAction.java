package de.adito.aditoweb.nbm.nodejs.impl.actions;

import de.adito.aditoweb.nbm.nodejs.impl.document.DocumentUtil;
import org.netbeans.api.editor.document.*;
import org.netbeans.editor.BaseAction;
import org.openide.awt.*;

import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * @author m.kaspera, 25.04.2022
 */
@ActionReferences({
    @ActionReference(path = "Editors/text/typescript/Toolbars/Default", position = 20175),
    @ActionReference(path = "Editors/text/javascript/Toolbars/Default", position = 20175)
})
@ActionID(category = "adito/editor/toolbar", id = "de.adito.aditoweb.nbm.nodejs.impl.actions.CommentAction")
@ActionRegistration(displayName = "Comment",
    iconBase = "de/adito/aditoweb/nbm/nodejs/impl/actions/uncomment.svg", lazy = false)
public class CommentAction extends BaseAction
{
  public CommentAction()
  {
    super("Comment");
    putValue(BaseAction.ICON_RESOURCE_PROPERTY, "de/adito/aditoweb/nbm/nodejs/impl/actions/comment.svg");
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
      insertCommnets(target, doc, ld);
    }
  }

  static void insertCommnets(JTextComponent target, AtomicLockDocument doc, LineDocument ld)
  {
    doc.runAtomic(() -> {
      try
      {
        List<Integer> selectedLineIndizes = DocumentUtil.getSelectedLineOffsets(target, ld);
        for (Integer lineIndex : selectedLineIndizes)
        {
          target.getDocument().insertString(DocumentUtil.getLineFirstNonWhiteSpaceForLine(ld, lineIndex), "//", null);
        }
      }
      catch (BadLocationException e)
      {
        target.getToolkit().beep();
      }
    });
  }
}
