package de.adito.aditoweb.nbm.nodejs.impl.document;

import org.jetbrains.annotations.NotNull;
import org.netbeans.api.editor.document.*;

import javax.swing.text.*;
import java.util.*;

/**
 * @author m.kaspera, 26.04.2022
 */
public class DocumentUtil
{

  /**
   *
   * @param pTextComponent TextComponent for which the selected lines should be calculated
   * @param pLineDocument line-oriented document of the textComponent
   * @return List with the line indices of the currently selected lines. A line index is the line number -1
   * @throws BadLocationException thrown if during calculating the line indices an invalid offset is used
   */
  public static List<Integer> getSelectedLineOffsets(@NotNull JTextComponent pTextComponent, @NotNull LineDocument pLineDocument) throws BadLocationException
  {
    List<Integer> selectedLineOffsets = new ArrayList<>();
    int selectionStart = pTextComponent.getSelectionStart();
    int selectionEnd = pTextComponent.getSelectionEnd();
    int lineCount = LineDocumentUtils.getLineCount(pLineDocument, selectionStart, selectionEnd);
    int startLine = LineDocumentUtils.getLineIndex(pLineDocument, selectionStart);
    for (int lineIndex = startLine; lineIndex < lineCount + startLine; lineIndex++)
    {
      selectedLineOffsets.add(lineIndex);
    }
    return selectedLineOffsets;
  }

  /**
   *
   * @param pLineDocument line-oriented document of the textpane
   * @param pLineOffset line index of the line for which the index of the first non-whitespace character should be returned. The line index is line number -1
   * @return the offset of the first non-whitespace character of the requested line. -1 if the given line index is invalid
   * @throws BadLocationException
   */
  public static int getLineFirstNonWhiteSpaceForLine(@NotNull LineDocument pLineDocument, int pLineOffset) throws BadLocationException
  {
    int lineStartFromIndex = LineDocumentUtils.getLineStartFromIndex(pLineDocument, pLineOffset);
    if (lineStartFromIndex >= 0)
    {
      // Math.max in case the line only contains whitespaces -> getLineFirstNonWhitespace returns -1, but the start of the line should be returned
      return Math.max(lineStartFromIndex, LineDocumentUtils.getLineFirstNonWhitespace(pLineDocument, lineStartFromIndex));
    }
    else
    {
      return -1;
    }
  }

}
