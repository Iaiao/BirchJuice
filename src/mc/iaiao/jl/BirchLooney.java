package mc.iaiao.jl;

import com.google.common.collect.Lists;
import net.minecraft.server.v1_15_R1.ChatComponentText;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.PacketPlayOutTitle;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class BirchLooney extends JavaPlugin implements Listener {
    private HashMap<Block, Long> blocks = new HashMap<>();
    private HashMap<Player, Long> effects = new HashMap<>();
    private List<String> lore;
    private String name;
    private boolean removeWither;
    private boolean hungerImmunity;
    private boolean poisonImmunity;

    public void onEnable() {
        saveDefaultConfig();
        lore = getConfig().getStringList("lore").stream().map(str -> ChatColor.translateAlternateColorCodes('&', str)).collect(Collectors.toList());
        name = ChatColor.translateAlternateColorCodes('&', getConfig().getString("item-name"));
        removeWither = getConfig().getBoolean("removeWither");
        hungerImmunity = getConfig().getBoolean("hungerImmunity");
        poisonImmunity = getConfig().getBoolean("poisonImmunity");

        getLogger().info(getConfig().getString("messages.onenable"));
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onOtverstie(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null || e.getItem() == null || !e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if (e.getItem().getType().equals(Material.STICK)) {
            if (e.getClickedBlock().getType().equals(Material.BIRCH_LOG)) {
                if (
                        e.getClickedBlock().getLocation().add(0, -1, 0).getBlock().getType().equals(Material.DIRT)
                                && e.getClickedBlock().getLocation().add(0, 1, 0).getBlock().getType().equals(Material.BIRCH_LOG)
                                && e.getClickedBlock().getLocation().add(0, 2, 0).getBlock().getType().equals(Material.BIRCH_LOG)
                ) {
                    if (!blocks.containsKey(e.getClickedBlock())) {
                        sendActionbar(e.getPlayer(), ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.started")));
                        if(!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                            ItemStack is = e.getPlayer().getInventory().getItemInMainHand();
                            is.setAmount(is.getAmount() - 1);
                        }
                        blocks.put(e.getClickedBlock(), System.currentTimeMillis() + 1000 * getConfig().getInt("juice-preparation"));
                    }
                }
            }
        }
        if (e.getItem().getType().equals(Material.GLASS_BOTTLE)) {
            if (e.getClickedBlock().getType().equals(Material.BIRCH_LOG)) {
                if (blocks.containsKey(e.getClickedBlock())) {
                    if (blocks.get(e.getClickedBlock()) < System.currentTimeMillis()) {
                        blocks.remove(e.getClickedBlock());
                        ItemStack is = e.getPlayer().getInventory().getItemInMainHand();
                        is.setAmount(is.getAmount() - 1);
                        ItemStack item = new ItemStack(Material.POTION);
                        PotionMeta meta = (PotionMeta) item.getItemMeta();
                        if (meta == null) meta = (PotionMeta) getServer().getItemFactory().getItemMeta(Material.POTION);
                        assert meta != null;
                        meta.setBasePotionData(new PotionData(PotionType.SLOW_FALLING));
                        meta.setDisplayName(name);
                        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
                        meta.setLore(Lists.newArrayList(lore));
                        item.setItemMeta(meta);
                        net.minecraft.server.v1_15_R1.ItemStack cok = CraftItemStack.asNMSCopy(item);
                        NBTTagCompound tag = cok.hasTag() ? cok.getTag() : new NBTTagCompound();
                        assert tag != null;
                        tag.setBoolean("isbirchjuice", true);
                        e.getPlayer().getInventory().addItem(CraftItemStack.asBukkitCopy(cok));
                    } else {
                        int time = (int) (blocks.get(e.getClickedBlock()) - System.currentTimeMillis()) / 1000;
                        sendActionbar(e.getPlayer(), ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.not-ready")).replace("{time}", String.valueOf(time)));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrinkJuice(PlayerItemConsumeEvent e) {
        ItemStack item = e.getItem();
        if (isJuiceBottle(item)) {
            e.setCancelled(true);
            if (e.getPlayer().getInventory().getItemInMainHand().equals(item)) {
                e.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.GLASS_BOTTLE));
            } else {
                e.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.GLASS_BOTTLE));
            }
            effects.put(e.getPlayer(), System.currentTimeMillis() + 1000 * getConfig().getInt("duration"));
            if(removeWither) {
                e.getPlayer().removePotionEffect(PotionEffectType.WITHER);
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!effects.containsKey(e.getPlayer()) || effects.get(e.getPlayer()) < System.currentTimeMillis()) {
                        cancel();
                    }
                    if(hungerImmunity) {
                        e.getPlayer().removePotionEffect(PotionEffectType.HUNGER);
                    }
                    if(poisonImmunity) {
                        e.getPlayer().removePotionEffect(PotionEffectType.POISON);
                    }
                }
            }.runTaskTimer(this, 1, 1);
        }
    }

    @EventHandler
    public void onBrewing(BrewEvent e) {
        if (
                isJuiceBottle(e.getContents().getItem(0))
                || isJuiceBottle(e.getContents().getItem(1))
                || isJuiceBottle(e.getContents().getItem(2))
        ) {
            e.setCancelled(true);
        }
    }

    private void sendActionbar(Player p, String message) {
        PacketPlayOutTitle packet = new PacketPlayOutTitle(
                PacketPlayOutTitle.EnumTitleAction.ACTIONBAR,
                new ChatComponentText(message)
        );
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
    }

    public boolean isJuiceBottle(ItemStack item) {
        if (item != null && item.getType().equals(Material.POTION)) {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            if (meta != null && meta.getBasePotionData().getType().equals(PotionType.SLOW_FALLING)) {
                if (meta.getItemFlags().contains(ItemFlag.HIDE_POTION_EFFECTS)) {
                    net.minecraft.server.v1_15_R1.ItemStack cok = CraftItemStack.asNMSCopy(item);
                    if (cok.hasTag() && cok.getTag().hasKey("isbirchjuice") && cok.getTag().getBoolean("isbirchjuice")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}