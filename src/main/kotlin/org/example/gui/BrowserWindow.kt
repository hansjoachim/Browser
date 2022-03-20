package org.example.gui

import org.example.layout.LayoutEngine
import org.example.network.NetworkFetcher
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

class BrowserWindow : JFrame("Browser") {
    private val network = NetworkFetcher()
    private val renderEngine = LayoutEngine()

    init {
        this.defaultCloseOperation = EXIT_ON_CLOSE
        this.setSize(800, 600)

        val addresseBar = JTextField("Where do you want to go?")

        val content = JPanel()
        content.layout = FlowLayout()
        val scrollableWrapper = JScrollPane(content)

        val goButton = JButton("Go")
        goButton.addActionListener {
            val address = addresseBar.text
            val response = network.getRequest(address)

            val renderHacky = renderEngine.renderHacky(response)
            replaceContent(content, renderHacky)
        }

        val rawStringButton = JButton("Raw string")
        rawStringButton.addActionListener {
            val address = addresseBar.text
            val response = network.getRequest(address)
            val textArea = JTextArea(renderEngine.renderAsString(response))
            textArea.caretPosition = 0 // Scroll to top

            replaceContent(content, textArea)
        }

        val debugButton = JButton("Debug")
        debugButton.addActionListener {
            val address = addresseBar.text
            val response = network.getRequest(address)
            val textArea = JTextArea(renderEngine.debug(response))
            textArea.caretPosition = 0 // Scroll to top

            replaceContent(content, textArea)
        }

        val chrome = JPanel()
        chrome.add(addresseBar)
        chrome.add(goButton)
        chrome.add(rawStringButton)
        chrome.add(debugButton)

        val pane = this.contentPane
        pane.add(chrome, BorderLayout.NORTH)
        pane.add(scrollableWrapper, BorderLayout.CENTER)
        this.isVisible = true
    }

    private fun replaceContent(contentWrapper: JPanel, newContent: JComponent) {
        contentWrapper.removeAll()
        contentWrapper.add(newContent)
        contentWrapper.revalidate()
        contentWrapper.repaint()
    }
}
