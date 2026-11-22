package app.multiwidgets.ai

import app.multiwidgets.ai.translation.LibreTranslate
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Le corps de requête du fournisseur cloud est conforme et sans fuite de clé. */
class CloudTranslatorTest {

    @Test fun `corps conforme au protocole LibreTranslate`() {
        val corps = JSONObject(LibreTranslate("https://exemple.test").corpsRequete("привет", "ru", "fr", ""))
        assertEquals("привет", corps.getString("q"))
        assertEquals("ru", corps.getString("source"))
        assertEquals("fr", corps.getString("target"))
        assertEquals("text", corps.getString("format"))
        assertFalse("aucune clé vide ne doit être envoyée", corps.has("api_key"))
    }

    @Test fun `cle fournie presente seulement si renseignee`() {
        val corps = JSONObject(LibreTranslate("https://exemple.test").corpsRequete("hi", "en", "fr", "sec"))
        assertEquals("sec", corps.getString("api_key"))
    }
}
