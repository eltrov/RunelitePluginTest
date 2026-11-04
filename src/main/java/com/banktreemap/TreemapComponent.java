package com.banktreemap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

class TreemapComponent extends JComponent
{
	private static final int GAP = 1;

	private final ItemManager itemManager;
	private final BankTreemapConfig config;
	private final Map<Integer, AsyncBufferedImage> icons = new HashMap<>();

	private List<StackNode> stacks = Collections.emptyList();
	private List<TreemapLayout.Cell> cells = Collections.emptyList();
	private long bankTotal;
	private int laidOutW = -1;
	private int laidOutH = -1;

	TreemapComponent(ItemManager itemManager, BankTreemapConfig config)
	{
		this.itemManager = itemManager;
		this.config = config;
		setToolTipText("");
		setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 10, 520));
	}

	/** Stacks must already be filtered and sorted by value, descending. */
	void setStacks(List<StackNode> stacks, long bankTotal)
	{
		this.stacks = stacks;
		this.bankTotal = bankTotal;
		this.laidOutW = -1;

		icons.clear();
		for (StackNode s : stacks)
		{
			AsyncBufferedImage img = itemManager.getImage(s.getItemId());
			img.onLoaded(this::repaint);
			icons.put(s.getItemId(), img);
		}
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g0)
	{
		Graphics2D g = (Graphics2D) g0.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setFont(FontManager.getRunescapeSmallFont());

		int w = getWidth();
		int h = getHeight();
		g.setColor(ColorScheme.DARKER_GRAY_COLOR);
		g.fillRect(0, 0, w, h);

		if (stacks.isEmpty())
		{
			String[] lines = bankTotal > 0
				? new String[]{"Nothing above", "the threshold"}
				: new String[]{"Open your bank", "to scan it"};
			g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			FontMetrics fm = g.getFontMetrics();
			int y = h / 2 - fm.getHeight();
			for (String line : lines)
			{
				g.drawString(line, (w - fm.stringWidth(line)) / 2, y);
				y += fm.getHeight();
			}
			g.dispose();
			return;
		}

		if (w != laidOutW || h != laidOutH)
		{
			cells = TreemapLayout.layout(stacks, 0, 0, w, h);
			laidOutW = w;
			laidOutH = h;
		}

		double logMax = Math.log(stacks.get(0).getValue());
		double logMin = Math.log(stacks.get(stacks.size() - 1).getValue());

		for (TreemapLayout.Cell cell : cells)
		{
			paintCell(g, cell, logMin, logMax);
		}
		g.dispose();
	}

	private void paintCell(Graphics2D g, TreemapLayout.Cell cell, double logMin, double logMax)
	{
		int rx = (int) Math.round(cell.rect.x);
		int ry = (int) Math.round(cell.rect.y);
		int rw = Math.max(1, (int) Math.round(cell.rect.x + cell.rect.width) - rx - GAP);
		int rh = Math.max(1, (int) Math.round(cell.rect.y + cell.rect.height) - ry - GAP);

		// Low value = blue, high value = orange/red, on a log scale
		double t = logMax > logMin ? (Math.log(cell.node.getValue()) - logMin) / (logMax - logMin) : 1.0;
		float hue = (float) (0.58 - 0.55 * t);

		if (config.cushionShading())
		{
			Color light = Color.getHSBColor(hue, 0.55f, 1.0f);
			Color dark = Color.getHSBColor(hue, 0.95f, 0.45f);
			Point2D center = new Point2D.Float(rx + rw / 2f, ry + rh / 2f);
			float radius = Math.max(rw, rh) * 0.75f;
			g.setPaint(new RadialGradientPaint(center, radius, new float[]{0f, 1f}, new Color[]{light, dark}));
		}
		else
		{
			g.setColor(Color.getHSBColor(hue, 0.85f, 0.80f));
		}
		g.fillRect(rx, ry, rw, rh);

		if (config.showLabels())
		{
			paintLabel(g, cell.node, rx, ry, rw, rh);
		}
	}

	private void paintLabel(Graphics2D g, StackNode node, int rx, int ry, int rw, int rh)
	{
		AsyncBufferedImage img = icons.get(node.getItemId());
		int iw = img != null ? img.getWidth() : 0;
		int ih = img != null ? img.getHeight() : 0;
		boolean iconFits = img != null && rw >= iw + 4 && rh >= ih + 4;

		String txt = formatGp(node.getValue());
		FontMetrics fm = g.getFontMetrics();
		int spareH = rh - 2 - (iconFits ? ih + 2 : 0);
		boolean textFits = fm.stringWidth(txt) <= rw - 4 && fm.getHeight() <= spareH;

		if (!iconFits && !textFits)
		{
			return;
		}

		int blockH = (iconFits ? ih : 0) + (iconFits && textFits ? 2 : 0) + (textFits ? fm.getHeight() : 0);
		int top = ry + (rh - blockH) / 2;

		if (iconFits)
		{
			g.drawImage(img, rx + (rw - iw) / 2, top, null);
			top += ih + 2;
		}
		if (textFits)
		{
			int tx = rx + (rw - fm.stringWidth(txt)) / 2;
			int ty = top + fm.getAscent();
			g.setColor(Color.BLACK);
			g.drawString(txt, tx + 1, ty + 1);
			g.setColor(Color.WHITE);
			g.drawString(txt, tx, ty);
		}
	}

	@Override
	public String getToolTipText(MouseEvent e)
	{
		for (TreemapLayout.Cell cell : cells)
		{
			if (cell.rect.contains(e.getX(), e.getY()))
			{
				StackNode n = cell.node;
				double pct = bankTotal > 0 ? 100.0 * n.getValue() / bankTotal : 0;
				return String.format("<html>%s x %,d<br>%,d gp (%.1f%% of bank)</html>",
					n.getName(), n.getQuantity(), n.getValue(), pct);
			}
		}
		return null;
	}

	static String formatGp(long v)
	{
		if (v >= 1_000_000_000L)
		{
			return String.format("%.2fB", v / 1_000_000_000.0);
		}
		if (v >= 1_000_000L)
		{
			return String.format("%.1fM", v / 1_000_000.0);
		}
		if (v >= 1_000L)
		{
			return (v / 1_000L) + "K";
		}
		return Long.toString(v);
	}
}
