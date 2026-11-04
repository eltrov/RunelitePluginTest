package com.banktreemap;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Varbits;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Bank Treemap",
	description = "Visualizes bank stack values per tab as a treemap, TreeSize-style",
	tags = {"bank", "value", "treemap", "gp", "wealth", "tab"}
)
public class BankTreemapPlugin extends Plugin
{
	private static final String CONFIG_GROUP = "banktreemap";

	// Item-count varbits for custom bank tabs 1-9, in tab order
	private static final int[] TAB_COUNT_VARBITS = {
		Varbits.BANK_TAB_ONE_COUNT,
		Varbits.BANK_TAB_TWO_COUNT,
		Varbits.BANK_TAB_THREE_COUNT,
		Varbits.BANK_TAB_FOUR_COUNT,
		Varbits.BANK_TAB_FIVE_COUNT,
		Varbits.BANK_TAB_SIX_COUNT,
		Varbits.BANK_TAB_SEVEN_COUNT,
		Varbits.BANK_TAB_EIGHT_COUNT,
		Varbits.BANK_TAB_NINE_COUNT,
	};

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private BankTreemapConfig config;

	private BankTreemapPanel panel;
	private NavigationButton navButton;

	// Raw bank slots in container order: {item id, quantity}. Includes empty slots and
	// placeholders, because tab boundaries are slice positions in the raw container.
	// Client thread only.
	private final List<int[]> snapshot = new ArrayList<>();
	private int lastActiveTab = -1;

	@Provides
	BankTreemapConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankTreemapConfig.class);
	}

	@Override
	protected void startUp()
	{
		lastActiveTab = -1;
		panel = new BankTreemapPanel(itemManager, config, () -> clientThread.invokeLater(this::rebuild));
		navButton = NavigationButton.builder()
			.tooltip("Bank Treemap")
			.icon(makeIcon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		clientThread.invoke(snapshot::clear);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.BANK.getId())
		{
			return;
		}

		ItemContainer container = event.getItemContainer();
		snapshot.clear();
		for (Item item : container.getItems())
		{
			snapshot.add(new int[]{item.getId(), item.getQuantity()});
		}
		rebuild();
		pushActiveTab();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int id = event.getVarbitId();
		if (id == Varbits.CURRENT_BANK_TAB)
		{
			pushActiveTab();
			return;
		}
		for (int countVarbit : TAB_COUNT_VARBITS)
		{
			if (id == countVarbit)
			{
				// Tab boundaries moved (item dragged between tabs, tab created or removed)
				rebuild();
				return;
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}
		BankTreemapPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(p::refreshView);
		}
	}

	private void pushActiveTab()
	{
		int tab = client.getVarbitValue(Varbits.CURRENT_BANK_TAB);
		if (tab == lastActiveTab)
		{
			return;
		}
		lastActiveTab = tab;
		BankTreemapPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(() -> p.setActiveTab(tab));
		}
	}

	/**
	 * Splits the cached bank into tabs and re-prices it. The container is ordered tab 1,
	 * tab 2 ... tab 9, then the main (untabbed) items; the count varbits give each tab's size.
	 * Must run on the client thread.
	 */
	private void rebuild()
	{
		if (snapshot.isEmpty())
		{
			return;
		}

		List<TabGroup> groups = new ArrayList<>();
		long total = 0;
		int size = snapshot.size();
		int pos = 0;

		for (int tab = 1; tab <= TAB_COUNT_VARBITS.length; tab++)
		{
			int count = Math.max(0, client.getVarbitValue(TAB_COUNT_VARBITS[tab - 1]));
			int end = Math.min(size, pos + count);
			TabGroup group = buildGroup(tab, "Tab " + tab, pos, end);
			pos = end;
			if (group != null)
			{
				groups.add(group);
				total += group.getTotal();
			}
		}

		TabGroup main = buildGroup(0, "Main tab", pos, size);
		if (main != null)
		{
			groups.add(0, main);
			total += main.getTotal();
		}

		final long finalTotal = total;
		BankTreemapPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(() -> p.update(groups, finalTotal));
		}
	}

	/** Prices the slots in [from, to) of the snapshot. Returns null if nothing in range is priced. */
	private TabGroup buildGroup(int index, String name, int from, int to)
	{
		List<StackNode> nodes = new ArrayList<>();
		long total = 0;
		for (int i = from; i < to; i++)
		{
			int id = snapshot.get(i)[0];
			int qty = snapshot.get(i)[1];
			// Skip empty slots (id -1) and placeholders (quantity 0)
			if (id <= 0 || qty <= 0)
			{
				continue;
			}
			long price = itemManager.getItemPrice(id);
			if (price <= 0)
			{
				continue; // untradeable or unpriced
			}
			long value = price * qty;
			String itemName = itemManager.getItemComposition(id).getName();
			nodes.add(new StackNode(id, itemName, qty, value));
			total += value;
		}
		return nodes.isEmpty() ? null : new TabGroup(index, name, nodes, total);
	}

	private static BufferedImage makeIcon()
	{
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(0, 170, 170));
		g.fillRect(0, 0, 10, 16);
		g.setColor(new Color(220, 150, 20));
		g.fillRect(10, 0, 6, 9);
		g.setColor(new Color(60, 180, 60));
		g.fillRect(10, 9, 6, 7);
		g.dispose();
		return img;
	}
}
