package org.zzl.minegaming.GBAUtils;

public class BitConverter
{
	protected BitConverter(){}
	
	public static long ToInt32(byte[] bytez)
	{
		//AB CD EF 08 -> 08EFCDAB
		return (long)((bytez[0] & 0xFF) + ((bytez[1] & 0xFF) << 8) + ((bytez[2] & 0xFF) << 16) + ((bytez[3] & 0xFF) << 24));
	}
	
	public static int shortenPointer(long pointer)
	{
		return (int)(pointer & 0x1FFFFFF);
	}
	
	public static byte[] GetBytes(long i)
	{
		return new byte[] { (byte)((i & 0xFF000000) >> 24), (byte)((i & 0x00FF0000) >> 16), (byte)((i & 0x0000FF00) >> 8), (byte)((i & 0x000000FF)) };
	}
	
	public static int[] GetInts(long i)
	{
		return new int[] { (int)((i & 0xFF000000) >> 24), (int)((i & 0x00FF0000) >> 16), (int)((i & 0x0000FF00) >> 8), (int)((i & 0x000000FF)) };
	}
	
	public static byte[] ReverseBytes(byte[] bytes)
	{
		byte[] toReturn = new byte[bytes.length];
		for(int i = 0; i < bytes.length; i++)
		{
			toReturn[i] = bytes[bytes.length-1-i];
		}
		return toReturn;
	}

	public static byte[] GrabBytes(byte[] array, int offset, int length)
	{
		byte[] result = new byte[length];
		for(int i = 0; i < length; i++)
		{
			try
			{
				result[i] = array[offset+i];
			}
			catch(Exception e)
			{
				System.out.println("Tried to read outside of bounds! (" + offset + ", " + i + ")");
			}
		}
		return result;
	}
	
	public static int[] GrabBytesAsInts(byte[] array, int offset, int length)
	{
		if(length > array.length)
			length = array.length;
		int[] result = new int[length];
		for(int i = 0; i < length; i++)
		{
			result[i] = (array[offset+i] & 0xFF);
		}
		return result;
	}

	public static byte[] PutBytes(byte[] array, byte[] toPut, int offset)
	{
		for(int i = 0; i < toPut.length; i++)
		{
			array[offset+i] = toPut[i];
		}
		return array;
	}
	
	public static int[] ToInts(byte[] array)
	{
		return GrabBytesAsInts(array,0,array.length);
	}

	public static String toHexString(int b)
	{
		return toHexString(b,false);
	}
	
	public static String toHexString(int b, boolean spacing)
	{
		if(spacing)
			return String.format("%02X", Math.abs(b)); //Use absolute value to prevent negative bytes
		else
			return String.format("%X", Math.abs(b));
	}
	
	public static String toDwordString(int b, boolean spacing)
	{
		if(spacing)
			return String.format("%06X", Math.abs(b)); //Use absolute value to prevent negative bytes
		else
			return String.format("%X", Math.abs(b));
	}
	
	public static String byteToStringNoZero(int b)
	{
		if(b != 0)
			return String.format("%X", Math.abs(b));
		else
			return "";
	}

	
	public static byte[] toBytes(int[] data)
	{
		byte[] result = new byte[data.length];
		for(int i = 0; i < result.length; i++)
		{
			result[i] = (byte)(data[i]);
		}
		return result;
	}
}
