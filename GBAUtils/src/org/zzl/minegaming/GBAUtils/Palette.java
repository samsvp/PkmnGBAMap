package org.zzl.minegaming.GBAUtils;

import java.awt.Color;

public class Palette
{
	private Color[] colors;
	private byte[] reds;
	private byte[] greens;
	private byte[] blues;
 	public Palette(GBAImageType type, int[] data)
	{
		if(type == GBAImageType.c16)
		{
			colors = new Color[16];
			reds = new byte[16];
			greens = new byte[16];
			blues = new byte[16];
		}
		else
		{
			colors = new Color[256];
			reds = new byte[256];
			greens = new byte[256];
			blues = new byte[256];
		}
		
		for(int i = 0; i < data.length; i++)
		{
			int color = data[i] + (data[i + 1] << 8);
			int r = (color & 0x1F) << 3;
			int g = (color & 0x3E0) >> 2;
			int b = (color & 0x7C00) >> 7;
			reds[i / 2] = (byte)r;
			greens[i / 2] = (byte)g;
			blues[i / 2] = (byte)b;
			colors[i / 2] = new Color(r,g,b);
			i++;
		}
	}
 	
 	public Palette(GBAImageType type, byte[] data)
	{
		this(type,BitConverter.GrabBytesAsInts(data,0,data.length));
	}
 	
 	public Palette(GBAImageType type, GBARom rom, int offset)
	{
		this(type,BitConverter.GrabBytes(rom.getData(), offset, (type == GBAImageType.c16 ? 32 : 512)));
	}
	
	public Color getIndex(int i)
	{
		if(i > colors.length)
		{
			System.out.println("WARNING: Program attempted to grab color outside of palette range! Returning Color.BLACK...");
			return Color.black;
		}
		
		return colors[i];
	}
	
	public int getIndexAsInt(int i)
	{
		if(i > colors.length)
		{
			System.out.println("WARNING: Program attempted to grab color outside of palette range! Returning Color.BLACK...");
			return Color.black.getRGB();
		}
		
		return colors[i].getRed() + (colors[i].getGreen() << 8) + (colors[i].getBlue() << 16);
	}
	
	public byte getRedValue(int i)
	{
		return reds[i];
	}
	
	public byte getGreenValue(int i)
	{
		return greens[i];
	}
	
	public byte getBlueValue(int i)
	{
		return blues[i];
	}
	
	public byte[] getReds()
	{
		return reds;
	}
	
	public byte[] getGreens()
	{
		return greens;
	}
	
	public byte[] getBlues()
	{
		return blues;
	}

	public void setReds(byte[] reds)
	{
		this.reds = reds;
		refreshColors();
	}
	
	public void setGreens(byte[] greens)
	{
		this.greens = greens;
		refreshColors();
	}
	
	public void setBlues(byte[] blues)
	{
		this.blues = blues;
		refreshColors();
	}
	
	public void setColors(byte[] reds, byte[] greens, byte[] blues)
	{
		this.reds = reds;
		this.blues = blues;
		this.greens = greens;
		refreshColors();
	}
	
	public void refreshColors()
	{
		for(int i = 0; i < 16; i++)
			colors[i] = new Color(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
	}
	
	public int getSize()
	{
		return colors.length;
	}

	public Palette xorColor(Color c)
	{
		for (int i = 0; i < 16; i++)
		{
			Color end = blend(c, colors[i]);

			byte red = (byte) end.getRed();
			byte blue = (byte) end.getBlue();
			byte green = (byte) end.getGreen();

			reds[i] = red;
			greens[i] = green;
			blues[i] = blue;
		}
		
		refreshColors();
		return this;
	}
	
	private Color blend(Color c0, Color c1) 
	{
	    double totalAlpha = c0.getAlpha() + c1.getAlpha();
	    double weight0 = c0.getAlpha() / totalAlpha;
	    double weight1 = c1.getAlpha() / totalAlpha;

	    double r = weight0 * c0.getRed() + weight1 * c1.getRed();
	    double g = weight0 * c0.getGreen() + weight1 * c1.getGreen();
	    double b = weight0 * c0.getBlue() + weight1 * c1.getBlue();
	    double a = Math.max(c0.getAlpha(), c1.getAlpha());

	    return new Color((int) r, (int) g, (int) b, (int) a);
	}
	
	public static Color[] gradient(Color c0, Color c1, int steps)
	{
		Color[] colors = new Color[steps];
		for(int i = 0; i < steps; i++ )
		{
			float n = (float) i / (float) (steps - 1);
			int r = (int) ((float) c0.getRed() * (1.0f - n) + (float) c1.getRed() * n);
			int g = (int) ((float) c0.getGreen() * (1.0f - n) + (float) c1.getGreen() * n);
			int b = (int) ((float) c0.getBlue() * (1.0f - n) + (float) c1.getBlue() * n);
			int a = (int) ((float) c0.getAlpha() * (1.0f - n) + (float) c1.getAlpha() * n);
			colors[i] = new Color(r,g,b,a);
		}
		return colors;
	}
	
 	public void save(GBARom rom)
	{
		byte[] data = new byte[0x20];
		for(int i = 0; i < 16; i++)
		{
			int color = 0;
			color |= ((reds[i] >> 3) & 0x1F);
			color |= (((greens[i] >> 3) & 0x1F) << 5);
			color |= (((blues[i] >> 3) & 0x3F) << 10);
			color &= 0x7FFF;
			
			data[(i*2)+1] = (byte)((color & 0xFF00) >> 8);
			data[(i*2)] = (byte)(color & 0xFF);
		}
		rom.writeBytes(data);
	}
}
