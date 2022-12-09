package net.wyvest.skindownloader

import cc.polyfrost.oneconfig.libs.universal.UChat
import cc.polyfrost.oneconfig.libs.universal.UDesktop
import cc.polyfrost.oneconfig.libs.universal.utils.MCClickEventAction
import cc.polyfrost.oneconfig.libs.universal.wrappers.message.UTextComponent
import cc.polyfrost.oneconfig.utils.NetworkUtils
import cc.polyfrost.oneconfig.utils.Notifications
import cc.polyfrost.oneconfig.utils.dsl.mc
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.wyvest.skindownloader.mixins.SkinManagerAccessor
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


internal const val NAME = "@NAME@"
internal const val VERSION = "@VER@"
internal const val ID = "@ID@"

@Mod(
    name = NAME,
    modid = ID,
    version = VERSION,
    modLanguageAdapter = "cc.polyfrost.oneconfig.utils.KotlinLanguageAdapter"
)
object SkinDownloader {
    private val fileFormatter = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")

    @EventHandler
    fun init(event: FMLInitializationEvent) {
        SkinConfig
    }

    fun download() {
        try {
            val entity = get()
            if (entity is AbstractClientPlayer) {
                if (entity.gameProfile != null) {
                    val minecraft = Minecraft.getMinecraft()
                    val map = minecraft.skinManager.loadSkinFromCache(entity.gameProfile)
                    if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                        map[MinecraftProfileTexture.Type.SKIN]!!.let {
                            val skinCacheDir = (mc.skinManager as SkinManagerAccessor).skinCacheDir
                            val skinFile = File(skinCacheDir, if (it.hash.length > 2) it.hash.substring(0, 2) else "xx")
                            val skinFile2 = File(skinFile, it.hash)
                            val downloadedFile = File("skindownloads/${entity.name}-${entity.skinType}-${it.hash}-${fileFormatter.format(Date())}.png")
                            downloadedFile.parentFile.mkdirs()
                            if (skinFile2.exists()) {
                                skinFile2.copyTo(downloadedFile, overwrite = true)
                                send("Skin dowloaded! Click here to open.", downloadedFile)
                            } else {
                                if (NetworkUtils.downloadFile(it.url, downloadedFile)) {
                                    send("Skin dowloaded! Click here to open.", downloadedFile)
                                    try {
                                        downloadedFile.copyTo(skinFile2)
                                    } catch (ignored: Exception) {
                                    }
                                } else {
                                    send("Failed to download skin.")
                                }
                            }
                        }
                    } else {
                        send("Skin not found!")
                    }
                } else {
                    send("Skin not found!")
                }
            } else {
                send("You must be looking at a player to download their skin!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            send("Error downloading skin: ${e.message}")
        }
    }

    private fun send(message: String, file: File? = null) {
        val exists = file != null
        when (SkinConfig.notificationType) {
            0 -> {
                UChat.chat(UTextComponent(message).also { if (exists) it.setClick(MCClickEventAction.OPEN_FILE, file!!.absolutePath) })
            }
            1 -> {
                if (exists) {
                    Notifications.INSTANCE.send("Skin Downloader", message, Runnable {
                        if (file != null) {
                            UDesktop.open(file)
                        }
                    })
                } else {
                    Notifications.INSTANCE.send("Skin Downloader", message)
                }
            }
        }
    }

    private val skinReachDistance
        get() = SkinConfig.skinReachDistance.toDouble()

    private fun get(): Entity? {
        mc.renderViewEntity?.let { entity ->
            if (mc.theWorld != null) {
                var pointedEntity: Entity? = null
                val objectMouseOver = entity.rayTrace(skinReachDistance, 1f)
                var distance1 = skinReachDistance
                val vec3 = entity.getPositionEyes(1f)
                if (objectMouseOver != null) {
                    distance1 = objectMouseOver.hitVec.distanceTo(vec3)
                }
                val vec31 = entity.getLook(1f)
                val vec32 = vec3.addVector(vec31.xCoord * skinReachDistance, vec31.yCoord * skinReachDistance, vec31.zCoord * skinReachDistance)
                val list: List<Entity> = mc.theWorld.getEntitiesInAABBexcluding(
                    entity, entity.entityBoundingBox.addCoord(
                        vec31.xCoord * skinReachDistance, vec31.yCoord * skinReachDistance, vec31.zCoord * skinReachDistance
                    ).expand(1.0, 1.0, 1.0)
                ) { it !is EntityPlayer || !it.isSpectator || it.canBeCollidedWith() }
                var distance2 = distance1
                list.forEachIndexed { j, entity1 ->
                    var distance3 = 0.0
                    val f1 = entity1.collisionBorderSize
                    val axisalignedbb = entity1.entityBoundingBox.expand(f1.toDouble(), f1.toDouble(), f1.toDouble())
                    val movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32)
                    if (axisalignedbb.isVecInside(vec3)) {
                        if (distance2 < 0.0) return@forEachIndexed
                        pointedEntity = entity1
                        distance2 = 0.0
                        return@forEachIndexed
                    }
                    if (movingobjectposition == null || vec3.distanceTo(movingobjectposition.hitVec)
                            .also { distance3 = it } >= distance2 && distance2 != 0.0
                    ) return@forEachIndexed
                    if (entity1 == entity.ridingEntity && !entity.canRiderInteract()) {
                        if (distance2 != 0.0) return@forEachIndexed
                        pointedEntity = entity1
                        return@forEachIndexed
                    }
                    pointedEntity = entity1
                    distance2 = distance3
                }
                if (pointedEntity != null && (distance2 < distance1 || mc.objectMouseOver == null)) {
                    return pointedEntity
                }
            }
        }
        return null
    }
}