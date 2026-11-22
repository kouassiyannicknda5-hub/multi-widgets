package app.multiwidgets.ai.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fournisseurs de traduction en ligne, interchangeables.
 *
 * Sécurité : aucune clé secrète n'est intégrée au code source. L'URL du
 * fournisseur et, si nécessaire, sa clé, sont saisies par l'utilisateur dans
 * les paramètres et stockées dans le DataStore privé de l'application.
 */
interface CloudTranslator {
    val nom: String
    suspend fun traduire(texte: String, source: String, cible: String, cle: String): Result<String>
}

/**
 * LibreTranslate (auto-hébergé ou instance publique choisie par l'utilisateur).
 * Le protocole est simple et documenté : POST /translate, JSON.
 */
class LibreTranslate(private val urlBase: String) : CloudTranslator {

    override val nom = "LibreTranslate"

    /** Corps JSON de la requête : isolé pour être testable sans réseau. */
    fun corpsRequete(texte: String, source: String, cible: String, cle: String): String =
        JSONObject().apply {
            put("q", texte)
            put("source", source)
            put("target", cible)
            put("format", "text")
            if (cle.isNotBlank()) put("api_key", cle)
        }.toString()

    override suspend fun traduire(
        texte: String, source: String, cible: String, cle: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = corpsRequete(texte, source, cible, cle)
            val conn = (URL("${urlBase.trimEnd('/')}/translate").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                return@runCatching throw ErreurReseau(code, err.take(200))
            }
            val rep = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(rep).optString("translatedText")
        }
    }

    class ErreurReseau(val code: Int, message: String) : Exception("HTTP $code : $message")
}

/** Résolution du fournisseur configuré ; null si aucun n'est configuré. */
fun fournisseurConfigure(url: String): CloudTranslator? =
    if (url.isBlank()) null else LibreTranslate(url)
