package com.framecut.ui.definitions

import android.content.Context
import org.json.JSONObject

data class LoadedUiDefinitions(
    val componentsJson: JSONObject,
    val layoutsJson: JSONObject,
    val animationsJson: JSONObject,
    val interactionsJson: JSONObject,
    val designTokensJson: JSONObject,
)

object UiDefinitionLoader {
    fun loadAll(context: Context): LoadedUiDefinitions = LoadedUiDefinitions(
        componentsJson = loadAssetJson(context, "ui/components.json"),
        layoutsJson = loadAssetJson(context, "ui/layouts.json"),
        animationsJson = loadAssetJson(context, "ui/animations.json"),
        interactionsJson = loadAssetJson(context, "ui/interactions.json"),
        designTokensJson = loadAssetJson(context, "ui/design-tokens.json"),
    )

    fun loadAssetJson(context: Context, path: String): JSONObject {
        val raw = context.assets.open(path).bufferedReader().use { it.readText() }
        return JSONObject(raw)
    }
}
