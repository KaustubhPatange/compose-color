package com.github.kaustubhpatange.composecolorplugin

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.json.JsonElementTypes
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.ui.ColorPicker
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.Icon
import javax.swing.ImageIcon

class DemoLineMarkerProvider : RelatedItemLineMarkerProvider() {
    companion object {
        const val REFERENCE_EXPRESSION = "val(.*?)=\\s+?Color\\((\\s+)?(.*?)(\\s+)?\\)"
    }

    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<PsiElement>>) {
        if (element.node.elementType.toString() != JsonElementTypes::PROPERTY.name) return

        if (REFERENCE_EXPRESSION.toRegex().containsMatchIn(element.text)) {
            val colorHex: String = REFERENCE_EXPRESSION.toRegex().find(element.text)?.groups?.get(3)?.value ?: return
            if (colorHex.length == 10 && colorHex.startsWith("0XFF", true)) {
                val color = Color.decode("#" + colorHex.substring(4))
                val lineMarker = object : RelatedItemLineMarkerInfo<PsiElement>(element, element.textRange, getColorIcon(color), Pass.UPDATE_ALL, null,
                        GutterIconNavigationHandler<PsiElement> { _, elt: PsiElement ->
                            ColorPicker.showColorPickerPopup(elt.project, color) { c: Color, _ ->
                               runWriteAction {
                                   val text = "Color(0X${Integer.toHexString(c.rgb).toUpperCase()})"
                                   val psiFile = PsiFileFactory.getInstance(element.project).createFileFromText(element.language, text)
                                   element.lastChild.replace(psiFile.children[2])
                                   CodeStyleManager.getInstance(element.project).reformat(element)
                               }
                            }
                        }, GutterIconRenderer.Alignment.CENTER, emptyList()) {
                }
                result.add(lineMarker)
            }
        }
    }

    private fun getColorIcon(color: Color): Icon {
        val image = UIUtil.createImage(7, 7, BufferedImage.TYPE_INT_RGB)
        image.graphics.apply {
            setColor(color)
            fillRect(0, 0, 7, 7)
        }
        return ImageIcon(image)
    }
}