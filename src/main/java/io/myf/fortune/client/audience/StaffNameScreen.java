package io.myf.fortune.client.audience;

import io.myf.fortune.expansion.audience.AudienceStaffItem;
import io.myf.fortune.expansion.audience.AudienceStaffNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public final class StaffNameScreen extends Screen {
    private static final Component INPUT = Component.translatable("screen.fortune_spells.audience_staff.input");
    private final UUID id;
    private int slot;
    private int left;
    private int top;
    private int panelWidth;
    private EditBox input;
    private Button confirm;
    private String draft = "";

    StaffNameScreen(UUID id, int slot) {
        super(Component.translatable("screen.fortune_spells.audience_staff.name"));
        this.id = id;
        this.slot = slot;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(286, width - 14);
        left = (width - panelWidth) / 2;
        top = (height - 132) / 2;
        int textX = left + 76;
        int fieldWidth = panelWidth - 88;
        input = new EditBox(font, textX, top + 47, fieldWidth, 20, INPUT);
        input.setHint(INPUT);
        input.setMaxLength(AudienceStaffItem.MAX_NAME_LENGTH * 2);
        input.setValue(draft);
        addRenderableWidget(input);
        int buttonWidth = (panelWidth - 28) / 2;
        confirm = addRenderableWidget(Button.builder(Component.translatable("screen.fortune_spells.audience_staff.confirm"),
                button -> submit()).bounds(left + 10, top + 99, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fortune_spells.audience_staff.later"),
                button -> onClose()).bounds(left + panelWidth - buttonWidth - 10, top + 99, buttonWidth, 20).build());
        input.setResponder(value -> {
            draft = value;
            confirm.active = AudienceStaffItem.canInscribe(value.strip());
        });
        confirm.active = AudienceStaffItem.canInscribe(draft.strip());
        setInitialFocus(input);
    }

    @Override
    public void tick() {
        input.tick();
        if (minecraft.player == null) {
            onClose();
            return;
        }
        slot = AudienceStaffItem.findUninscribed(minecraft.player.getInventory(), id, slot);
        if (slot < 0 || !minecraft.player.isAlive()) onClose();
    }

    private void submit() {
        if (confirm.active && slot >= 0) {
            AudienceStaffNetwork.submit(slot, id, draft.strip());
            onClose();
        }
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int right = left + panelWidth;
        graphics.fill(left - 1, top - 1, right + 1, top + 133, 0xff967747);
        graphics.fillGradient(left, top, right, top + 132, 0xff201c2b, 0xff13111b);
        graphics.fill(left + 11, top + 30, right - 11, top + 31, 0xff5b4451);
        graphics.drawString(font, title, left + 12, top + 12, 0xffffe2ae);
        if (minecraft.player != null && slot >= 0) {
            ItemStack staff = minecraft.player.getInventory().getItem(slot);
            graphics.pose().pushPose();
            graphics.pose().translate(left + 11, top + 33, 0);
            graphics.pose().scale(3.7f, 3.7f, 1);
            graphics.renderItem(staff, 0, 0);
            graphics.pose().popPose();
        }
        graphics.enableScissor(left + 76, top + 70, right - 12, top + 94);
        graphics.drawString(font, AudienceStaffItem.colouredName(draft.strip()), left + 77, top + 77, 0xffffffff);
        graphics.disableScissor();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
