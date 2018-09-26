package de.lighti.clipper.gui

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.SystemColor

import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel

class StatusBar : JPanel() {
	internal class AngledLinesWindowsCornerIcon : Icon {

		override fun getIconHeight(): Int {
			return WIDTH
		}

		override fun getIconWidth(): Int {
			return HEIGHT
		}

		override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {

			g.color = WHITE_LINE_COLOR
			g.drawLine(0, 12, 12, 0)
			g.drawLine(5, 12, 12, 5)
			g.drawLine(10, 12, 12, 10)

			g.color = GRAY_LINE_COLOR
			g.drawLine(1, 12, 12, 1)
			g.drawLine(2, 12, 12, 2)
			g.drawLine(3, 12, 12, 3)

			g.drawLine(6, 12, 12, 6)
			g.drawLine(7, 12, 12, 7)
			g.drawLine(8, 12, 12, 8)

			g.drawLine(11, 12, 12, 11)
			g.drawLine(12, 12, 12, 12)

		}

		companion object {
			private val WHITE_LINE_COLOR = Color(255, 255, 255)

			private val GRAY_LINE_COLOR = Color(172, 168, 153)
			private val WIDTH = 13

			private val HEIGHT = 13
		}
	}

	private val text: JLabel

	init {
		layout = BorderLayout()
		preferredSize = Dimension(10, 23)

		val rightPanel = JPanel(BorderLayout())
		rightPanel.add(JLabel(AngledLinesWindowsCornerIcon()), BorderLayout.SOUTH)
		rightPanel.isOpaque = false

		text = JLabel()
		add(text, BorderLayout.WEST)
		add(rightPanel, BorderLayout.EAST)
		background = SystemColor.control
	}

	override fun paintComponent(g: Graphics) {
		super.paintComponent(g)

		var y = 0
		g.color = Color(156, 154, 140)
		g.drawLine(0, y, width, y)
		y++
		g.color = Color(196, 194, 183)
		g.drawLine(0, y, width, y)
		y++
		g.color = Color(218, 215, 201)
		g.drawLine(0, y, width, y)
		y++
		g.color = Color(233, 231, 217)
		g.drawLine(0, y, width, y)

		y = height - 3
		g.color = Color(233, 232, 218)
		g.drawLine(0, y, width, y)
		y++
		g.color = Color(233, 231, 216)
		g.drawLine(0, y, width, y)
		y = height - 1
		g.color = Color(221, 221, 220)
		g.drawLine(0, y, width, y)

	}

	fun setText(text: String) {
		this.text.text = text
	}

	companion object {

		/**

		 */
		private val serialVersionUID = 3434051407308227123L
	}
}
