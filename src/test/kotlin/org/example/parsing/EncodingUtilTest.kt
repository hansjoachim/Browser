package org.example.parsing

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class EncodingUtilTest {
    @Test
    fun find_encoding() {
        val encoding = getEncoding("utf-8")

        assertThat(encoding).isEqualTo(Encoding.UTF_8)
    }

    @Test
    fun find_encoding_case_insensitive_check() {
        val encoding = getEncoding("UtF8")

        assertThat(encoding).isEqualTo(Encoding.UTF_8)
    }

    @Test
    fun no_encoding_if_no_match_is_found() {
        val encoding = getEncoding("non-existing")

        assertThat(encoding).isNull()
    }

    @Test
    fun extract_encoding_from_meta_tag() {
        val encoding = extractCharacterEncodingFromMetaElement("text/html; charset=utf-8")

        assertThat(encoding).isEqualTo(Encoding.UTF_8)
    }

    @Test
    fun extract_encoding_from_meta_tag_quoted() {
        val encoding = extractCharacterEncodingFromMetaElement("text/html; charset='utf-8'")

        assertThat(encoding).isEqualTo(Encoding.UTF_8)
    }

    @Test
    fun extract_encoding_from_meta_tag_misquoted() {
        val encoding = extractCharacterEncodingFromMetaElement("text/html; charset='utf-8")

        assertThat(encoding).isNull()
    }
}