package com.banktreemap;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
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
	description = "Visualizes bank stack values as a treemap, TreeSize-style",
	tags = {"bank", "value", "treemap", "gp", "wealth"}
)
public class BankTreemapPlugin extends Plugin
{
	private static final String CONFIG_GROUP = "banktreemap";

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

	// Last bank snapshot (item id, quantity). Client thread only.
	private final List<int[]> snapshot = new ArrayList<>();

	@Provides
	BankTreemapConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankTreemapConfig.class);
	}

	@Override
	protected void startUp()
	{
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
			// Skips empty slots (id -1) and placeholders (quantity 0)
			if (item.getId() > 0 && item.getQuantity() > 0)
			{
				snapshot.add(new int[]{item.getId(), item.getQuantity()});
			}
		}
		rebuild();
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

	/** Re-prices the cached snapshot. Must run on the client thread. */
	private void rebuild()
	{
		if (snapshot.isEmpty())
		{
			return;
		}

		List<StackNode> nodes = new ArrayList<>();
		long total = 0;
		for (int[] entry : snapshot)
		{
			int id = entry[0];
			int qty = entry[1];
			long price = itemManager.getItemPrice(id);
			if (price <= 0)
			{
				continue; // untradeable or unpriced
			}
			long value = price * qty;
			String name = itemManager.getItemComposition(id).getName();
			nodes.add(new StackNode(id, name, qty, value));
			total += value;
		}

		final long finalTotal = total;
		BankTreemapPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(() -> p.update(nodes, finalTotal));
		}
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
