package nu.nerd.beastmaster.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;
import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.Util;

// ----------------------------------------------------------------------------
/**
 * Executor for the /best-item command.
 */
public class BeastItemExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     */
    public BeastItemExecutor() {
        super("beast-item", "help", "define", "redefine", "remove", "get", "list");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equals("help"))) {
            return false;
        }

        if (args.length >= 1) {
            if (args[0].equals("define")) {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " define <id>");
                    return true;
                }

                if (!checkInGame(sender)) {
                    return true;
                }
                Player player = (Player) sender;

                String id = args[1];
                if (BeastMaster.CONFIG.ITEMS.containsKey(id)) {
                    sender.sendMessage(ChatColor.RED + "An item named \"" + id + "\" is already defined. " +
                                       "Use \"/" + getName() + " redefine " + id + "\" to redefine the item.");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) {
                    sender.sendMessage(ChatColor.RED + "Hold the object to define as an item in your main hand!");
                    return true;
                }

                ItemStack definition = item.clone();
                definition.setAmount(1);
                BeastMaster.CONFIG.ITEMS.put(id, definition);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Item " + ChatColor.YELLOW + id +
                                   ChatColor.GOLD + " is now defined as: " + ChatColor.WHITE + Util.getItemDescription(definition));
                return true;

            } else if (args[0].equals("redefine")) {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " redefine <id>");
                    return true;
                }

                if (!checkInGame(sender)) {
                    return true;
                }
                Player player = (Player) sender;

                String id = args[1];
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) {
                    sender.sendMessage(ChatColor.RED + "Hold the object to define as an item in your main hand!");
                    return true;
                }

                ItemStack definition = item.clone();
                definition.setAmount(1);
                BeastMaster.CONFIG.ITEMS.put(id, definition);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Item " + ChatColor.YELLOW + id +
                                   ChatColor.GOLD + " is now defined as: " + ChatColor.WHITE + Util.getItemDescription(definition));
                return true;

            } else if (args[0].equals("remove")) {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " remove <id>");
                    return true;
                }

                String id = args[1];
                if (!BeastMaster.CONFIG.ITEMS.containsKey(id)) {
                    sender.sendMessage(ChatColor.RED + "There is no item with the ID \"" + id + "\"!");
                    return true;
                }

                BeastMaster.CONFIG.ITEMS.remove(id);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Item " + ChatColor.YELLOW + id + ChatColor.GOLD + " definition removed.");
                return true;

            } else if (args[0].equals("get")) {
                if (args.length < 2 || args.length > 3) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " get <id> [<count>]");
                    return true;
                }

                if (!checkInGame(sender)) {
                    return true;
                }
                Player player = (Player) sender;

                String id = args[1];
                if (!BeastMaster.CONFIG.ITEMS.containsKey(id)) {
                    sender.sendMessage(ChatColor.RED + "There is no item with the ID \"" + id + "\"!");
                    return true;
                }

                int count = 1;
                if (args.length == 3) {
                    String countArg = args[2];
                    try {
                        count = Integer.parseInt(countArg);
                    } catch (NumberFormatException ex) {
                        count = 0;
                    }

                    if (count <= 0) {
                        sender.sendMessage(ChatColor.RED + countArg + " is not a valid number of items!");
                        return true;
                    }
                }

                // Let's be careful not to mess with the original item.
                ItemStack item = BeastMaster.CONFIG.ITEMS.get(id).clone();

                // Split oversized item stacks prior to adding to the inventory.
                ArrayList<ItemStack> stacks = new ArrayList<>();
                if (item.getMaxStackSize() < 0) {
                    item.setAmount(count);
                    stacks.add(item);
                } else {
                    int fullStacks = count / item.getMaxStackSize();
                    int partialStack = count % item.getMaxStackSize();
                    sender.sendMessage("count: " + count + " fullStacks: " + fullStacks + " partialStack: " + partialStack);

                    for (int i = 0; i < fullStacks; ++i) {
                        ItemStack stack = item.clone();
                        stack.setAmount(item.getMaxStackSize());
                        stacks.add(stack);
                    }
                    if (partialStack != 0) {
                        ItemStack stack = item.clone();
                        stack.setAmount(partialStack);
                        stacks.add(stack);
                    }
                }

                // If there are items that could not fit in the player's
                // inventory, drop them on the ground.
                Location loc = player.getLocation();
                HashMap<Integer, ItemStack> didntFit = player.getInventory().addItem(stacks.toArray(new ItemStack[stacks.size()]));
                for (ItemStack drop : didntFit.values()) {
                    loc.getWorld().dropItemNaturally(loc, drop);
                }
                player.sendMessage(ChatColor.GOLD + "You got " + ChatColor.YELLOW + count +
                                   ChatColor.GOLD + " of " + ChatColor.WHITE + Util.getItemDescription(item));
                return true;

            } else if (args[0].equals("list")) {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /" + getName() + " list");
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Items:");
                for (Entry<String, ItemStack> entry : BeastMaster.CONFIG.ITEMS.entrySet()) {
                    sender.sendMessage(ChatColor.YELLOW + entry.getKey() +
                                       ChatColor.WHITE + ": " + Util.getItemDescription(entry.getValue()));
                }
                return true;
            }
        }
        return false;
    } // onCommand

    // ------------------------------------------------------------------------
    /**
     * Return true if the command sender is a Player (in game).
     * 
     * Otherwise, send an error message and return false.
     * 
     * @return true if the command sender is a Player (in game).
     */
    protected boolean checkInGame(CommandSender sender) {
        boolean inGame = (sender instanceof Player);
        if (!inGame) {
            sender.sendMessage("You must be in-game to use this comamnd.");
        }
        return inGame;
    }
} // class BeastItemExecutor