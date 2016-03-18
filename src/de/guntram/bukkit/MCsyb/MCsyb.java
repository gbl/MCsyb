/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.MCsyb;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author gbl
 */
public class MCsyb extends JavaPlugin implements Listener {
    static Logger logger=null;
    HashMap<String, Location> locationList=null;
    HashMap<String, String> fileNames=null;
    String outputPath;

    @Override
    public void onEnable() {
        if (logger==null)
            logger=this.getLogger();
        if (logger!=null) {
            //logger.info("Test1 enabled");
        }
        locationList=new HashMap();
        fileNames=new HashMap();
        saveDefaultConfig();
        outputPath=this.getConfig().getString("outputpath");
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public void onDisable() {
        if (logger!=null) {
            //logger.info("Test1 disabled");
        }
        locationList=null;
        fileNames=null;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length==0)
            return false;

        Player player;
        String cmdName=cmd.getName();
        if (sender instanceof Player) {
            player=(Player)sender;
        } else {
            sender.sendMessage("You need to be logged in to do that");
            return true;
        }
        
        Location loc=player.getLocation();
        String name=player.getName();
        if (args[0].equalsIgnoreCase("c1")) {
            locationList.put("C1-"+name, loc);
            sender.sendMessage("Corner 1 set to "+loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ());
            return true;
        }
            
        if (args[0].equalsIgnoreCase("c2")) {
            locationList.put("C2-"+name, loc);
            sender.sendMessage("Corner 2 set to "+loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ());
            return true;
        }
        
        if (args.length==2) {
            if (args[0].equalsIgnoreCase("c1x")) { sender.sendMessage(patchLocation("C1", loc, name, 'x', args[1])); return true; }
            if (args[0].equalsIgnoreCase("c1y")) { sender.sendMessage(patchLocation("C1", loc, name, 'y', args[1])); return true; }
            if (args[0].equalsIgnoreCase("c1z")) { sender.sendMessage(patchLocation("C1", loc, name, 'z', args[1])); return true; }
            if (args[0].equalsIgnoreCase("c2x")) { sender.sendMessage(patchLocation("C2", loc, name, 'x', args[1])); return true; }
            if (args[0].equalsIgnoreCase("c2y")) { sender.sendMessage(patchLocation("C2", loc, name, 'y', args[1])); return true; }
            if (args[0].equalsIgnoreCase("c2z")) { sender.sendMessage(patchLocation("C2", loc, name, 'z', args[1])); return true; }
        }
        
        if (args[0].equalsIgnoreCase("name")) {
            if (args.length!=2)
                return false;
            if (!isHarmlessFilename(args[1])) {
                sender.sendMessage("Your filename is an attempt to hack");
                return true;
            }
            fileNames.put(name, args[1]);
            sender.sendMessage("Export will be done to "+args[1]+".js");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("export")) {
            Location loc1=locationList.get("C1-"+name);
            Location loc2=locationList.get("C2-"+name);
            if (loc1==null) { sender.sendMessage("Location 1 unset"); return true; }
            if (loc2==null) { sender.sendMessage("Location 2 unset"); return true; }
            
            int x1=loc1.getBlockX(); int x2=loc2.getBlockX();
            int y1=loc1.getBlockY(); int y2=loc2.getBlockY();
            int z1=loc1.getBlockZ(); int z2=loc2.getBlockZ();
            String fileName=fileNames.get(player.getName());
            if (fileName==null) {
                fileName="export"+System.currentTimeMillis();
            }
            
            String result=export(fileName, loc1.getWorld(), x1, x2, y1, y2, z1, z2);
            sender.sendMessage(result);
            return true;
        }

        return false;
    }
    
    private boolean isHarmlessFilename(String name) {
        if (name.indexOf('/') != -1
        ||  name.indexOf('\\')!= -1
        ||  name.indexOf(':') != -1) {
            return false;
        }
        return true;
    }
        
    private String export(String fileName, World world, int x1, int x2, int y1, int y2, int z1, int z2) {
        int dx, dy, dz;
        if (x1>x2) { dx=x1; x1=x2; x2=dx; }
        if (y1>y2) { dy=y1; y1=y2; y2=dy; }
        if (z1>z2) { dz=z1; z1=z2; z2=dz; }
        if (x1==x2 || y1==y2 || z1==z2) {
            return ("At least one dimension delta is 0");
        }

        MaterialCharacterMap map=new MaterialCharacterMap();
        for (dy=y1; dy<=y2; dy++) {
            for (dz=z1; dz<=z2; dz++) {
                for (dx=x1; dx<=x2; dx++) {
                    Block block=world.getBlockAt(dx, dy, dz);
                    // int material=block.getType().ordinal();
                    int material=block.getTypeId();
                    byte data=block.getData();
                    map.addOccurence(material, data);
                }
            }
        }
        map.analyzeHistogram();

        try {
            PrintWriter exportWriter=new PrintWriter(
                    new FileWriter(outputPath+File.separatorChar+fileName+".js"));
            exportWriter.println("// world coords: X="+x1+"-"+x2+" Y="+y1+"-"+y2+" Z="+z1+"-"+z2);
            exportWriter.println("build={");
            exportWriter.println("\theight:"+(y2-y1+1)+",");
            exportWriter.println("\twidth:" +(x2-x1+1)+",");
            exportWriter.println("\tdepth:" +(z2-z1+1)+",");
            exportWriter.println("\tmap:{");
            for (MaterialCharacterMapInstance mi: map) {
                exportWriter.println("\t\t'"+mi.symbol+"': "+mi.code+",");
            }
            exportWriter.println("\t},");
            exportWriter.println("\tdata:[");

            for (dy=y1; dy<=y2; dy++) {
                exportWriter.println("/* level "+dy+" */");
                for (dz=z1; dz<=z2; dz++) {
                    exportWriter.print("\t\t\"");
                    for (dx=x1; dx<=x2; dx++) {
                        Block block=world.getBlockAt(dx, dy, dz);
                        // int material=block.getType().ordinal();
                        int material=block.getTypeId();
                        byte data=block.getData();
                        exportWriter.print(map.get(material, data));
                    }
                    exportWriter.println("\"+    // "+dz);
                }
                exportWriter.println("\t\"\",");
            }
            exportWriter.println("]};");
            exportWriter.close();
            return ("Exported "+(x2-x1+1)*(y2-y1+1)*(z2-z1+1)+" blocks to "+fileName+".js.");
        } catch (IOException ex) {
            logger.warning(ex.getMessage());
            return ("Some exception occured; see server logs.");
        }
    }
    
    private String patchLocation(String prefix, Location defLoc, String playerName, char coord, String newValue) {
        String hashName=prefix+"-"+playerName;
        Location old=locationList.get(hashName);
        if (old==null)
            old=defLoc;
        try {
            int newCoord=Integer.parseInt(newValue);
            switch (coord) {
                case 'x': case 'X': old.setX(newCoord); break;
                case 'y': case 'Y': old.setY(newCoord); break;
                case 'z': case 'Z': old.setZ(newCoord); break;
                default: return "Internal error, coord is not x or y or z";
            }
            locationList.put(hashName, old);
            return "Corner "+prefix+" is now at "+old.getBlockX()+" "+old.getBlockY()+" "+old.getBlockZ();
        } catch(NumberFormatException E) {
            return newValue+" does not seem to be a number";
        } catch(NullPointerException E) {
            return "You did not provide a new value";
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Block  block =e.getClickedBlock();
        if (block==null)
            return;
        Material mat=block.getType();
        if (mat!=Material.SIGN_POST && mat!=Material.WALL_SIGN)
            return;
        Action action=e.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK)
            return;
        Sign sign=(Sign) block.getState();
        if (!sign.getLine(0).equalsIgnoreCase("[mcsyb]")) {
            return;
        }
        Player player=e.getPlayer();
        World world=player.getWorld();
        int[] c1, c2;
        if ((c1=parseCorner(sign.getLine(1)))==null) {
            player.sendMessage("Second line of this sign can't be parsed as a corner");
            return;
        }
        if ((c2=parseCorner(sign.getLine(2)))==null) {
            player.sendMessage("Third line of this sign can't be parsed as a corner");
            return;
        }
        String fileName=sign.getLine(3);
        player.sendMessage(export(fileName, world, c1[0], c2[0], c1[1], c2[1], c1[2], c2[2]));
    }
    
    private int[] parseCorner(String s) {
        int[] result=new int[3];
        Pattern pattern=Pattern.compile("^(-?[0-9]+) +(-?[0-9]+) +(-?[0-9]+)$");
        Matcher matcher=pattern.matcher(s);
        if (matcher.matches()) {
            result[0]=Integer.parseInt(matcher.group(1));
            result[1]=Integer.parseInt(matcher.group(2));
            result[2]=Integer.parseInt(matcher.group(3));
            return result;
        }
        return null;
    }
}
