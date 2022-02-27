package org.example.parsing

import org.example.html.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.html.HTMLElement
import java.lang.Character.isWhitespace

class Parser(private val document: String) {

    private val tokenizer = Tokenizer(document)

    private val scripting: Boolean = false
    private var framsetOk: Boolean = true
    private val fosterParenting = false

    private var currentMode: InsertionMode = InsertionMode.initial
    private var originalInsertionMode: InsertionMode? = null

    private var headElementPointer: Element? = null
    private val openElements: ArrayDeque<Node> = ArrayDeque()

    fun parse(): Document {
        //A Document object has an associated parser cannot change the mode flag (a boolean). It is initially false.
        val root = DocumentImpl()



        do {
            val token = tokenizer.nextToken()
            when (currentMode) {
                InsertionMode.initial -> {
                    if (token is CharacterToken && isWhitespace(token.data)) {
                        //ignore
                    } else if (token is CommentToken) {
                        val comment = CommentImpl(token.data)
                        root.appendChild(comment)
                    } else if (token is DOCTYPEToken) {
                        //FIXME: might be parse error based on values
                        val documentType =
                            DocumentTypeImpl(token.name, token.publicIdentifier, token.systemIdentifier)
                        root.doctype = documentType

                        //deal with doctypes, various types and quirks...

                        switchTo(InsertionMode.beforeHtml)
                    } else {
                        unhandledMode(InsertionMode.initial, token)
                    }
                }
                InsertionMode.beforeHtml -> {
                    if (token is DOCTYPEToken) {
                        unhandledMode(InsertionMode.beforeHtml, token)
                    } else if (token is CommentToken) {
                        val comment = CommentImpl(token.data)
                        root.appendChild(comment)
                    } else if (token is CharacterToken && isWhitespace(token.data)) {
                        //ignore
                    } else if (token is StartTagToken && token.tagName == "html") {
                        val element = createElementFromTagName(token.tagName)
                        root.appendChild(element)
                        openElements.addLast(element)
                        switchTo(InsertionMode.beforeHead)
                    } else if (token is EndTagToken && listOf(
                            "head",
                            "body",
                            "html",
                            "br"
                        ).contains(token.tagName)
                    ) {
                        unhandledMode(InsertionMode.beforeHtml, token)
                    } else if (token is EndTagToken) {
                        unhandledMode(InsertionMode.beforeHtml, token)
                    } else {
                        val element = createElementFromTagName("html")
                        root.appendChild(element)
                        openElements.addLast(element)
                        switchTo(InsertionMode.beforeHead)
                    }
                }
                InsertionMode.beforeHead -> {
                    if (token is CharacterToken && isWhitespace(token.data)) {
                        //ignore
                    } else if (token is CommentToken) {
                        insertComment(token)
                    } else if (token is DOCTYPEToken) {
                        unhandledMode(InsertionMode.beforeHead, token)
                    } else if (token is StartTagToken && token.tagName == "html") {
                        unhandledMode(InsertionMode.beforeHead, token)
                    } else if (token is StartTagToken && token.tagName == "head") {
                        val head = insertHtmlElement(token)

                        headElementPointer = head
                        switchTo(InsertionMode.inHead)
                    } else if (token is EndTagToken && listOf(
                            "head",
                            "body",
                            "html",
                            "br"
                        ).contains(token.tagName)
                    ) {
                        unhandledMode(InsertionMode.beforeHead, token)
                    } else if (token is EndTagToken) {
                        unhandledMode(InsertionMode.beforeHead, token)
                    } else {
                        val fakeHead = StartTagToken("head")
                        val head = insertHtmlElement(fakeHead)

                        headElementPointer = head
                        switchTo(InsertionMode.inHead)

                        reprocessCurrentToken(token)
                    }
                }
                InsertionMode.inHead -> {
                    //TODO : lots of if cases
                    //FIXME: if we stumble across a start tag of type <script>, we should change the state of the tokenizer
                    //In other words, we need to wrap the tokenizer inside the parser, process each token as they appear
                    //and be able to manipulate the tokenizer during parsing. Hm...

                    if (token is CharacterToken && isWhitespace((token.data))) {
                        insertCharacter(token)
                    } else if (token is CommentToken) {
                        insertComment(token)
                    } else if (token is StartTagToken && listOf(
                            "base",
                            "basefont",
                            "bgsound",
                            "link"
                        ).contains(token.tagName)
                    ) {
                        //FIXME
                        unhandledMode(InsertionMode.inHead, token)
                    } else if (token is StartTagToken && token.tagName == "meta") {
                        //FIXME
                        unhandledMode(InsertionMode.inHead, token)
                    } else if (token is StartTagToken && token.tagName == "title") {
                        genericRCDATAparsing(token)
                    } else if ((token is StartTagToken && token.tagName == "noscript" && scripting)
                        || (token is StartTagToken && listOf(
                            "noframes",
                            "style"
                        ).contains(token.tagName))
                    ) {
                        genericRawTextElementParsing(token)
                    } else if (token is StartTagToken && token.tagName == "noscript" && !scripting) {
                        insertHtmlElement(token)
                        switchTo(InsertionMode.inHeadNoscript)
                    } else if (token is StartTagToken && token.tagName == "script") {
                        //FIXME
                        unhandledMode(InsertionMode.inHead, token)
                    } else if (token is EndTagToken && token.tagName == "head") {
                        openElements.removeLast()
                        switchTo(InsertionMode.afterHead)
                    } else {
                        //Pop the current node (which will be the head element) off the stack of open elements.
                        openElements.removeLast()

                        switchTo(InsertionMode.afterHead)

                        reprocessCurrentToken(token)
                    }
                }
                InsertionMode.inHeadNoscript -> {
                    if (token is EndTagToken && token.tagName == "noscript") {
                        openElements.removeLast()
                        //TODO: update current node to head
                        switchTo(InsertionMode.inHead)
                    } else if (token is CommentToken) {
                        //Like in head
                        insertComment(token)
                    } else {
                        unhandledMode(InsertionMode.inHeadNoscript, token)
                    }
                }
                InsertionMode.afterHead -> {
                    if (token is CharacterToken && isWhitespace(token.data)) {
                        insertCharacter(token)
                    } else if (token is CommentToken) {
                        insertComment(token)
                    } else if (token is StartTagToken && token.tagName == "body") {
                        insertHtmlElement(token)
                        //Set the Document's awaiting parser-inserted body flag to false.

                        framsetOk = false
                        switchTo(InsertionMode.inBody)
                        //TODO: other cases
                    } else {
                        unhandledMode(InsertionMode.afterHead, token)
                    }
                }
                InsertionMode.inBody -> {
                    if (token is CharacterToken && isWhitespace(token.data)) {
                        //FIXME: reconstruct the active formatting elements
                        insertCharacter(token)
                    } else if (token is CharacterToken) {
                        //FIXME: reconstruct the active formatting elements
                        insertCharacter(token)
                        framsetOk = false
                    } else if (token is CommentToken) {
                        insertComment(token)
                    } else if (token is EndTagToken && token.tagName == "body") {
                        //Check stack and look for parse errors
                        switchTo(InsertionMode.afterBody)
                        //TODO: other cases
                    } else if (token is EndTagToken && token.tagName == "html") {
                        //Check stack and look for parse errors
                        switchTo(InsertionMode.afterBody)
                        //TODO: other cases
                    } else if (token is StartTagToken && listOf(
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
                        ).contains(token.tagName)
                    ) {
//                        if(openElementsHasAPElementInButtonScope()) {
//                            closePElement()
//                        }
                        insertHtmlElement(token)
                    } else if (token is StartTagToken && listOf(
                            "h1",
                            "h2",
                            "h3",
                            "h4",
                            "h5",
                            "h6"
                        ).contains(token.tagName)
                    ) {
                        //                        if(openElementsHasAPElementInButtonScope()) {
//                            closePElement()
//                        }

                        //TODO: if current node is another header, that's a parse error
                        insertHtmlElement(token)

                    } else if (token is EndTagToken && listOf(
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
                        ).contains(token.tagName)
                    ) {
                        //More stuff

                        //FIXME: find matching tag before popping
                        openElements.removeLast()
                    } else if (token is EndTagToken && listOf(
                            "h1",
                            "h2",
                            "h3",
                            "h4",
                            "h5",
                            "h6"
                        ).contains(token.tagName)
                    ) {
                        //FIXME: only slime
                        openElements.removeLast()
                    } else if ((token is StartTagToken && token.tagName == "noembed") ||
                        (token is StartTagToken && token.tagName == "noscript") && scripting
                    ) {
                        genericRawTextElementParsing(token)
                    } else if (token is StartTagToken) {
                        //TODO: Reconstruct the active formatting elements, if any.
                        insertHtmlElement(token)
                    } else if (token is EndTagToken) {
                        //FIXME deal with the stack
                        openElements.removeLast()
                    } else {
                        unhandledMode(InsertionMode.inBody, token)
                    }
                }
                InsertionMode.text -> {
                    if (token is CharacterToken) {
                        insertCharacter(token)
                    } else if (token is EndTagToken) {
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
                    if (token is CharacterToken && isWhitespace(token.data)) {
                        //TODO: same rules as in body
                        //FIXME: reconstruct the active formatting elements
                        insertCharacter(token)
                    } else if (token is CommentToken) {
                        //Insert a comment as the last child of the first element in the stack of open elements (the html element).
                        val comment = CommentImpl(token.data)
                        openElements.first().appendChild(comment)
                    } else if (token is EndTagToken && "html" == token.tagName) {
                        //Check  for parse errors
                        switchTo(InsertionMode.afterAfterBody)
                        //TODO: other cases
                    } else if (token is EndOfFileToken) {
                        //stop parsing
                        return root
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
                    if (token is CommentToken) {
                        val comment = CommentImpl(token.data)
                        root.appendChild(comment)
                    } else if (token is CharacterToken && isWhitespace(token.data)) {
                        //TODO: same rules as in body
                        //FIXME: reconstruct the active formatting elements
                        insertCharacter(token)
                    } else if (token is EndOfFileToken) {
                        //Stop parsing
                        return root
                    } else {
                        unhandledMode(InsertionMode.afterAfterBody, token)
                    }
                }
                InsertionMode.afterAfterFrameset -> {
                    unhandledMode(InsertionMode.afterAfterFrameset, token)
                }
            }
        } while (token !is EndOfFileToken)

        println("Stopped parsing due to end of file, should probably be handled by some mode??")
        return root
    }

    private fun reprocessCurrentToken(token: Token) {
        //A bit hacky but puts it back so that it is the next token returned :D
        tokenizer.emittedToken = token
    }

    private fun insertComment(token: CommentToken) {
        val data = token.data

        //if position
        val adjustedInsertionLocation = findAppropriatePlaceForInsertingANode()

        val comment = CommentImpl(data)
        adjustedInsertionLocation.appendChild(comment)
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
            if (node is Text) {
                return node
            }
        }
        return null
    }

    private fun genericRawTextElementParsing(token: StartTagToken) {
        genericRCDATAparsing(token)
    }

    private fun genericRCDATAparsing(token: StartTagToken) {
        insertHtmlElement(token)
        //FIXME: switch tokenizer mode
        originalInsertionMode = currentMode
        switchTo(InsertionMode.text)
    }

    private fun insertHtmlElement(token: StartTagToken): Element {
        val element = createElementFromTagName(token.tagName)

        val adjustedInsertionLocation = findAppropriatePlaceForInsertingANode()
        adjustedInsertionLocation.appendChild(element)
        openElements.addLast(element)
        return element
    }

    private fun createElementFromTagName(tagName: String): HTMLElement {
        when (tagName) {
            "head" -> {
                return HTMLHeadElementImpl()
            }
            "title" -> {
                return HTMLTitleElementImpl()
            }
            "body" -> {
                return HTMLBodyElementImpl()
            }
            "div" -> {
                return HtmlDivElementImpl()
            }
            "p" -> {
                return HTMLParagraphElementImpl()
            }
            else -> {
                println("Didn't find more specific implementation for $tagName")
                return HTMLElementImpl(tagName = tagName)
            }
        }
    }

    private fun findAppropriatePlaceForInsertingANode(): Node {
        var adjustedInsertionLocation: Node?

        //if override
        val target = openElements.last()
        if (fosterParenting) {
            TODO()
        } else {
            adjustedInsertionLocation = target
        }

        //if template
        return adjustedInsertionLocation
    }

    private fun switchTo(mode: InsertionMode) {
        this.currentMode = mode
    }

    private fun unhandledMode(mode: InsertionMode, token: Token) {
        println("Unhandled mode $mode for $token")
    }

}