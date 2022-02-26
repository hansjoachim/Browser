package org.example

import org.example.html.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.html.HTMLElement
import java.lang.Character.isWhitespace

class Parser {

    private var framsetOk: Boolean = true
    private val fosterParenting = false

    private var currentMode: InsertionMode = InsertionMode.initial
    private var originalInsertionMode: InsertionMode? = null

    private var headElementPointer: Element? = null
    private val openElements: ArrayDeque<Node> = ArrayDeque()


    fun parse(document: String): Document {
        return parse(Tokenizer(document).tokenize())
    }

    private fun parse(originalTokens: List<Token>): Document {
        //A Document object has an associated parser cannot change the mode flag (a boolean). It is initially false.
        val root = DocumentImpl()
        val tokens: MutableList<Token> = originalTokens.toMutableList()

        while (tokens.isNotEmpty()) {
            val token = tokens.removeFirst()
            when (currentMode) {
                InsertionMode.initial -> {
                    if (token.type == TokenType.Character && isWhitespace((token as CharacterToken).data)) {
                        //ignore
                    } else if (token.type == TokenType.DOCTYPE) {
                        //FIXME: might be parse error based on values
                        val doctype = (token as DOCTYPEToken)
                        val documentType =
                            DocumentTypeImpl(doctype.name, doctype.publicIdentifier, doctype.systemIdentifier)
                        root.doctype = documentType

                        //deal with doctypes, various types and quirks...

                        switchTo(InsertionMode.beforeHtml)
                    } else {
                        unhandledMode(InsertionMode.initial, token)
                    }
                }
                InsertionMode.beforeHtml -> {
                    if (token.type == TokenType.DOCTYPE) {
                        unhandledMode(InsertionMode.beforeHtml, token)
                    } else if (token.type == TokenType.Comment) {
                        unhandledMode(InsertionMode.beforeHtml, token)
                    } else if (token.type == TokenType.Character && isWhitespace((token as CharacterToken).data)) {
                        //ignore
                    } else if (token.type == TokenType.StartTag && (token as StartTagToken).tagName == "html") {
                        val element = createElementFromTagName(token.tagName)
                        root.appendChild(element)
                        openElements.addLast(element)
                        switchTo(InsertionMode.beforeHead)
                    } else if (token.type == TokenType.EndTag && listOf(
                            "head",
                            "body",
                            "html",
                            "br"
                        ).contains((token as EndTagToken).tagName)
                    ) {
                        unhandledMode(InsertionMode.beforeHtml, token)
                    } else if (token.type == TokenType.EndTag) {
                        unhandledMode(InsertionMode.beforeHtml, token)
                    } else {
                        val element = createElementFromTagName("html")
                        root.appendChild(element)
                        openElements.addLast(element)
                        switchTo(InsertionMode.beforeHead)
                    }
                }
                InsertionMode.beforeHead -> {
                    if (token.type == TokenType.Character && isWhitespace((token as CharacterToken).data)) {
                        //ignore
                    } else if (token.type == TokenType.Comment) {
                        unhandledMode(InsertionMode.beforeHead, token)
                    } else if (token.type == TokenType.DOCTYPE) {
                        unhandledMode(InsertionMode.beforeHead, token)
                    } else if (token.type == TokenType.StartTag && (token as StartTagToken).tagName == "html") {
                        unhandledMode(InsertionMode.beforeHead, token)
                    } else if (token.type == TokenType.StartTag && (token as StartTagToken).tagName == "head") {
                        val head = createHtmlElement(token)

                        headElementPointer = head
                        switchTo(InsertionMode.inHead)
                    } else if (token.type == TokenType.EndTag && listOf(
                            "head",
                            "body",
                            "html",
                            "br"
                        ).contains((token as EndTagToken).tagName)
                    ) {
                        unhandledMode(InsertionMode.beforeHead, token)
                    } else if (token.type == TokenType.EndTag) {
                        unhandledMode(InsertionMode.beforeHead, token)
                    } else {
                        val fakeHead = StartTagToken("head")
                        val head = createHtmlElement(fakeHead)

                        headElementPointer = head
                        switchTo(InsertionMode.inHead)

                        //    Reprocess the current token.
                        tokens.add(0, token)
                    }
                }
                InsertionMode.inHead -> {
                    //TODO : lots of if cases
                    //FIXME: if we stumble across a start tag of type <script>, we should change the state of the tokenizer
                    //In other words, we need to wrap the tokenizer inside the parser, process each token as they appear
                    //and be able to manipulate the tokenizer during parsing. Hm...

                    if (token.type == TokenType.Character && isWhitespace(((token as CharacterToken).data))) {
                        insertCharacter(token)
                    } else if (token.type == TokenType.StartTag && (token as StartTagToken).tagName == "title") {
                        RCDATAparsing(token)
                    } else if (token.type == TokenType.EndTag && (token as EndTagToken).tagName == "head") {
                        openElements.removeLast()
                        switchTo(InsertionMode.afterHead)
                    } else {
                        //Pop the current node (which will be the head element) off the stack of open elements.
                        openElements.removeLast()

                        switchTo(InsertionMode.afterHead)

                        //Reprocess the token.
                        tokens.add(0, token)
                    }
                }
                InsertionMode.inHeadNoscript -> {
                    unhandledMode(InsertionMode.inHeadNoscript, token)
                }
                InsertionMode.afterHead -> {
                    if (token.type == TokenType.Character && isWhitespace((token as CharacterToken).data)) {
                        insertCharacter(token)
                    } else if (token.type == TokenType.StartTag && (token as StartTagToken).tagName == "body") {
                        createHtmlElement(token)
                        //Set the Document's awaiting parser-inserted body flag to false.

                        framsetOk = false
                        switchTo(InsertionMode.inBody)
                        //TODO: other cases
                    } else {
                        unhandledMode(InsertionMode.afterHead, token)
                    }
                }
                InsertionMode.inBody -> {
                    if (token.type == TokenType.Character && isWhitespace((token as CharacterToken).data)) {
                        //FIXME: reconstruct the active formatting elements
                        insertCharacter(token)
                    } else if (token.type == TokenType.Character) {
                        //FIXME: reconstruct the active formatting elements
                        insertCharacter((token as CharacterToken))
                        framsetOk = false
                    } else if (token.type == TokenType.EndTag && (token as EndTagToken).tagName == "body") {
                        //Check stack and look for parse errors
                        switchTo(InsertionMode.afterBody)
                        //TODO: other cases
                    } else if (token.type == TokenType.EndTag && (token as EndTagToken).tagName == "html") {
                        //Check stack and look for parse errors
                        switchTo(InsertionMode.afterBody)
                        //TODO: other cases
                    } else if (token.type == TokenType.StartTag && listOf(
                            "address",
                            "article",
                            "aside",
                            "blockquote",
                            "center",
                            "details",
                            "dialog",
                            "dir",
                            "div",
                            "dl",
                            "fieldset",
                            "figcaption",
                            "figure",
                            "footer",
                            "header",
                            "hgroup",
                            "main",
                            "menu",
                            "nav",
                            "ol",
                            "p",
                            "section",
                            "summary",
                            "ul"
                        ).contains((token as StartTagToken).tagName)
                    ) {
//                        if(openElementsHasAPElementInButtonScope()) {
//                            closePElement()
//                        }
                        createHtmlElement(token)
                    } else if (token.type == TokenType.StartTag && listOf(
                            "h1",
                            "h2",
                            "h3",
                            "h4",
                            "h5",
                            "h6"
                        ).contains((token as StartTagToken).tagName)
                    ) {
                        //                        if(openElementsHasAPElementInButtonScope()) {
//                            closePElement()
//                        }

                        //TODO: if current node is another header, that's a parse error
                        createHtmlElement(token)

                    } else if (token.type == TokenType.EndTag && listOf(
                            "address",
                            "article",
                            "aside",
                            "blockquote",
                            "button",
                            "center",
                            "details",
                            "dialog",
                            "dir",
                            "div",
                            "dl",
                            "fieldset",
                            "figcaption",
                            "figure",
                            "footer",
                            "header",
                            "hgroup",
                            "listing",
                            "main",
                            "menu",
                            "nav",
                            "ol",
                            "pre",
                            "section",
                            "summary",
                            "ul"
                        ).contains((token as EndTagToken).tagName)
                    ) {
                        //More stuff

                        //FIXME: find matching tag before popping
                        openElements.removeLast()
                    } else if (token.type == TokenType.EndTag && listOf(
                            "h1",
                            "h2",
                            "h3",
                            "h4",
                            "h5",
                            "h6"
                        ).contains((token as EndTagToken).tagName)
                    ) {
                        //FIXME: only slime
                        openElements.removeLast()
                    } else {
                        unhandledMode(InsertionMode.inBody, token)
                    }
                }
                InsertionMode.text -> {
                    if (token.type == TokenType.Character) {
                        insertCharacter((token as CharacterToken))
                    } else if (token.type == TokenType.EndTag) {
                        openElements.removeLast()
                        switchTo(originalInsertionMode!!)
                    } else {
                        unhandledMode(InsertionMode.text, token)
                    }
                }
                InsertionMode.inTable -> {
                    unhandledMode(InsertionMode.inTable, token)
                }
                InsertionMode.inTableText -> {
                    unhandledMode(InsertionMode.inTableText, token)
                }
                InsertionMode.inCaption -> {
                    unhandledMode(InsertionMode.inCaption, token)
                }
                InsertionMode.inColumnGroup -> {
                    unhandledMode(InsertionMode.inColumnGroup, token)
                }
                InsertionMode.inTableBody -> {
                    unhandledMode(InsertionMode.inTableBody, token)
                }
                InsertionMode.inRow -> {
                    unhandledMode(InsertionMode.inRow, token)
                }
                InsertionMode.inCell -> {
                    unhandledMode(InsertionMode.inCell, token)
                }
                InsertionMode.inSelect -> {
                    unhandledMode(InsertionMode.inSelect, token)
                }
                InsertionMode.inSelectInTable -> {
                    unhandledMode(InsertionMode.inSelectInTable, token)
                }
                InsertionMode.inTemplate -> {
                    unhandledMode(InsertionMode.inTemplate, token)
                }
                InsertionMode.afterBody -> {
                    if (token.type == TokenType.Character && isWhitespace((token as CharacterToken).data)) {
                        //TODO: same rules as in body
                        //FIXME: reconstruct the active formatting elements
                        insertCharacter(token)
                    } else if (token.type == TokenType.EndTag && "html" == (token as EndTagToken).tagName) {
                        //Check  for parse errors
                        switchTo(InsertionMode.afterAfterBody)
                        //TODO: other cases
                    } else {
                        unhandledMode(InsertionMode.afterBody, token)
                    }
                }
                InsertionMode.inFrameset -> {
                    unhandledMode(InsertionMode.inFrameset, token)
                }
                InsertionMode.afterFrameset -> {
                    unhandledMode(InsertionMode.afterFrameset, token)
                }
                InsertionMode.afterAfterBody -> {
                    if (token.type == TokenType.Character && isWhitespace((token as CharacterToken).data)) {
                        //TODO: same rules as in body
                        //FIXME: reconstruct the active formatting elements
                        insertCharacter(token)
                    } else if (token.type == TokenType.EndOfFile) {
                        //Stop parsing
                    } else {
                        unhandledMode(InsertionMode.afterAfterBody, token)
                    }
                }
                InsertionMode.afterAfterFrameset -> {
                    unhandledMode(InsertionMode.afterAfterFrameset, token)
                }
            }
        }
        return root
    }

    private fun insertCharacter(characterToken: CharacterToken) {
        val data = characterToken.data
        val adjustedInsertionLocation = findAppropriatePlaceForInsertingANode()
        //FIXME: if this is a document, return
        //FIXME: if a text node exists, append

        val existingTextNode: Text? = findTextNodeImmediatelyBefore(adjustedInsertionLocation)

        if (existingTextNode != null) {
            existingTextNode.data += data.toString()
        } else {
            val textNode = TextImpl(data.toString())
            adjustedInsertionLocation.appendChild(textNode)
        }
    }

    private fun findTextNodeImmediatelyBefore(adjustedInsertionLocation: Node): Text? {
        if (adjustedInsertionLocation.childNodes.length > 0) {
            val node = adjustedInsertionLocation.childNodes.item(adjustedInsertionLocation.childNodes.length - 1)
            if(node != null) {
                if(node.nodeName == "#text") {
                    return (node as Text)
                }
            }
        }
        return null
    }

    private fun RCDATAparsing(token: StartTagToken) {
        val element = createHtmlElement(token)
        //FIXME: switch tokenizer mode
        originalInsertionMode = currentMode
        switchTo(InsertionMode.text)
    }

    private fun createHtmlElement(token: StartTagToken): Element {
        val element = createElementFromTagName(token.tagName)

        val adjustedInsertionLocation = findAppropriatePlaceForInsertingANode()
        adjustedInsertionLocation.appendChild(element)
        openElements.addLast(element)
        return element
    }

    private fun createElementFromTagName(tagName: String): HTMLElement {
        if (tagName == "head") {
            return HTMLHeadElementImpl()
        } else if (tagName == "title") {
            return HTMLTitleElementImpl()
        } else if (tagName == "body") {
            return HTMLBodyElementImpl()
        } else if (tagName == "div") {
            return HtmlDivElementImpl()
        }

        println("Didn't find more specific implementation for $tagName")
        return HTMLElementImpl(tagName = tagName)
    }

    private fun findAppropriatePlaceForInsertingANode(): Node {
        var adjustedInsertionLocation: Node? = null

        //if override
        val target = openElements.last()
        if (fosterParenting) {
            TODO()
        } else {
            adjustedInsertionLocation = target
        }

        //if template
        return adjustedInsertionLocation!!
    }

    private fun switchTo(mode: InsertionMode) {
        this.currentMode = mode
    }

    private fun unhandledMode(mode: InsertionMode, token: Token) {
        println("Unhandled mode $mode for $token")
    }

}