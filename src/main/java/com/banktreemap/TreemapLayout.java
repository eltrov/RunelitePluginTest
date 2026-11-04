package com.banktreemap;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Squarified treemap layout (Bruls, Huizing, van Wijk). No RuneLite dependencies. */
final class TreemapLayout
{
	static final class Cell
	{
		final StackNode node;
		final Rectangle2D.Double rect;

		Cell(StackNode node, Rectangle2D.Double rect)
		{
			this.node = node;
			this.rect = rect;
		}
	}

	private TreemapLayout()
	{
	}

	/** Stacks must be sorted by value, descending, and every value must be positive. */
	static List<Cell> layout(List<StackNode> stacks, double x, double y, double w, double h)
	{
		List<Cell> cells = new ArrayList<>();
		int n = stacks.size();
		if (n == 0 || w <= 0 || h <= 0)
		{
			return cells;
		}

		double total = 0;
		for (StackNode s : stacks)
		{
			total += s.getValue();
		}
		if (total <= 0)
		{
			return cells;
		}

		double scale = w * h / total;
		double[] areas = new double[n];
		for (int i = 0; i < n; i++)
		{
			areas[i] = stacks.get(i).getValue() * scale;
		}

		double rx = x, ry = y, rw = w, rh = h;
		int start = 0;
		while (start < n && rw > 0 && rh > 0)
		{
			double side = Math.min(rw, rh);
			int end = start + 1;
			double sum = areas[start];
			double worst = worstRatio(areas[start], areas[start], sum, side);

			while (end < n)
			{
				double newSum = sum + areas[end];
				double newWorst = worstRatio(areas[start], areas[end], newSum, side);
				if (newWorst > worst)
				{
					break;
				}
				sum = newSum;
				worst = newWorst;
				end++;
			}

			if (rw >= rh)
			{
				// Vertical strip on the left edge
				double stripW = sum / rh;
				double cy = ry;
				for (int i = start; i < end; i++)
				{
					double ch = areas[i] / stripW;
					cells.add(new Cell(stacks.get(i), new Rectangle2D.Double(rx, cy, stripW, ch)));
					cy += ch;
				}
				rx += stripW;
				rw -= stripW;
			}
			else
			{
				// Horizontal strip along the top edge
				double stripH = sum / rw;
				double cx = rx;
				for (int i = start; i < end; i++)
				{
					double cw = areas[i] / stripH;
					cells.add(new Cell(stacks.get(i), new Rectangle2D.Double(cx, ry, cw, stripH)));
					cx += cw;
				}
				ry += stripH;
				rh -= stripH;
			}
			start = end;
		}
		return cells;
	}

	private static double worstRatio(double max, double min, double sum, double side)
	{
		double s2 = side * side;
		double sum2 = sum * sum;
		return Math.max(s2 * max / sum2, sum2 / (s2 * min));
	}
}
