package com.github.kaustubhpatange.composecolor

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
        const val RGBA_EXPRESSION = "(\\d+)f(\\s+)?,(\\s+)?(\\d+)f(\\s+)?,(\\s+)?(\\d+)f(,(\\s+)?(\\d+)f)?"
    }

    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<PsiElement>>) {
        if (element.node.elementType.toString() != JsonElementTypes::PROPERTY.name) return

        if (REFERENCE_EXPRESSION.toRegex().containsMatchIn(element.text)) {
            val colorHex: String = REFERENCE_EXPRESSION.toRegex().find(element.text)?.groups?.get(3)?.value ?: return

            // Support for Color(0xFFFFFFFF)
            if (colorHex.length == 10 && colorHex.startsWith("0XFF", true)) {
                val color = Color.decode("#" + colorHex.substring(4))
                result.add(getLineMarker(element, color, true))
            }
            // Support for Color(red,blue,green,alpha,...)
            else if (colorHex.matches(RGBA_EXPRESSION.toRegex())) {
                val matches = RGBA_EXPRESSION.toRegex().find(colorHex)?.groups!!
                val red = matches[1]?.value?.toInt() ?: 0
                val green = matches[4]?.value?.toInt() ?: 0
                val blue = matches[7]?.value?.toInt() ?: 0
                val alpha = matches[10]?.value?.toInt() ?: 255

                val color = Color(red, green, blue, alpha)
                result.add(getLineMarker(element, color))
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

    private fun getLineMarker(element: PsiElement, color: Color, runAction: Boolean = false): RelatedItemLineMarkerInfo<PsiElement> {
        val navHandler = if (runAction) GutterIconNavigationHandler { _, elt: PsiElement ->
            if (elt.containingFile.isWritable) {
                ColorPicker.showColorPickerPopup(elt.project, color) { c: Color, _ ->
                    if (c.rgb == color.rgb) return@showColorPickerPopup
                    runWriteAction {
                        val original = "Color(0x${Integer.toHexString(color.rgb).toUpperCase()})"
                        val new = "Color(0x${Integer.toHexString(c.rgb).toUpperCase()})"
                        val psiFile = PsiFileFactory.getInstance(element.project).createFileFromText(element.language, new)
                        val rElement = element.children.find { it.text.equals(original, ignoreCase = true) }
                        rElement?.replace(psiFile.children[2])
                        CodeStyleManager.getInstance(element.project).reformat(element)
                    }
                }
            }
        } else null

        return object : RelatedItemLineMarkerInfo<PsiElement>(
                element,
                element.textRange,
                getColorIcon(color),
                Pass.UPDATE_ALL,
                null,
                navHandler,
                GutterIconRenderer.Alignment.CENTER,
                emptyList()
        ) { }
    }
}
