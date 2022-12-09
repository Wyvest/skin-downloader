package net.wyvest.skindownloader.mixins;

import net.minecraft.client.resources.SkinManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@Mixin(SkinManager.class)
public interface SkinManagerAccessor {
    @Accessor("skinCacheDir")
    File getSkinCacheDir();
}
