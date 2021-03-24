package de.adito.aditoweb.nbm.javascript.impl.ls;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.lsp.client.spi.LanguageServerProvider;

/**
 * @author w.glanzer, 10.03.2021
 */
@MimeRegistration(mimeType = "text/typescript", service = LanguageServerProvider.class)
public class TypeScriptLanguageServerProvider extends AbstractTypeScriptLanguageServerProvider
{
}
