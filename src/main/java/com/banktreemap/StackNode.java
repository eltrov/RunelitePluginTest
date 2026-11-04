package com.banktreemap;

/** One bank stack: an item, how many of it, and its total GE value. */
final class StackNode
{
	private final int itemId;
	private final String name;
	private final int quantity;
	private final long value;

	StackNode(int itemId, String name, int quantity, long value)
	{
		this.itemId = itemId;
		this.name = name;
		this.quantity = quantity;
		this.value = value;
	}

	int getItemId()
	{
		return itemId;
	}

	String getName()
	{
		return name;
	}

	int getQuantity()
	{
		return quantity;
	}

	long getValue()
	{
		return value;
	}
}
