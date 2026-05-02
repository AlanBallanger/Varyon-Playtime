package com.varyon.playtime.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.playtime.Playtime;
import com.varyon.playtime.config.PlaytimeConfig;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class PlaytimeLeaderboardGui extends InteractiveCustomUIPage<PlaytimeLeaderboardGui.LeaderboardData> {

    private final PlayerRef playerRef;
    private String currentPeriod = "all";

    public PlaytimeLeaderboardGui(@NonNullDecl PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, LeaderboardData.CODEC);
        this.playerRef = playerRef;
    }

    @Override
    public void build(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl UICommandBuilder cmd,
            @NonNullDecl UIEventBuilder events,
            @NonNullDecl Store<EntityStore> store) {

        cmd.append("Pages/PlaytimeLeaderboard.ui");

        PlaytimeConfig.GuiSettings gui = Playtime.get().getConfigManager().getConfig().gui;

        cmd.set("#BtnAll.Text", gui.buttonAll);
        cmd.set("#BtnDaily.Text", gui.buttonDaily);
        cmd.set("#BtnWeekly.Text", gui.buttonWeekly);
        cmd.set("#BtnMonthly.Text", gui.buttonMonthly);
        cmd.set("#FooterLabel.Text", gui.footerTitle);
        cmd.set("#SelfRankKey.Text", gui.footerRankCaption);
        cmd.set("#SelfTimeKey.Text", gui.footerTimeCaption);

        bindButton(events, "#BtnAll", "all");
        bindButton(events, "#BtnDaily", "daily");
        bindButton(events, "#BtnWeekly", "weekly");
        bindButton(events, "#BtnMonthly", "monthly");

        refreshLeaderboard(cmd);
    }

    private void refreshLeaderboard(UICommandBuilder cmd) {
        PlaytimeConfig.GuiSettings gui = Playtime.get().getConfigManager().getConfig().gui;

        String periodName = gui.buttonAll;
        if (currentPeriod.equals("daily")) {
            periodName = gui.buttonDaily;
        } else if (currentPeriod.equals("weekly")) {
            periodName = gui.buttonWeekly;
        } else if (currentPeriod.equals("monthly")) {
            periodName = gui.buttonMonthly;
        }

        cmd.set("#TitleText.Text", gui.title + " (" + periodName + ")");

        cmd.clear("#ListContainer");

        Map<String, Long> top = Playtime.get().getService().getTopPlayers(currentPeriod);

        int index = 0;
        for (Map.Entry<String, Long> entry : top.entrySet()) {
            if (index >= 10) {
                break;
            }
            cmd.append("#ListContainer", "Pages/PlaytimeLeaderboardEntry.ui");

            String elementId = "#ListContainer[" + index + "]";
            cmd.set(elementId + " #Rank.Text", "#" + (index + 1));
            cmd.set(elementId + " #Name.Text", entry.getKey());
            cmd.set(elementId + " #Time.Text", format(entry.getValue()));

            index++;
        }

        long myTime = Playtime.get().getService().getPlaytime(playerRef.getUuid().toString(), currentPeriod);
        int myRank = Playtime.get().getService().getRank(playerRef.getUuid().toString(), currentPeriod);

        cmd.set("#SelfRankValue.Text", myRank > 0 ? "#" + myRank : gui.rankIfNone);
        cmd.set("#SelfTimeValue.Text", format(myTime));

        applyPeriodTabStyles(cmd);
    }

    private void applyPeriodTabStyles(UICommandBuilder cmd) {
        cmd.set(
                "#BtnAll.Style",
                Value.ref(
                        "Common.ui",
                        currentPeriod.equals("all") ? "SmallTertiaryTextButtonStyle" : "SmallSecondaryTextButtonStyle"));
        cmd.set(
                "#BtnDaily.Style",
                Value.ref(
                        "Common.ui",
                        currentPeriod.equals("daily") ? "SmallTertiaryTextButtonStyle" : "SmallSecondaryTextButtonStyle"));
        cmd.set(
                "#BtnWeekly.Style",
                Value.ref(
                        "Common.ui",
                        currentPeriod.equals("weekly") ? "SmallTertiaryTextButtonStyle" : "SmallSecondaryTextButtonStyle"));
        cmd.set(
                "#BtnMonthly.Style",
                Value.ref(
                        "Common.ui",
                        currentPeriod.equals("monthly") ? "SmallTertiaryTextButtonStyle" : "SmallSecondaryTextButtonStyle"));
    }

    private void bindButton(UIEventBuilder events, String id, String action) {
        events.addEventBinding(CustomUIEventBindingType.Activating, id, EventData.of("Action", action), false);
    }

    @Override
    public void handleDataEvent(
            @NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl LeaderboardData data) {
        super.handleDataEvent(ref, store, data);

        if (data.action != null) {
            this.currentPeriod = data.action;
            UICommandBuilder cmd = new UICommandBuilder();
            refreshLeaderboard(cmd);
            sendUpdate(cmd, new UIEventBuilder(), false);
        }
    }

    private String format(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return hours + "h " + minutes + "m";
    }

    public static class LeaderboardData {
        public static final BuilderCodec<LeaderboardData> CODEC =
                BuilderCodec.<LeaderboardData>builder(LeaderboardData.class, LeaderboardData::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING), (d, v, e) -> d.action = v, (d, e) -> d.action)
                        .add()
                        .build();
        private String action;
    }
}
