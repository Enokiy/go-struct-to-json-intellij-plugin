package com.github.enokiy;

import com.goide.psi.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;

/**
 * Created on 2021-08-28
 *
 * @author: enokiy
 */
public class GoStruct2JsonAction extends AnAction {

    private static boolean isShowComment = true;


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getDataContext().getData(CommonDataKeys.EDITOR);
        Project project = editor.getProject();
        Document document = editor.getDocument();

        String extension = Objects.requireNonNull(FileDocumentManager.getInstance().getFile(document)).getExtension();
        if (!(extension != null && extension.toLowerCase().equals("go"))) {
            return;
        }
        GoFile psiFile = (GoFile) e.getDataContext().getData(CommonDataKeys.PSI_FILE);
        int offset = editor.getCaretModel().getOffset();

        PsiElement cursorOffset = psiFile.findElementAt(offset);
        GoTypeDeclaration goTypeDeclarationContext = PsiTreeUtil.getContextOfType(cursorOffset,GoTypeDeclaration.class);

        if (goTypeDeclarationContext == null){
            GoStruct2JsonNotifier.notifyWarning(project,
                    "can't get go struct,please put the cursor on a struct then right click 'Convert Struct To JSON' label");
            return;
        }

        GoTypeSpec goTypeSpec = PsiTreeUtil.getChildOfType(goTypeDeclarationContext,GoTypeSpec.class);

        if (goTypeSpec == null) {
            GoStruct2JsonNotifier.notifyWarning(project,
                    "can't get go struct,please put the cursor on a struct then right click 'Convert Struct To JSON' label");
            return;
        } else {
            GoSpecType goSpecType = goTypeSpec.getSpecType();

            GoStructType goStructType = PsiTreeUtil.getChildOfType(goSpecType,GoStructType.class);
            if (goStructType == null){
                GoStruct2JsonNotifier.notifyWarning(project,
                        "can't get go struct,please put the cursor on a struct then right click 'Convert Struct To JSON' label");
                return;
            }
            String goStructName = goTypeSpec.getIdentifier().getText();

            String result = Utils.convertGoStructToJson(goStructType);
            if(result != "error"){
                StringSelection selection = new StringSelection(result);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection,selection);
                String msg = "Convert Struct " + goStructName + "to json success,copied to clipboard.";
                GoStruct2JsonNotifier.notifyInfo(project,msg + "\n" + result);
            }else{
                String err = "Convert Struct " + goStructName + " failed,please put the cursor on a struct then right click 'Convert Struct To JSON' label.";
                GoStruct2JsonNotifier.notifyError(project, err);
            }
        }
    }
}