package org.duollectis.client.modules;

import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.settings.IntSetting;
import org.duollectis.client.settings.Setting;
import org.duollectis.client.settings.SettingGroup;
import org.duollectis.client.utils.FindItemResult;
import org.duollectis.client.utils.InvUtils;
import org.duollectis.client.utils.PacketUtil;

@ModuleInfo(
        name = "auto-gap",
        description = ""
)
public class AutoGap extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> minHealth = sgGeneral.add(new IntSetting.Builder()
        .name("min-health")
        .defaultValue(10)
        .min(0)
        .sliderRange(1, 36)
        .build());

    private final Setting<Integer> eatDelay = sgGeneral.add(new IntSetting.Builder()
        .name("eat-delay")
        .defaultValue(40)
        .min(0)
        .sliderRange(1, 100)
        .build());

//    private final Setting<Boolean> instantEat = sgGeneral.add(new BoolSetting.Builder()
//        .name("instant-eat")
//        .defaultValue(false)
//        .build());

    private int timer;

    @Override
    public void onDeactivate() {
        timer = 0;
    }

    @Override
    public void onTickPre() {

        if (mc.player == null) {
            return;
        }

        if (timer > 0) {
            timer--;
            return;
        }

        if (Math.round(mc.player.getHealth() + mc.player.getAbsorptionAmount()) <= minHealth.get()) {
            FindItemResult result = InvUtils.findInHotbar(Items.ENCHANTED_GOLDEN_APPLE);

            if (result.found()) {
                timer = eatDelay.get();
                InvUtils.swap(result.slot(), true);
                PacketUtil.useItem(Hand.MAIN_HAND);
                InvUtils.swapBack();
                PacketUtil.stopUsingItem();
            }
        }
    }
}
