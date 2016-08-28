package com.krisp.minecraft.whitelist;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import fr.xephi.authme.api.NewAPI;


public class Whitelist extends JavaPlugin implements Listener {				
	
	public static final Permission ADMIN_PERM = new Permission ("whitelist.admin");
	
	public void onEnable() {
		this.saveDefaultConfig();
		//Bukkit.getPluginManager().addPermission(ADMIN_PERM);
		Bukkit.getServer().getPluginManager().registerEvents(this, this);		
		reprocessWhitelist();
	}
	
	public void reprocessWhitelist() {			
		for(Player p : Bukkit.getServer().getOnlinePlayers())
			doWhitelist(p, p.getAddress().getAddress());				
	}
		
	private boolean doWhitelist(Player player, InetAddress ip) {
		if(!NewAPI.getInstance().isRegistered(player.getName())) {
			Bukkit.getServer().getLogger().info("[Whitelist] user is not registered, skipping whitelist");
			return false;
		}
						
		for(String user : this.getConfig().getConfigurationSection("whitelist").getKeys(false)) {
			try {				
				if(user.equals(player.getName())) {
					for(String host : this.getConfig().getStringList("whitelist." + user + ".hosts"))
					{											
						if(InetAddress.getByName(host).equals(ip)) {
							if(NewAPI.getInstance().isAuthenticated(player)) {
								Bukkit.getServer().getLogger().info("[Whitelist] user is already authenticated, skipping whitelist");							
							} else {
								NewAPI.getInstance().forceLogin(player);		
								Bukkit.getServer().getLogger().info("[Whitelist] bypassed AuthMe for whitelisted user " +
										player.getName() + " [" + ip.getHostAddress() + "]");								
							}
							return true;
						}
					}
				}
			} catch (UnknownHostException ex) {
				Bukkit.getServer().getLogger().info("[Whitelist] failed to lookup host");
			}
		}
		return false;
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent loginEvent) {				
		doWhitelist(loginEvent.getPlayer(), loginEvent.getAddress());
    }

	@EventHandler
	public void onPlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent e) {
		String msg = e.getMessage();
		Player sender = e.getPlayer();
		String[] args = msg.split(" ");
		FileConfiguration conf = this.getConfig();
		
		if(args[0].equalsIgnoreCase("/whitelist")) {
			if(!sender.hasPermission(ADMIN_PERM)) {
				sender.sendMessage(ChatColor.RED+ "Not authorized");
				return;
			}
			
			if(args.length < 2) {
				sender.sendMessage(ChatColor.BLUE + "Users with whitelist entries: ");
				for(String user : conf.getConfigurationSection("whitelist").getKeys(false))			
					sender.sendMessage(ChatColor.BLUE + "  " + user);
			} else if(args.length == 2) {
				if(args[1].equalsIgnoreCase("help")) { 
					sendHelpMessage(sender);					
				} else if(args[1].equalsIgnoreCase("reload")) {
					this.reloadConfig();
					sender.sendMessage(ChatColor.BLUE + "whitelist config reloaded");
				} else {
					if(!conf.isConfigurationSection("whitelist." + args[1])) {
						sender.sendMessage(ChatColor.RED + "No whitelist entries for user " + args[1]);
						return;
					}
					sender.sendMessage(ChatColor.BLUE + "Whitelisted hosts for " + args[1] + ":");
					for(String host : conf.getStringList("whitelist." + args[1] + ".hosts")) 
						sender.sendMessage(ChatColor.BLUE + "  " + host);
				}
			} else if(args.length == 3) {
				if(args[2].equalsIgnoreCase("delete")) {
					if(conf.isConfigurationSection("whitelist." + args[1])) {
						conf.set("whitelist." + args[1], null);
						this.saveConfig();
						sender.sendMessage(ChatColor.BLUE + "whitelist user deleted.");
					}
				} else {
					sender.sendMessage(ChatColor.RED + "Invalid arguments");
				}
			} else if(args.length == 4) {
				if(args[2].equalsIgnoreCase("add")) {
					List<String> h;				
					if(conf.isConfigurationSection("whitelist." + args[1]))
						h = conf.getStringList("whitelist." + args[1] + ".hosts");
					else {
						conf.createSection("whitelist." + args[1]);
						h = new ArrayList<String>();
					}				
					h.add(args[3]);						
					conf.set("whitelist." + args[1] + ".hosts", h);
					this.saveConfig();
					sender.sendMessage(ChatColor.BLUE + "host added.");
				} else if(args[2].equalsIgnoreCase("del")) {
					if(conf.isConfigurationSection("whitelist." + args[1])) {
						List<String> h = conf.getStringList("whitelist." + args[1] + ".hosts");
						if(h.remove(args[3])) {
							conf.set("whitelist." + args[1] + ".hosts", h);
							this.saveConfig();
							sender.sendMessage(ChatColor.BLUE + "host deleted.");
						} else {
							sender.sendMessage(ChatColor.RED + "host not deleted.");					
						}					
					} else {
						sender.sendMessage(ChatColor.RED + "user not found");
					}
				} else {
					sender.sendMessage(ChatColor.RED + "Invalid arguments");
				}
			}
		}
	}	
	
	private void sendHelpMessage(Player sender)
	{
		sender.sendMessage(ChatColor.BLUE + "Usage:");
		sender.sendMessage(ChatColor.BLUE + " /whitelist <user> add <host> - Add host to user");
		sender.sendMessage(ChatColor.BLUE + " /whitelist <user> del <host> - Del host from user");
		sender.sendMessage(ChatColor.BLUE + " /whitelist <user> delete - Delete user from config");
		sender.sendMessage(ChatColor.BLUE + " /whitelist reload - Reload config from config");
	}	
}