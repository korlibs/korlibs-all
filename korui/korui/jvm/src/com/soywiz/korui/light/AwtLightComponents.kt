package com.soywiz.korui.light

import com.soywiz.korag.*
import com.soywiz.korim.awt.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.lang.Closeable
import com.soywiz.korui.event.*
import com.soywiz.korui.event.Event
import java.awt.*
import java.awt.datatransfer.*
import java.awt.dnd.*
import java.awt.event.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.image.*
import java.io.*
import java.net.*
import java.util.concurrent.*
import javax.swing.*
import javax.swing.border.*
import javax.swing.event.*
import javax.swing.text.*
import kotlin.reflect.*

typealias KoruiMouseEvent = com.soywiz.korui.event.MouseEvent
typealias KoruiMouseEventType = com.soywiz.korui.event.MouseEvent.Type
typealias KoruiChangeEvent = com.soywiz.korui.event.ChangeEvent

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
class AwtLightComponents : LightComponents() {
	init {
		if (UIManager.getLookAndFeel().name == UIManager.getCrossPlatformLookAndFeelClassName()) {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
		}
	}

	override fun create(type: LightType): LightComponentInfo {
		var agg: AG? = null
		val handle: Component = when (type) {
			LightType.FRAME -> JFrame2().apply {
				defaultCloseOperation = JFrame.EXIT_ON_CLOSE
			}
			LightType.CONTAINER -> JPanel2().apply {
				layout = null
			}
			LightType.BUTTON -> JButton()
			LightType.IMAGE -> JImage()
			LightType.PROGRESS -> JProgressBar(0, 100)
			LightType.LABEL -> JLabel()
			LightType.TEXT_FIELD -> JTextField()
			LightType.TEXT_AREA -> JScrollableTextArea()
			LightType.CHECK_BOX -> JCheckBox()
			LightType.SCROLL_PANE -> JScrollPane2()
			LightType.AGCANVAS -> {
				agg = agFactory.create()
				agg.nativeComponent as Component
			}
			else -> throw UnsupportedOperationException("Type: $type")
		}
		return LightComponentInfo(handle).apply {
			if (agg != null) {
				this.ag = agg!!
			}
		}
	}

	override fun <T : Event> registerEventKind(c: Any, clazz: KClass<T>, ed: EventDispatcher): Closeable {
		when (clazz) {
			KoruiMouseEvent::class -> {
				val cc = c as Component

				val adapter = object : MouseAdapter() {
					private val info = KoruiMouseEvent()

					private fun dispatch(ntype: KoruiMouseEventType, e: MouseEvent) {
						info.apply {
							type = ntype
							x = e.x
							y = e.y
							buttons = 1 shl e.button
							isAltDown = e.isAltDown
							isCtrlDown = e.isControlDown
							isShiftDown = e.isShiftDown
							isMetaDown = e.isMetaDown
						}
						ed.dispatch(info)
					}

					override fun mouseReleased(e: MouseEvent) = dispatch(KoruiMouseEventType.UP, e)
					override fun mousePressed(e: MouseEvent) = dispatch(KoruiMouseEventType.DOWN, e)
					override fun mouseClicked(e: MouseEvent) = dispatch(KoruiMouseEventType.CLICK, e)
					override fun mouseMoved(e: MouseEvent) = dispatch(KoruiMouseEventType.MOVE, e)
					override fun mouseDragged(e: MouseEvent) = dispatch(KoruiMouseEventType.DRAG, e)
					override fun mouseEntered(e: MouseEvent) = dispatch(KoruiMouseEventType.ENTER, e)
					override fun mouseExited(e: MouseEvent) = dispatch(KoruiMouseEventType.EXIT, e)
				}

				cc.addMouseListener(adapter)
				cc.addMouseMotionListener(adapter)

				return Closeable {
					cc.removeMouseListener(adapter)
					cc.removeMouseMotionListener(adapter)
				}
			}
			com.soywiz.korui.event.ChangeEvent::class -> {
				var rc = c as Component
				if (rc is JScrollableTextArea) rc = rc.textArea
				val cc = rc as? JTextComponent

				val adaptor = object : DocumentListener {
					val info = KoruiChangeEvent(null, null)

					override fun changedUpdate(e: DocumentEvent?) = ed.dispatch(info)
					override fun insertUpdate(e: DocumentEvent?) = ed.dispatch(info)
					override fun removeUpdate(e: DocumentEvent?) = ed.dispatch(info)
				}

				cc?.document?.addDocumentListener(adaptor)

				return Closeable {
					cc?.document?.removeDocumentListener(adaptor)
				}
			}
			DropFileEvent::class -> {
				val cc = c as JFrame

				val oldTH = cc.transferHandler
				cc.transferHandler = object : TransferHandler() {
					override fun canImport(support: TransferHandler.TransferSupport): Boolean {
						return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
					}

					override fun importData(support: TransferHandler.TransferSupport): Boolean {
						if (!canImport(support)) return false
						val l = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
						ed.dispatch(DropFileEvent(DropFileEvent.Type.DROP, l.map { LocalVfs(it) }))
						return true
					}
				}
				val adapter = object : DropTargetAdapter() {
					override fun dragEnter(dtde: DropTargetDragEvent) {
						ed.dispatch(DropFileEvent(DropFileEvent.Type.ENTER, null))
						//if (listener.enter(LightDropHandler.EnterInfo())) {
							dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE)
						//}
					}

					override fun dragExit(dte: DropTargetEvent) {
						ed.dispatch(DropFileEvent(DropFileEvent.Type.EXIT, null))
					}

					override fun drop(dtde: DropTargetDropEvent?) {
					}
				}
				cc.dropTarget.addDropTargetListener(adapter)

				return Closeable {
					cc.transferHandler = oldTH
					cc.dropTarget.removeDropTargetListener(adapter)
				}
			}
			ResizedEvent::class -> {
				val info = ResizedEvent(0, 0)
				val cc = c as? Container?

				fun send() {
					val cc2 = (c as? JFrame2?)
					val cp = cc2?.contentPane
					if (cp != null) {
						ed.dispatch(info.apply {
							width = cp.width
							height = cp.height
						})
					}
				}

				val adapter = object : ComponentAdapter() {
					override fun componentResized(e: ComponentEvent) {
						send()
					}
				}

				cc?.addComponentListener(adapter)
				send()

				return Closeable {
					cc?.removeComponentListener(adapter)
				}
			}
			com.soywiz.korui.event.KeyEvent::class -> {
				val cc = c as Component
				val ev = com.soywiz.korui.event.KeyEvent()

				val adapter = object : KeyAdapter() {
					private fun populate(type: com.soywiz.korui.event.KeyEvent.Type, e: KeyEvent) = ev.apply {
						this.type = type
						this.keyCode = e.keyCode
					}

					override fun keyTyped(e: KeyEvent) = ed.dispatch(populate(com.soywiz.korui.event.KeyEvent.Type.TYPE, e))
					override fun keyPressed(e: KeyEvent) = ed.dispatch(populate(com.soywiz.korui.event.KeyEvent.Type.DOWN, e))
					override fun keyReleased(e: KeyEvent) = ed.dispatch(populate(com.soywiz.korui.event.KeyEvent.Type.UP, e))
				}

				cc.addKeyListener(adapter)

				return Closeable {
					cc.removeKeyListener(adapter)
				}
			}
		}
		return Closeable { }
	}

	val Any.actualComponent: Component get() = if (this is JFrame2) this.panel else (this as Component)
	val Any.actualContainer: Container? get() = if (this is JFrame2) this.panel else (this as? Container)

	override fun setParent(c: Any, parent: Any?) {
		val actualParent = (parent as? ChildContainer)?.childContainer ?: parent?.actualContainer
		actualParent?.add((c as Component), 0)
		//println("$parent <- $c")
	}

	override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int) {
		//println("setBounds[${c.javaClass.simpleName}]($x, $y, $width, $height) : Thread(${Thread.currentThread().id})")
		when (c) {
			is JFrame2 -> {
				c.panel.preferredSize = Dimension(width, height)
				//c.preferredSize = Dimension(width, height)
				c.pack()
				//c.contentPane.setBounds(x, y, width, height)
			}
			is Component -> {
				if (c is JScrollPane2) {
					//c.preferredSize = Dimension(100, 100)
					//c.viewport.viewSize = Dimension(100, 100)
					c.viewport.setSize(width, height)
					val rightMost = c.childContainer.components.map { it.bounds.x + it.bounds.width }.max() ?: 0
					val bottomMost = c.childContainer.components.map { it.bounds.y + it.bounds.height }.max() ?: 0
					c.childContainer.preferredSize = Dimension(rightMost, bottomMost)
					c.setBounds(x, y, width, height)
					c.revalidate()
				} else {
					c.setBounds(x, y, width, height)
				}
			}
		}
	}

	override fun <T> callAction(c: Any, key: LightAction<T>, param: T) {
		when (key) {
			LightAction.FOCUS -> {
				(c as Component).requestFocus()
			}
		}
	}

	override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
		when (key) {
			LightProperty.VISIBLE -> {
				val visible = key[value]
				if (c is JFrame2) {
					if (!c.isVisible && visible) {
						c.setLocationRelativeTo(null)
					}
				}
				(c as Component).isVisible = visible
			}
			LightProperty.TEXT -> {
				val text = key[value]
				(c as? JLabel)?.text = text
				(c as? JScrollableTextArea)?.text = text
				(c as? JTextComponent)?.text = text
				(c as? AbstractButton)?.text = text
				(c as? Frame)?.title = text
			}
			LightProperty.IMAGE -> {
				val bmp = key[value]
				val image = (c as? JImage)
				if (image != null) {
					if (bmp == null) {
						image.image = null
					} else {
						if (bmp is AwtNativeImage) {
							image.image = bmp.awtImage.clone()

						} else {
							if ((image.width != bmp.width) || (image.height != bmp.height)) {
								//println("*********************** RECREATED NATIVE IMAGE!")
								image.image = bmp.toAwt()
							}
							bmp.toBMP32().transferTo(image.image!!)
						}
					}
					image.repaint()
				}
			}
			LightProperty.ICON -> {
				val bmp = key[value]
				when (c) {
					is JFrame2 -> {
						c.iconImage = bmp?.toBMP32()?.toAwt()
					}
				}
			}
			LightProperty.IMAGE_SMOOTH -> {
				val v = key[value]
				when (c) {
					is JImage -> {
						c.smooth = v
					}
				}
			}
			LightProperty.BGCOLOR -> {
				val v = key[value]
				(c as? Component)?.background = Color(v, true)
			}
			LightProperty.PROGRESS_CURRENT -> {
				(c as? JProgressBar)?.value = key[value]
			}
			LightProperty.PROGRESS_MAX -> {
				(c as? JProgressBar)?.maximum = key[value]
			}
			LightProperty.CHECKED -> {
				(c as? JCheckBox)?.isSelected = key[value]
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T> getProperty(c: Any, key: LightProperty<T>): T {
		return when (key) {
			LightProperty.CHECKED -> {
				(c as? JCheckBox)?.isSelected ?: false
			}
			LightProperty.TEXT -> {
				(c as? JLabel)?.text ?: (c as? JScrollableTextArea)?.text ?: (c as? JTextComponent)?.text
				?: (c as? AbstractButton)?.text ?: (c as? Frame)?.title
			}
			else -> super.getProperty(c, key)
		} as T
	}

	suspend override fun dialogAlert(c: Any, message: String) {
		JOptionPane.showMessageDialog(null, message)
	}

	suspend override fun dialogPrompt(c: Any, message: String, initialValue: String): String {
		val jpf = JTextField()
		jpf.addAncestorListener(RequestFocusListener())
		jpf.text = initialValue
		jpf.selectAll()
		val result =
			JOptionPane.showConfirmDialog(null, arrayOf(JLabel(message), jpf), "Reply:", JOptionPane.OK_CANCEL_OPTION)
		if (result != JFileChooser.APPROVE_OPTION) throw CancellationException()
		return jpf.text
	}

	suspend override fun dialogOpenFile(c: Any, filter: String): VfsFile {
		val fd = FileDialog(c as JFrame2, "Open file", FileDialog.LOAD)
		fd.isVisible = true
		return if (fd.files.isNotEmpty()) {
			LocalVfs(fd.files.first())
		} else {
			throw CancellationException()
		}
	}

	override fun repaint(c: Any) {
		(c as? Component)?.repaint()
	}

	override fun openURL(url: String): Unit {
		Desktop.getDesktop().browse(URI(url))
	}

	override fun open(file: VfsFile): Unit {
		Desktop.getDesktop().open(File(file.absolutePath))
	}

	override fun getDpi(): Double {
		val sr = Toolkit.getDefaultToolkit().screenResolution
		return sr.toDouble()
	}

	override fun configuredFrame(handle: Any) {
	}
}

class JFrame2 : JFrame() {
	val panel = JPanel2().apply {
		layout = null
	}

	init {
		add(panel)
	}
}

interface ChildContainer {
	val childContainer: Container
}

class JScrollableTextArea(val textArea: JTextArea = JTextArea()) : JScrollPane(textArea) {
	var text: String
		get() = textArea.text;
		set(value) {
			textArea.text = value
		}
}

class JScrollPane2(override val childContainer: JPanel = JPanel().apply { layout = null }) : JScrollPane(
	childContainer,
	ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
	ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
), ChildContainer {
	init {
		isOpaque = false
		val unitIncrement = 16
		verticalScrollBar.unitIncrement = unitIncrement
		horizontalScrollBar.unitIncrement = unitIncrement
		border = EmptyBorder(0, 0, 0, 0)
	}

	override fun paintComponent(g: Graphics) {
		g.clearRect(0, 0, width, height)
	}
}

class JPanel2 : JPanel() {
	init {
		isOpaque = false
	}

	//override fun paintComponent(g: Graphics) {
	//	g.clearRect(0, 0, width, height)
	//}
	//override fun paintComponent(g: Graphics) {
	//g.clearRect(0, 0, width, height)
	//}
}

class JImage : JComponent() {
	var image: BufferedImage? = null
	var smooth: Boolean = false

	override fun paintComponent(g: Graphics) {
		val g2 = (g as? Graphics2D)
		if (image != null) {
			g2?.setRenderingHint(
				RenderingHints.KEY_INTERPOLATION,
				if (smooth) RenderingHints.VALUE_INTERPOLATION_BILINEAR else RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
			)
			g.drawImage(image, 0, 0, width, height, null)
		} else {
			g.clearRect(0, 0, width, height)
		}
		//super.paintComponent(g)
	}
}

class RequestFocusListener(private val removeListener: Boolean = true) : AncestorListener {
	override fun ancestorAdded(e: AncestorEvent) {
		val component = e.component
		component.requestFocusInWindow()
		if (removeListener) component.removeAncestorListener(this)
	}

	override fun ancestorMoved(e: AncestorEvent) {}

	override fun ancestorRemoved(e: AncestorEvent) {}
}