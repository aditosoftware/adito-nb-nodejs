package de.adito.aditoweb.nbm.nodejs.impl.document;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.netbeans.api.editor.document.*;
import org.netbeans.editor.BaseDocument;

import javax.swing.*;
import javax.swing.text.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author m.kaspera, 27.04.2022
 */
class DocumentUtilTest
{

  @Nested
  class GetSelectedLineOffsetsTest
  {

    @Test
    void isSingleLineSelectionValid() throws BadLocationException
    {
      LineDocument lineDocument = new BaseDocument(false, "text/javascript");
      try (MockedStatic<LineDocumentUtils> lineDocumentUtilsMockedStatic = Mockito.mockStatic(LineDocumentUtils.class))
      {

        lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.getLineCount(lineDocument, 2, 2)).thenReturn(1);
        lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.getLineIndex(lineDocument, 2)).thenReturn(0);
        JTextComponent textComponent = new JTextPane();
        textComponent.setText("test\ntest\ntest}\n");
        textComponent.setCaretPosition(2);
        List<Integer> selectedLineOffsets = DocumentUtil.getSelectedLineOffsets(textComponent, lineDocument);
        assertEquals(List.of(0), selectedLineOffsets);
      }
    }

    @Test
    void isAllLineSelectionValid() throws BadLocationException
    {
      LineDocument lineDocument = new BaseDocument(false, "text/javascript");
      try (MockedStatic<LineDocumentUtils> lineDocumentUtilsMockedStatic = Mockito.mockStatic(LineDocumentUtils.class))
      {

        lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.getLineCount(lineDocument, 2, 10)).thenReturn(3);
        lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.getLineIndex(lineDocument, 2)).thenReturn(0);
        JTextComponent textComponent = new JTextPane();
        textComponent.setText("test\ntest\ntest}\n");
        textComponent.setCaretPosition(2);
        textComponent.setSelectionStart(2);
        textComponent.setSelectionEnd(10);
        List<Integer> selectedLineOffsets = DocumentUtil.getSelectedLineOffsets(textComponent, lineDocument);
        assertEquals(List.of(0, 1, 2), selectedLineOffsets);
      }
    }

    @Test
    void isMultiLineSelectionValid() throws BadLocationException
    {
      LineDocument lineDocument = new BaseDocument(false, "text/javascript");
      try (MockedStatic<LineDocumentUtils> lineDocumentUtilsMockedStatic = Mockito.mockStatic(LineDocumentUtils.class))
      {

        lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.getLineCount(lineDocument, 6, 10)).thenReturn(2);
        lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.getLineIndex(lineDocument, 6)).thenReturn(1);
        JTextComponent textComponent = new JTextPane();
        textComponent.setText("test\ntest\ntest}\n");
        textComponent.setCaretPosition(6);
        textComponent.setSelectionStart(6);
        textComponent.setSelectionEnd(10);
        List<Integer> selectedLineOffsets = DocumentUtil.getSelectedLineOffsets(textComponent, lineDocument);
        assertEquals(List.of(1, 2), selectedLineOffsets);
      }
    }
  }

  @Nested
  class GetLineFirstNonWhiteSpaceForLineTest
  {
    @Test
    void isValidReturnedLineIndexCorrect() throws BadLocationException
    {
      LineDocument lineDocument = new BaseDocument(false, "text/javascript");
      try (MockedStatic<LineDocumentUtils> lineDocumentUtilsMockedStatic = Mockito.mockStatic(LineDocumentUtils.class))
      {

        lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.getLineStartFromIndex(lineDocument, 1)).thenReturn(10);
        lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.getLineFirstNonWhitespace(lineDocument, 10)).thenReturn(12);
        int asdf = DocumentUtil.getLineFirstNonWhiteSpaceForLine(lineDocument, 1);
        assertEquals(12, asdf);
      }
    }

    @Test
    void isInvalidLineReturnInvalid() throws BadLocationException
    {
      LineDocument lineDocument = new BaseDocument(false, "text/javascript");
      try (MockedStatic<LineDocumentUtils> lineDocumentUtilsMockedStatic = Mockito.mockStatic(LineDocumentUtils.class))
      {

        lineDocumentUtilsMockedStatic.when(() -> LineDocumentUtils.getLineStartFromIndex(lineDocument, 1)).thenReturn(-1);
        int asdf = DocumentUtil.getLineFirstNonWhiteSpaceForLine(lineDocument, 1);
        assertEquals(-1, asdf);
      }
    }
  }
}