package de.adito.aditoweb.nbm.nodejs.impl.ls;

import lombok.NonNull;
import org.netbeans.api.project.*;
import org.netbeans.spi.editor.hints.*;
import org.openide.filesystems.*;

import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Map;

/**
 * @author m.kaspera, 26.08.2022
 */
public class IgnoreWarningFix implements Fix
{
  private final int id;
  private final String description;
  private final FileObject fileObject;

  public IgnoreWarningFix(int pId, @NonNull String pDescription, @NonNull FileObject pFileObject)
  {
    id = pId;
    description = pDescription;
    fileObject = pFileObject;
  }

  @Override
  public String getText()
  {
    String text = "Ignore all warnings of type: '" + description + "'";
    if(text.length() > 130)
      text = text.substring(0, 130) + "...";
    return text;
  }

  @Override
  public ChangeInfo implement() throws Exception
  {
    Project project = FileOwnerQuery.getOwner(fileObject);
    IgnoredWarningsFacade.addIgnoredWarning(project, id, description);
    // "touch" the given file so that the warnings/errors are generated anew
    Files.setLastModifiedTime(FileUtil.toFile(fileObject).toPath(), FileTime.from(Instant.now()));
    fileObject.refresh();
    return new ChangeInfo();
  }

  static class FileContent
  {
    public Map<String, String> content;
  }

}
