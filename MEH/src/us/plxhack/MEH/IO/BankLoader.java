package us.plxhack.MEH.IO;

import org.zzl.minegaming.GBAUtils.BitConverter;
import org.zzl.minegaming.GBAUtils.DataStore;
import org.zzl.minegaming.GBAUtils.GBARom;
import org.zzl.minegaming.GBAUtils.ROMManager;

import us.plxhack.MEH.MapElements.SpriteExit;
import us.plxhack.MEH.MapElements.SpriteNPC;
import us.plxhack.MEH.MapElements.SpriteSign;
import us.plxhack.MEH.MapElements.Trigger;

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
			String events = GetEvents(map);
			SaveMap(sMap, connections, events, mtn.mapName);
		}
		// Debug
	}
	

	public String GetEvents(Map map) {
		String events = "{\n";
		
		events += "\t" + GetExits(map) + ",\n";
		events += "\t" + GetNPCs(map) + ",\n";
		events += "\t" + GetSigns(map) + ",\n";
		events += "\t" + GetTriggers(map) + ",\n";

		return events + "\n}";
	}


	/**
	 * Returns the NPCs from a given map
	 * @param map
	 * @return
	 */
	public String GetNPCs(Map map)
	{
		String NPCs = "\n\t\t'NPCs':[\n";

		for (SpriteNPC npc : map.mapNPCManager.mapNPCs) {
			NPCs += "\t\t\t{'is_trainer':" + npc.bIsTrainer + ",'b1':" + npc.bBehavior1 + 
				",'b2':" + npc.bBehavior2 + ",'sprite':'" + npc.bSpriteSet + "','x':" + npc.bX +
				",'y':" + npc.bY + "},\n";
		}

		NPCs += "\t\t]";
		return NPCs;
	}

	/**
	 * Gets the signs for a given map
	 * @param map
	 * @return
	 */
	public String GetTriggers(Map map)
	{
		String triggers = "\n\t\t'triggers':[\n";

		for (Trigger trigger: map.mapTriggerManager.mapTriggers) {
			triggers += "\t\t\t{'x':" + trigger.bX + ",'y':" + trigger.bY + 
				",'flag':" + trigger.hFlagCheck + ",'scriptPointer':" + trigger.pScript + "},\n";
		}

		triggers += "\t\t]";
		return triggers;
	}


	/**
	 * Gets the signs for a given map
	 * @param map
	 * @return
	 */
	public String GetSigns(Map map)
	{
		String signs = "\n\t\t'signs':[\n";

		for (SpriteSign sign: map.mapSignManager.mapSigns) {
			signs += "\t\t\t{'x':" + sign.bX + ",'y':" + sign.bY + "},\n";
		}

		signs += "\t\t]";
		return signs;
	}

	/**
	 * Gets the exits for a given map
	 * @param map
	 * @return
	 */
	public String GetExits(Map map)
	{
		String exits = "\n\t\t'exits':[";

		for (SpriteExit n : map.mapExitManager.mapExits)
		{
			try {
				MapTreeNode mtn = BankLoader.GetTreeNodeFromBankMap(n.bBank, n.bMap);
				exits += "{'name':'" + mtn.mapName + "','x':" +  n.bX + ",'y':" + n.bY + "},";
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		exits +=  "\t\t]";
		return exits;
	}

	/**
	 * Gets connections for the given map
	 * @param map
	 * @return
	 */
	public String GetConnections(Map map)
	{
		String connections = "[\n";

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
					connections += "\t\t\t{'down':'" + mtn.mapName + "'},\n";
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
					connections += "\t\t\t{'up':'" + mtn.mapName + "'},\n";
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
					connections += "\t\t\t{'left':'" + mtn.mapName + "'},\n";
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
					connections += "\t\t\t{'right':'" + mtn.mapName + "'},\n";
				} catch (Exception e) {
					e.printStackTrace();
				}

				rC++;
			}
		}	

		connections += "\t\t]";
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
	private void SaveMap(ArrayList<ArrayList<String>> sMap, String connections, String events, String fileName)
	{
		PrintWriter out;
		try {
			out = new PrintWriter("collision_map/" + fileName + ".txt");
			
			out.println("connections=" + connections + ",");
			out.println("events=" + events + ",");

			out.println("partial_map=[");
			for (ArrayList<String> row : sMap) {
				out.print("\t[");
				for (String string : row) {
					out.print(string + ", ");
				}
				out.println("],");
			}
			out.println("]");
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
