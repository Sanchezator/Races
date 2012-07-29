package com.github.sanchezator.Races;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

public class Races extends JavaPlugin implements Listener {
	
	private Logger lg;
    private File configFile, templateFile;
    private YamlConfiguration config, template;
	private List<Race> lst_Races;
	private Map<String, PermissionAttachment> map_perms_attach;
	private Map<String, Location> map_player_loc;
	private Map<String, String> map_localization;
	
    public void onEnable() {
    	lg = getLogger();
    	map_perms_attach = new HashMap<String, PermissionAttachment>();
    	map_player_loc = new HashMap<String, Location>();
    	map_localization = new HashMap<String, String>();
    	lst_Races = new ArrayList<Race>();
    	Log("Loading configuration...");
    	
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        templateFile = new File(getDataFolder(), "template.yml");
        if (!templateFile.exists()) {
            saveDefaultTemplate();
        } 
        
        reloadConfig();
		
		ConfigurationSection cfg_sect =  config.getConfigurationSection("races");
		ConfigurationSection cfg_perms;
		for(String str: cfg_sect.getKeys(false)) {
			cfg_perms = cfg_sect.getConfigurationSection(str + "/permissions");
			
			Map<String, Boolean> map_perms = new HashMap<String, Boolean>();
			
			for(String str_perms: cfg_perms.getKeys(false)) {
				//_plg.Log(cfg_perms.getKeys(true).toString());
				map_perms.put(str_perms, cfg_perms.getBoolean(str_perms));
			}
			Log("races/" + str + "/" + map_perms.toString());
			
			lst_Races.add(new Race(str, cfg_sect.getBoolean(str + "/isOpen"), 
					cfg_sect.getBoolean(str + "/hasBase"), 
					map_perms, cfg_sect.getStringList(str + "/players"),
					new Location(getServer().getWorlds().get(0),
							cfg_sect.getLong(str + "/baseX"), 
							cfg_sect.getLong(str + "/baseY"), 
							cfg_sect.getLong(str + "/baseZ"),
							cfg_sect.getLong(str + "/yaw"), 
							cfg_sect.getLong(str + "/pitch"))));
			Log(cfg_sect.getStringList(str + "/players").toString());
		}
		
		for(String str_localiz: template.getConfigurationSection("localization").getKeys(false)) {
			map_localization.put(str_localiz, template.getString("localization/" + str_localiz));
		}

        Log("Loaded.");
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			
			@Override
			public void run() {
				SaveAll();
				Log("Config was saved.");
			}
		}, 6000L, 6000L);
        
        for (Player plr: getServer().getOnlinePlayers()) {
        	registerPlayer(plr);
        }
    }

	public void onDisable() { 
    	SaveAll();
    }
    
    public void Log(String msg) {
    	lg.info(msg);
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
    	Player cur_player = null;
    	if (sender instanceof Player) {
    		cur_player = (Player) sender;
    	}

    	if (cmd.getName().equalsIgnoreCase("races")) { // Проверяем набранные команды
    		if (cur_player != null) {
    			switch (args.length) {
				case 0:
					cur_player.sendMessage(map_localization.get("help_all"));
					break;
				case 1:
					if (args[0].equalsIgnoreCase("list")) {
						String sendMsg =  map_localization.get("cmd_list");
						boolean onlyOpen;
						if (cur_player.hasPermission("races.select.closed")) {
							onlyOpen = false;
						} else  { 
							onlyOpen = true;
						}
						for(String open_races: GetOpenRacesName(onlyOpen)) {
							sendMsg += ChatColor.AQUA + open_races + ChatColor.WHITE + map_localization.get("cmd_list_separator");
						}
						cur_player.sendMessage(sendMsg);
					}
					break;
				case 2:
					if (args[0].equalsIgnoreCase("select")) {
						Race player_race = GetPlayerRace(cur_player);
						if (player_race != null) {
							cur_player.sendMessage(ChatColor.RED + 
									map_localization.get("cmd_select_2ndrace") +
									ChatColor.AQUA + player_race.getName());
							break;
						}
						Race add_race = GetRaceByName(args[1]);
						if (add_race != null) {
							add_race.addPlayer(cur_player.getName());
							config.set("races/" + add_race.getName() + "/players",
									add_race.getPlayers());
							map_player_loc.remove(cur_player.getName());
							registerPlayer(cur_player);
							cur_player.sendMessage(ChatColor.GREEN + map_localization.get("cmd_select_success") + ChatColor.AQUA + args[1]);
						} else {
							cur_player.sendMessage(ChatColor.RED + map_localization.get("cmd_select_norace"));
						}
					}
					break;
				}
    		} else {
    			
    		}
    		return true;
    	} else if (cmd.getName().equalsIgnoreCase("base")) {
    		if (cur_player != null) {
    			if (config.getBoolean("settings/AllowBase")) {
    				Race player_race = GetPlayerRace(cur_player);
    				if (player_race != null) {
    					if (player_race.hasBase()) cur_player.teleport(player_race.getBaseLoc());
    					cur_player.sendMessage(ChatColor.GREEN + map_localization.get("cmd_base_success"));
    				}
    			} else {
    				cur_player.sendMessage(ChatColor.RED + map_localization.get("cmd_base_noallow"));
    			}
    		} else {
    			//TODO: Проверить аргументы и использовать /base <player>
    		}
    	}
    	return false; 
    }
    
	private void saveDefaultTemplate() {
	    YamlConfiguration defTempl = YamlConfiguration.loadConfiguration(this.getResource("template.yml"));
	    try {
	        defTempl.save(templateFile);
	    } catch (IOException ex) {
	        Log("Could save default template!");
	    }
	}
	
	private void SaveAll() {
		saveConfig();
	    try {
	        template.save(templateFile);
	    } catch (IOException ex) {
	        Log("Could not save template");
	    }
	}
    
    @Override
    public void reloadConfig() {
        config = new YamlConfiguration();
        config.options().pathSeparator('/');
        try {
            config.load(configFile);
        } catch (Exception e) {
            Log("Unable to load configuration!");
        }
        
        template = new YamlConfiguration();
        template.options().pathSeparator('/');
        try {
        	template.load(templateFile);
        } catch (Exception e) {
            Log("Unable to load template!");
        }
    }

    @Override
    public FileConfiguration getConfig() {
        return config;
    }

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent event) {
    	registerPlayer(event.getPlayer());
    }
    
    @EventHandler
    public void OnPlayerMove(PlayerMoveEvent event) {
    	if (map_player_loc.containsKey(event.getPlayer().getName())) {
    		event.getPlayer().teleport(map_player_loc.get(event.getPlayer().getName()));
    		event.getPlayer().sendMessage(ChatColor.RED + map_localization.get("norace"));	
    	}
    }
    
    private void registerPlayer(Player plr) {
	   Map<String, Boolean> map_perms = GetPlayerPermissions(plr);
	   if (map_perms != null) {
		   SetPermissions(plr, map_perms);
		   if (plr.hasPermission("races.fly")) {
			   plr.setAllowFlight(true);
		   }
	   } else {
		   if (!config.getBoolean("settings/AllowNoRace")) {
			   plr.sendMessage(ChatColor.RED + map_localization.get("norace"));
			   map_player_loc.put(plr.getName(), plr.getLocation());
		   }
	   }
	 }
   
    private void SetPermissions(Player plr, Map<String, Boolean> map_perms) {
	   PermissionAttachment perm_attach = plr.addAttachment(this);
	   for(String key: map_perms.keySet()) {
		   perm_attach.setPermission(key, map_perms.get(key));
	   }
	   map_perms_attach.put(plr.getName(), perm_attach);
    }
   
    private Map<String, Boolean> GetPlayerPermissions(Player plr) {
	   Map<String, Boolean> ret = null;
	   for(Race rc: lst_Races) {
		   ret = rc.GetPlayerPermissions(plr);
		   if (ret != null) break;
	   }
	   return ret;
	}
    
    private Race GetPlayerRace(Player plr) {
    	Race ret = null;
 	    for(Race rc: lst_Races) {
		    ret = rc.GetPlayerRace(plr);
		    if (ret != null) break;
	    }
 	    return ret;
    }
    
    private List<String> GetOpenRacesName(boolean onlyOpen) {
    	List<String> ret = new ArrayList<String>();
    	for(Race rc: lst_Races) {
    		if (onlyOpen) if (rc.IsOpen()) ret.add(rc.getName());
    	}
    	return ret;
    }
    
    private Race GetRaceByName(String name) {
    	Race ret = null;
    	for(Race rc: lst_Races) {
    		if (rc.getName().equalsIgnoreCase(name)) {
    			ret = rc;
    		}
    	}
    	Log("name = " + name);
    	return ret;
    }
}
