package com.banktreemap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class BankTreemapPanel extends PluginPanel
{
	private final BankTreemapConfig config;
	private final ItemManager itemManager;
	private final JPanel groupsPanel = new JPanel();
	private final JLabel totalLabel = new JLabel("Bank value: -");
	private final JLabel shownLabel = new JLabel("Open your bank to scan it");

	// Expanded state per tab index (0 = main tab, 1-9 = custom tabs). Missing = expanded.
	private final Map<Integer, Boolean> expanded = new HashMap<>();

	private List<TabGroup> groups = Collections.emptyList();
	private long total;
	private int activeTab = -1;

	BankTreemapPanel(ItemManager itemManager, BankTreemapConfig config, Runnable onRefresh)
	{
		super();
		this.config = config;
		this.itemManager = itemManager;

		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel("Bank Treemap");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		totalLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		shownLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JButton refresh = new JButton("Refresh prices");
		refresh.setFocusable(false);
		refresh.addActionListener(e -> onRefresh.run());

		JPanel header = new JPanel(new GridLayout(0, 1, 0, 2));
		header.setOpaque(false);
		header.add(title);
		header.add(totalLabel);
		header.add(shownLabel);
		header.add(refresh);

		groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.Y_AXIS));
		groupsPanel.setOpaque(false);

		add(header, BorderLayout.NORTH);
		add(groupsPanel, BorderLayout.CENTER);
	}

	/** Swing thread only. */
	void update(List<TabGroup> groups, long total)
	{
		this.groups = groups;
		this.total = total;
		rebuildSections();
	}

	/** Called when the in-game bank tab changes. Swing thread only. */
	void setActiveTab(int tab)
	{
		activeTab = tab;
		if (config.followActiveTab())
		{
			// Tab 0 is the "all items" view, so show everything there
			for (int i = 0; i <= 9; i++)
			{
				expanded.put(i, tab == 0 || i == tab);
			}
		}
		rebuildSections();
	}

	/** Re-applies the current config to the cached snapshot. Swing thread only. */
	void refreshView()
	{
		rebuildSections();
	}

	private void rebuildSections()
	{
		groupsPanel.removeAll();

		long min = config.minStackValue();
		int shownStacks = 0;

		for (TabGroup g : groups)
		{
			List<StackNode> shown = new ArrayList<>();
			for (StackNode s : g.getStacks())
			{
				if (s.getValue() >= min)
				{
					shown.add(s);
				}
			}
			shown.sort(Comparator.comparingLong(StackNode::getValue).reversed());
			shownStacks += shown.size();

			final int index = g.getIndex();
			boolean isExpanded = expanded.getOrDefault(index, true);
			boolean isActive = activeTab > 0 && index == activeTab;
			TabSection section = new TabSection(g, shown, isExpanded, isActive, itemManager, config,
				state -> expanded.put(index, state));
			groupsPanel.add(section);
			groupsPanel.add(Box.createVerticalStrut(6));
		}

		if (total > 0)
		{
			totalLabel.setText("Bank value: " + TreemapComponent.formatGp(total));
			shownLabel.setText(shownStacks + " stacks above threshold");
		}

		groupsPanel.revalidate();
		groupsPanel.repaint();
	}
}
