package org.example.html

import org.w3c.dom.CharacterData

open class CharacterDataImpl(nodeName: String, private var data: String) : CharacterData,
    NodeImpl(nodeName = nodeName) {
    override fun getData(): String {
        return data
    }

    override fun setData(data: String?) {
        this.data=data!!
    }

    override fun getLength(): Int {
        TODO("Not yet implemented")
    }

    override fun substringData(offset: Int, count: Int): String {
        TODO("Not yet implemented")
    }

    override fun appendData(arg: String?) {
        TODO("Not yet implemented")
    }

    override fun insertData(offset: Int, arg: String?) {
        TODO("Not yet implemented")
    }

    override fun deleteData(offset: Int, count: Int) {
        TODO("Not yet implemented")
    }

    override fun replaceData(offset: Int, count: Int, arg: String?) {
        TODO("Not yet implemented")
    }
}