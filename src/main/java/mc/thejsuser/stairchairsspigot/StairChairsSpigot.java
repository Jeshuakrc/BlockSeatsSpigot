package mc.thejsuser.stairchairsspigot;

import de.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class StairChairsSpigot extends JavaPlugin implements Listener {

    //FIELDS
    private final NamespacedKey isChairNSK = new NamespacedKey(this,"isChair");
    private final NamespacedKey standIdNSK = new NamespacedKey(this,"standID");

    @Override
    public void onEnable() {
        System.out.println("Hola xd");
        getServer().getPluginManager().registerEvents(this,this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onClickOnStairs(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();

        if(!(
                block != null &&
                e.getAction().equals(Action.RIGHT_CLICK_BLOCK) &&
                e.getHand().equals(EquipmentSlot.HAND)
        )) { return; }

        if (block.getType().toString().contains("STAIRS")) {
            if (!isChair(block)) {
                turnIntoChair(block, e.getPlayer());
                e.getPlayer().sendMessage("Convertido en silla! xd");
            }
        }

    }

    @EventHandler
    public void onClickCreeper(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        if (e.getRightClicked() instanceof Creeper creeper) {
            e.getRightClicked().setPassenger(player);
        }
    }

    private void turnIntoChair(Block block, Player player) {
        PersistentDataContainer dataContainer = new CustomBlockData(block, this);
        dataContainer.set(isChairNSK, PersistentDataType.BYTE, (byte) 1);
        Entity stand = block.getWorld().spawnEntity(block.getLocation().add(.5, -1, .5), EntityType.ARMOR_STAND);
        stand.setPersistent(true);
        stand.setSilent(true);
        stand.setGravity(false);
        dataContainer.set(standIdNSK, PersistentDataType.STRING, stand.getUniqueId().toString());
        stand.setPassenger(player);
    }
    private boolean isChair(Block block) {
        PersistentDataContainer dataContainer = new CustomBlockData(block,this);
        return dataContainer.has(isChairNSK,PersistentDataType.BYTE);
    }
}
