package betathunder.eleganceofanvils;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = EleganceOfAnvils.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
	private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

	//private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
	//		.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);

	private static final ForgeConfigSpec.IntValue BONUS_REPAIR_PERCENTAGE = BUILDER.comment(
			"Anvil repairs using two of the same weapon/armor restore a bonus percentage of the item's max durability\n"
					+ "A value of 0 means that combining two damaged weapons/armor will result in an item with the sum of their durabilities.\n"
					+ "A value of 100 means that any repair of weapons/armor will restore said item to max durability.")
			.defineInRange("bonusRepairPercentage", 25, 0, 100);

	//public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
	//		.comment("What you want the introduction message to be for the magic number")
	//		.define("magicNumberIntroduction", "The magic number is... ");

	// a list of strings that are treated as resource locations for items
	// private static final ForgeConfigSpec.ConfigValue<List<? extends String>>
	// ITEM_STRINGS = BUILDER
	// .comment("A list of items to log on common setup.")
	// .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"),
	// Config::validateItemName);

	static final ForgeConfigSpec SPEC = BUILDER.build();

	//public static boolean logDirtBlock;
	public static int bonusRepairPercentage;
	//public static String magicNumberIntroduction;
	// public static Set<Item> items;

	// private static boolean validateItemName(final Object obj)
	// {
	// return obj instanceof final String itemName &&
	// ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
	// }

	@SubscribeEvent
	static void onLoad(final ModConfigEvent event) {
		//logDirtBlock = LOG_DIRT_BLOCK.get();
		//magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();
		
		bonusRepairPercentage = BONUS_REPAIR_PERCENTAGE.get();

		// convert the list of strings into a set of items
		// items = ITEM_STRINGS.get().stream()
		// .map(itemName -> ForgeRegistries.ITEMS.getValue(new
		// ResourceLocation(itemName)))
		// .collect(Collectors.toSet());
	}
}
