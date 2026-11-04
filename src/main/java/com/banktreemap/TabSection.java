package com.banktreemap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** A collapsible section: a clickable header (tab name and value) over that tab's treemap. */
class TabSection extends JPanel
{
	private final JLabel arrow = new JLabel();
	private final TreemapComponent chart;
	private final Consumer<Boolean> onToggle;
	private boolean expanded;

	TabSection(TabGroup group, List<StackNode> shown, boolean expanded, boolean active,
		ItemManager itemManager, BankTreemapConfig config, Consumer<Boolean> onToggle)
	{
		this.expanded = expanded;
		this.onToggle = onToggle;

		setLayout(new BorderLayout(0, 2));
		setOpaque(false);

		JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setBackground(active ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(new EmptyBorder(4, 6, 4, 6));
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		arrow.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		updateArrow();

		JLabel name = new JLabel(group.getName() + "  " + TreemapComponent.formatGp(group.getTotal()));
		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(active ? ColorScheme.BRAND_ORANGE : Color.WHITE);

		JLabel count = new JLabel(shown.size() + " shown");
		count.setFont(FontManager.getRunescapeSmallFont());
		count.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		header.add(arrow, BorderLayout.WEST);
		header.add(name, BorderLayout.CENTER);
		header.add(count, BorderLayout.EAST);
		header.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				toggle();
			}
		});

		// Taller treemap for tabs with more stacks, within sane bounds
		int height = Math.max(150, Math.min(420, 120 + 6 * shown.size()));
		chart = new TreemapComponent(itemManager, config, height);
		chart.setStacks(shown, group.getTotal());
		chart.setVisible(expanded);

		add(header, BorderLayout.NORTH);
		add(chart, BorderLayout.CENTER);
	}

	private void toggle()
	{
		expanded = !expanded;
		updateArrow();
		chart.setVisible(expanded);
		onToggle.accept(expanded);
		revalidate();
		repaint();
	}

	private void updateArrow()
	{
		arrow.setText(expanded ? "▼" : "▶");
	}

	/** Keeps the section from being stretched taller than its content inside a BoxLayout. */
	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}
}
