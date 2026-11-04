package com.banktreemap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
	private final TreemapComponent chart;
	private final JLabel totalLabel = new JLabel("Bank value: -");
	private final JLabel shownLabel = new JLabel("Open your bank to scan it");

	private List<StackNode> all = Collections.emptyList();
	private long total;

	BankTreemapPanel(ItemManager itemManager, BankTreemapConfig config, Runnable onRefresh)
	{
		super();
		this.config = config;
		this.chart = new TreemapComponent(itemManager, config);

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

		add(header, BorderLayout.NORTH);
		add(chart, BorderLayout.CENTER);
	}

	/** Swing thread only. */
	void update(List<StackNode> all, long total)
	{
		this.all = all;
		this.total = total;
		refreshView();
	}

	/** Re-applies the current config to the cached snapshot. Swing thread only. */
	void refreshView()
	{
		long min = config.minStackValue();
		List<StackNode> shown = new ArrayList<>();
		long shownValue = 0;
		for (StackNode s : all)
		{
			if (s.getValue() >= min)
			{
				shown.add(s);
				shownValue += s.getValue();
			}
		}
		shown.sort(Comparator.comparingLong(StackNode::getValue).reversed());

		if (total > 0)
		{
			totalLabel.setText("Bank value: " + TreemapComponent.formatGp(total));
			shownLabel.setText(String.format("%d stacks shown (%.0f%%)", shown.size(), 100.0 * shownValue / total));
		}
		chart.setStacks(shown, total);
	}
}
