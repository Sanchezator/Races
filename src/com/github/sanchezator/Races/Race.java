package com.github.sanchezator.Races;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Race {

	private String _name;
	private boolean _IsOpen, _hasBase;
	private Location _baseLoc;
	private Map<String, Boolean> _map_perms;
	private List<String> _lst_players;
	
	public Race(String name, boolean IsOpen, boolean hasBase, Map<String, Boolean> map_perms,
			List<String> lst_players, Location baseLoc) {
		_name = name;
		_IsOpen = IsOpen;
		_hasBase = hasBase;
		_baseLoc = baseLoc;
		_lst_players = lst_players;
		_map_perms = map_perms;
	}
	
	private boolean hasPlayer(String player_name) {
		for(String str_cmp: _lst_players) {
			if (str_cmp.equals(player_name)) return true;
		}
		return false;
	}
	
	public Map<String, Boolean> GetPlayerPermissions(Player plr) {
		if (hasPlayer(plr.getName())) {
			return _map_perms;
		} else {
			return null;
		}
	}
	
	public Race GetPlayerRace(Player plr) {
		if (hasPlayer(plr.getName())) {
			return this;
		} else {
			return null;
		}
	}
	
	public String getName() {
		return _name;
	}
	
	public List<String> getPlayers() {
		return _lst_players;
	}
	
	public boolean IsOpen() {
		return _IsOpen;
	}
	
	public boolean hasBase() {
		return _hasBase;
	}
	
	public Location getBaseLoc() {
		return _baseLoc;
	}
	
	public void addPlayer(String player_name) {
		_lst_players.add(player_name);
	}
}
