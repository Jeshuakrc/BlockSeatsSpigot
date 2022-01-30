package mc.thejsuser.stairchairsspigot;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

public final class BlockSeats extends JavaPlugin implements Listener {

    private static BlockSeats mainInstance_;

    @Override
    public void onEnable() {
        mainInstance_ = this;
        Seat.initialize();
        getServer().getPluginManager().registerEvents(this,this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static BlockSeats getMainInstance() {
        return mainInstance_;
    }

    @EventHandler
    public void onClickOnStairs(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (player.isSneaking()) { return; }
        Block block = e.getClickedBlock();
        if(!(
                block != null &&
                e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
        )) { return; }

        if (Seat.isSeat(block)) {
            try {
                Seat.getSeat(block).seat(player);
            } catch (NullPointerException ex) {
                Location loc = block.getLocation();
                Bukkit.getLogger().warning(String.format(
                        "Chair expected at [%1$s, %2$s, %3$s] in %4$s, but it wasn't there!",
                        loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), Objects.requireNonNull(loc.getWorld()).getName()
                ));
                onClickOnStairs(e);
                return;
            }
        } else {
            if (!Seat.isSeatEligible(block)) { return; }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType().toString().contains("CARPET")) {
                new Seat(block,new ItemStack(item.getType()));
                if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        item.setType(null);
                    }
                    player.getInventory().setItem(e.getHand(), item);
                }
            } else { return; }
        }
        e.setCancelled(true);
    }
}
