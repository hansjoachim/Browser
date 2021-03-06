package org.example.parsing

import org.example.html.*
import org.example.parsing.Tokenizer.Companion.NULL_CHARACTER
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.html.HTMLElement
import org.w3c.dom.html.HTMLHeadElement
import java.lang.Character.isWhitespace

class Parser(document: String) {

    private val activeFormattingElements = mutableListOf<String>()
    private val templateInsertionModes = mutableListOf<InsertionMode>()
    private val tokenizer = Tokenizer(document)

    private val scripting: Boolean = false
    private var framesetOk: Boolean = true
    private val fosterParenting = false
    private val createdAsPartOfTheHTMLFragmentParsingAlgorithm = false

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
                    if (token is CharacterToken && token.isWhitespace()) {
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
                        parseError(InsertionMode.beforeHtml, token)
                        //Ignore
                    } else if (token is CommentToken) {
                        val comment = CommentImpl(token.data)
                        root.appendChild(comment)
                    } else if (token is CharacterToken && token.isWhitespace()) {
                        //ignore
                    } else if (token is StartTagToken && token.tagName == "html") {
                        val element = createElementFrom(token, intendedParent = root)
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
                        BeforeHTMLAnythingElse(root, token)
                    } else if (token is EndTagToken) {
                        parseError(InsertionMode.beforeHtml, token)
                        //Ignore
                    } else {
                        BeforeHTMLAnythingElse(root, token)
                    }
                }
                InsertionMode.beforeHead -> {
                    if (token is CharacterToken && token.isWhitespace()) {
                        //ignore
                    } else if (token is CommentToken) {
                        insertComment(token)
                    } else if (token is DOCTYPEToken) {
                        parseError(InsertionMode.beforeHead, token)
                        //Ignore
                    } else if (token is StartTagToken && token.tagName == "html") {
                        //FIXME: like in body
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
                        BeforeHeadAnythingElse(token)
                    } else if (token is EndTagToken) {
                        parseError(InsertionMode.beforeHead, token)
                        //Ignore
                    } else {
                        BeforeHeadAnythingElse(token)
                    }
                }
                InsertionMode.inHead -> {
                    if (token is CharacterToken && token.isWhitespace()) {
                        insertCharacter(token)
                    } else if (token is CommentToken) {
                        insertComment(token)
                    } else if (token is DOCTYPEToken) {
                        parseError(InsertionMode.inHead, token)
                        //Ignore
                    } else if (token is StartTagToken && token.tagName == "html") {
                        //FIXME: like in body
                        unhandledMode(InsertionMode.inHead, token)
                    } else if (token is StartTagToken && listOf(
                            "base",
                            "basefont",
                            "bgsound",
                            "link"
                        ).contains(token.tagName)
                    ) {
                        inHeadHandleBaseBasefontBgsoundLink(token)
                    } else if (token is StartTagToken && token.tagName == "meta") {
                        InHeadMetaStartTag(token)
                    } else if (token is StartTagToken && token.tagName == "title") {
                        InHeadStartTagTitle(token)
                    } else if ((token is StartTagToken && token.tagName == "noscript" && scripting)) {
                        InHeadNoscriptAndScriptingFlag(token)
                    } else if ((token is StartTagToken && listOf(
                            "noframes",
                            "style"
                        ).contains(token.tagName))
                    ) {
                        InHeadNoframesOrStyles(token)
                    } else if (token is StartTagToken && token.tagName == "noscript" && !scripting) {
                        insertHtmlElement(token)
                        switchTo(InsertionMode.inHeadNoscript)
                    } else if (token is StartTagToken && token.tagName == "script") {
                        handleScriptTagStartTag(token)
                    } else if (token is EndTagToken && token.tagName == "head") {
                        openElements.removeLast()
                        switchTo(InsertionMode.afterHead)
                    } else if (token is EndTagToken && listOf("body", "html", "br").contains(token.tagName)) {
                        unhandledMode(InsertionMode.inHead, token)
                    } else if (token is StartTagToken && token.tagName == "template") {
                        InHeadStartTagTemplate(token)
                    } else if (token is EndTagToken && token.tagName == "template") {
                        InHeadEndTagTemplate(token)
                    } else if ((token is StartTagToken && token.tagName == "head")
                        || token is EndTagToken
                    ) {
                        parseError(InsertionMode.inHead, token)
                        //Ignore
                    } else {
                        InHeadAnythingElse(token)
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
                    if (token is CharacterToken && token.isWhitespace()) {
                        insertCharacter(token)
                    } else if (token is CommentToken) {
                        insertComment(token)
                    } else if (token is DOCTYPEToken) {
                        parseError(InsertionMode.afterHead, token)
                        //Ignore
                    } else if (token is StartTagToken && token.tagName == "html") {
                        //FIXME: like in body
                        unhandledMode(InsertionMode.afterHead, token)
                    } else if (token is StartTagToken && token.tagName == "body") {
                        insertHtmlElement(token)
                        //Set the Document's awaiting parser-inserted body flag to false.

                        framesetOk = false
                        switchTo(InsertionMode.inBody)
                    } else if (token is StartTagToken && token.tagName == "frameset") {
                        insertHtmlElement(token)
                        switchTo(InsertionMode.inFrameset)
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
                        unhandledMode(InsertionMode.afterHead, token)
                    } else if (token is EndTagToken && token.tagName == "template") {
                        unhandledMode(InsertionMode.afterHead, token)
                    } else if (token is EndTagToken && listOf("body", "html", "br").contains(token.tagName)) {
                        unhandledMode(InsertionMode.afterHead, token)
                    } else if ((token is StartTagToken && token.tagName == "head")
                        || token is EndTagToken
                    ) {
                        parseError(InsertionMode.afterHead, token)
                        //Ignore
                    } else {
                        val fakeBody = StartTagToken("body")
                        insertHtmlElement(fakeBody)
                        switchTo(InsertionMode.inBody)

                        reprocessCurrentToken(token)
                    }
                }
                InsertionMode.inBody -> {
                    if (token is CharacterToken && token.data == NULL_CHARACTER) {
                        InBodyNullCharacter(token)
                    } else if (token is CharacterToken && token.isWhitespace()) {
                        InBodyWhitespaceCharacter(token)
                    } else if (token is CharacterToken) {
                        InBodyAnyOtherCharacter(token)
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
                        //Process like in head, but separate cases
                        if (listOf("base", "basefont", "bgsound", "link").contains(token.tagName)) {
                            inHeadHandleBaseBasefontBgsoundLink(token)
                        } else if (token.tagName == "meta") {
                            InHeadMetaStartTag(token)
                        } else if (listOf("noframes", "style").contains(token.tagName)) {
                            InHeadNoframesOrStyles(token)
                        } else if (token.tagName == "script") {
                            handleScriptTagStartTag(token)
                        } else if (token.tagName == "title") {
                            InHeadStartTagTitle(token)
                        } else if (token.tagName == "template") {
                            InHeadStartTagTemplate(token)
                        }
                    } else if (token is EndTagToken && token.tagName == "template") {
                        InHeadEndTagTemplate(token)
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
                    if (token is CharacterToken
                        || token is CommentToken
                        || token is DOCTYPEToken
                    ) {
                        if (token is CharacterToken && token.data == NULL_CHARACTER) {
                            InBodyNullCharacter(token)
                        } else if (token is CharacterToken && token.isWhitespace()) {
                            InBodyWhitespaceCharacter(token)
                        } else if (token is CharacterToken) {
                            InBodyAnyOtherCharacter(token)
                        } else {
                            unhandledMode(InsertionMode.inTemplate, token)
                        }
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
                        if (listOf(
                                "base",
                                "basefont",
                                "bgsound",
                                "link"
                            ).contains(token.tagName)
                        ) {
                            inHeadHandleBaseBasefontBgsoundLink(token)
                        } else if (token.tagName == "meta") {
                            InHeadMetaStartTag(token)
                        } else if (listOf(
                                "noframes",
                                "style"
                            ).contains(token.tagName)
                        ) {
                            InHeadNoframesOrStyles(token)
                        } else if (token.tagName == "script") {
                            handleScriptTagStartTag(token)
                        } else if (token.tagName == "title") {
                            InHeadStartTagTitle(token)
                        } else if (token.tagName == "template") {
                            InHeadStartTagTitle(token)
                        }
                    } else if (token is EndTagToken && token.tagName == "template") {
                        InHeadEndTagTemplate(token)
                    } else if (token is StartTagToken && listOf(
                            "caption",
                            "colgroup",
                            "tbody",
                            "tfoot",
                            "thead"
                        ).contains(token.tagName)
                    ) {
                        templateInsertionModes.removeLast()

                        templateInsertionModes.add(InsertionMode.inTable)

                        switchTo(InsertionMode.inTable)
                        reprocessCurrentToken(token)
                    } else if (token is StartTagToken && token.tagName == "col") {
                        templateInsertionModes.removeLast()

                        templateInsertionModes.add(InsertionMode.inColumnGroup)

                        switchTo(InsertionMode.inColumnGroup)
                        reprocessCurrentToken(token)
                    } else if (token is StartTagToken && token.tagName == "tr") {
                        templateInsertionModes.removeLast()

                        templateInsertionModes.add(InsertionMode.inTableBody)

                        switchTo(InsertionMode.inTableBody)
                        reprocessCurrentToken(token)
                    } else if (token is StartTagToken && listOf("td", "th").contains(token.tagName)) {
                        templateInsertionModes.removeLast()

                        templateInsertionModes.add(InsertionMode.inRow)

                        switchTo(InsertionMode.inRow)
                        reprocessCurrentToken(token)
                    } else if (token is StartTagToken) {
                        templateInsertionModes.removeLast()

                        templateInsertionModes.add(InsertionMode.inBody)

                        switchTo(InsertionMode.inBody)
                        reprocessCurrentToken(token)
                    } else if (token is EndTagToken) {
                        parseError(InsertionMode.inTemplate, token)
                    } else {
                        unhandledMode(InsertionMode.inTemplate, token)
                    }
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
                    if (token is CharacterToken && token.isWhitespace()) {
                        insertCharacter(token)
                    } else if (token is CommentToken) {
                        insertComment(token)
                    } else if (token is DOCTYPEToken) {
                        parseError(InsertionMode.inFrameset, token)
                        //Ignore
                    } else if (token is StartTagToken && token.tagName == "html") {
                        unhandledMode(InsertionMode.inFrameset, token)
                    } else if (token is StartTagToken && token.tagName == "frameset") {
                        insertHtmlElement(token)
                    } else if (token is EndTagToken && token.tagName == "frameset") {
                        if (currentNodeIsTheRootHtmlElement()) {
                            parseError(InsertionMode.inFrameset, token)
                            //Ignore
                        } else {
                            openElements.removeLast()

                            val currentNode = openElements.last()
                            if (!createdAsPartOfTheHTMLFragmentParsingAlgorithm
                                && (currentNode as HTMLElement).tagName != "frameset"
                            ) {
                                switchTo(InsertionMode.afterFrameset)
                            }
                        }
                    } else if (token is StartTagToken && token.tagName == "frame") {
                        insertHtmlElement(token)
                        openElements.removeLast()

                        //FIXME Acknowledge the token's self-closing flag, if it is set.
                    } else if (token is StartTagToken && token.tagName == "noframes") {
                        InHeadNoframesOrStyles(token)
                    } else if (token is EndOfFileToken) {
                        if (currentNodeIsTheRootHtmlElement()) {
                            parseError(InsertionMode.inFrameset, token)
                        }
                        return stopParsing(root)
                    } else {
                        parseError(InsertionMode.inFrameset, token)
                        //Ignore
                    }
                }
                InsertionMode.afterFrameset -> {
                    if (token is CharacterToken && token.isWhitespace()) {
                        insertCharacter(token)
                    } else if (token is CommentToken) {
                        insertComment(token)
                    } else if (token is DOCTYPEToken) {
                        parseError(InsertionMode.afterFrameset, token)
                        //Ignore
                    } else if (token is StartTagToken && token.tagName == "html") {
                        unhandledMode(InsertionMode.afterFrameset, token)
                    } else if (token is EndTagToken && token.tagName == "html") {
                        switchTo(InsertionMode.afterAfterFrameset)
                    } else if (token is StartTagToken && token.tagName == "noframes") {
                        InHeadNoframesOrStyles(token)
                    } else if (token is EndTagToken) {
                        return stopParsing(root)
                    } else {
                        parseError(InsertionMode.afterFrameset, token)
                        //Ignore
                    }
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
                    if (token is CharacterToken && token.isWhitespace()) {
                        InBodyWhitespaceCharacter(token)
                    } else if (token is EndOfFileToken) {
                        return stopParsing(root)
                    } else {
                        unhandledMode(InsertionMode.afterAfterFrameset, token)
                    }
                }
            }
        } while (token !is EndOfFileToken)

        println("Stopped parsing due to end of file, should probably be handled by some mode??")
        return root
    }

    private fun InHeadStartTagTemplate(token: StartTagToken) {
        insertHtmlElement(token)

        val marker = "template"
        activeFormattingElements.add(marker)

        framesetOk = false

        switchTo(InsertionMode.inTemplate)
        templateInsertionModes.add(InsertionMode.inTemplate)
    }

    private fun InHeadStartTagTitle(token: StartTagToken) {
        genericRCDATAparsing(token)
    }

    private fun currentNodeIsTheRootHtmlElement(): Boolean {
        val currentNode = openElements.last()
        //TODO: check if it is the _root_ html element
        return currentNode is HTMLElement && currentNode.tagName == "html"
    }

    private fun InHeadNoscriptAndScriptingFlag(token: StartTagToken) {
        genericRawTextElementParsing(token)
    }

    private fun InHeadNoframesOrStyles(token: StartTagToken) {
        genericRawTextElementParsing(token)
    }

    private fun InBodyAnyOtherCharacter(token: CharacterToken) {
        reconstructTheActiveFormattingElements()
        insertCharacter(token)
        framesetOk = false
    }

    private fun InBodyWhitespaceCharacter(token: CharacterToken) {
        reconstructTheActiveFormattingElements()
        insertCharacter(token)
    }

    private fun InBodyNullCharacter(token: CharacterToken) {
        parseError(InsertionMode.inBody, token)
        //Ignore
    }

    private fun InHeadEndTagTemplate(token: Token) {
        if (!openElements.any { it is HTMLElement && it.tagName == "template" }) {
            parseError(InsertionMode.inHead, token)
            //Ignore
        } else {
            generateAllImpliedEndTagsThoroughly()

            val currentNode = openElements.last() as HTMLElement
            if (currentNode.tagName != "template") {
                parseError(InsertionMode.inHead, token)
            }

            popElementsFromStackOfOpenElementsUntilAElementHasBeenPopped("template")

            clearListOfActiveFormattingElementsUpToTheLastMarker()

            templateInsertionModes.removeLast()

            resetInsertionModeAppropriately()
        }
    }

    private fun resetInsertionModeAppropriately() {
        //FIXME implement loop with more steps/branches
        var last = false
        var node = openElements.last()

        if (node == openElements.first()) {
            last = true
        }

        if (node is HTMLHeadElement && !last) {
            switchTo(InsertionMode.inHead)
            return
        }

        val name = when (node) {
            is HTMLElement -> node.tagName
            else -> {
                "Unknown"
            }
        }
        println("Could not reset insertion mode for $name")
    }

    private fun clearListOfActiveFormattingElementsUpToTheLastMarker() {
        var entry = activeFormattingElements.last()

        activeFormattingElements.removeLast()

        //FIXME: if entry is a marker, finished, otherwise loop again
        // Need to look more into what I actually should (or can) insert in the list
    }

    private fun popElementsFromStackOfOpenElementsUntilAElementHasBeenPopped(targetTagName: String) {
        var lastPopped: Node? = null

        do {
            lastPopped = openElements.removeLast()
        } while (!(lastPopped is HTMLElement && lastPopped.tagName == targetTagName))
    }

    private fun generateAllImpliedEndTagsThoroughly() {
        //FIXME: implement
    }

    private fun InHeadAnythingElse(token: Token) {
        //Pop the current node (which will be the head element) off the stack of open elements.
        openElements.removeLast()

        switchTo(InsertionMode.afterHead)

        reprocessCurrentToken(token)
    }

    private fun InHeadMetaStartTag(token: StartTagToken) {
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
    }

    private fun inHeadHandleBaseBasefontBgsoundLink(token: StartTagToken) {
        insertHtmlElement(token)
        openElements.removeLast()

        //FIXME Acknowledge the token's self-closing flag, if it is set.
    }

    private fun BeforeHeadAnythingElse(token: Token) {
        val fakeHead = StartTagToken("head")
        val head = insertHtmlElement(fakeHead)

        headElementPointer = head
        switchTo(InsertionMode.inHead)

        reprocessCurrentToken(token)
    }

    private fun BeforeHTMLAnythingElse(document: DocumentImpl, token: Token) {
        // FIXME: Create an html element whose node document is the Document object.
        val element = createElementFromTagName("html")
        document.appendChild(element)
        openElements.addLast(element)

        switchTo(InsertionMode.beforeHead)
        reprocessCurrentToken(token)
    }

    private fun parseError(mode: InsertionMode, token: Token) {
        println("Parse error in $mode , encountered $token")
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

    private fun createElementFrom(token: StartTagToken, intendedParent: DocumentImpl? = null): HTMLElement {
        //FIXME: mark parent element in the created element
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
            "caption" -> {
                return HTMLTableCaptionElementImpl()
            }
            "col", "colgroup" -> {
                return HTMLTableColElementImpl(tagName)
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
            "optgroup" -> {
                return HTMLOptGroupElementImpl()
            }
            "option" -> {
                return HTMLOptionElementImpl()
            }
            "p" -> {
                return HTMLParagraphElementImpl()
            }
            "select" -> {
                return HTMLSelectElementImpl()
            }
            "script" -> {
                return HTMLScriptElementImpl()
            }
            "style" -> {
                return HTMLStyleElementImpl()
            }
            "table" -> {
                return HTMLTableElementImpl()
            }
            "tbody", "thead", "tfoot" -> {
                return HTMLTableSectionElementImpl(tagName)
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