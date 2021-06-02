package us.plxhack.MEH.IO;

import org.zzl.minegaming.GBAUtils.BitConverter;
import org.zzl.minegaming.GBAUtils.DataStore;
import org.zzl.minegaming.GBAUtils.GBARom;
import org.zzl.minegaming.GBAUtils.ROMManager;

import us.plxhack.MEH.MapElements.SpriteExit;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BankLoader extends Thread implements Runnable
{
	private static GBARom rom;
	int tblOffs;
	JLabel lbl;
	JTree tree;
	DefaultMutableTreeNode node;
	private static int mapNamesPtr;
	public static ArrayList<Long>[] maps;
	public static ArrayList<Long> bankPointers = new ArrayList<Long>();
	public static boolean banksLoaded = false;
	public static HashMap<Integer,String> mapNames = new HashMap<Integer,String>();
	public static ArrayList<MapTreeNode> allMaps = new ArrayList<BankLoader.MapTreeNode>();
	
	public static void reset()
	{
		try
		{
			mapNamesPtr = rom.getPointerAsInt((int)DataStore.MapLabels);
			maps = new ArrayList[DataStore.NumBanks];
			bankPointers = new ArrayList<Long>();
			banksLoaded = false;
		}
		catch(Exception e)
		{
			
		}
	}

	public BankLoader(int tableOffset, GBARom rom, JLabel label, JTree tree, DefaultMutableTreeNode node)
	{
		BankLoader.rom = rom;
		tblOffs = (int) ROMManager.currentROM.getPointer(tableOffset);
	
		lbl = label;
		this.tree = tree;
		this.node = node;
		reset();
	}

	@Override
	public void run()
	{
		Date d = new Date();
		ArrayList<byte[]> bankPointersPre = rom.loadArrayOfStructuredData(tblOffs, DataStore.NumBanks, 4);
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		DefaultMutableTreeNode root = node;

		int bankNum = 0;
		for (byte[] b : bankPointersPre)
		{
			setStatus("Loading banks into tree...\t" + bankNum);
			bankPointers.add(BitConverter.ToInt32(b));
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(String.valueOf(bankNum));
			model.insertNodeInto(node, root, root.getChildCount());
			bankNum++;
		}

		int mapNum = 0;
		for(long l : bankPointers)
		{
			ArrayList<byte[]> preMapList = rom.loadArrayOfStructuredData((BitConverter.shortenPointer(l)), DataStore.MapBankSize[mapNum], 4);
			ArrayList<Long> mapList = new ArrayList<Long>();
			int miniMapNum = 0;
			for(byte[] b : preMapList)
			{
				setStatus("Loading maps into tree...\tBank " + mapNum + ", map " + miniMapNum);
				try
				{
					long dataPtr = BitConverter.ToInt32(b);
					mapList.add(dataPtr);
					int mapName = BitConverter.GrabBytesAsInts(rom.getData(), (int)((dataPtr - (8 << 24)) + 0x14), 1)[0];
					//mapName -= 0x58; //TODO: Add Jambo51's map header hack
					int mapNamePokePtr = 0;
					String convMapName = "";
					if(DataStore.EngineVersion==1)
					{
						if(!mapNames.containsKey(mapName))
						{
							mapNamePokePtr = rom.getPointerAsInt((int)DataStore.MapLabels+ ((mapName - 0x58) * 4)); //TODO use the actual structure
							convMapName = rom.readPokeText(mapNamePokePtr);
							mapNames.put(mapName, convMapName);
						}
						else
						{
							convMapName = mapNames.get(mapName);
						}
					}
					else if(DataStore.EngineVersion==0)//RSE
					{
						if(!mapNames.containsKey(mapName))
						{
							mapNamePokePtr = rom.getPointerAsInt((int)DataStore.MapLabels+ ((mapName*8)+ 4));
							convMapName = rom.readPokeText(mapNamePokePtr);
							mapNames.put(mapName, convMapName);
						}
						else
						{
							convMapName = mapNames.get(mapName);
						}
					}
					
					//System.out.println(convMapName + " (" + mapNum + "." + miniMapNum + ")");

					MapTreeNode node = new MapTreeNode(convMapName + " (" + mapNum + "." + miniMapNum + ")",mapNum,miniMapNum); //TODO: Pull PokeText from header
					
					allMaps.add(node);
					
					findNode(root,String.valueOf(mapNum)).add(node);
					miniMapNum++;
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			maps[mapNum] = mapList;
			mapNum++;
		}

		setStatus("Refreshing tree...");
		model.reload(root);
		for (int i = 0; i < tree.getRowCount(); i++)
		{
			TreePath path = tree.getPathForRow(i);
			if (path != null)
			{
				javax.swing.tree.TreeNode node = (javax.swing.tree.TreeNode) path.getLastPathComponent();
				String str = node.toString();
				DefaultTreeModel models = (DefaultTreeModel) tree.getModel();
				models.valueForPathChanged(path, str);
			}
		}
		banksLoaded = true;
		
		Date eD = new Date();

        double loadTime = eD.getTime() - d.getTime();

		setStatus("Banks loaded in " + loadTime + "ms" + (loadTime < 1000 ? "! :DDD" : "."));

		// Debug
		for (MapTreeNode mtn : allMaps) 
		{
			System.out.println("Map name = " + mtn.mapName + " " + mtn.bank + ", " + mtn.map);
			
			long offset = maps[mtn.bank].get(mtn.map);
			Map map = new Map(ROMManager.getActiveROM(), (int) (offset));
			
			ArrayList<ArrayList<String>> sMap = GetMapCol(map);
			String connections = GetConnections(map);
			String exits = GetExits(map);
			SaveMap(sMap, connections, exits, mtn.mapName);
		}
		// Debug
	}
	

	/**
	 * Gets the exits for a given map
	 * @param map
	 * @return
	 */
	public String GetExits(Map map)
	{
		String exits = "Exits;";

		for (SpriteExit n : map.mapExitManager.mapExits)
		{
			try {
				MapTreeNode mtn = BankLoader.GetTreeNodeFromBankMap(n.bBank, n.bMap);
				exits += mtn.mapName + ";";
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return exits;
	}

	/**
	 * Gets connections for the given map
	 * @param map
	 * @return
	 */
	public String GetConnections(Map map)
	{
		String connections = "Connections;";

		int dC = 0, uC = 0, lC = 0, rC = 0;
		
		for(Connection c : map.mapConnections.aConnections)
		{
			if(c == null)
				continue;
			if(c.lType == 0x1)
				dC++;
			else if (c.lType == 0x2)
				uC++;
			else if(c.lType == 0x3)
				lC++;
			else if(c.lType == 0x4)
				rC++;

			//TODO Diving maps!
		}	
		
		Map[] up = new Map[uC];
		Map[] down = new Map[dC];
		Map[] left = new Map[lC];
		Map[] right = new Map[rC];
		
		dC = 0;
		uC = 0; 
		lC = 0;
		rC = 0;
		
		for(Connection c : map.mapConnections.aConnections)
		{
			if(c == null)
				continue;
			if(c.lType == 0x1)
			{
				down[dC] = new Map(ROMManager.currentROM, c.bBank & 0xFF, c.bMap & 0xFF);
				
				try {
					MapTreeNode mtn = BankLoader.GetTreeNodeFromBankMap(c.bBank & 0xFF, c.bMap & 0xFF);
					connections += "down:" + mtn.mapName + ";";
				} catch (Exception e) {
					e.printStackTrace();
				}

				dC++;
			}
			else if (c.lType == 0x2)
			{
				up[uC] = new Map(ROMManager.currentROM, c.bBank & 0xFF, c.bMap & 0xFF);
				
				try {
					MapTreeNode mtn = BankLoader.GetTreeNodeFromBankMap(c.bBank & 0xFF, c.bMap & 0xFF);
					connections += "up:" + mtn.mapName + ";";
				} catch (Exception e) {
					e.printStackTrace();
				}

				uC++;
			}
			else if(c.lType == 0x3)
			{
				left[lC] = new Map(ROMManager.currentROM, c.bBank & 0xFF, c.bMap & 0xFF);
				
				try {
					MapTreeNode mtn = BankLoader.GetTreeNodeFromBankMap(c.bBank & 0xFF, c.bMap & 0xFF);
					connections += "left:" + mtn.mapName + ";";
				} catch (Exception e) {
					e.printStackTrace();
				}

				lC++;
			}
			else if(c.lType == 0x4)
			{
				right[rC] = new Map(ROMManager.currentROM, c.bBank & 0xFF, c.bMap & 0xFF);

				try {
					MapTreeNode mtn = BankLoader.GetTreeNodeFromBankMap(c.bBank & 0xFF, c.bMap & 0xFF);
					connections += "right:" + mtn.mapName + ";";
				} catch (Exception e) {
					e.printStackTrace();
				}

				rC++;
			}
		}	

		return connections;
	}

	/**
	 * Returns the tree node with the given bank and map
	 * @param bank
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public static MapTreeNode GetTreeNodeFromBankMap(int bank, int map) throws Exception
	{
		long offset = maps[bank].get(map);
		for (MapTreeNode mtn : allMaps) {
			long mOffset = maps[mtn.bank].get(mtn.map);
			if (mOffset == offset) return mtn;
		}
		throw new Exception("Node not found");
	}

	/**
	 * Gets the collision data of the given map
	 * @param map
	 * @return a list of list of strings containing the tile meta data
	 */
	private ArrayList<ArrayList<String>> GetMapCol(Map map)
	{
		ArrayList<ArrayList<String>> sMap = new ArrayList<ArrayList<String>>();
		for (int y = 0; y < map.getMapData().mapHeight; y++) {
			ArrayList<String> row = new ArrayList<String>();
			for (int x = 0; x < map.getMapData().mapWidth; x++) {
				int TileMeta=(map.getMapTileData().getTile(x, y).getMeta());
				row.add(Integer.toString(TileMeta));
			}
			sMap.add(row);
		}
		return sMap;
	}

	/**
	 * Saves maps collision and connections
	 * @param sMap
	 * @param fileName
	 */
	private void SaveMap(ArrayList<ArrayList<String>> sMap, String connections, String exits, String fileName)
	{
		PrintWriter out;
		try {
			out = new PrintWriter("collision_map/" + fileName + ".txt");
			
			out.println(connections);
			out.println(exits);

			for (ArrayList<String> row : sMap) {
				for (String string : row) {
					out.print(string + " ");
				}
				out.println("");
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setStatus(String status)
	{
		lbl.setText(status);
	}

	private TreePath findPath(DefaultMutableTreeNode root, String s)
	{
		@SuppressWarnings("unchecked")
		Enumeration<TreeNode> e = root.depthFirstEnumeration();
		while (e.hasMoreElements())
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			if (node.toString().equalsIgnoreCase(s))
			{
				return new TreePath(node.getPath());
			}
		}
		return null;
	}
	
	private DefaultMutableTreeNode findNode(DefaultMutableTreeNode root, String s)
	{
		@SuppressWarnings("unchecked")
		Enumeration<TreeNode> e = root.depthFirstEnumeration();
		while (e.hasMoreElements())
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			if (node.toString().equalsIgnoreCase(s))
			{
				return node;
			}
		}
		return null;
	}
	
	public class MapTreeNode extends DefaultMutableTreeNode
	{
		public int bank;
		public int map;
		public String mapName;

		public MapTreeNode (String name, int bank2, int map2)
		{
			super(name);
			mapName = name;
			bank = bank2;
			map = map2;
		}
	}
}
