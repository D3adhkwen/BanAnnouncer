/*
 * Copyright (C) 2019  OopsieWoopsie
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package eu.mcdb.ban_announcer.bukkit.listener;

import java.time.Duration;

import org.bukkit.event.EventHandler;
import org.maxgamer.maxbans.event.BanAddressEvent;
import org.maxgamer.maxbans.event.BanUserEvent;
import org.maxgamer.maxbans.event.KickUserEvent;
import org.maxgamer.maxbans.event.MaxBansRestrictEvent;
import org.maxgamer.maxbans.event.MuteAddressEvent;
import org.maxgamer.maxbans.event.MuteUserEvent;
import org.maxgamer.maxbans.event.UnbanAddressEvent;
import org.maxgamer.maxbans.event.UnbanUserEvent;
import org.maxgamer.maxbans.event.UnmuteAddressEvent;
import org.maxgamer.maxbans.event.UnmuteUserEvent;
import org.maxgamer.maxbans.event.WarnUserEvent;
import org.maxgamer.maxbans.orm.Restriction;
import org.maxgamer.maxbans.util.TemporalDuration;

import eu.mcdb.ban_announcer.PunishmentAction;
import eu.mcdb.ban_announcer.PunishmentAction.Type;
import eu.mcdb.ban_announcer.bukkit.BanAnnouncerBukkit;
import eu.mcdb.ban_announcer.bukkit.BukkitPunishmentListener;

public class MaxBansListener extends BukkitPunishmentListener {

    public MaxBansListener(BanAnnouncerBukkit plugin) {
        super(plugin);
    }

    @EventHandler
    public void onBanAddress(BanAddressEvent event) {
        handleRestriction(event, event.getBan(), Type.BANIP, Type.TEMPBANIP);
    }

    @EventHandler
    public void onBanUser(BanUserEvent event) {
        handleRestriction(event, event.getBan(), Type.BAN, Type.TEMPBAN);
    }

    @EventHandler
    public void onKickUser(KickUserEvent event) {
        final PunishmentAction punishment = new PunishmentAction(Type.KICK);
        final String staff = event.getSource().getName();
        final String player = event.getTarget().getName();

        punishment.setReason("unknown");
        punishment.setOperator(staff);
        punishment.setPlayer(player);

        handlePunishment(punishment);
    }

    @EventHandler
    public void onMuteAddress(MuteAddressEvent event) {} // unused

    @EventHandler
    public void onMuteUser(MuteUserEvent event) {
        handleRestriction(event, event.getMute(), Type.MUTE, Type.TEMPMUTE);
    }

    @EventHandler
    public void onUnbanAddress(UnbanAddressEvent event) {
        handleRevokedRestriction(event, Type.UNBANIP);
    }

    @EventHandler
    public void onUnbanUser(UnbanUserEvent event) {
        handleRevokedRestriction(event, Type.UNBAN);
    }

    @EventHandler
    public void onUnmuteAddress(UnmuteAddressEvent event) {} // unused

    @EventHandler
    public void onUnmuteUser(UnmuteUserEvent event) {
        handleRevokedRestriction(event, Type.UNMUTE);
    }

    @EventHandler
    public void onWarnUser(WarnUserEvent event) {
        handleRestriction(event, event.getWarning(), Type.WARN, Type.TEMPWARN);
    }

    // missing UnwarnUserEvent

    private void handleRestriction(MaxBansRestrictEvent<?> event, Restriction restriction, Type perm, Type temp) {
        final PunishmentAction punishment = new PunishmentAction();

        punishment.setPlayer(event.getTarget().getName());
        punishment.setOperator(event.isPlayerAdministered() ? event.getAdmin().getName() : "Console");
        punishment.setPermanent(restriction.getExpiresAt() == null);
        punishment.setType(punishment.isPermanent() ? perm : temp);
        punishment.setReason(restriction.getReason());
        punishment.setDuration(getDuration(restriction));

        handlePunishment(punishment);
    }

    private void handleRevokedRestriction(MaxBansRestrictEvent<?> event, Type type) {
        final PunishmentAction punishment = new PunishmentAction(type);

        punishment.setPlayer(event.getTarget().getName());
        punishment.setOperator(event.isPlayerAdministered() ? event.getAdmin().getName() : "Console");

        handlePunishment(punishment);
    }

    private String getDuration(Restriction restriction) {
        if (restriction.getExpiresAt() == null) return "permanent";
        long millis = (restriction.getExpiresAt().getEpochSecond() - restriction.getCreated().getEpochSecond()) * 1000;
        return new TemporalDuration(Duration.ofMillis(millis)).toString();
    }
}
