package net.wyvest.skindownloader

import cc.polyfrost.oneconfig.config.Config
import cc.polyfrost.oneconfig.config.annotations.Dropdown
import cc.polyfrost.oneconfig.config.annotations.KeyBind
import cc.polyfrost.oneconfig.config.annotations.Slider
import cc.polyfrost.oneconfig.config.core.OneKeyBind
import cc.polyfrost.oneconfig.config.data.Mod
import cc.polyfrost.oneconfig.config.data.ModType
import cc.polyfrost.oneconfig.libs.universal.UKeyboard

object SkinConfig : Config(Mod(NAME, ModType.UTIL_QOL), "skindownloader.json") {
    @KeyBind(
        name = "Download Keybind",
        description = "The keybind to download the skin of the entity you're looking at",
    )
    var downloadKeybind = OneKeyBind(UKeyboard.KEY_M)

    @Slider(
        name = "Skin Reach Distance",
        description = "The maximum distance you can be from an entity to download their skin",
        min = 0f,
        max = 300f,
        step = 10
    )
    var skinReachDistance = 50f

    @Dropdown(
        name = "Notification Type",
        description = "The type of notification to send when a skin is downloaded",
        options = ["Chat", "Notification"]
    )
    var notificationType = 1

    init {
        initialize()
        registerKeyBind(downloadKeybind) {
            SkinDownloader.download()
        }
    }
}