package com.massivecraft.factions.listeners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.*;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.zcore.util.TextUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class FactionsPlayerListener implements Listener {
    public P p;
    // for handling people who repeatedly spam attempts to open a door (or similar) in another faction's territory
    private Map<String, InteractAttemptSpam> interactSpammers = new HashMap<String, InteractAttemptSpam>();

    public FactionsPlayerListener(P p) {
        this.p = p;
    }

    public static boolean playerCanUseItemHere(Player player, Location location, Item material, boolean justCheck) {
        String name = player.getName();
        if (Conf.playersWhoBypassAllProtection.contains(name)) return true;

        FPlayer me = FPlayers.i.get(name);
        if (me.isAdminBypassing()) return true;

        FLocation loc = new FLocation(location);
        Faction otherFaction = Board.getFactionAt(loc);

        if (otherFaction.hasPlayersOnline()) {
            if (!Conf.territoryDenyUseageMaterials.contains(material))
                return true; // Item isn't one we're preventing for online factions.
        } else {
            if (!Conf.territoryDenyUseageMaterialsWhenOffline.contains(material))
                return true; // Item isn't one we're preventing for offline factions.
        }

        if (otherFaction.isNone()) {
            if (!Conf.wildernessDenyUseage || Conf.worldsNoWildernessProtection.contains(location.getLevel().getName()))
                return true; // This is not faction territory. Use whatever you like here.

            if (!justCheck)
                me.msg("<b>You can't use <h>%s<b> in the wilderness.", TextUtil.getMaterialName(material));

            return false;
        } else if (otherFaction.isSafeZone()) {
            if (!Conf.safeZoneDenyUseage || Permission.MANAGE_SAFE_ZONE.has(player))
                return true;

            if (!justCheck)
                me.msg("<b>You can't use <h>%s<b> in a safe zone.", TextUtil.getMaterialName(material));

            return false;
        } else if (otherFaction.isWarZone()) {
            if (!Conf.warZoneDenyUseage || Permission.MANAGE_WAR_ZONE.has(player))
                return true;

            if (!justCheck)
                me.msg("<b>You can't use <h>%s<b> in a war zone.", TextUtil.getMaterialName(material));

            return false;
        }

        Faction myFaction = me.getFaction();
        Relation rel = myFaction.getRelationTo(otherFaction);

        // Cancel if we are not in our own territory
        if (rel.confDenyUseage()) {
            if (!justCheck)
                me.msg("<b>You can't use <h>%s<b> in the territory of <h>%s<b>.", TextUtil.getMaterialName(material), otherFaction.getTag(myFaction));

            return false;
        }

        // Also cancel if player doesn't have ownership rights for this claim
        if (Conf.ownedAreasEnabled && Conf.ownedAreaDenyUseage && !otherFaction.playerHasOwnershipRights(me, loc)) {
            if (!justCheck)
                me.msg("<b>You can't use <h>%s<b> in this territory, it is owned by: %s<b>.", TextUtil.getMaterialName(material), otherFaction.getOwnerListString(loc));

            return false;
        }

        return true;
    }

    public static boolean canPlayerUseBlock(Player player, Block block, boolean justCheck) {
        String name = player.getName();
        if (Conf.playersWhoBypassAllProtection.contains(name)) return true;

        FPlayer me = FPlayers.i.get(name);
        if (me.isAdminBypassing()) return true;

        String material = block.getId();
        FLocation loc = new FLocation(block);
        Faction otherFaction = Board.getFactionAt(loc);

        // no door/chest/whatever protection in wilderness, war zones, or safe zones
        if (!otherFaction.isNormal())
            return true;

        // We only care about some material types.
        if (otherFaction.hasPlayersOnline()) {
            if (!Conf.territoryProtectedMaterials.contains(material))
                return true;
        } else {
            if (!Conf.territoryProtectedMaterialsWhenOffline.contains(material))
                return true;
        }

        Faction myFaction = me.getFaction();
        Relation rel = myFaction.getRelationTo(otherFaction);

        // You may use any block unless it is another faction's territory...
        if (rel.isNeutral() || (rel.isEnemy() && Conf.territoryEnemyProtectMaterials) || (rel.isAlly() && Conf.territoryAllyProtectMaterials)) {
            if (!justCheck)
                me.msg("<b>You can't %s <h>%s<b> in the territory of <h>%s<b>.", (material == Block.FARMLAND ? "trample" : "use"), TextUtil.getMaterialName(Item.get(material)), otherFaction.getTag(myFaction));

            return false;
        }

        // Also cancel if player doesn't have ownership rights for this claim
        if (Conf.ownedAreasEnabled && Conf.ownedAreaProtectMaterials && !otherFaction.playerHasOwnershipRights(me, loc)) {
            if (!justCheck)
                me.msg("<b>You can't use <h>%s<b> in this territory, it is owned by: %s<b>.", TextUtil.getMaterialName(Item.get(material)), otherFaction.getOwnerListString(loc));

            return false;
        }

        return true;
    }

    public static boolean preventCommand(String fullCmd, Player player) {
        if ((Conf.territoryNeutralDenyCommands.isEmpty() && Conf.territoryEnemyDenyCommands.isEmpty() && Conf.permanentFactionMemberDenyCommands.isEmpty()))
            return false;

        fullCmd = fullCmd.toLowerCase();

        FPlayer me = FPlayers.i.get(player);

        String shortCmd;  // command without the slash at the beginning
        if (fullCmd.startsWith("/"))
            shortCmd = fullCmd.substring(1);
        else {
            shortCmd = fullCmd;
            fullCmd = "/" + fullCmd;
        }

        if
                (
                me.hasFaction()
                        &&
                        !me.isAdminBypassing()
                        &&
                        !Conf.permanentFactionMemberDenyCommands.isEmpty()
                        &&
                        me.getFaction().isPermanent()
                        &&
                        isCommandInList(fullCmd, shortCmd, Conf.permanentFactionMemberDenyCommands.iterator())
                ) {
            me.msg("<b>You can't use the command \"" + fullCmd + "\" because you are in a permanent faction.");
            return true;
        }

        if (!me.isInOthersTerritory()) {
            return false;
        }

        Relation rel = me.getRelationToLocation();
        if (rel.isAtLeast(Relation.ALLY)) {
            return false;
        }

        if
                (
                rel.isNeutral()
                        &&
                        !Conf.territoryNeutralDenyCommands.isEmpty()
                        &&
                        !me.isAdminBypassing()
                        &&
                        isCommandInList(fullCmd, shortCmd, Conf.territoryNeutralDenyCommands.iterator())
                ) {
            me.msg("<b>You can't use the command \"" + fullCmd + "\" in neutral territory.");
            return true;
        }

        if
                (
                rel.isEnemy()
                        &&
                        !Conf.territoryEnemyDenyCommands.isEmpty()
                        &&
                        !me.isAdminBypassing()
                        &&
                        isCommandInList(fullCmd, shortCmd, Conf.territoryEnemyDenyCommands.iterator())
                ) {
            me.msg("<b>You can't use the command \"" + fullCmd + "\" in enemy territory.");
            return true;
        }

        return false;
    }

    private static boolean isCommandInList(String fullCmd, String shortCmd, Iterator<String> iter) {
        String cmdCheck;
        while (iter.hasNext()) {
            cmdCheck = iter.next();
            if (cmdCheck == null) {
                iter.remove();
                continue;
            }

            cmdCheck = cmdCheck.toLowerCase();
            if (fullCmd.startsWith(cmdCheck) || shortCmd.startsWith(cmdCheck))
                return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Make sure that all online players do have a fplayer.
        final FPlayer me = FPlayers.i.get(event.getPlayer());

        // Update the lastLoginTime for this fplayer
        me.setLastLoginTime(System.currentTimeMillis());

        // Store player's current FLocation and notify them where they are
        me.setLastStoodAt(new FLocation(event.getPlayer().getLocation()));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        FPlayer me = FPlayers.i.get(event.getPlayer());

        // Make sure player's power is up to date when they log off.
        me.getPower();
        // and update their last login time to point to when the logged off, for auto-remove routine
        me.setLastLoginTime(System.currentTimeMillis());

        Faction myFaction = me.getFaction();
        if (myFaction != null) {
            myFaction.memberLoggedOff();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;

        // quick check to make sure player is moving between chunks; good performance boost
        if
                (
                event.getFrom().getFloorX() >> 4 == event.getTo().getFloorX() >> 4
                        &&
                        event.getFrom().getFloorZ() >> 4 == event.getTo().getFloorZ() >> 4
                        &&
                        event.getFrom().getLevel() == event.getTo().getLevel()
                )
            return;

        Player player = event.getPlayer();
        FPlayer me = FPlayers.i.get(player);

        // Did we change coord?
        FLocation from = me.getLastStoodAt();
        FLocation to = new FLocation(event.getTo());

        if (from.equals(to)) {
            return;
        }

        // Yes we did change coord (:

        me.setLastStoodAt(to);

        // Did we change "host"(faction)?
        Faction factionFrom = Board.getFactionAt(from);
        Faction factionTo = Board.getFactionAt(to);
        boolean changedFaction = (factionFrom != factionTo);

        if (me.isMapAutoUpdating()) {
            me.sendMessage(Board.getMap(me.getFaction(), to, player.getLocation().getYaw()));
        } else {
            Faction myFaction = me.getFaction();
            String ownersTo = myFaction.getOwnerListString(to);

            if (changedFaction) {
                me.sendFactionHereMessage();
                if (Conf.ownedAreasEnabled && Conf.ownedMessageOnBorder && myFaction == factionTo && !ownersTo.isEmpty()) {
                    {
                        me.sendMessage(Conf.ownedLandMessage + ownersTo);
                    }
                } else if
                        (
                        Conf.ownedAreasEnabled
                                &&
                                Conf.ownedMessageInsideTerritory
                                &&
                                factionFrom == factionTo
                                &&
                                myFaction == factionTo
                        ) {
                    String ownersFrom = myFaction.getOwnerListString(from);
                    if (Conf.ownedMessageByChunk || !ownersFrom.equals(ownersTo)) {
                        if (!ownersTo.isEmpty())
                            me.sendMessage(Conf.ownedLandMessage + ownersTo);
                        else if (!Conf.publicLandMessage.isEmpty())
                            me.sendMessage(Conf.publicLandMessage);
                    }
                }
            }

            if (me.getAutoClaimFor() != null) {
                me.attemptClaim(me.getAutoClaimFor(), event.getTo(), true);
            } else if (me.isAutoSafeClaimEnabled()) {
                if (!Permission.MANAGE_SAFE_ZONE.has(player)) {
                    me.setIsAutoSafeClaimEnabled(false);
                } else {
                    if (!Board.getFactionAt(to).isSafeZone()) {
                        Board.setFactionAt(Factions.i.getSafeZone(), to);
                        me.msg("<i>This land is now a safe zone.");
                    }
                }
            } else if (me.isAutoWarClaimEnabled()) {
                if (!Permission.MANAGE_WAR_ZONE.has(player)) {
                    me.setIsAutoWarClaimEnabled(false);
                } else {
                    if (!Board.getFactionAt(to).isWarZone()) {
                        Board.setFactionAt(Factions.i.getWarZone(), to);
                        me.msg("<i>This land is now a war zone.");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        // only need to check right-clicks and physical as of MC 1.4+; good performance boost
        if (event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && event.getAction() != PlayerInteractEvent.Action.PHYSICAL)
            return;

        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block == null) return;  // clicked in air, apparently

        if (event.getAction() == PlayerInteractEvent.Action.PHYSICAL && block.getId() == Block.FARMLAND) {
            if (!FactionsBlockListener.playerCanBuildDestroyBlock(player, new Location(block.getFloorX(), block.getFloorY(), block.getFloorZ(), 0, 0, block.getLevel()), "destroy", true)) {
                // Hack: Farmland protection
                event.setCancelled(true);
            }
        } else if (!canPlayerUseBlock(player, block, false)) {
            event.setCancelled(true);
            if (Conf.handleExploitInteractionSpam) {
                String name = player.getName();
                InteractAttemptSpam attempt = interactSpammers.get(name);
                if (attempt == null) {
                    attempt = new InteractAttemptSpam();
                    interactSpammers.put(name, attempt);
                }
                int count = attempt.increment();
                if (count >= 10) {
                    FPlayer me = FPlayers.i.get(name);
                    me.msg("<b>Ouch, that is starting to hurt. You should give it a rest.");
                    player.attack((float) Math.floor((double) count / 10));
                }
            }
            return;
        }

        if (event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
            return;  // only interested on right-clicks for below

        if (!playerCanUseItemHere(player, new Location(block.getFloorX(), block.getFloorY(), block.getFloorZ(), 0, 0, block.getLevel()), event.getItem(), false)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        FPlayer me = FPlayers.i.get(event.getPlayer());

        me.getPower();  // update power, so they won't have gained any while dead

        // PASSED

//        Pair home = me.getFaction().getHome();
//        if
//                (
//                Conf.homesEnabled
//                        &&
//                        Conf.homesTeleportToOnDeath
//                        &&
//                        home != null
//                        &&
//                        (
//                                Conf.homesRespawnFromNoPowerLossWorlds
//                                        ||
//                                        !Conf.worldsNoPowerLoss.contains(event.getPlayer().getLevel().getName())
//                        )
//                ) {
//            event.setRespawnPosition(home);
//        }
    }

    // For some reason onPlayerInteract() sometimes misses bucket events depending on distance (something like 2-3 blocks away isn't detected),
    // but these separate bucket events below always fire without fail
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlockClicked();
        Player player = event.getPlayer();

        if (!playerCanUseItemHere(player, new Location(block.getFloorX(), block.getFloorY(), block.getFloorZ(), 0, 0, block.getLevel()), event.getBucket(), false)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlockClicked();
        Player player = event.getPlayer();

        if (!playerCanUseItemHere(player, new Location(block.getFloorX(), block.getFloorY(), block.getFloorZ(), 0, 0, block.getLevel()), event.getBucket(), false)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.isCancelled()) return;

        FPlayer badGuy = FPlayers.i.get(event.getPlayer());
        if (badGuy == null) {
            return;
        }

        // if player was banned (not just kicked), get rid of their stored info
        if (Conf.removePlayerDataWhenBanned && (event.getReasonEnum() == PlayerKickEvent.Reason.NAME_BANNED || event.getReasonEnum() == PlayerKickEvent.Reason.IP_BANNED)) {
            if (badGuy.getRole() == Role.ADMIN)
                badGuy.getFaction().promoteNewLeader();

            badGuy.leave(false);
            badGuy.detach();
        }
    }

    private static class InteractAttemptSpam {
        private int attempts = 0;
        private long lastAttempt = System.currentTimeMillis();

        // returns the current attempt count
        public int increment() {
            long Now = System.currentTimeMillis();
            if (Now > lastAttempt + 2000)
                attempts = 1;
            else
                attempts++;
            lastAttempt = Now;
            return attempts;
        }
    }
}
