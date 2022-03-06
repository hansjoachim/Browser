package org.example.gui

import org.example.layout.LayoutEngine
import org.example.network.NetworkFetcher
import java.awt.BorderLayout
import javax.swing.*

class BrowserWindow : JFrame("Browser") {
    private val network = NetworkFetcher()
    private val renderEngine = LayoutEngine()

    init {
        this.defaultCloseOperation = EXIT_ON_CLOSE
        this.setSize(800, 600)

        val addresseBar = JTextField("Where do you want to go?")

        val textArea = JTextArea("")
        val scrollableWrapper = JScrollPane(textArea)

        //TODO: insert a proper Go-button which does rendering
        val goButton = JButton("Go")
        goButton.addActionListener {
            val address = addresseBar.text
            val response = network.getRequest(address)
            textArea.text = renderEngine.render(response)
            textArea.caretPosition = 0 // Scroll to top
        }
        val debugButton = JButton("Debug")
        debugButton.addActionListener {
            val address = addresseBar.text
            val response = network.getRequest(address)
            textArea.text = renderEngine.debug(response)
            textArea.caretPosition = 0 // Scroll to top
        }

        val chrome = JPanel()
        chrome.add(addresseBar)
        chrome.add(goButton)
        chrome.add(debugButton)

        val pane = this.contentPane
        pane.add(chrome, BorderLayout.NORTH)
        pane.add(scrollableWrapper, BorderLayout.CENTER)
        this.isVisible = true
    }
}
