package nu.nerd.beastmaster.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import nu.nerd.beastmaster.BeastMaster;
import nu.nerd.beastmaster.DropType;
import nu.nerd.beastmaster.Item;
import nu.nerd.beastmaster.Util;
import nu.nerd.beastmaster.mobs.MobType;

// ----------------------------------------------------------------------------
/**
 * Executor for the {@code /beast-item} command.
 */
public class BeastItemExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     */
    public BeastItemExecutor() {
        super("beast-item", "help", "define", "redefine", "remove", "get", "list", "info", "name", "lore");
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
                    Commands.invalidArguments(sender, getName() + " define <item-id>");
                    return true;
                }

                if (!isInGame(sender)) {
                    return true;
                }
                Player player = (Player) sender;

                String idArg = args[1];
                if (DropType.isDropType(idArg)) {
                    sender.sendMessage(ChatColor.RED + "You can't use The ID \"" + idArg + "\"; it is reserved.");
                    return true;
                }

                Item item = BeastMaster.ITEMS.getItem(idArg);
                if (item != null) {
                    sender.sendMessage(ChatColor.RED + "An item named \"" + idArg + "\" is already defined. " +
                                       "Use \"/" + getName() + " redefine " + idArg + "\" to redefine the item.");
                    return true;
                }

                MobType mobType = BeastMaster.MOBS.getMobType(idArg);
                if (mobType != null) {
                    sender.sendMessage(ChatColor.RED + "A mob type named \"" + idArg + "\" is already defined. " +
                                       "Items can't have the same ID as an existing mob.");
                    return true;
                }

                ItemStack itemStack = player.getInventory().getItemInMainHand();
                if (itemStack == null || itemStack.getType() == Material.AIR) {
                    sender.sendMessage(ChatColor.RED + "Hold the object to define as an item in your main hand!");
                    return true;
                }

                ItemStack definition = itemStack.clone();
                definition.setAmount(1);
                BeastMaster.ITEMS.addItem(idArg, definition);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Item " + ChatColor.YELLOW + idArg +
                                   ChatColor.GOLD + " is now defined as: " + ChatColor.WHITE + Util.getItemDescription(definition));
                return true;

            } else if (args[0].equals("redefine")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " redefine <item-id>");
                    return true;
                }

                if (!isInGame(sender)) {
                    return true;
                }
                Player player = (Player) sender;

                String idArg = args[1];
                ItemStack itemStack = player.getInventory().getItemInMainHand();
                if (itemStack == null || itemStack.getType() == Material.AIR) {
                    sender.sendMessage(ChatColor.RED + "Hold the object to define as an item in your main hand!");
                    return true;
                }

                ItemStack definition = itemStack.clone();
                definition.setAmount(1);

                BeastMaster.ITEMS.addItem(idArg, definition);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Item " + ChatColor.YELLOW + idArg +
                                   ChatColor.GOLD + " is now defined as: " + ChatColor.WHITE + Util.getItemDescription(definition));
                return true;

            } else if (args[0].equals("remove")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " remove <item-id>");
                    return true;
                }

                String idArg = args[1];
                Item item = BeastMaster.ITEMS.getItem(idArg);
                if (item == null) {
                    sender.sendMessage(ChatColor.RED + "There is no item with the ID \"" + idArg + "\"!");
                    return true;
                }

                BeastMaster.ITEMS.removeItem(idArg);
                BeastMaster.CONFIG.save();
                sender.sendMessage(ChatColor.GOLD + "Item " + ChatColor.YELLOW + idArg + ChatColor.GOLD + " definition removed.");
                return true;

            } else if (args[0].equals("get")) {
                if (args.length < 2 || args.length > 3) {
                    Commands.invalidArguments(sender, getName() + " get <item-id> [<count>]");
                    return true;
                }

                if (!isInGame(sender)) {
                    return true;
                }
                Player player = (Player) sender;

                String idArg = args[1];
                Item item = BeastMaster.ITEMS.getItem(idArg);
                if (item == null) {
                    Commands.errorNull(sender, "item", idArg);
                    return true;
                }

                Integer count = 1;
                if (args.length == 3) {
                    count = Commands.parseNumber(args[2], Commands::parseInt,
                                                 (c) -> c > 0,
                                                 () -> sender.sendMessage(ChatColor.RED + "The number of items must be more than zero!"),
                                                 null);
                    if (count == null) {
                        return true;
                    }
                }

                // Let's be careful not to mess with the original ItemStack.
                ItemStack itemStack = item.getItemStack().clone();

                // Split oversized item stacks prior to adding to the inventory.
                ArrayList<ItemStack> stacks = new ArrayList<>();
                if (itemStack.getMaxStackSize() < 0) {
                    itemStack.setAmount(count);
                    stacks.add(itemStack);
                } else {
                    int fullStacks = count / itemStack.getMaxStackSize();
                    int partialStack = count % itemStack.getMaxStackSize();
                    // sender.sendMessage("count: " + count + " fullStacks: " +
                    // fullStacks + " partialStack: " + partialStack);

                    for (int i = 0; i < fullStacks; ++i) {
                        ItemStack stack = itemStack.clone();
                        stack.setAmount(itemStack.getMaxStackSize());
                        stacks.add(stack);
                    }
                    if (partialStack != 0) {
                        ItemStack stack = itemStack.clone();
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
                                   ChatColor.GOLD + " of " + ChatColor.WHITE + Util.getItemDescription(item.getItemStack()));
                return true;

            } else if (args[0].equals("list")) {
                if (args.length != 1) {
                    Commands.invalidArguments(sender, getName() + " list");
                    return true;
                }

                if (BeastMaster.ITEMS.getAllItems().isEmpty()) {
                    sender.sendMessage(ChatColor.GOLD + "There are no items.");
                } else {
                    List<String> ids = BeastMaster.ITEMS.getAllItems().stream()
                    .map(Item::getId)
                    .sorted((s1, s2) -> s1.compareToIgnoreCase(s2))
                    .collect(Collectors.toList());
                    sender.sendMessage(ChatColor.GOLD + "Items: " +
                                       Util.alternateColours(ids, ChatColor.GRAY + " ", ChatColor.WHITE, ChatColor.YELLOW));
                }
                return true;

            } else if (args[0].equals("info")) {
                if (args.length != 2) {
                    Commands.invalidArguments(sender, getName() + " info <item-id>");
                    return true;
                }

                String idArg = args[1];
                Item item = BeastMaster.ITEMS.getItem(idArg);
                if (item == null) {
                    Commands.errorNull(sender, "item", idArg);
                    return true;
                }

                sender.sendMessage(ChatColor.YELLOW + item.getId() + ChatColor.GOLD + " is: " +
                                   ChatColor.WHITE + Util.getItemDescription(item.getItemStack()));
                return true;

            } else if (args[0].equals("name")) {
                if (args.length < 1) {
                    Commands.invalidArguments(sender, getName() + " name [<text>]");
                    return true;
                }

                if (!isInGame(sender)) {
                    return true;
                }
                Player player = (Player) sender;

                ItemStack itemStack = player.getInventory().getItemInMainHand();
                if (itemStack == null || itemStack.getType() == Material.AIR) {
                    sender.sendMessage(ChatColor.RED + "Hold the object to rename as an item in your main hand!");
                    return true;
                }

                String nameArg = ChatColor.translateAlternateColorCodes('&', String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                ItemMeta meta = itemStack.getItemMeta();
                meta.setDisplayName(nameArg);
                itemStack.setItemMeta(meta);
                player.getInventory().setItemInMainHand(itemStack);

                if (nameArg.isEmpty()) {
                    sender.sendMessage(ChatColor.GOLD + "The item in your main hand has had its name cleared.");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "The item in your main hand is now named:\n" +
                                       ChatColor.WHITE + nameArg);

                }
                return true;

            } else if (args[0].equals("lore")) {
                if (args.length < 1) {
                    Commands.invalidArguments(sender, getName() + " lore [<text>]");
                    return true;
                }

                if (!isInGame(sender)) {
                    return true;
                }
                Player player = (Player) sender;

                ItemStack itemStack = player.getInventory().getItemInMainHand();
                if (itemStack == null || itemStack.getType() == Material.AIR) {
                    sender.sendMessage(ChatColor.RED + "Hold the object to modify as an item in your main hand!");
                    return true;
                }

                String loreTextArg = ChatColor.translateAlternateColorCodes('&', String.join(" ", Arrays.copyOfRange(args, 1, args.length)))
                .replace('|', '\n')
                .replaceAll("\n\n", "|");
                ArrayList<String> loreList = loreTextArg.isEmpty() ? null : new ArrayList<>(Arrays.asList(loreTextArg.split("\n")));

                ItemMeta meta = itemStack.getItemMeta();
                meta.setLore(loreList);
                itemStack.setItemMeta(meta);
                player.getInventory().setItemInMainHand(itemStack);

                if (loreTextArg.isEmpty()) {
                    sender.sendMessage(ChatColor.GOLD + "The item in your main hand has had its lore cleared.");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "The item in your main hand is now lored:\n" +
                                       ChatColor.WHITE + loreTextArg);
                }
                return true;
            }
        }
        return false;
    } // onCommand
} // class BeastItemExecutor