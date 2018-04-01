package de.lighti.clipper.gui

import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.geom.clipper.Clipper.*
import com.soywiz.korma.geom.clipper.Path
import com.soywiz.korma.geom.clipper.Paths
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*
import javax.swing.*

class ClipperDialog : JFrame() {
	private lateinit var statusStrip1: StatusBar
	private var panel1: JPanel? = null
	private var groupBox3: JPanel? = null
	private var rbNone: JRadioButton? = null
	private var rbXor: JRadioButton? = null
	private var rbDifference: JRadioButton? = null
	private var rbUnion: JRadioButton? = null
	private var rbIntersect: JRadioButton? = null
	private var groupBox2: JPanel? = null
	private var rbTest2: JRadioButton? = null
	private var rbTest1: JRadioButton? = null
	private var groupBox1: JPanel? = null
	private var label2: JLabel? = null
	private var nudOffset: JSpinner? = null
	private var lblCount: JLabel? = null
	private var nudCount: JSpinner? = null
	private var rbNonZero: JRadioButton? = null
	private var rbEvenOdd: JRadioButton? = null
	private var bRefresh: JButton? = null
	private var panel2: JPanel? = null
	private var pictureBox1: PolygonCanvas? = null
	private var bSave: JButton? = null

	init {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
			UIManager.getDefaults().put("Button.showMnemonics", java.lang.Boolean.TRUE)
		} catch (e: ClassNotFoundException) {
			//Too bad ...
		} catch (e: InstantiationException) {
		} catch (e: IllegalAccessException) {
		} catch (e: UnsupportedLookAndFeelException) {
		}

		jMenuBar = createMenuBar()
		createControls()

		defaultCloseOperation = JFrame.EXIT_ON_CLOSE
		preferredSize = Dimension(716, 521)
		isResizable = false
		title = "Clipper Java Demo"
		pack()

	}

	private fun createControls() {

		statusStrip1 = StatusBar()
		//        this.toolStripStatusLabel1 = new System.Windows.Forms.ToolStripStatusLabel();
		panel1 = JPanel()
		bSave = JButton()
		groupBox3 = JPanel()
		rbNone = JRadioButton()
		rbXor = JRadioButton()
		rbDifference = JRadioButton()
		rbUnion = JRadioButton()
		rbIntersect = JRadioButton()
		groupBox2 = JPanel()
		rbTest2 = JRadioButton()
		rbTest1 = JRadioButton()
		groupBox1 = JPanel()
		label2 = JLabel()
		nudOffset = JSpinner()
		lblCount = JLabel()
		nudCount = JSpinner()
		rbNonZero = JRadioButton()
		rbEvenOdd = JRadioButton()
		bRefresh = JButton()
		panel2 = JPanel()
		pictureBox1 = PolygonCanvas(statusStrip1)

		//
		// panel1
		//

		panel1!!.layout = FlowLayout(FlowLayout.LEFT)
		panel1!!.add(groupBox3)

		panel1!!.add(groupBox1)
		panel1!!.add(groupBox2)
		panel1!!.add(bRefresh)
		panel1!!.add(bSave)

		panel1!!.preferredSize = Dimension(121, 459)

		//
		// bSave
		//

		bSave!!.preferredSize = Dimension(100, 25)

		val bSaveAction = object : AbstractAction("Save SVG") {
			/**

			 */
			private val serialVersionUID = -8863563653315329743L

			override fun actionPerformed(e: ActionEvent) {
				val fc = JFileChooser(System.getProperty("user.dir"))
				val returnVal = fc.showSaveDialog(this@ClipperDialog)

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {

						val file = fc.selectedFile
						val svg = SVGBuilder()
						svg.style.brushClr = Color(0, 0, 0x9c, 0x20)
						svg.style.penClr = Color(0xd3, 0xd3, 0xda)
						svg.addPaths(pictureBox1!!.subjects)
						svg.style.brushClr = Color(0x20, 0x9c, 0, 0)
						svg.style.penClr = Color(0xff, 0xa0, 0x7a)
						svg.addPaths(pictureBox1!!.clips)
						svg.style.brushClr = Color(0x80, 0xff, 0x9c, 0xAA)
						svg.style.penClr = Color(0, 0x33, 0)
						svg.addPaths(pictureBox1!!.solution)
						svg.saveToFile(file.absolutePath, 1.0)

						statusStrip1!!.setText("Save successful")
					} catch (ex: IOException) {
						statusStrip1!!.setText("Error: " + ex.message)
					}

				}

			}
		}
		bSaveAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A)
		bSave!!.action = bSaveAction
		bSave!!.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "Save")
		bSave!!.actionMap.put("Save", bSaveAction)

		//
		// groupBox3
		//
		groupBox3!!.add(rbIntersect)
		groupBox3!!.add(rbUnion)
		groupBox3!!.add(rbDifference)
		groupBox3!!.add(rbXor)
		groupBox3!!.add(rbNone)

		groupBox3!!.border = BorderFactory.createTitledBorder("Boolean Op")
		groupBox3!!.layout = FlowLayout(FlowLayout.LEFT, 0, 0)
		groupBox3!!.preferredSize = Dimension(100, 135)

		val group3 = ButtonGroup()
		group3.add(rbNone)
		group3.add(rbXor)
		group3.add(rbDifference)
		group3.add(rbUnion)
		group3.add(rbIntersect)

		//
		// rbNone
		//
		rbNone!!.action = object : AbstractAction("None") {
			private val serialVersionUID = 4405963373838217293L

			override fun actionPerformed(e: ActionEvent) {
				pictureBox1!!.clipType = null
			}
		}
		//
		// rbXor
		//

		rbXor!!.action = object : AbstractAction("XOR") {
			private val serialVersionUID = -4865012993106866716L

			override fun actionPerformed(e: ActionEvent) {
				pictureBox1!!.clipType = ClipType.XOR
			}
		}
		//
		// rbDifference
		//
		rbDifference!!.action = object : AbstractAction("Difference") {
			private val serialVersionUID = -619610168436846559L

			override fun actionPerformed(e: ActionEvent) {
				pictureBox1!!.clipType = ClipType.DIFFERENCE
			}
		}
		//
		// rbUnion
		//
		rbUnion!!.action = object : AbstractAction("Union") {
			private val serialVersionUID = -8369519233115242994L

			override fun actionPerformed(e: ActionEvent) {
				pictureBox1!!.clipType = ClipType.UNION
			}
		}
		//
		// rbIntersect
		//

		rbIntersect!!.isSelected = true
		rbIntersect!!.action = object : AbstractAction("Intersect") {
			private val serialVersionUID = 5202593451595347999L

			override fun actionPerformed(e: ActionEvent) {
				pictureBox1!!.clipType = ClipType.INTERSECTION
			}
		}

		//
		// groupBox2
		//
		groupBox2!!.add(rbTest1)
		groupBox2!!.add(rbTest2)

		val group2 = ButtonGroup()
		group2.add(rbTest1)
		group2.add(rbTest2)

		groupBox2!!.layout = FlowLayout(FlowLayout.LEFT, 0, 0)
		groupBox2!!.border = BorderFactory.createTitledBorder("Sample")
		groupBox2!!.preferredSize = Dimension(100, 61)
		//
		// rbTest2
		//
		rbTest2!!.text = "Two"
		//
		// rbTest1
		//
		rbTest1!!.text = "One"
		rbTest1!!.isSelected = true
		//
		// groupBox1
		//
		groupBox1!!.layout = FlowLayout(FlowLayout.LEFT, 0, 0)
		groupBox1!!.add(rbEvenOdd)
		groupBox1!!.add(rbNonZero)
		groupBox1!!.add(lblCount)
		groupBox1!!.add(nudCount)
		groupBox1!!.add(label2)
		groupBox1!!.add(nudOffset)

		val group1 = ButtonGroup()
		group1.add(rbEvenOdd)
		group1.add(rbNonZero)

		groupBox1!!.border = BorderFactory.createTitledBorder("Options")
		groupBox1!!.preferredSize = Dimension(100, 159)

		//
		// label2
		//
		label2!!.text = "Offset:"

		//
		// nudOffset
		nudOffset!!.preferredSize = Dimension(54, 20)
		val nudOffsetModel = SpinnerNumberModel(0.0, -10.0, 10.0, 1.0)
		nudOffsetModel.addChangeListener { e -> pictureBox1!!.setOffset(nudOffsetModel.number.toFloat()) }
		nudOffset!!.model = nudOffsetModel
		nudOffset!!.addChangeListener { pictureBox1!!.updateSolution() }
		val nudOffsetEditor = nudOffset!!.editor as JSpinner.NumberEditor
		val nudOffsetEditorFormat = nudOffsetEditor.format
		nudOffsetEditorFormat.minimumFractionDigits = 1

		//
		// lblCount
		//
		lblCount!!.text = "Vertex Count:"
		//
		// nudCount
		//

		val nudCountModel = SpinnerNumberModel(DEFAULT_VERTEX_COUNT, 3, 100, 1)
		nudCountModel.addChangeListener { e -> pictureBox1!!.setVertexCount(nudCountModel.number.toInt()) }
		nudCount!!.model = nudCountModel
		val nudCountEditor = nudCount!!.editor as JSpinner.NumberEditor
		val nudCountEditorFormat = nudCountEditor.format
		nudCountEditorFormat.maximumFractionDigits = 0
		nudCount!!.preferredSize = Dimension(54, 20)

		//
		// rbNonZero
		//
		rbNonZero!!.action = object : AbstractAction("NonZero") {
			private val serialVersionUID = 5202593451595347999L

			override fun actionPerformed(e: ActionEvent) {
				pictureBox1!!.fillType = PolyFillType.NON_ZERO
			}
		}

		//
		// rbEvenOdd
		//

		rbEvenOdd!!.action = object : AbstractAction("EvenOdd") {
			private val serialVersionUID = 5202593451595347999L

			override fun actionPerformed(e: ActionEvent) {
				pictureBox1!!.fillType = PolyFillType.EVEN_ODD
			}
		}
		rbEvenOdd!!.isSelected = true

		//
		// bRefresh
		//

		bRefresh!!.preferredSize = Dimension(100, 25)
		val bRefreshAction = object : AbstractAction("New Sample") {

			/**

			 */
			private val serialVersionUID = 4405963373838217293L

			override fun actionPerformed(e: ActionEvent) {
				if (rbTest1!!.isSelected) {
					pictureBox1!!.generateRandomPolygon()
				} else {
					pictureBox1!!.generateAustPlusRandomEllipses()
				}
			}
		}
		bRefreshAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N)

		bRefresh!!.action = bRefreshAction
		bRefresh!!.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), "Refresh")
		bRefresh!!.actionMap.put("Refresh", bRefreshAction)

		//
		// panel2
		//
		panel2!!.add(pictureBox1)
		//        this.panel2.Dock = System.Windows.Forms.DockStyle.Fill;
		panel2!!.preferredSize = Dimension(595, 459)

		//
		// pictureBox1
		//

		pictureBox1!!.preferredSize = Dimension(591, 455)

		//
		// Form1
		//

		val root = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
		root.leftComponent = panel1
		root.rightComponent = panel2
		root.dividerLocation = panel1!!.preferredSize.width
		root.dividerSize = 1

		contentPane = JPanel(BorderLayout())
		contentPane.add(root, BorderLayout.CENTER)
		contentPane.add(statusStrip1!!, BorderLayout.SOUTH)

	}

	private fun createMenuBar(): JMenuBar {
		val menubar = JMenuBar()
		menubar.layout = FlowLayout(FlowLayout.LEFT, 0, 0)
		val loadSubjectItem = JMenuItem(object : AbstractAction("Load Subject") {

			/**

			 */
			private val serialVersionUID = 5372200924672915516L

			override fun actionPerformed(e: ActionEvent) {
				loadFile(PolyType.SUBJECT)

			}
		})
		menubar.add(loadSubjectItem)

		val loadCLipItem = JMenuItem(object : AbstractAction("Load Clip") {

			/**

			 */
			private val serialVersionUID = -6723609311301727992L

			override fun actionPerformed(e: ActionEvent) {
				loadFile(PolyType.CLIP)

			}
		})
		menubar.add(loadCLipItem)

		return menubar
	}

	private fun loadFile(type: PolyType) {
		val fc = JFileChooser(System.getProperty("user.dir"))
		val returnVal = fc.showOpenDialog(this@ClipperDialog)

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			val file = fc.selectedFile
			//This is where a real application would open the file.

			try {
				val paths = Paths()
				val success = loadFromFile(file.absolutePath, paths, 0)

				if (!success) {
					statusStrip1!!.setText("Error: check file syntax")
				} else {
					pictureBox1!!.setPolygon(type, paths)
					statusStrip1!!.setText("File loaded successful")
				}
			} catch (e: IOException) {
				statusStrip1!!.setText("Error: " + e.message)
			}

		} else {
			statusStrip1!!.setText("User cancelled")
		}
	}

	companion object {

		@Throws(IOException::class)
		@JvmOverloads internal fun loadFromFile(filename: String, ppg: Paths, dec_places: Int, xOffset: Long = 0, yOffset: Long = 0): Boolean {
			val scaling = Math.pow(10.0, dec_places.toDouble())

			ppg.clear()
			if (!File(filename).exists()) {
				return false
			}
			val delimiters = ", "
			val sr = BufferedReader(FileReader(filename))
			try {
				var line: String
				line = sr.readLine() ?: return false
				val polyCnt = Integer.parseInt(line)
				if (polyCnt < 0) {
					return false
				}

				for (i in 0..polyCnt - 1) {
					line = sr.readLine() ?: return false
					val vertCnt = Integer.parseInt(line)
					if (vertCnt < 0) {
						return false
					}
					val pg = Path(vertCnt)
					ppg.add(pg)
					if ((scaling > 0.999) and (scaling < 1.001)) {
						for (j in 0..vertCnt - 1) {
							var x: Long
							var y: Long
							line = sr.readLine() ?: return false
							val tokens = StringTokenizer(line, delimiters)

							if (tokens.countTokens() < 2) {
								return false
							}

							x = java.lang.Long.parseLong(tokens.nextToken())
							y = java.lang.Long.parseLong(tokens.nextToken())

							x += xOffset
							y += yOffset
							pg.add(Point2d(x, y))
						}
					} else {
						for (j in 0..vertCnt - 1) {
							var x: Double
							var y: Double
							line = sr.readLine() ?: return false
							val tokens = StringTokenizer(line, delimiters)

							if (tokens.countTokens() < 2) {
								return false
							}
							x = java.lang.Double.parseDouble(tokens.nextToken())
							y = java.lang.Double.parseDouble(tokens.nextToken())

							x = x * scaling + xOffset
							y = y * scaling + yOffset
							pg.add(Point2d(Math.round(x).toInt().toLong(), Math.round(y).toInt().toLong()))
						}
					}
				}
				return true
			} finally {
				sr.close()
			}
		}

		@JvmStatic fun main(args: Array<String>) {
			ClipperDialog().isVisible = true
		}

		var DEFAULT_VERTEX_COUNT = 5
		private val serialVersionUID = 7437089068822709778L
	}
}
