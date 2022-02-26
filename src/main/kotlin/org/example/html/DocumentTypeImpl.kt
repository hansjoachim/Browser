package org.example.html

import org.w3c.dom.DocumentType
import org.w3c.dom.NamedNodeMap

class DocumentTypeImpl(
    private val name: String,
    private val publicId: String,
    private val systemId: String
) : DocumentType, NodeImpl(nodeName = name) {
    override fun getName(): String {
        return name
    }

    override fun getEntities(): NamedNodeMap {
        TODO("Not yet implemented")
    }

    override fun getNotations(): NamedNodeMap {
        TODO("Not yet implemented")
    }

    override fun getPublicId(): String {
        return publicId
    }

    override fun getSystemId(): String {
        return systemId
    }

    override fun getInternalSubset(): String {
        TODO("Not yet implemented")
    }
}
