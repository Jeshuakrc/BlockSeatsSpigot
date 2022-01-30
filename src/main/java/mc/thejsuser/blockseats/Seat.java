package mc.thejsuser.blockseats;

import de.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.spigotmc.event.entity.EntityDismountEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

public class Seat implements Listener {

    //STATIC FIELDS
    private static final BlockSeats mainInstance_ = BlockSeats.getMainInstance();
    private static final NamespacedKey isChairNSK_ = new NamespacedKey(mainInstance_,"isChair");
    private static final NamespacedKey standIdNSK_ = new NamespacedKey(mainInstance_,"standID");
    private static final NamespacedKey isChairMountNSK_ = new NamespacedKey(mainInstance_,"isChairMount");
    private static final HashMap<Block, Seat> seats_ = new HashMap<>();
    private static final HashMap<Seat,ArmorStand> mounts_ = new HashMap<>();

    //STATIC STATEMENT
    static {
        //Registering the EventListener
        BlockSeats.getMainInstance().getServer().getPluginManager().registerEvents(new eventListener(),mainInstance_);
    }

    //FIELDS
    private final Block block_;
    private final SeatTop top_;

    //GETTERS
    public Block getBlock() {
        return this.block_;
    }
    public SeatTop getTop() {
        return this.top_;
    }

    //CONSTRUCTORS
    public Seat(Block chair, ItemStack topCover) {
        this.block_ = chair;
        PersistentDataContainer dataContainer = new CustomBlockData(chair,mainInstance_);
        dataContainer.set(isChairNSK_, PersistentDataType.BYTE, (byte) 1);

        this.top_ = new SeatTop(this,topCover);
        dataContainer.set(standIdNSK_,PersistentDataType.STRING,this.top_.armorStand_.getUniqueId().toString());

        seats_.put(this.block_,this);
    }
    private Seat(Block block, ArmorStand armorStand){
        this.block_ = block;
        this.top_ = new SeatTop(this, armorStand);
    }

    //STATIC METHODS
    public static void initialize() {}
    public static boolean isSeat(Block block){
        return new CustomBlockData(block,mainInstance_).has(isChairNSK_,PersistentDataType.BYTE);
    }
    public static Seat getSeat(Block block) {
        // Checking if the provided block has been marked as chair
        if (!isSeat(block)) { return null; }

        // Making sure there's a top armor stand still existing for this one.
        String uuid = new CustomBlockData(block, BlockSeats.getMainInstance()).get(standIdNSK_, PersistentDataType.STRING);
        BoundingBox boundingBox = new BoundingBox(block.getX(),block.getY() - 1, block.getZ(), block.getX() + 1, block.getY(), block.getZ() + 1);
        Collection<Entity> entities = block.getWorld().getNearbyEntities(boundingBox);
        ArmorStand armorStand = null;
        for (Entity entity : entities) {
            if (entity.getUniqueId().toString().equals(uuid)) {
                if (entity instanceof ArmorStand stand) {
                    armorStand = stand;
                    break;
                }
            }
        }

        // If not, returning null and un-marking this block as chair.
        if (armorStand == null) {
            destroy(block);
            return null;
        }

        // Checking if there's chair related to this block in memory, if so, returning that one
        // if not, returning a new one
        return (seats_.containsKey(block)) ? seats_.get(block) : new Seat(block,armorStand);
    }
    public static boolean isSeatEligible(Block block) {

        BlockData blockData = block.getBlockData();
        if (blockData instanceof Stairs stairs) {
            return stairs.getHalf().equals(Stairs.Half.BOTTOM);
        } else if (blockData instanceof Slab slab) {
            return slab.getType().equals(Slab.Type.BOTTOM);
        }
        return false;
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

        Block block = this.block_;
        float yaw = 0;
        double[] shift = {.5,.3,.5};
        if(block.getBlockData() instanceof Stairs stairs){
            switch (stairs.getFacing()) {
                case SOUTH -> { yaw = 180; shift[2] += -.1; }
                case EAST -> { yaw = 90; shift[0] += -.1; }
                case WEST -> { yaw = -90; shift[0] += .1; }
                default -> { yaw = 0; shift[2] += .1; }
            }
            switch (stairs.getShape()) {
                case OUTER_LEFT, INNER_LEFT -> yaw += -45;
                case OUTER_RIGHT, INNER_RIGHT -> yaw += 45;
                default -> {}
            }
        } else if (block.getBlockData() instanceof Slab slab) {
            Location entityLoc = entity.getLocation(), blockLoc = block.getLocation().add(.5,0,.5);

            double  dx = entityLoc.getX() - blockLoc.getX(),
                    dz = entityLoc.getZ() - blockLoc.getZ(),
                    ang = Math.toDegrees(Math.atan2(dx,dz));

            yaw = 45*Math.round(-ang/45);
            shift[0] += .3 * Math.sin(Math.toRadians(yaw));
            shift[2] += -.3 * Math.cos(Math.toRadians(yaw));
        }

        Location location = entity.getLocation();
        location.setYaw(yaw);
        entity.teleport(location);

        location = block.getLocation().add(shift[0],shift[1],shift[2]);
        location.setYaw(yaw);
        ArmorStand armorStand = (ArmorStand) this.block_.getWorld().spawnEntity(location,EntityType.ARMOR_STAND);
        armorStand.setInvulnerable(true);
        armorStand.setSilent(true);
        armorStand.setMarker(true);
        armorStand.setGravity(false);
        armorStand.setInvisible(true);
        armorStand.getPersistentDataContainer().set(isChairMountNSK_,PersistentDataType.BYTE,(byte)1);

        armorStand.addPassenger(entity);
        mounts_.put(this,armorStand);
    }
    public void destroy(){
        if (mounts_.containsKey(this)) {
            ArmorStand mount = mounts_.get(this);
            for (Entity entity : mount.getPassengers()) {
                mount.removePassenger(entity);
            }
            mount.remove();
        }
        seats_.remove(this.block_);
        destroy(this.block_);
        this.top_.destroy();
    }

    //CLASSES
    private class SeatTop {

        //FIELDS
        private ArmorStand armorStand_;
        private Seat chair_;
        private static final NamespacedKey isChairTopNSK_ = new NamespacedKey(mainInstance_,"isChairTop");

        //CONSTRUCTORS
        private SeatTop(Seat chair, ItemStack top){
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
            this.armorStand_.getPersistentDataContainer().set(isChairTopNSK_,PersistentDataType.BYTE,(byte)1);
        }
        private SeatTop(Seat seat, ArmorStand armorStand) {
            this.chair_ = seat;
            this.armorStand_ = armorStand;
        }

        //GETTERS
        public ItemStack getCover() {
            return this.armorStand_.getEquipment().getHelmet();
        }
        private void destroy() {
            ArmorStand armorStand = this.armorStand_;
            armorStand.getWorld().dropItem(
                    chair_.getBlock().getLocation(),
                    armorStand.getEquipment().getHelmet()
            );
            armorStand.remove();
        }

        //Static methods
        private static boolean isTop(ArmorStand armorStand) {
            return armorStand.getPersistentDataContainer().has(isChairTopNSK_,PersistentDataType.BYTE);
        }
    }

    private static class eventListener implements Listener {
        @EventHandler
        public void onDismount(EntityDismountEvent e) {
            if(e.getDismounted() instanceof ArmorStand mount) {
                if (mount.getPersistentDataContainer().has(isChairMountNSK_, PersistentDataType.BYTE)) {
                    for (Seat seat : Seat.mounts_.keySet()) {
                        if (mounts_.get(seat) == mount) {
                            mounts_.remove(seat);
                        }
                    }
                    mount.remove();
                    Entity entity = e.getEntity();
                    entity.teleport(entity.getLocation().add(0, 1, 0));
                }
            }
        }

        @EventHandler
        public void onBlockEvent(BlockPhysicsEvent e) {
            Block block = e.getBlock();
            if (!Seat.isSeat(block)) { return; }
            if (!Seat.isSeatEligible(block)) {
                try {
                    Seat.getSeat(block).destroy();
                } catch (NullPointerException ex) {
                    Location loc = block.getLocation();
                    Bukkit.getLogger().warning(String.format(
                            "Chair expected at [%1$s, %2$s, %3$s] in %4$s, but it wasn't there!",
                            loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), Objects.requireNonNull(loc.getWorld()).getName()
                    ));
                }
            }
        }

        @EventHandler
        public void onUnloadChunk(ChunkUnloadEvent e) {
            for (Entity entity : e.getChunk().getEntities()) {
                if (entity instanceof ArmorStand armorStand) {
                    if (armorStand.getPersistentDataContainer().has(isChairMountNSK_,PersistentDataType.BYTE)) {
                        armorStand.remove();
                    }
                    if (SeatTop.isTop(armorStand)) {
                        if (!Seat.isSeat(armorStand.getWorld().getBlockAt(armorStand.getLocation().add(0,1,0)))) {
                            armorStand.remove();
                        }
                    }
                }
            }
        }
    }
}
