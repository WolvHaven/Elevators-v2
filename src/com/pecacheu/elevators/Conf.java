//Elevators v2, ©2020 Pecacheu. Licensed under GNU GPL 3.0

package com.pecacheu.elevators;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.TreeMap;

import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class Conf {
	//Global Variables:
	public static TreeMap<String,Elevator> elevators = new TreeMap<String,Elevator>();
	public static ChuList<ChuList<FallingBlock>> movingFloors = new ChuList<ChuList<FallingBlock>>();
	public static BukkitTask CLTMR = null; public static Main plugin = null;
	public static final Object API_SYNC = new Object();
	
	//Global Config Settings:
	public static String TITLE, CALL, ERROR, L_ST, L_END, NODOOR, MSG_GOTO_ST, MSG_GOTO_END, MSG_CALL, NOMV, M_ATLV, ATLV,
	C_UP, UP, C_DOWN, DOWN; public static int RADIUS_MAX, MOVE_RES, DOOR_HOLD, SAVE_INT; public static ChuList<String> BLOCKS;
	public static ChuList<Integer> BL_SPEED; public static Material DOOR_SET; public static boolean DEBUG = false;
	
	//Constants:
	public static final String MSG_NEW_CONF = "§e[Elevators] §bCould not load config. Creating new config file...";
	public static final String MSG_ERR_CONF = "§e[Elevators] §cError while loading config!";
	public static final String MSG_DBG = "§e[Elevators] §r";
	public static final String MSG_ERR_ST = "§e[Elevators] §eError in §b";
	public static final String MSG_ERR_MID = "§e: §c";
	public static final String MSG_DEL_ST = "§e[Elevators] §b";
	public static final String MSG_DEL_END = " §esaved elevators were deleted because they were invalid!";
	public static final String CONFIG_PATH = "plugins/Elevators/config.yml";
	public static final Material AIR = Material.AIR;
	
	public static MemoryConfiguration defaults = new MemoryConfiguration();
	public static void initDefaults(Main _plugin) {
		defaults.set("debug", false);
		defaults.set("title", "&1[&3Elevator&1]"); defaults.set("call", "&1[&3Call&1]"); defaults.set("error", "[&4???&r]");
		defaults.set("selStart", "&8> &5"); defaults.set("selEnd", " &8<"); defaults.set("noDoor", "&1[nodoor]");
		defaults.set("msgGotoStart", "&eTraveling to &a"); defaults.set("msgGotoEnd", "&e.");
		defaults.set("msgCall", "&eCalling elevator."); defaults.set("noMove",   "&4⦿  ⦿  ⦿  ⦿  ⦿  ⦿");
		defaults.set("mAtLevel", "&3⦿  ⦿  ⦿  ⦿  ⦿  ⦿"); defaults.set("atLevel",  "&2⦿  ⦿  ⦿  ⦿  ⦿  ⦿");
		defaults.set("callUp",   "&3▲  ▲  ▲  ▲  ▲  ▲"); defaults.set("up",       "&4△  △  △  △  △  △");
		defaults.set("callDown", "&3▼  ▼  ▼  ▼  ▼  ▼"); defaults.set("down",     "&4▽  ▽  ▽  ▽  ▽  ▽");
		defaults.set("floorMaxRadius", 8); defaults.set("updateDelay", 50); defaults.set("doorHoldTime", 4000);
		defaults.set("saveInterval", 15); defaults.set("doorBlock", "GLASS_PANE"); plugin = _plugin;
	}
	
	//------------------- Config Save & Load Functions -------------------
	
	public static boolean saveConfig() {
		File path = new File(CONFIG_PATH); String data = "";
		
		//If Not Found, Create New Config File:
		if(!path.exists()) data = newConfig(path); else {
			try { //Read Current Config File:
				FileReader file = new FileReader(path); int p = 0;
				while(p < 3000) { int read = file.read(); if(read < 0 || read >= 65535) break; data += fromCharCode(read); p++; } file.close();
			} catch(IOException e) { err("saveConfig", "IOException while reading file!"); return false; }
		}
		
		if(data.length() == 0) { err("saveConfig", "Save data string empty!"); return false; }
		
		//Separate And Overwrite BLOCKS Section:
		int bPos = data.lastIndexOf("blockList:"); String bStr = data.substring(bPos);
		
		int bEnd = 0, nl = 0; while(bEnd < 600) {
			if(nl==1 && bStr.charAt(bEnd) != ' ') nl = 2;
			if(nl==2) { if(bStr.charAt(bEnd) == '\n') break; }
			else nl = (bStr.charAt(bEnd) == '\n') ? 1 : 0; bEnd++;
		} bEnd += bPos;
		
		YamlConfiguration bConf = new YamlConfiguration(); ConfigurationSection bList = bConf.createSection("blockList");
		for(int i=0,l=BLOCKS.length; i<l; i++) bList.set(BLOCKS.get(i), BL_SPEED.get(i));
		data = data.substring(0,bPos)+bConf.saveToString()+data.substring(bEnd);
		
		//Separate And Overwrite Elevators Section:
		int sPos = data.lastIndexOf("elevators:"); data = data.substring(0,sPos);
		YamlConfiguration eConf = new YamlConfiguration();
		
		ConfigurationSection eList = eConf.createSection("elevators"); Object[] eKeys = elevators.keySet().toArray();
		
		//Generate Compressed Elevator Data:
		for(int i=0,l=eKeys.length; i<l; i++) eList.set((String)eKeys[i], elevators.get(eKeys[i]).toSaveData());
		
		//Append New Data And Save File:
		String eStr = eConf.saveToString(); eStr.substring(0,eStr.length()-1); data += eStr;
		Writer file; try { file = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8")); file.write(data); file.close(); }
		catch (IOException e) { err("saveConfig", "IOException while saving file!"); return false; } return true;
	}
	
	private static String newConfig(File file) {
		Bukkit.getServer().getConsoleSender().sendMessage(MSG_NEW_CONF);
		Path path = file.toPath(); try { java.nio.file.Files.createDirectories(path.getParent()); }
		catch (IOException e) { err("newConfig", "IOException while creating directories!"); return ""; }
		return unpackFile("config.yml", file);
	}
	
	public static Object loadConfig() { try {
		File path = new File(CONFIG_PATH); YamlConfiguration config; boolean pathFound = path.exists();
		if(pathFound) config = YamlConfiguration.loadConfiguration(path); else config = new YamlConfiguration();
		
		config.setDefaults(defaults); movingFloors = new ChuList<ChuList<FallingBlock>>(); CLTMR = null;
		
		//Load Global Settings:
		DEBUG = config.getBoolean("debug");
		TITLE = c(config.getString("title"));
		CALL = c(config.getString("call"));
		ERROR = c(config.getString("error"));
		L_ST = c(config.getString("selStart"));
		L_END = c(config.getString("selEnd"));
		NODOOR = c(config.getString("noDoor"));
		
		MSG_GOTO_ST = c(config.getString("msgGotoStart"));
		MSG_GOTO_END = c(config.getString("msgGotoEnd"));
		MSG_CALL = c(config.getString("msgCall"));
		
		MSG_GOTO_ST = c(config.getString("msgGotoStart"));
		MSG_GOTO_END = c(config.getString("msgGotoEnd"));
		MSG_CALL = c(config.getString("msgCall"));
		
		NOMV = c(StringEscapeUtils.unescapeJava(config.getString("noMove")));
		M_ATLV = c(StringEscapeUtils.unescapeJava(config.getString("mAtLevel")));
		ATLV = c(StringEscapeUtils.unescapeJava(config.getString("atLevel")));
		C_UP = c(StringEscapeUtils.unescapeJava(config.getString("callUp")));
		UP = c(StringEscapeUtils.unescapeJava(config.getString("up")));
		C_DOWN = c(StringEscapeUtils.unescapeJava(config.getString("callDown")));
		DOWN = c(StringEscapeUtils.unescapeJava(config.getString("down")));
		
		RADIUS_MAX = config.getInt("floorMaxRadius");
		MOVE_RES = config.getInt("updateDelay");
		DOOR_HOLD = config.getInt("doorHoldTime");
		SAVE_INT = config.getInt("saveInterval");
		DOOR_SET = Material.valueOf(config.getString("doorBlock"));
		
		ConfigurationSection bList = config.getConfigurationSection("blockList");
		BLOCKS = new ChuList<String>(); BL_SPEED = new ChuList<Integer>();
		if(bList != null) {
			Object[] bKeys = bList.getKeys(false).toArray();
			for(int b=0,g=bKeys.length; b<g; b++) { //Remove any items from block list that aren't solid blocks:
				Material mat = Material.valueOf((String)bKeys[b]);
				if(mat != null && mat.isSolid()) { BLOCKS.push((String)bKeys[b]); BL_SPEED.push(bList.getInt((String)bKeys[b])); }
			}
		}
		
		if(BLOCKS.length < 1) { //Load Default Block Settings:
			BLOCKS = new ChuList<String>("IRON_BLOCK", "GOLD_BLOCK", "EMERALD_BLOCK", "DIAMOND_BLOCK", "LEGACY_STAINED_GLASS", "GLASS");
			BL_SPEED = new ChuList<Integer>(8, 10, 12, 15, 5, 4);
		}
		
		//Load Compressed Elevator Data:
		elevators.clear(); ConfigurationSection eList = config.getConfigurationSection("elevators"); Integer eCnt = 0;
		if(eList != null) { Object[] eKeys = eList.getKeys(false).toArray(); for(int i=0,l=eKeys.length; i<l; i++) {
			Elevator elev = Elevator.fromSaveData(eList.getStringList((String)eKeys[i]));
			if(elev != null) elevators.put((String)eKeys[i], elev); else { eCnt++; err("loadConfig", "fromSaveData returned null at ID "+eKeys[i]); }
		}}
		return pathFound?eCnt:"NOCONF";
	} catch(Exception e) { err("loadConfig", "Caught Exception: "+e.getMessage()); return e.getMessage(); }}
	
	public static void doConfigLoad(CommandSender sender) {
		Object err = loadConfig();
		if(err == "NOCONF") saveConfig(); //Create New Config.
		else if(err instanceof Integer) { //Loaded Config Successfully.
			if((Integer)err > 0) {
				String delMsg = MSG_DEL_ST+(Integer)err+MSG_DEL_END;
				Bukkit.getServer().getConsoleSender().sendMessage(delMsg);
				if(sender instanceof Player) sender.sendMessage(delMsg);
			} saveConfig();
		} else if(err!=null) { //Error While Loading Config.
			Bukkit.getServer().getConsoleSender().sendMessage(MSG_ERR_CONF+"\n"+err);
			if(sender instanceof Player) sender.sendMessage(MSG_ERR_CONF);
		}
		if(sender != null) sender.sendMessage("§aElevators Plugin Reloaded!");
	} public static void doConfigLoad() { doConfigLoad(null); }
	
	//-------------------  Useful Functions -------------------
	
	public static String locToString(Location loc) {
		return loc.getWorld().getName()+"-"+(int)loc.getX()+"-"+(int)loc.getZ();
	}
	
	public static Location locFromString(String str) {
		String[] data = str.split("-"); World world = plugin.getServer().getWorld(data[0]);
		if(world==null) { err("locFromString", "World '"+data[0]+"' not found!"); return null; }
		return new Location(world, Integer.parseInt(data[1]), 0, Integer.parseInt(data[2]));
	}
	
	//Open/close doors & gates:
	public static void setDoor(Block b, boolean on) {
		if(b.getBlockData() instanceof Openable) {
			Openable d = (Openable)b.getBlockData();
			if(d instanceof Door && ((Door)d).getHalf() != Bisected.Half.BOTTOM) return;
			d.setOpen(on); b.setBlockData(d); playDoorSound(b,on);
		}
	}
	
	//Play door sound effect:
	private static void playDoorSound(Block b, boolean on) {
		Location l = b.getLocation(); Openable d = (Openable)b.getBlockData(); Sound s;
		if(d instanceof Gate) s = on?Sound.BLOCK_FENCE_GATE_OPEN:Sound.BLOCK_FENCE_GATE_CLOSE;
		else if(d instanceof TrapDoor) {
			if(b.getType() == Material.IRON_TRAPDOOR) s = on?Sound.BLOCK_IRON_TRAPDOOR_OPEN:Sound.BLOCK_IRON_TRAPDOOR_CLOSE;
			else s = on?Sound.BLOCK_WOODEN_TRAPDOOR_OPEN:Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE;
		} else if(b.getType() == Material.IRON_DOOR) s = on?Sound.BLOCK_IRON_DOOR_OPEN:Sound.BLOCK_IRON_DOOR_CLOSE;
		else s = on?Sound.BLOCK_WOODEN_DOOR_OPEN:Sound.BLOCK_WOODEN_DOOR_CLOSE;
		l.getWorld().playSound(l,s,1,1);
	}
	
	//Set state of levers:
	public static void setPowered(Block b, boolean on) {
		if(b.getBlockData() instanceof Switch) {
			Switch s = (Switch)b.getBlockData(); s.setPowered(on); b.setBlockData(s);
		}
	}
	
	//Write lines to sign:
	public static void setSign(Block sign, String[] lines) {
		Sign s = ((Sign)sign.getState());
		s.setLine(0, lines[0]==null?TITLE:lines[0]); s.setLine(1, lines[1]==null?"":lines[1]);
		s.setLine(2, lines[2]==null?"":lines[2]); s.setLine(3, lines[3]==null?"":lines[3]);
		s.update();
	} public static void setSign(Block sign, String lineOne) {
		Sign s = ((Sign)sign.getState());
		s.setLine(0, lineOne==null?TITLE:lineOne); s.setLine(1, "");
		s.setLine(2, ""); s.setLine(3, ""); s.update();
	}
	
	public static void setLine(Block sign, int l, String str) {
		Sign s = ((Sign)sign.getState()); s.setLine(l, str==null?"":str); s.update();
	}
	
	//Read lines from sign:
	public static String[] lines(Block sign) {
		return ((Sign)sign.getState()).getLines();
	}
	
	//Get block sign is attached to:
	public static Block getSignBlock(Block s) {
		World w = s.getWorld(); int x = s.getX(), y = s.getY(), z = s.getZ();
		BlockFace f = ((WallSign)s.getBlockData()).getFacing();
		switch(f) {
			case NORTH: return w.getBlockAt(x,y,z+1); case SOUTH: return w.getBlockAt(x,y,z-1);
			case WEST: return w.getBlockAt(x+1,y,z); case EAST: return w.getBlockAt(x-1,y,z);
			default: return null;
		}
	}
	
	//Ensure there is a solid block behind the sign.
	public static void addSignBlock(Block s) {
		Block b = Conf.getSignBlock(s); if(!b.getType().isSolid()) setDoorBlock(b,true);
	}
	
	public static void setDoorBlock(Block b, boolean on) {
		b.setType(on?Conf.DOOR_SET:Conf.AIR);
		//TODO This doesn't work properly. Fix?
		/*if(on && Conf.DOOR_SET == Material.GLASS_PANE) {
			MultipleFacing f = (MultipleFacing)b.getBlockData();
			f.setFace(BlockFace.NORTH, true); f.setFace(BlockFace.SOUTH, true);
			f.setFace(BlockFace.EAST, true); f.setFace(BlockFace.WEST, true);
			b.setBlockData(f);
		}*/
	}
	
	//Determine if sign or call sign was clicked on:
	public static boolean isElevSign(Block b, ConfData ref, Player pl, String perm) {
		if(!hasPerm(pl, perm)) return false; if(b.getBlockData() instanceof WallSign && TITLE.equals
		(lines(b)[0])) { ref.data = Elevator.fromElevSign(b); return (ref.data!=null); } return false;
	}
	
	public static boolean isCallSign(Block b, ConfData ref, Player pl, String perm) {
		if(!hasPerm(pl, perm)) return false; if(b.getBlockData() instanceof WallSign && CALL.equals
		(lines(b)[0])) { ref.data = Elevator.fromCallSign(b); return (ref.data!=null); } return false;
	}
	
	public static boolean isElevPlayer(Player pl, ConfData ref, String perm) {
		if(!hasPerm(pl, perm)) return false; ref.data = Elevator.fromPlayer(pl); return (ref.data!=null);
	}
	
	//Find first null element in a RaichuList:
	public static int findFirstEmpty(ChuList<ChuList<FallingBlock>> list) {
		int l=list.length; for(int i=0; i<l; i++) if(list.get(i)==null) return i; return l;
	}
	
	//Check if player has permission:
	public static boolean hasPerm(Player pl, String perm) {
		return pl.hasPermission(perm);
	}
	
	//Unpack a file internal to the JAR.
	public static String unpackFile(String internalPath, File dest) {
		InputStream stream = plugin.getClass().getResourceAsStream(internalPath);
		String str = ""; int p = 0; try {
			while(p < 3000) { int read = stream.read(); if(read < 0 || read >=
			65535) break; str += fromCharCode(read); p++; } stream.close();
			Writer file = new BufferedWriter(new OutputStreamWriter(new
			FileOutputStream(dest), "UTF-8")); file.write(str); file.close(); //<- Needed for Proper UTF-8 Encoding.
		} catch (Exception e) { err("unpackFile", "Caught Exception: "+e.getMessage()); return ""; } return str;
	}
	
	//Emulate JavaScript's fromCharCode Function:
	public static String fromCharCode(int... codePoints) {
		return new String(codePoints, 0, codePoints.length);
	}
	
	public static void dbg(String str) {
		if(DEBUG) {
			String msg = MSG_DBG+str; Bukkit.getConsoleSender().sendMessage(msg);
			Collection<? extends Player> pl = Bukkit.getOnlinePlayers();
			for(Player p: pl) if(hasPerm(p, Main.PERM_RELOAD)) p.sendMessage(msg);
		}
	}
	
	public static void err(String func, String cause) {
		if(DEBUG) {
			String msg = MSG_ERR_ST+func+MSG_ERR_MID+cause; Bukkit.getConsoleSender().sendMessage(msg);
			Collection<? extends Player> pl = Bukkit.getOnlinePlayers();
			for(Player p: pl) if(hasPerm(p, Main.PERM_RELOAD)) p.sendMessage(msg);
		}
	}
	
	//-------------------  PecacheuLib Functions -------------------
	
	public static String c(String str) {
		String clr[] = str.split("&"), cStr = clr[0];
		for(int i=1,l=clr.length; i<l; i++) cStr += org.bukkit.ChatColor
		.getByChar(clr[i].charAt(0)).toString()+clr[i].substring(1);
		return cStr;
	}
	
	public static Block getBlockBelowPlayer(Player p, boolean above) {
		Location loc = p.getLocation(); int pX = (int)Math.floor(loc.getX()), pZ = (int)Math.floor(loc.getZ());
		double pY = loc.getY(); return p.getWorld().getBlockAt(pX, (int)(above?Math.ceil(pY+1.99):Math.floor(pY-1)), pZ);
	} public static Block getBlockBelowPlayer(Player p) { return getBlockBelowPlayer(p, false); }
}

class ConfData {
	public Object data = null;
}