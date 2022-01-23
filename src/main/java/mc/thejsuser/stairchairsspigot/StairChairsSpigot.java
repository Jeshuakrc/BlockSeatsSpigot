package mc.thejsuser.stairchairsspigot;

import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.entity.EntityDismountEvent;

public final class StairChairsSpigot extends JavaPlugin implements Listener {

    private static StairChairsSpigot mainInstance_;

    @Override
    public void onEnable() {
        mainInstance_ = this;
        System.out.println("Hola xd");
        getServer().getPluginManager().registerEvents(this,this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static StairChairsSpigot getMainInstance() {
        return mainInstance_;
    }

    @EventHandler
    public void onClickOnStairs(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();

        if(!(
                block != null &&
                e.getAction().equals(Action.RIGHT_CLICK_BLOCK) &&
                e.getHand().equals(EquipmentSlot.HAND)
        )) { return; }

        Player player = e.getPlayer();

        if (Chair.isChair(block)) {
            player.sendMessage("Seat!");
            Chair.getChair(block).seat(player);
        } else {
            ItemStack item = player.getInventory().getItemInMainHand();
            String blockMaterial = block.getType().toString();
            if(
                    (blockMaterial.contains("STAIRS") || blockMaterial.contains("SLAB")) &&
                    item.getType().toString().contains("CARPET")
            ){
                ItemStack newItem = item.clone();
                newItem.setAmount(1);
                new Chair(block,newItem);
            }

        }

    }

    @EventHandler
    public void onDismount(EntityDismountEvent e) {

    }
}
