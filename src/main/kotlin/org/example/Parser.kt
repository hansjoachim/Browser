package org.example

import java.lang.Character.isWhitespace

class Parser {

    private var framsetOk: Boolean = true
    private val fosterParenting = false

    private var currentMode: InsertionMode = InsertionMode.initial

    private var headElementPointer: Element? = null
    private val openElements: ArrayDeque<Node> = ArrayDeque()


    fun parse(document: String): Document {
        return parse(Tokenizer(document).tokenize())
    }

    fun parse(originalTokens: List<Token>): Document {
        //A Document object has an associated parser cannot change the mode flag (a boolean). It is initially false.
        val root = Document()
        val tokens: MutableList<Token> = originalTokens.toMutableList()

        while (!tokens.isEmpty()) {
            val token = tokens.removeFirst()
            when (currentMode) {
                InsertionMode.initial -> {
                    if (token.type == TokenType.DOCTYPE) {
                        val doctype = (token as DOCTYPEToken)
                        if ("html".equals(doctype.name)) {
                            val documentType =
                                DocumentType(doctype.name, doctype.publicIdentifier, doctype.systemIdentifier)
                            root.children.add(documentType)

                            //deal with doctypes, various types and quirks...

                            switchTo(InsertionMode.beforeHtml)
                        }
                    } else {
                        unhandledMode(InsertionMode.initial, token)
                    }
                }
                InsertionMode.beforeHtml -> {
                    if (token.type == TokenType.StartTag) {
                        val startTag = (token as StartTagToken)

                        if ("html".equals(startTag.tagName)) {
                            val e = Element(tagName = startTag.tagName)
                            root.children.add(e)
                            openElements.addLast(e)
                            switchTo(InsertionMode.beforeHead)
                        } else {
                            unhandledMode(InsertionMode.beforeHtml, token)
                        }

                    } else {
                        unhandledMode(InsertionMode.beforeHtml, token)
                    }
                }
                InsertionMode.beforeHead -> {
                    if (token.type == TokenType.Character && isWhitespace((token as CharacterToken).data)) {
                        unhandledMode(InsertionMode.beforeHead, token)
                    } else if (token.type == TokenType.Comment) {
                        unhandledMode(InsertionMode.beforeHead, token)
                    } else if (token.type == TokenType.DOCTYPE) {
                        unhandledMode(InsertionMode.beforeHead, token)
                    } else if (token.type == TokenType.StartTag && "html".equals((token as StartTagToken).tagName)) {
                        unhandledMode(InsertionMode.beforeHead, token)
                    } else if (token.type == TokenType.StartTag && "head".equals((token as StartTagToken).tagName)) {
                        unhandledMode(InsertionMode.beforeHead, token)
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
                    //else
                    //Pop the current node (which will be the head element) off the stack of open elements.
                    openElements.removeLast()

                    switchTo(InsertionMode.afterHead)

                    //Reprocess the token.
                    tokens.add(0, token)
                }
                InsertionMode.inHeadNoscript -> {
                    unhandledMode(InsertionMode.inHeadNoscript, token)
                }
                InsertionMode.afterHead -> {
                    if (token.type == TokenType.StartTag && "body" == (token as StartTagToken).tagName) {
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
                    if (token.type == TokenType.EndTag && "body" == (token as EndTagToken).tagName) {
                        //Check stack and look for parse errors
                        switchTo(InsertionMode.afterBody)
                        //TODO: other cases
                    } else if (token.type == TokenType.EndTag && "html" == (token as EndTagToken).tagName) {
                        //Check stack and look for parse errors
                        switchTo(InsertionMode.afterBody)
                        //TODO: other cases
                    } else {
                        unhandledMode(InsertionMode.inBody, token)
                    }
                }
                InsertionMode.text -> {
                    unhandledMode(InsertionMode.text, token)
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
                    if (token.type == TokenType.EndTag && "html" == (token as EndTagToken).tagName) {
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
                    if (token.type == TokenType.EndOfFile) {
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

    private fun createHtmlElement(token: StartTagToken): Element {
        val head = Element(tagName = token.tagName)

        val adjustedInsertionLocation = findAppropriatePlaceForInsertingANode()
        adjustedInsertionLocation.appendChild(head)
        openElements.addLast(head)
        return head
    }

    private fun findAppropriatePlaceForInsertingANode(): Node {
        val adjustedInsertionLocation = openElements.last()
        if (fosterParenting) {
            TODO()
        }
        return adjustedInsertionLocation
    }

    private fun switchTo(mode: InsertionMode) {
        this.currentMode = mode
    }

    private fun unhandledMode(mode: InsertionMode, token: Token) {
        println("Unhandled mode $mode for $token")
    }

    //Inherit node??
    class Document(val children: MutableList<Node> = mutableListOf())

    interface EventTarget
    open class Node : EventTarget {
        val childNodes: MutableList<Node> = mutableListOf()
        fun appendChild(node: Node) {
            childNodes.add(node)
        }
    }

    class DocumentType(
        val name: DOMString,
        val publicId: DOMString,
        val systemId: DOMString
    ) : Node()

    class Element(
        val namespaceURI: DOMString? = null,
        val prefix: DOMString? = null,
        //  val localName: DOMString,
        val tagName: DOMString
    ) : Node()
}

typealias DOMString = String