package net.wyvest.skindownloader

import com.google.common.base.Predicates
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import gg.essential.api.EssentialAPI
import gg.essential.api.utils.Multithreading
import gg.essential.universal.utils.MCClickEventAction
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityItemFrame
import net.minecraft.util.BlockPos
import net.minecraft.util.EntitySelectors
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.lwjgl.input.Keyboard
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


private const val NAME = "Skin Downloader"
private const val VERSION = "1.0.0"
private const val ID = "skindownloader"
private const val ADAPTER = "gg.essential.api.utils.KotlinAdapter"

@Mod(name = NAME, modid = ID, version = VERSION, modLanguageAdapter = ADAPTER)
object SkinDownloader {
    private val fileFormatter = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")
    private val mc
        get() = Minecraft.getMinecraft()
    private val statsKey: KeyBinding = KeyBinding("The Funny", Keyboard.KEY_M, "Skin Downloader")

    @Mod.EventHandler
    fun onFMLInitialization(event: FMLInitializationEvent) {
        ClientRegistry.registerKeyBinding(statsKey)
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onKeyPress(event: InputEvent.KeyInputEvent) {
        if (Keyboard.getEventKey() == statsKey.keyCode) {
            if (Keyboard.getEventKeyState()) {
                try {
                    val entity = get(mc.timer.renderPartialTicks)
                    if (entity is AbstractClientPlayer) {
                        mc.skinManager.loadProfileTextures(
                            entity.playerInfo.gameProfile,
                            { p_180521_1_, _, profileTexture ->
                                if (p_180521_1_ == MinecraftProfileTexture.Type.SKIN) {
                                    Multithreading.runAsync {
                                        val file = File("skindownloads/${profileTexture.url.substring(profileTexture.url.lastIndexOf("/"))}-${entity.skinType}-${fileFormatter.format(Date())}.png")
                                        download(profileTexture.url, file)
                                        EssentialAPI.getMinecraftUtil().sendMessage(UTextComponent("Skin dowloaded! Click here to open.").setClick(MCClickEventAction.OPEN_FILE, file.absolutePath))
                                    }
                                }
                            }, true
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }


        }
    }

    /**
     * Adapted from RequisiteLaunchwrapper under LGPLv3
     * https://github.com/Qalcyo/RequisiteLaunchwrapper/blob/main/LICENSE
     */
    private fun download(url: String, file: File): Boolean {
        if (file.exists()) return true
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        var newUrl = url
        newUrl = newUrl.replace(" ", "%20")
        val downloadClient: HttpClient =
            HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(10000).build())
                .build()
        try {
            FileOutputStream(file).use { fileOut ->
                val downloadResponse: HttpResponse = downloadClient.execute(HttpGet(newUrl))
                val buffer = ByteArray(1024)
                var read: Int
                while (downloadResponse.entity.content.read(buffer).also { read = it } > 0) {
                    fileOut.write(buffer, 0, read)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * Adapted from quickStats under GPLv3
     * https://github.com/nxtdaydelivery/quickStats/blob/main/LICENSE
     */
    fun get(partialTicks: Float): Entity? {
        val entity = mc.renderViewEntity
        var pointedEntity: Entity? = null
        if (entity != null) {
            if (mc.theWorld != null) {
                mc.mcProfiler.startSection("pick")
                mc.pointedEntity = null
                val d0 = 200.0
                mc.objectMouseOver = entity.rayTrace(d0, partialTicks)
                var d1 = d0
                val vec3 = entity.getPositionEyes(partialTicks)
                val flag = false
                if (mc.objectMouseOver != null) {
                    d1 = mc.objectMouseOver.hitVec.distanceTo(vec3)
                }
                val vec31 = entity.getLook(partialTicks)
                val vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0)
                var vec33: Vec3? = null
                val f = 1.0f
                val list = mc.theWorld.getEntitiesInAABBexcluding(
                    entity,
                    entity.entityBoundingBox.addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0)
                        .expand(f.toDouble(), f.toDouble(), f.toDouble()),
                    Predicates.and(EntitySelectors.NOT_SPECTATING, { it?.canBeCollidedWith() ?: false })
                )
                var d2 = d1
                for (entity1 in list) {
                    val f1 = entity1.collisionBorderSize
                    val axisalignedbb = entity1.entityBoundingBox.expand(
                        f1.toDouble(), f1.toDouble(),
                        f1.toDouble()
                    )
                    val movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32)
                    if (axisalignedbb.isVecInside(vec3)) {
                        if (d2 >= 0.0) {
                            pointedEntity = entity1
                            vec33 = if (movingobjectposition == null) vec3 else movingobjectposition.hitVec
                            d2 = 0.0
                        }
                    } else if (movingobjectposition != null) {
                        val d3 = vec3.distanceTo(movingobjectposition.hitVec)
                        if (d3 < d2 || d2 == 0.0) {
                            if (entity1 === entity.ridingEntity && !entity.canRiderInteract()) {
                                if (d2 == 0.0) {
                                    pointedEntity = entity1
                                    vec33 = movingobjectposition.hitVec
                                }
                            } else {
                                pointedEntity = entity1
                                vec33 = movingobjectposition.hitVec
                                d2 = d3
                            }
                        }
                    }
                }
                if (pointedEntity != null && flag && vec3.distanceTo(vec33) > 3.0) {
                    pointedEntity = null
                    mc.objectMouseOver = MovingObjectPosition(
                        MovingObjectPosition.MovingObjectType.MISS, vec33,
                        null, BlockPos(vec33)
                    )
                }
                if (pointedEntity != null && (d2 < d1 || mc.objectMouseOver == null)) {
                    mc.objectMouseOver = MovingObjectPosition(pointedEntity, vec33)
                    if (pointedEntity is EntityLivingBase || pointedEntity is EntityItemFrame) {
                        mc.pointedEntity = pointedEntity
                    }
                }
                mc.mcProfiler.endSection()
            }
        }
        return pointedEntity
    }
}