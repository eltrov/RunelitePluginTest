package com.banktreemap;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("banktreemap")
public interface BankTreemapConfig extends Config
{
	@Range(min = 0)
	@ConfigItem(
		keyName = "minStackValue",
		name = "Min stack value",
		description = "Stacks worth less than this (GE price x quantity, in gp) are hidden",
		position = 1
	)
	default int minStackValue()
	{
		return 1_000_000;
	}

	@ConfigItem(
		keyName = "showLabels",
		name = "Show icons and values",
		description = "Draw item icons and gp values inside blocks that are big enough",
		position = 2
	)
	default boolean showLabels()
	{
		return true;
	}

	@ConfigItem(
		keyName = "cushionShading",
		name = "Cushion shading",
		description = "TreeSize-style radial shading on each block",
		position = 3
	)
	default boolean cushionShading()
	{
		return true;
	}
}
