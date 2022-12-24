package net.elidhan.anim_guns.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.buffer.Unpooled;
import net.elidhan.anim_guns.AnimatedGuns;
import net.elidhan.anim_guns.item.BlueprintItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BlueprintScreen extends HandledScreen<BlueprintScreenHandler>
{
    private static final Identifier TEXTURE = new Identifier(AnimatedGuns.MOD_ID, "textures/gui/inventory/blueprint.png");
    private int currentBlueprintIndex;

    public BlueprintScreen(BlueprintScreenHandler handler, PlayerInventory inventory, Text title)
    {
        super(handler, inventory, title);
    }

    protected void setBlueprint(int index)
    {
        if(index == -1)
        {
            index = BlueprintItem.BLUEPRINT_ITEM_LIST.size()-1;
        }
        else if(index >= BlueprintItem.BLUEPRINT_ITEM_LIST.size())
        {
            index = 0;
        }

        currentBlueprintIndex = index;
    }

    @Override
    protected void init()
    {
        addDrawableChild(new ButtonWidget((this.width/2)-20, (this.height/2)-23, 10, 20, Text.literal("\u276E"), (button) -> setBlueprint(currentBlueprintIndex - 1)));
        addDrawableChild(new ButtonWidget((this.width/2)+10, (this.height/2)-23, 10, 20, Text.literal("\u276F"), (button) -> setBlueprint(currentBlueprintIndex + 1)));

        addDrawableChild(new ButtonWidget((this.width/2)-16, this.height/2, 32, 20 , Text.literal("Set"), (button) ->
        {
            if (client != null)
            {
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                buf.writeInt(getCurrentBlueprint());
                ClientPlayNetworking.send(new Identifier(AnimatedGuns.MOD_ID, "select_blueprint"), buf);

                this.close();
            }
        }));

        setBlueprint(0);
    }

    private int getCurrentBlueprint()
    {
        return this.currentBlueprintIndex;
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY)
    {

    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY)
    {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int i = (this.width/2)-88;
        int j = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.itemRenderer.renderInGui(new ItemStack(BlueprintItem.BLUEPRINT_ITEM_LIST.get(getCurrentBlueprint())), 80+(this.width-this.backgroundWidth)/2, 62+(this.height-this.backgroundHeight)/2);
        RenderSystem.enableDepthTest();
        drawMouseoverTooltip(matrices,mouseX,mouseY);
    }
}
