package com.streamlux.app.utils

object VidVaultUrlBuilder {

    fun build(mediaType: String, tmdbId: String, season: Int? = null, episode: Int? = null): String {
        val id = tmdbId.trim()
        return when {
            mediaType == "tv" && season != null && episode != null ->
                "https://vidvault.ru/tv/$id/$season/$episode"
            mediaType == "tv" ->
                "https://vidvault.ru/tv/$id"
            else ->
                "https://vidvault.ru/movie/$id"
        }
    }

    fun tmdbInjectionScript(tmdbId: String): String = """
        (function() {
            try {
                var id = '$tmdbId';
                var input = document.querySelector(
                    'input[type="text"], input[type="search"], input[placeholder*="TMDB" i], input[placeholder*="IMDb" i], input[placeholder*="ID" i]'
                );
                if (input) {
                    input.value = id;
                    input.dispatchEvent(new Event('input', { bubbles: true }));
                    input.dispatchEvent(new Event('change', { bubbles: true }));
                }
            } catch (e) {}
        })();
    """.trimIndent()
}
