import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.muonmc.loader.api.MuonLoader;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;

public class JunitTest {
	@BeforeAll
	public static void setup() {
		System.out.println("Initializing Minecraft");
		MixinExtrasBootstrap.init();
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		System.out.println("Minecraft initialized");
	}

	@Test
	public void testItems() {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(Items.DIAMOND);
		assertEquals(id.toString(), "minecraft:diamond");

		System.out.println(id);
	}

	@Test
	public void testMixin() {
		// MixinGrassBlock sets canGrow to false
		GrassBlock grassBlock = (GrassBlock) Blocks.GRASS_BLOCK;
		boolean canGrow = grassBlock.isBonemealSuccess(null, null, null, null);
		assertFalse(canGrow);
	}

	@Test
	public void testMixinExtras() {
		// MixinGrassBlock sets isFertilizable to true
		GrassBlock grassBlock = (GrassBlock) Blocks.GRASS_BLOCK;
		System.out.println("Grass Block = " + grassBlock);
		boolean isFertilizable = grassBlock.isValidBonemealTarget(null, BlockPos.ZERO, null);
		assertTrue(isFertilizable);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testAccessLoader() {
		MuonLoader.getAllMods();
		FabricLoader.getInstance().getAllMods();
	}
}
