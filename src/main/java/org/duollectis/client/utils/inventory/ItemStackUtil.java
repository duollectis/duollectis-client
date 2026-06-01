package org.duollectis.client.utils.inventory;

import lombok.experimental.UtilityClass;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.duollectis.client.modules.mapartbuilder.CachedBlock;

import java.util.List;
import java.util.Optional;

@UtilityClass
public class ItemStackUtil {

	public static List<CachedBlock> toBlocks(List<ItemStack> stacks) {
		return stacks.stream()
			.filter(stack -> stack.getItem() instanceof BlockItem)
			.map(stack -> new CachedBlock(
				((BlockItem) stack.getItem()).getBlock(),
				stack.getCount()
			))
			.toList();
	}

	public static Optional<Block> toBlock(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem
			? Optional.of(blockItem.getBlock())
			: Optional.empty();
	}
}
