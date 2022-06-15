package de.adito.aditoweb.nbm.nodejs.impl.actions;

import de.adito.aditoweb.nbm.nodejs.impl.document.DocumentUtil;
import org.netbeans.api.editor.document.*;
import org.netbeans.editor.BaseAction;
import org.openide.awt.*;

import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author m.kaspera, 08.06.2022
 */
@ActionID(category = "adito/editor/toolbar", id = "de.adito.aditoweb.nbm.nodejs.impl.actions.ToggleCommentAction")
@ActionRegistration(displayName = "Toggle Comment", lazy = false)
public class ToggleCommentAction extends BaseAction
{

  public ToggleCommentAction()
  {
    super("toggle-comment-action-adito");
  }

  @Override
  public void actionPerformed(ActionEvent evt, JTextComponent target)
  {
    AtomicLockDocument doc = LineDocumentUtils.as(target.getDocument(), AtomicLockDocument.class);
    final LineDocument ld = LineDocumentUtils.as(target.getDocument(), LineDocument.class);
    if (doc == null || ld == null)
    {
      target.getToolkit().beep();
      return;
    }
    if (!containsCommentedLine(target, doc, ld))
    {
      CommentAction.insertCommnets(target, doc, ld);
    }
    else
    {
      UnCommentAction.uncommentCode(target, doc, ld);
    }
  }

  private boolean containsCommentedLine(JTextComponent target, AtomicLockDocument doc, LineDocument ld)
  {
    AtomicBoolean isContainsCommentedLine = new AtomicBoolean(false);
    doc.runAtomic(() -> {
      try
      {
        List<Integer> selectedLineIndizes = DocumentUtil.getSelectedLineOffsets(target, ld);
        for (Integer lineIndex : selectedLineIndizes)
        {
          int startIndex = DocumentUtil.getLineFirstNonWhiteSpaceForLine(ld, lineIndex);
          if (target.getDocument().getText(startIndex, 2).equals("//"))
          {
            isContainsCommentedLine.set(true);
            return;
          }
        }
      }
      catch (BadLocationException e)
      {
        target.getToolkit().beep();
      }
    });
    return isContainsCommentedLine.get();
  }
}
