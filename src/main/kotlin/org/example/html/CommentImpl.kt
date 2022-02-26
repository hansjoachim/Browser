package org.example.html

import org.w3c.dom.Comment

class CommentImpl(data: String) : Comment, CharacterDataImpl(nodeName = "#comment", data = data)
