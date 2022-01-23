package mc.thejsuser.stairchairsspigot;

import de.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

public class Chair implements Listener {

    //STATIC FIELDS
    private static final StairChairsSpigot mainInstance_ = StairChairsSpigot.getMainInstance();
    private static final NamespacedKey isChairNSK_ = new NamespacedKey(mainInstance_,"isChair");
    private static final NamespacedKey standIdNSK_ = new NamespacedKey(mainInstance_,"standID");
    private static final NamespacedKey isChairMountNSK_ = new NamespacedKey(mainInstance_,"isChairMount");
    private static final HashMap<Block,Chair> chairs_ = new HashMap<>();
    private static final HashMap<Chair,ArmorStand> mounts_ = new HashMap<>();

    //STATIC STATEMENT
    static {
        StairChairsSpigot.getMainInstance().getServer().getPluginManager().registerEvents(new eventListener(),mainInstance_);
    }

    //FIELDS
    private final Block block_;
    private final ChairTop top_;

    //GETTERS
    public Block getBlock() {
        return this.block_;
    }
    public ChairTop getTop() {
        return this.top_;
    }

    //CONSTRUCTORS
    public Chair(Block chair, ItemStack topCover) {
        this.block_ = chair;
        PersistentDataContainer dataContainer = new CustomBlockData(chair,mainInstance_);
        dataContainer.set(isChairNSK_, PersistentDataType.BYTE, (byte) 1);

        this.top_ = new ChairTop(this,topCover);
        dataContainer.set(standIdNSK_,PersistentDataType.STRING,this.top_.armorStand_.getUniqueId().toString());

        chairs_.put(this.block_,this);
    }
    private Chair(Block block, ArmorStand armorStand){
        this.block_ = block;
        this.top_ = new ChairTop(this, armorStand);
    }

    //STATIC METHODS
    public static boolean isChair(Block block){
        return new CustomBlockData(block,StairChairsSpigot.getMainInstance()).has(isChairNSK_,PersistentDataType.BYTE);
    }
    public static Chair getChair(Block block) {
        if (!isChair(block)) { return null; }

        if (chairs_.containsKey(block)) {
            return chairs_.get(block);
        } else {

            String uuid = new CustomBlockData(block, StairChairsSpigot.getMainInstance()).get(standIdNSK_, PersistentDataType.STRING);
            Collection<Entity> entities = block.getWorld().getNearbyEntities(block.getBoundingBox().shift(0, -1, 0));
            ArmorStand armorStand = null;
            for (Entity entity : entities) {
                if (entity.getUniqueId().toString().equals(uuid)) {
                    if (entity instanceof ArmorStand stand) {
                        armorStand = stand;
                        break;
                    }
                }
            }
            if (armorStand == null) {
                destroy(block);
                return null;
            }
            return new Chair(block,armorStand);
        }
    }
    private static void destroy(Block block) {
        PersistentDataContainer dataContainer = new CustomBlockData(block,mainInstance_);
        dataContainer.remove(isChairNSK_);
        dataContainer.remove(standIdNSK_);
    }

    //METHODS
    public void seat(Entity entity) {

        if (mounts_.containsKey(this)) {
            ArmorStand mount = mounts_.get(this);
            if(mount.getPassengers().size() > 0){
                return;
            } else {
                mount.remove();
            }
        }

        if(this.block_.getBlockData() instanceof Stairs stairs){
            float yaw;
            switch (stairs.getFacing()) {
                case SOUTH -> yaw = 180;
                case EAST -> yaw = 90;
                case WEST -> yaw = -90;
                default -> yaw = 0;
            }
            switch (stairs.getShape()) {
                case OUTER_LEFT, INNER_LEFT -> yaw += -45;
                case OUTER_RIGHT, INNER_RIGHT -> yaw += 45;
                default -> {}
            }
            Location location = this.block_.getLocation().add(.5,.3,.5);
            location.setYaw(yaw);
            ArmorStand armorStand = (ArmorStand) this.block_.getWorld().spawnEntity(location,EntityType.ARMOR_STAND);
            mounts_.put(this,armorStand);
            armorStand.setInvulnerable(true);
            armorStand.setSilent(true);
            armorStand.setMarker(true);
            armorStand.setGravity(false);
            //armorStand.setInvisible(true);
            armorStand.getPersistentDataContainer().set(isChairMountNSK_,PersistentDataType.BYTE,(byte)1);

            armorStand.addPassenger(entity);
        }
    }
    public void destroy(){
        chairs_.remove(this.block_);
        destroy(this.block_);
        this.top_.destroy();
    }

    //CLASSES
    private class ChairTop{

        //FIELDS
        private ArmorStand armorStand_;
        private Chair chair_;

        //CONSTRUCTORS
        private ChairTop(Chair chair, ItemStack top){
            this.chair_ = chair;
            Block chairBlock = this.chair_.getBlock();

            LivingEntity armorStand = (LivingEntity) chairBlock.getWorld().spawnEntity(
                    chairBlock.getLocation().add(.5,-.9,.5),EntityType.ARMOR_STAND
            );
            Objects.requireNonNull(armorStand.getEquipment()).setHelmet(top);
            armorStand.setGravity(false);
            armorStand.setSilent(true);
            armorStand.setInvisible(true);
            armorStand.setInvulnerable(true);
            this.armorStand_ = (ArmorStand) armorStand;
            this.armorStand_.setMarker(true);
            this.armorStand_.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING);
            this.armorStand_.addEquipmentLock(EquipmentSlot.HAND, ArmorStand.LockType.ADDING_OR_CHANGING);
            this.armorStand_.addEquipmentLock(EquipmentSlot.OFF_HAND, ArmorStand.LockType.ADDING_OR_CHANGING);
            this.armorStand_.addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.ADDING_OR_CHANGING);
            this.armorStand_.addEquipmentLock(EquipmentSlot.LEGS, ArmorStand.LockType.ADDING_OR_CHANGING);
            this.armorStand_.addEquipmentLock(EquipmentSlot.FEET, ArmorStand.LockType.ADDING_OR_CHANGING);
        }
        private ChairTop(Chair chair, ArmorStand armorStand) {
            this.chair_ = chair;
            this.armorStand_ = armorStand;
        }

        //GETTERS
        public ItemStack getCover() {
            return this.armorStand_.getEquipment().getHelmet();
        }
        private void destroy() {
            this.armorStand_.remove();
        }
    }

    private static class eventListener implements Listener {

        @EventHandler
        public void onDismount(EntityDismountEvent e) {
            if(e.getDismounted() instanceof ArmorStand mount) {
                if (mount.getPersistentDataContainer().has(isChairMountNSK_, PersistentDataType.BYTE)) {
                    for (Chair chair : Chair.mounts_.keySet()) {
                        if (mounts_.get(chair) == mount) {
                            mounts_.remove(chair);
                        }
                    }
                    mount.remove();
                    Entity entity = e.getEntity();
                    entity.teleport(entity.getLocation().add(0, 1, 0));
                }
            }
        }
    }
}
