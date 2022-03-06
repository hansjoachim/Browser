package org.example.parsing

import org.example.html.*
import org.example.parsing.Tokenizer.Companion.NULL_CHARACTER
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.html.HTMLElement
import java.lang.Character.isWhitespace

class Parser(document: String) {

    private val templateInsertionModes = mutableListOf<InsertionMode>()
    private val tokenizer = Tokenizer(document)

    private val scripting: Boolean = false
    private var framsetOk: Boolean = true
    private val fosterParenting = false

    private var currentInsertionMode: InsertionMode = InsertionMode.initial
    private var originalInsertionMode: InsertionMode? = null

    private var headElementPointer: Element? = null
    private val openElements: ArrayDeque<Node> = ArrayDeque()

    private val activeSpeculativeHtmlParser: Parser? = null

    private val confidence: EncodingConfidence = EncodingConfidence.tentative

    fun parse(): Document {
        //A Document object has an associated parser cannot change the mode flag (a boolean). It is initially false.
        val root = DocumentImpl()

        do {
            val token = tokenizer.nextToken()
            when (currentInsertionMode) {
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
                        //TODO:
                        //if the document is NOT an iframe srcdoc, then parse error
                        //if parser cannot change the mode flag is false, then set Document to quirks mode
                        // That's all fine and nice, but where do I find these flags?

                        switchTo(InsertionMode.beforeHtml)
                        reprocessCurrentToken(token)
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
                        val element = createElementFrom(token)
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

                    if (token is CharacterToken && isWhitespace((token.data))) {
                        insertCharacter(token)
                    } else if (token is CommentToken) {
                        insertComment(token)
                    } else if (token is DOCTYPEToken) {
                        //Parse error. Ignore
                    } else if (token is StartTagToken && listOf(
                            "base",
                            "basefont",
                            "bgsound",
                            "link"
                        ).contains(token.tagName)
                    ) {
                        insertHtmlElement(token)
                        openElements.removeLast()

                        //Acknowledge the token's self-closing flag, if it is set.
                    } else if (token is StartTagToken && token.tagName == "meta") {
                        val element = insertHtmlElement(token)
                        openElements.removeLast()

                        //Acknowledge the token's self-closing flag, if it is set.

                        if (activeSpeculativeHtmlParser == null) {

                            if (element.hasAttribute("charset")) {
                                val encoding = getEncoding(element.getAttribute("charset"))
                                if (encoding != null && confidence == EncodingConfidence.tentative) {
                                    changeEncoding(encoding)
                                }

                            } else if (element.hasAttribute("http-equiv")) {
                                if (element.getAttribute("http-equiv").equals("Content-Type", ignoreCase = true)) {
                                    if (element.hasAttribute("content")) {
                                        val encoding =
                                            extractCharacterEncodingFromMetaElement(element.getAttribute("content"))
                                        if (encoding != null && confidence == EncodingConfidence.tentative) {
                                            changeEncoding(encoding)
                                        }
                                    }
                                }
                            }
                        }

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
                        handleScriptTagStartTag(token)
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
                    if (token is DOCTYPEToken) {
                        //Parse error. Ignore
                    } else if (token is StartTagToken && token.tagName == "html") {
                        //TODO process the token like in body
                        unhandledMode(InsertionMode.inHeadNoscript, token)
                    } else if (token is EndTagToken && token.tagName == "noscript") {
                        openElements.removeLast()
                        //TODO: update current node to head
                        switchTo(InsertionMode.inHead)
                    } else if (token is CharacterToken && isWhitespace(token.data)) {
                        //Like in head
                        insertCharacter(token)
                    } else if (token is CommentToken) {
                        //Like in head
                        insertComment(token)
                    } else if (token is StartTagToken && listOf(
                            "basefont",
                            "bgsound",
                            "link",
                            "meta",
                            "noframes",
                            "style"
                        ).contains(token.tagName)
                    ) {
                        //FIXME: like in head . NOTE: these tags are treated by separate cases in head!!

                        if (listOf("noframes", "style").contains(token.tagName)) {
                            // Like in head
                            genericRawTextElementParsing(token)
                        } else {
                            unhandledMode(InsertionMode.inHeadNoscript, token)
                        }
                    } else if (token is EndTagToken && token.tagName == "br") {
                        // Same as any case below
                        //Parse error
                        openElements.removeLast()
                        switchTo(InsertionMode.inHead)
                        reprocessCurrentToken(token)
                    } else if ((token is StartTagToken && listOf(
                            "head",
                            "noscript"
                        ).contains(token.tagName)) || token is EndTagToken
                    ) {
                        //Parse error. Ignore
                    } else {
                        //Parse error
                        openElements.removeLast()
                        switchTo(InsertionMode.inHead)
                        reprocessCurrentToken(token)
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
                    if (token is CharacterToken && token.data == NULL_CHARACTER) {
                        //TODO parseError without comment/type in spec
                        //Ignore
                        //FIXME: replace whitespace check
                    } else if (token is CharacterToken && isWhitespace(token.data)) {
                        reconstructTheActiveFormattingElements()
                        insertCharacter(token)
                    } else if (token is CharacterToken) {
                        reconstructTheActiveFormattingElements()
                        insertCharacter(token)
                        framsetOk = false
                    } else if (token is CommentToken) {
                        insertComment(token)
                    } else if (token is DOCTYPEToken) {
                        //TODO parseError without comment/type in spec
                        //Ignore
                    } else if (token is StartTagToken && listOf(
                            "base",
                            "basefont",
                            "bgsound",
                            "link",
                            "meta",
                            "noframes",
                            "script",
                            "style",
                            "template",
                            "title"
                        ).contains(token.tagName)
                    ) {
                        //Like in head, but separate cases

                        if (listOf("noframes", "style").contains(token.tagName)) {
                            genericRawTextElementParsing(token)
                        } else if (token.tagName == "script") {
                            handleScriptTagStartTag(token)
                        } else {
                            unhandledMode(InsertionMode.inBody, token)
                        }
                    } else if (token is EndOfFileToken) {
                        if (templateInsertionModes.isNotEmpty()) {
                            TODO("follow the rules for 'in template'")
                        }
                        //TODO: check stack for nodes, might be parse error

                        if (stackOfOpenElementsHasANodeThatIsNotEither(
                                "dd",
                                "dt",
                                "li",
                                "optgroup",
                                "option",
                                "p",
                                "rb",
                                "rp",
                                "rt",
                                "rtc",
                                "tbody",
                                "td",
                                "tfoot",
                                "th",
                                "thead",
                                "tr",
                                "body",
                                "html"
                            )
                        ) {
                            //TODO parse error
                        }

                        return stopParsing(root)
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
                        if (openElementsHasAPElementInButtonScope()) {
                            closePElement()
                        }
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
                        if (openElementsHasAPElementInButtonScope()) {
                            closePElement()
                        }

                        //TODO: if current node is another header, that's a parse error
                        insertHtmlElement(token)
                    } else if (token is StartTagToken && token.tagName == "plaintext") {
                        if (openElementsHasAPElementInButtonScope()) {
                            closePElement()
                        }
                        insertHtmlElement(token)
                        tokenizer.switchTo(TokenizationState.PLAINTEXTState)
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
                        reconstructTheActiveFormattingElements()
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
                        return stopParsing(root)
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
                        reconstructTheActiveFormattingElements()
                        insertCharacter(token)
                    } else if (token is EndOfFileToken) {
                        return stopParsing(root)
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

    private fun stackOfOpenElementsHasANodeThatIsNotEither(vararg strings: String): Boolean {
        val allowedElements = strings.asList()

        return openElements.any { !allowedElements.contains(it.nodeName) }
    }

    private fun stopParsing(document: DocumentImpl): Document {
        if (activeSpeculativeHtmlParser != null) {
            TODO("stop the active speculative parser")
        }
        //set document readiness

        while (openElements.isNotEmpty()) {
            openElements.removeLast()
        }

        //Script things

        //DOM task+++

        //Set document status
        return document
    }

    private fun handleScriptTagStartTag(startScript: StartTagToken) {
        val adjustedInsertionLocation = findAppropriatePlaceForInsertingANode()

        val element = createElementFrom(startScript)

        //Set the element's parser document to the Document, and unset the element's "non-blocking" flag.
        // But how??

        //TODO: handle other script cases

        adjustedInsertionLocation.appendChild(element)
        openElements.addLast(element)

        tokenizer.switchTo(TokenizationState.ScriptDataState)
        originalInsertionMode = currentInsertionMode
        switchTo(InsertionMode.text)
    }

    private fun reconstructTheActiveFormattingElements() {
        //FIXME: implement
    }

    private fun closePElement() {
        TODO("Not yet implemented")
    }

    private fun openElementsHasAPElementInButtonScope(): Boolean {
        //FIXME: implement check
        return false
    }

    private fun changeEncoding(encoding: Encoding) {
        println("Parser wanted to change encoding to ${encoding.encodingName} , though changing encoding is not implemented")
    }

    private fun reprocessCurrentToken(token: Token) {
        //A bit hacky but puts it back so that it is the next token returned :D
        tokenizer.reprocess(token)
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
        genericRCDATAparsing(token, TokenizationState.RAWTEXTState)
    }

    private fun genericRCDATAparsing(
        token: StartTagToken,
        switchToState: TokenizationState = TokenizationState.RCDATAState
    ) {
        insertHtmlElement(token)
        tokenizer.switchTo(switchToState)
        originalInsertionMode = currentInsertionMode
        switchTo(InsertionMode.text)
    }

    private fun insertHtmlElement(token: StartTagToken): Element {
        val element = createElementFrom(token)

        val adjustedInsertionLocation = findAppropriatePlaceForInsertingANode()
        adjustedInsertionLocation.appendChild(element)
        openElements.addLast(element)
        return element
    }

    private fun createElementFrom(token: StartTagToken): HTMLElement {
        val element = createElementFromTagName(token.tagName)

        //TODO: handle attribute changes
        token.attributes.map { element.setAttribute(it.attributeName, it.value) }
        return element
    }

    private fun createElementFromTagName(tagName: String): HTMLElement {
        when (tagName) {
            "a" -> {
                return HTMLAnchorElementImpl()
            }
            "body" -> {
                return HTMLBodyElementImpl()
            }
            "button" -> {
                return HTMLButtonElementImpl()
            }
            "div" -> {
                return HtmlDivElementImpl()
            }
            "form" -> {
                return HTMLFormElementImpl()
            }
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                return HTMLHeadingElementImpl(tagName)
            }
            "head" -> {
                return HTMLHeadElementImpl()
            }
            "hr" -> {
                return HTMLHRElementImpl()
            }
            "img" -> {
                return HTMLImageElementImpl()
            }
            "input" -> {
                return HTMLInputElementImpl()
            }
            "li" -> {
                return HTMLLIElementImpl()
            }
            "link" -> {
                return HTMLLinkElementImpl()
            }
            "meta" -> {
                return HTMLMetaElementImpl()
            }
            "ol" -> {
                return HTMLOListElementImpl()
            }
            "p" -> {
                return HTMLParagraphElementImpl()
            }
            "script" -> {
                return HTMLScriptElementImpl()
            }
            "table" -> {
                return HTMLTableElementImpl()
            }
            "td", "th" -> {
                return HTMLTableCellElementImpl(tagName)
            }
            "tr" -> {
                return HTMLTableRowElementImpl()
            }
            "title" -> {
                return HTMLTitleElementImpl()
            }
            "ul" -> {
                return HTMLUListElementImpl()
            }
            "applet", "bgsound", "blink", "isindex", "keygen", "multicol", "nextid", "spacer" -> {
                return HTMLUnknownElementImpl(tagName)
            }
            "acronym", "basefont", "big", "center", "nobr", "noembed", "noframes", "plaintext", "rb", "rtc", "strike", "tt" -> {
                return HTMLElementImpl(tagName)
            }
            "listing", "xmp" -> {
                return HTMLPreElementImpl(tagName)
            }
            else -> {
                println("Didn't find more specific implementation for $tagName")
                return HTMLUnknownElementImpl(tagName)
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
        this.currentInsertionMode = mode
    }

    private fun unhandledMode(mode: InsertionMode, token: Token) {
        println("Unhandled mode $mode for $token")
    }

}