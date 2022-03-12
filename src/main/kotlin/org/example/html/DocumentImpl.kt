package org.example.html

import org.w3c.dom.*

class DocumentImpl(val parserCannotChangeTheMode: Boolean = false) : Document, NodeImpl(
    nodeName = "#document"
) {
    //FIXME: should not exist when Document is created. Though then I need to deal with nullable fields :(
    private var doctype: DocumentType = DocumentTypeImpl("FIXME", "FIXME", "FIXME")

    override fun getDoctype(): DocumentType {
        return doctype
    }

    fun setDoctype(docType: DocumentType) {
        this.doctype = docType
    }

    override fun getImplementation(): DOMImplementation {
        TODO("Not yet implemented")
    }

    override fun getDocumentElement(): Element {
        TODO("Not yet implemented")
    }

    override fun createElement(tagName: String?): Element {
        TODO("Not yet implemented")
    }

    override fun createDocumentFragment(): DocumentFragment {
        TODO("Not yet implemented")
    }

    override fun createTextNode(data: String?): Text {
        TODO("Not yet implemented")
    }

    override fun createComment(data: String?): Comment {
        TODO("Not yet implemented")
    }

    override fun createCDATASection(data: String?): CDATASection {
        TODO("Not yet implemented")
    }

    override fun createProcessingInstruction(target: String?, data: String?): ProcessingInstruction {
        TODO("Not yet implemented")
    }

    override fun createAttribute(name: String?): Attr {
        TODO("Not yet implemented")
    }

    override fun createEntityReference(name: String?): EntityReference {
        TODO("Not yet implemented")
    }

    override fun getElementsByTagName(tagname: String?): NodeList {
        TODO("Not yet implemented")
    }

    override fun importNode(importedNode: Node?, deep: Boolean): Node {
        TODO("Not yet implemented")
    }

    override fun createElementNS(namespaceURI: String?, qualifiedName: String?): Element {
        TODO("Not yet implemented")
    }

    override fun createAttributeNS(namespaceURI: String?, qualifiedName: String?): Attr {
        TODO("Not yet implemented")
    }

    override fun getElementsByTagNameNS(namespaceURI: String?, localName: String?): NodeList {
        TODO("Not yet implemented")
    }

    override fun getElementById(elementId: String?): Element {
        TODO("Not yet implemented")
    }

    override fun getInputEncoding(): String {
        TODO("Not yet implemented")
    }

    override fun getXmlEncoding(): String {
        TODO("Not yet implemented")
    }

    override fun getXmlStandalone(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setXmlStandalone(xmlStandalone: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getXmlVersion(): String {
        TODO("Not yet implemented")
    }

    override fun setXmlVersion(xmlVersion: String?) {
        TODO("Not yet implemented")
    }

    override fun getStrictErrorChecking(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setStrictErrorChecking(strictErrorChecking: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getDocumentURI(): String {
        TODO("Not yet implemented")
    }

    override fun setDocumentURI(documentURI: String?) {
        TODO("Not yet implemented")
    }

    override fun adoptNode(source: Node?): Node {
        TODO("Not yet implemented")
    }

    override fun getDomConfig(): DOMConfiguration {
        TODO("Not yet implemented")
    }

    override fun normalizeDocument() {
        TODO("Not yet implemented")
    }

    override fun renameNode(n: Node?, namespaceURI: String?, qualifiedName: String?): Node {
        TODO("Not yet implemented")
    }
}
