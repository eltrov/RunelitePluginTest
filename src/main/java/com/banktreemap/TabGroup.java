package com.banktreemap;

import java.util.List;

/** One bank tab (index 0 = the main/untabbed section, 1-9 = custom tabs) with its priced stacks. */
final class TabGroup
{
	private final int index;
	private final String name;
	private final List<StackNode> stacks;
	private final long total;

	TabGroup(int index, String name, List<StackNode> stacks, long total)
	{
		this.index = index;
		this.name = name;
		this.stacks = stacks;
		this.total = total;
	}

	int getIndex()
	{
		return index;
	}

	String getName()
	{
		return name;
	}

	/** Every priced stack in the tab, unfiltered and unsorted. */
	List<StackNode> getStacks()
	{
		return stacks;
	}

	/** Total GE value of the whole tab, including stacks below the display threshold. */
	long getTotal()
	{
		return total;
	}
}
