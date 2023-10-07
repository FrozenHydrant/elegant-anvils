package betathunder.eleganceofanvils;

import java.util.*;
import java.util.Map.Entry;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EleganceOfAnvils.MODID)
public class AnvilMenuEvent {

	@SubscribeEvent
	public static void onAnvilMenu(AnvilUpdateEvent e) {
		ItemStack toForge = e.getLeft();
		ItemStack catalyst = e.getRight();
		ItemStack theForged = toForge.copy();

		int levelCost = 0;

		// Repairs (Using Catalyst)
		if (toForge.isDamageableItem() && toForge.getItem().isValidRepairItem(toForge, catalyst)
				&& toForge.getDamageValue() > 0) {
			int repairAmount = toForge.getMaxDamage() / 2;
			int amountNeeded = (toForge.getDamageValue() / repairAmount) + 1;
			int amountUsed = Math.min(amountNeeded, catalyst.getCount());

			levelCost += amountUsed;
			theForged.setDamageValue(Math.max(toForge.getDamageValue() - amountUsed * repairAmount, 0));
			e.setMaterialCost(amountUsed);
		}

		// Repairs (Same Item)
		if (toForge.isDamageableItem() && toForge.getItem() == catalyst.getItem() && toForge.getDamageValue() > 0) {
			// The anvil will also grant a bonus 25% max durability of the item
			// (configurable)
			int repairAmount = (int) (catalyst.getMaxDamage() - catalyst.getDamageValue()
					+ catalyst.getMaxDamage() * (((double) Config.bonusRepairPercentage) / 100));

			levelCost += 2;
			theForged.setDamageValue(Math.max(toForge.getDamageValue() - repairAmount, 0));
		}

		// Renaming
		String requestedName = e.getName();
		if (requestedName != null && !Util.isBlank(requestedName)) {
			if (!requestedName.equals(toForge.getHoverName().getString())) {
				theForged.setHoverName(Component.literal(requestedName));
				levelCost += 1;
			}
		} else if (toForge.hasCustomHoverName()) {
			theForged.resetHoverName();
			levelCost += 1;
		}

		// Enchantments (Book or Same Item)
		if (toForge.getItem() == catalyst.getItem() || catalyst.getItem() == Items.ENCHANTED_BOOK) {
			// Initial cost of AT LEAST ONE
			levelCost += 1;

			// Get the enchants of the forging item and catalyst
			Map<Enchantment, Integer> baseEnchants = EnchantmentHelper.getEnchantments(toForge);
			Map<Enchantment, Integer> bonusEnchants = EnchantmentHelper.getEnchantments(catalyst);
			Map<Enchantment, Integer> allEnchants = new HashMap<>();

			for (Entry<Enchantment, Integer> bonusEnchantEntry : bonusEnchants.entrySet()) {
				Enchantment bonusEnchant = bonusEnchantEntry.getKey();
				int level = bonusEnchantEntry.getValue();

				// Stack same types of enchantments
				if (baseEnchants.containsKey(bonusEnchant)) {
					int baseLevel = baseEnchants.get(bonusEnchant);
					int newLevel = baseLevel;

					if (baseLevel == level) {
						// Note: a combination tax of 1 level is applied for every enchantment which
						// becomes more powerful.
						if (baseLevel < bonusEnchant.getMaxLevel() * (Config.maxLevelPercentage/100.0)) {
							newLevel = baseLevel + 1;
							levelCost += 1;
						}
					} else {
						newLevel = Math.max(baseLevel, level);
					}

					allEnchants.put(bonusEnchant, newLevel);
				}

				// Add compatible enchants from catalyst:
				// isEnchantmentCompatible() makes sure we don't have smite and sharpness, for
				// example
				// bonusEnchant.canEnchant() makes sure you can't put sharpness on boots
				// But for books, almost anything goes.
				else {
					if (EnchantmentHelper.isEnchantmentCompatible(baseEnchants.keySet(), bonusEnchant)
							&& (bonusEnchant.canEnchant(toForge) || toForge.getItem() == Items.ENCHANTED_BOOK)) {
						allEnchants.put(bonusEnchant, level);
					}
				}

			}

			// Add remaining enchants from the base item
			for (Entry<Enchantment, Integer> baseEnchantEntry : baseEnchants.entrySet()) {
				Enchantment baseEnchant = baseEnchantEntry.getKey();
				int level = baseEnchantEntry.getValue();

				if (!allEnchants.containsKey(baseEnchant)) {
					allEnchants.put(baseEnchant, level);
				}
			}

			// Get the # of each rarity of enchant in total: subtract how much we started
			// with to get a "delta enchantment": we'll calculate this delta enchantment for
			// both the forging item and catalyst.
			Map<Rarity, Integer> allEnchantRarities = getEnchantmentRarities(allEnchants);
			Map<Rarity, Integer> baseEnchantRarities = getEnchantmentRarities(baseEnchants);
			Map<Rarity, Integer> bonusEnchantRarities = getEnchantmentRarities(bonusEnchants);
			for (Entry<Rarity, Integer> enchantEntry : allEnchantRarities.entrySet()) {
				Rarity rarity = enchantEntry.getKey();
				int amount = enchantEntry.getValue();
				baseEnchantRarities.put(rarity, Math.max(amount - baseEnchantRarities.getOrDefault(rarity, 0), 0));
				bonusEnchantRarities.put(rarity, Math.max(amount - bonusEnchantRarities.getOrDefault(rarity, 0), 0));
			}

			// Give levels back for no-enchanted-book anvil operations (Configurable)
			if (toForge.getItem() == catalyst.getItem() && toForge.getItem() != Items.ENCHANTED_BOOK) {
				levelCost = Math.max(levelCost - Config.sameTypeBonus, 1);
			}

			// We'll take the lesser of the 2 rarity deltas: the catalyst, and the forging
			// item.
			levelCost += Math.min(getRaritiesCost(baseEnchantRarities), getRaritiesCost(bonusEnchantRarities));
			EnchantmentHelper.setEnchantments(allEnchants, theForged);
		}

		// Hard capped at 39 to avoid "too expensive"
		levelCost = Math.min(39, levelCost);
		e.setCost(levelCost);

		// Default to vanilla behaviour if this event didn't change the item output
		if (levelCost > 0) {
			e.setOutput(theForged);
		} else {
			e.setOutput(ItemStack.EMPTY);
		}

	}

	// Maps enchantment rarity to how many levels of each rarity occur in total.
	private static Map<Rarity, Integer> getEnchantmentRarities(Map<Enchantment, Integer> enchants) {
		Map<Rarity, Integer> enchantRarities = new HashMap<>();
		for (Entry<Enchantment, Integer> enchant : enchants.entrySet()) {
			Rarity rarity = enchant.getKey().getRarity();
			int level = enchant.getValue();
			enchantRarities.put(rarity, enchantRarities.getOrDefault(rarity, 0) + level);
		}
		return enchantRarities;
	}

	// The rarity cost is the combined levels of all common enchants + combined
	// levels of all uncommon enchants + ...etc, and each tier is multiplied by a
	// different, increasingly expensive cost.
	private static int getRaritiesCost(Map<Rarity, Integer> rarities) {
		double cost = 0;
		cost += rarities.getOrDefault(Rarity.COMMON, 0);
		cost += rarities.getOrDefault(Rarity.UNCOMMON, 0) * 1.5;
		cost += rarities.getOrDefault(Rarity.RARE, 0) * 2;
		cost += rarities.getOrDefault(Rarity.VERY_RARE, 0) * 2.5;
		return (int) cost;
	}

	// A reasonable average function
	// private static int truncatedAverage(int a, int b) {
	// return (a + b) / 2;
	// };
}
