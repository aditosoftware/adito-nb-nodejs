package de.adito.aditoweb.nbm.nodejs.impl.actions;

import de.adito.aditoweb.nbm.nodejs.impl.document.DocumentUtil;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.netbeans.api.editor.document.*;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.editor.document.StubImpl;

import javax.swing.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author m.kaspera, 27.04.2022
 */
class UnCommentActionTest
{

  @Test
  void isUncommentSingleLine()
  {
    JTextPane textPane = new JTextPane();
    textPane.setText("//test\ntest\ntest\n\ntest\n");
    textPane.setCaretPosition(4);
    AtomicLockDocument atomicLockDocument = new StubImpl(textPane.getDocument());
    LineDocument lineDocument = new BaseDocument(false, "text/javascript");
    try (MockedStatic<LineDocumentUtils> lineDocumentUtilsMockedStatic = Mockito.mockStatic(LineDocumentUtils.class);
         MockedStatic<DocumentUtil> documentUtilMockedStatic = Mockito.mockStatic(DocumentUtil.class))
    {
      lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.as(textPane.getDocument(), AtomicLockDocument.class)).thenReturn(atomicLockDocument);
      lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.as(textPane.getDocument(), LineDocument.class)).thenReturn(lineDocument);
      // mocking this is fine since DocumentUtil has its own tests checking if it works as intended
      documentUtilMockedStatic.when(() -> DocumentUtil.getSelectedLineOffsets(textPane, lineDocument)).thenReturn(List.of(0));
      documentUtilMockedStatic.when(() -> DocumentUtil.getLineFirstNonWhiteSpaceForLine(lineDocument, 0)).thenReturn(0);
      new UnCommentAction().actionPerformed(null, textPane);
      assertEquals("test\ntest\ntest\n\ntest\n", textPane.getText());
    }
  }

  @Test
  void isUncommentSingleLineWihtNoComment()
  {
    JTextPane textPane = new JTextPane();
    textPane.setText("test\ntest\ntest\n\ntest\n");
    textPane.setCaretPosition(4);
    AtomicLockDocument atomicLockDocument = new StubImpl(textPane.getDocument());
    LineDocument lineDocument = new BaseDocument(false, "text/javascript");
    try (MockedStatic<LineDocumentUtils> lineDocumentUtilsMockedStatic = Mockito.mockStatic(LineDocumentUtils.class);
         MockedStatic<DocumentUtil> documentUtilMockedStatic = Mockito.mockStatic(DocumentUtil.class))
    {
      lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.as(textPane.getDocument(), AtomicLockDocument.class)).thenReturn(atomicLockDocument);
      lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.as(textPane.getDocument(), LineDocument.class)).thenReturn(lineDocument);
      // mocking this is fine since DocumentUtil has its own tests checking if it works as intended
      documentUtilMockedStatic.when(() -> DocumentUtil.getSelectedLineOffsets(textPane, lineDocument)).thenReturn(List.of(0));
      documentUtilMockedStatic.when(() -> DocumentUtil.getLineFirstNonWhiteSpaceForLine(lineDocument, 0)).thenReturn(0);
      new UnCommentAction().actionPerformed(null, textPane);
      assertEquals("test\ntest\ntest\n\ntest\n", textPane.getText());
    }
  }

  @Test
  void isUncommentMultiLines()
  {
    JTextPane textPane = new JTextPane();
    textPane.setText("test\n//test\n//test\n\ntest\n");
    textPane.setCaretPosition(7);
    textPane.setSelectionStart(7);
    textPane.setSelectionEnd(14);
    AtomicLockDocument atomicLockDocument = new StubImpl(textPane.getDocument());
    LineDocument lineDocument = new BaseDocument(false, "text/javascript");
    try (MockedStatic<LineDocumentUtils> lineDocumentUtilsMockedStatic = Mockito.mockStatic(LineDocumentUtils.class);
         MockedStatic<DocumentUtil> documentUtilMockedStatic = Mockito.mockStatic(DocumentUtil.class))
    {
      lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.as(textPane.getDocument(), AtomicLockDocument.class)).thenReturn(atomicLockDocument);
      lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.as(textPane.getDocument(), LineDocument.class)).thenReturn(lineDocument);
      // mocking this is fine since DocumentUtil has its own tests checking if it works as intended
      documentUtilMockedStatic.when(() -> DocumentUtil.getSelectedLineOffsets(textPane, lineDocument)).thenReturn(List.of(1, 2));
      documentUtilMockedStatic.when(() -> DocumentUtil.getLineFirstNonWhiteSpaceForLine(lineDocument, 1)).thenReturn(5);
      documentUtilMockedStatic.when(() -> DocumentUtil.getLineFirstNonWhiteSpaceForLine(lineDocument, 2)).thenReturn(10);
      new UnCommentAction().actionPerformed(null, textPane);
      assertEquals("test\ntest\ntest\n\ntest\n", textPane.getText());
    }
  }
}