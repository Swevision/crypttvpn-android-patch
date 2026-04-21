package com.crypttvpn.ui.activate

import android.content.Intent
import android.net.Uri

/**
 * Извлекает ключ активации из:
 *  - crypttvpn://activate?key=CRYPT-XXXX-XXXX-XXXX-XXXX
 *  - https://crypttvpn.com/activate/CRYPT-XXXX-XXXX-XXXX-XXXX
 *  - https://crypttvpn.xyz/activate/CRYPT-XXXX-XXXX-XXXX-XXXX
 *
 * Использование в MainActivity:
 *   DeepLinkHandler.extractKey(intent)?.let { viewModel.activate(it) }
 */
object DeepLinkHandler {

    private val KEY_REGEX = Regex("""CRYPT-[A-Z0-9-]{4,}""", RegexOption.IGNORE_CASE)

    fun extractKey(intent: Intent?): String? {
        val uri: Uri = intent?.data ?: return null
        // custom scheme: ?key=
        uri.getQueryParameter("key")?.let { return sanitize(it) }
        // universal link: /activate/{key}
        val segments = uri.pathSegments
        val idx = segments.indexOf("activate")
        if (idx >= 0 && idx + 1 < segments.size) {
            return sanitize(segments[idx + 1])
        }
        // last-resort: regex on whole uri
        KEY_REGEX.find(uri.toString())?.value?.let { return sanitize(it) }
        return null
    }

    private fun sanitize(raw: String): String? {
        val cleaned = raw.trim().uppercase()
        return if (KEY_REGEX.matches(cleaned)) cleaned else null
    }
}
