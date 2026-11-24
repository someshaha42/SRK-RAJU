package com.framecut.ui.definitions

object UiPathRegistry {
    val entries = listOf(
        UiPathEntry("ui/components", ":framecut-ui-components", "ui/components.json"),
        UiPathEntry("ui/layouts", ":framecut-ui-core", "ui/layouts.json"),
        UiPathEntry("ui/animations", ":framecut-ui-definitions", "ui/animations.json"),
        UiPathEntry("ui/interactions", ":framecut-ui-definitions", "ui/interactions.json"),
        UiPathEntry("ui/tokens", ":framecut-ui-theme", "ui/design-tokens.json"),
    )
}
